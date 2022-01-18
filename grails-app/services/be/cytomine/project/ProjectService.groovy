package be.cytomine.project

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.*
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.Ontology
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Software
import be.cytomine.security.ForgotPasswordToken
import be.cytomine.security.SecRole
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import be.cytomine.utils.Utils
import grails.converters.JSON
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.security.acls.domain.BasePermission

import static org.springframework.security.acls.domain.BasePermission.*

class ProjectService extends ModelService {

    static transactional = true
    def cytomineService
    def modelService
    def springSecurityService
    def aclUtilService
    def dataSource
    def jobService
    def transactionService
    def algoAnnotationService
    def annotationTermService
    def algoAnnotationTermService
    def imageInstanceService
    def reviewedAnnotationService
    def userAnnotationService
    def propertyService
    def secUserService
    def permissionService
    def securityACLService
    def mongo
    def noSQLCollectionService
    def secUserSecRoleService
    def secRoleService
    def notificationService
    def projectRepresentativeUserService
    def ontologyService

    def currentDomain() {
        Project
    }

    def read(def id) {
        def project = Project.read(id)
        if(project) {
            securityACLService.check(project,READ)
            checkDeleted(project)
        }
        project
    }

    def readMany(def ids) {
        def projects = Project.findAllByIdInList(ids)
        if(projects) {
            projects.each { project ->
                securityACLService.check(project,READ)
                checkDeleted(project)
            }
        }
        projects
    }

    /**
     * List last project opened by user
     * If the user has less than "max" project opened, add last created project to complete list
     */
    def listLastOpened(User user, Long offset = null, Long max = null) {
        //get id of last open image
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        def data = []

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

        def result = db.persistentProjectConnection.aggregate(
                [$match : [ user : user.id ]],
                [$group : [_id : '$project', "date":[$max:'$created']]],
                [$sort : [ date : -1]],
                [$limit: (max==null? 5 : max)]
        )

        result.results().each {
            try {
                data << [id:it['_id'],date:it['date'], opened:true]
            } catch(CytomineException e) {
                //if user has data but has no access to picture,  ImageInstance.read will throw a forbiddenException
            }
        }
        if(data.size()<max) {
            //user has open less than max project, so we add last created project

            String request2 = """
                SELECT id, created as date
                FROM project
                WHERE deleted IS NULL AND ${data.isEmpty()? "true": "id NOT IN (${data.collect{it.id}.join(',')})"}
                ORDER BY date desc
            """

            def sql = new Sql(dataSource)
            sql.eachRow(request2,[]) {
                data << [id:it.id, date:it.date, opened: false]
            }
            sql.close()
        }
        data = data.sort{-it.date.getTime()}
        return data
    }

    def list(SecUser user = null) {
        if (user) {
            return Project.executeQuery(
                    "select distinct project " +
                            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, Project as project " +
                            "where aclObjectId.objectId = project.id " +
                            "and aclEntry.aclObjectIdentity = aclObjectId.id " +
                            "and aclEntry.sid = aclSid.id and aclSid.sid like '" + user.humanUsername() + "' and project.deleted is null")
        } else {
            Project.findAllByDeletedIsNull()
        }
    }

    def list(SecUser user = null, Long max , Long offset) {
        return list(user, [:], [], null, null, max , offset)
    }
    def list(SecUser user = null, LinkedHashMap extended, List searchParameters = [], String sortColumn = null, String sortDirection = null, Long max  = 0, Long offset = 0) {

        for (def parameter : searchParameters){
            if(parameter.field.equals("numberOfImages")) parameter.field = "countImages"
            if(parameter.field.equals("numberOfAnnotations")) parameter.field = "countAnnotations"
            if(parameter.field.equals("numberOfJobAnnotations")) parameter.field = "countJobAnnotations"
            if(parameter.field.equals("numberOfReviewedAnnotations")) parameter.field = "countReviewedAnnotations"
            if(parameter.field.equals("ontology")) parameter.field = "ontology_id"
        }

        if (sortColumn == "lastActivity" && !extended.withLastActivity) throw new WrongArgumentException("Cannot sort on lastActivity without argument withLastActivity")
        if (sortColumn == "membersCount" && !extended.withMembersCount) throw new WrongArgumentException("Cannot sort on membersCount without argument withMembersCount")


        def validParameters = getDomainAssociatedSearchParameters(Project, searchParameters).each{it.property = "p."+it.property}
        loop:for (def parameter : searchParameters){
            String property
            switch(parameter.field) {
                case "ontology_id" :
                    property = "ontology.id"
                    parameter.values= convertSearchParameter(Long.class, parameter.values)
                    break
                case "membersCount" :
                    property = "members.member_count"
                    parameter.values= convertSearchParameter(Long.class, parameter.values)
                    break
                case "tag" :
                    property = "t.tag_id"
                    parameter.values= convertSearchParameter(Long.class, parameter.values)
                    break
                default:
                    continue loop
            }

            validParameters << [operator: parameter.operator, property: property, value: parameter.values]
        }

        def sqlSearchConditions = searchParametersToSQLConstraints(validParameters)

        sqlSearchConditions = [
                project : sqlSearchConditions.data.findAll{it.property.startsWith("p.")}.collect{it.sql}.join(" AND "),
                ontology : sqlSearchConditions.data.findAll{it.property.startsWith("ontology.")}.collect{it.sql}.join(" AND "),
                members : sqlSearchConditions.data.findAll{it.property.startsWith("members.")}.collect{it.sql}.join(" AND "),
                tags : sqlSearchConditions.data.findAll{it.property.startsWith("t.")}.collect{it.sql}.join(" AND "),
                parameters: sqlSearchConditions.sqlParameters
        ]

        if (sqlSearchConditions.members && !extended.withMembersCount) throw new WrongArgumentException("Cannot search on members attributes without argument withMembersCount")

        String select, from, where, search, sort
        String request
        //faster method
        if (user) {
            select = "SELECT DISTINCT p.* "
            from = "FROM project p " +
                    "JOIN acl_object_identity as aclObjectId ON aclObjectId.object_id_identity = p.id " +
                    "JOIN acl_entry as aclEntry ON aclEntry.acl_object_identity = aclObjectId.id " +
                    "JOIN acl_sid as aclSid ON aclEntry.sid = aclSid.id "
            where = "WHERE aclSid.sid like '"+user.username+"' and p.deleted is null "
        }
        else {
            select = "SELECT DISTINCT(p.id), p.* "
            from = "FROM project p "
            where = "WHERE p.deleted is null "
        }
        select += ", ontology.name as ontology_name, ontology.id as ontology "
        from += "LEFT OUTER JOIN ontology ON p.ontology_id = ontology.id "
        select += ", discipline.name as discipline_name, discipline.id as discipline "
        from += "LEFT OUTER JOIN discipline ON p.discipline_id = discipline.id "

        search = "";sort = ""
        if(sqlSearchConditions.project){
            search +=" AND "
            search += sqlSearchConditions.project
        }

        if(sqlSearchConditions.ontology){
            search +=" AND "
            search += sqlSearchConditions.ontology
        }

        if(sqlSearchConditions.tags){
            from += "LEFT OUTER JOIN tag_domain_association t ON p.id = t.domain_ident AND t.domain_class_name = 'be.cytomine.project.Project' "
            search +=" AND "
            search += sqlSearchConditions.tags
        }


        if(extended.withLastActivity) {
            select += ", activities.max_date "
            from += "LEFT OUTER JOIN " +
                    "( SELECT  project_id, MAX(created) max_date " +
                    "  FROM command_history " +
                    "  GROUP BY project_id " +
                    ") activities ON p.id = activities.project_id "
        }
        if(extended.withMembersCount) {
            select += ", members.member_count "
            from += "LEFT OUTER JOIN " +
                    " ( SELECT aclObjectId.object_id_identity as project_id, COUNT(DISTINCT secUser.id) as member_count " +
                    "   FROM acl_object_identity as aclObjectId, acl_entry as aclEntry, acl_sid as aclSid, sec_user as secUser " +
                    "   WHERE aclEntry.acl_object_identity = aclObjectId.id and aclEntry.sid = aclSid.id and aclSid.sid = secUser.username and secUser.class = 'be.cytomine.security.User' " +
                    "   GROUP BY aclObjectId.object_id_identity " +
                    ") members ON p.id = members.project_id "

            if(sqlSearchConditions.members){
                search +=" AND "
                search += sqlSearchConditions.members
            }
        }
        if (extended.withDescription) {
            select += ", d.data as description "
            from += "LEFT OUTER JOIN description d ON d.domain_ident = p.id "
        }
        if(extended.withCurrentUserRoles) {
            SecUser currentUser = cytomineService.currentUser // cannot use user param because it is set to null if user connected as admin
            select += ", (admin_project.id IS NOT NULL) AS is_admin, (repr.id IS NOT NULL) AS is_representative "
            from += "LEFT OUTER JOIN admin_project " +
                    "ON admin_project.id = p.id AND admin_project.user_id = $currentUser.id " +
                    "LEFT OUTER JOIN project_representative_user repr " +
                    "ON repr.project_id = p.id AND repr.user_id = $currentUser.id "

            def searchedRole = searchParameters.find {it.field == "currentUserRole"}
            if(searchedRole) {
                if(searchedRole.values.contains("manager") && searchedRole.values.contains("contributor")){} // nothing because null or not null
                else if(searchedRole.values.contains("manager")) search += " AND admin_project.id IS NOT NULL  "
                else if(searchedRole.values.contains("contributor")) search += " AND admin_project.id IS NULL  "
            }

        }

        switch(sortColumn) {
            case "currentUserRole" : 
                if(extended.withCurrentUserRoles) sortColumn="is_representative "+((sortDirection.equals("desc")) ? " DESC " : " ASC ")+", is_admin"
            break
            case "membersCount" :
                if(extended.withMembersCount) sortColumn="members.member_count"
                break
            case "lastActivity" :
                if(extended.withLastActivity) sortColumn="activities.max_date"
                break
            case "name":
            case "numberOfImages":
            case "numberOfAnnotations":
            case "numberOfJobAnnotations":
            case "numberOfReviewedAnnotations":
                String regex = "([a-z])([A-Z]+)"
                String replacement = "\$1_\$2"
                sortColumn ="p."+sortColumn.replaceAll("numberOf", "count").replaceAll(regex, replacement).toLowerCase()
                break
        }

        if(sortColumn) {
            sort = " ORDER BY "+sortColumn
            sort += (sortDirection.equals("desc")) ? " DESC " : " ASC "
            sort += (sortDirection.equals("desc")) ? " NULLS LAST " : " NULLS FIRST "
        }


        request = select + from + where + search + sort
        if(max > 0) request += " LIMIT $max"
        if(offset > 0) request += " OFFSET $offset"


        def sql = new Sql(dataSource)
        def data = []
        def mapParams = sqlSearchConditions.parameters

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1;i<=((GroovyResultSet) it).getMetaData().getColumnCount();i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() } )
                map.putAt(objectKey, it[key])
            }

            // I mock methods and fields to pass through getDataFromDomain of Project
            map["class"] = Project.class
            map.getMetaClass().countSamples = { return 0 }
            map.getMetaClass().countImageInstance = { return map.countImages }
            map.getMetaClass().countAnnotations = { return map.countAnnotations }
            map.getMetaClass().countJobAnnotations = { return map.countJobAnnotations }
            map['ontology'] = [id : map['ontology'], name : map['ontologyName']]
            map['discipline'] = [id : map['discipline'], name : map['disciplineName']]

            def line = Project.getDataFromDomain(map)

            if(extended.withLastActivity) {
                line.putAt("lastActivity", map.maxDate)
            }
            if(extended.withMembersCount) {
                if(!map.memberCount) map.memberCount = 0
                line.putAt("membersCount", map.memberCount)
            }
            if (extended.withDescription) {
                line.putAt("description", map.description ?: "")
            }
            if(extended.withCurrentUserRoles) {
                line.putAt("currentUserRoles", [admin: map.isAdmin, representative: map.isRepresentative])
            }
            data << line

        }

        def size
        request = "SELECT COUNT(DISTINCT p.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        def result = [data:data, total:size]
        max = (max > 0) ? max : Integer.MAX_VALUE
        result.offset = offset
        result.perPage = Math.min(max, result.total)
        result.totalPages = Math.ceil(result.total / max)

        return result
    }

    def listByOntology(Ontology ontology) {
        //very slow method because it check right access for each project ontology
        securityACLService.getProjectList(cytomineService.currentUser,ontology)
    }

    def listBySoftware(Software software) {
        securityACLService.getProjectList(cytomineService.currentUser,software)
    }

    def lastAction(Project project, def max) {
        securityACLService.check(project, READ)
        return CommandHistory.findAllByProject(project, [sort: "created", order: "desc", max: max])
    }


    def listByCreator(User user) {
        def data = []
        def sql = new Sql(dataSource)
         sql.eachRow("select * from creator_project where user_id = ?",[user.id]) {
            data << [id:it.id, name:it.name]
        }
        sql.close()
        return data
    }

    def listByAdmin(User user) {
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow("select * from admin_project where user_id = ?",[user.id]) {
            data << [id:it.id, name:it.name]
        }
        sql.close()
        return data
    }

    def listByUser(User user) {
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow("select * from user_project where user_id = ?",[user.id]) {
            data << [id:it.id, name:it.name]
        }
        sql.close()
        return data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json,Task task = null) {
        taskService.updateTask(task,5,"Start creating project ${json.name}")
        SecUser currentUser = cytomineService.getCurrentUser()

        securityACLService.checkUser(currentUser)

        if(json.ontology && !json.ontology instanceof JSONObject.Null) {
            securityACLService.check(json.ontology,Ontology, READ)
        }

        taskService.updateTask(task,10,"Check retrieval consistency")
        checkRetrievalConsistency(json)
        def result = executeCommand(new AddCommand(user: currentUser),null,json)
        def project = Project.read(result?.data?.project?.id)
        taskService.updateTask(task,20,"Project $project created")
        log.info "project=" + project + " json.users=" + json.users + " json.admins=" + json.admins
        //Add annotation-term if term
        int progress = 20

        if (project) {
            def users = JSONUtils.getJSONList(json.users);
            def admins = JSONUtils.getJSONList(json.admins);
            log.info "users=${users}"
            if (users) {
                users.addAll(admins)
                users = users.unique()
                users.each { idUser ->
                    SecUser user = SecUser.read(Long.parseLong(idUser+""))
                    log.info "addUserToProject project=${project} user=${user}"
                    secUserService.addUserToProject(user, project, false)
                    progress = progress + (40/users.size())
                    taskService.updateTask(task,Math.min(100,progress),"User ${user.username} added as User")
                }
            }
            log.info "admins=${admins}"
            if (admins) {
                admins.each { idUser ->
                    SecUser user = SecUser.read(Long.parseLong(idUser+""))
                    if(user.id!=cytomineService.currentUser.id) {
                        //current user is already added to admin group
                        log.info "addUserToProject project=${project} user=${user}"
                        secUserService.addUserToProject(user, project, true)
                        progress = progress + (40/admins.size())
                        taskService.updateTask(task,Math.min(100,progress),"User ${user.username} added as Admin")
                    }

                }
            }
        }
        return result

    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Project project, def jsonNewData,task = null) {
        taskService.updateTask(task,5,"Start editing project ${project.name}")
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(project.container(),WRITE)
        Ontology ontology = project.ontology

        if(ontology?.id != jsonNewData.ontology){
            boolean deleteTerms = jsonNewData.forceOntologyUpdate
            long associatedTermsCount
            long userAssociatedTermsCount = 0L
            long algoAssociatedTermsCount = 0L
            long reviewedAssociatedTermsCount = 0L
            if(!deleteTerms) userAssociatedTermsCount += annotationTermService.list(project).size()
            algoAssociatedTermsCount += algoAnnotationTermService.list(project).size()
            reviewedAssociatedTermsCount += ReviewedAnnotation.countByProjectAndTermsIsNotNull(project)
            associatedTermsCount = userAssociatedTermsCount + algoAssociatedTermsCount + reviewedAssociatedTermsCount

            if(associatedTermsCount > 0){
                String message = "This project has $associatedTermsCount associated terms: "
                if(!deleteTerms) message += "$userAssociatedTermsCount from project members, "
                message += "$algoAssociatedTermsCount from jobs and "
                message += "$reviewedAssociatedTermsCount reviewed. "
                message += "The ontology cannot be updated."
                throw new ForbiddenException(message, [
                        userAssociatedTermsCount: userAssociatedTermsCount,
                        algoAssociatedTermsCount: algoAssociatedTermsCount,
                        reviewedAssociatedTermsCount: reviewedAssociatedTermsCount
                ])
            }
            if(deleteTerms) {
                for(def at : annotationTermService.list(project)){
                    annotationTermService.delete(at)
                }
            }
        }

        def result = executeCommand(new EditCommand(user: currentUser),project, jsonNewData)

        project = Project.read(result?.data?.project?.id)
        taskService.updateTask(task,20,"Project ${project.name} edited")


        def users = jsonNewData.users
        if(users == JSONObject.NULL) users = null;

        def admins = jsonNewData.admins
        if(admins == JSONObject.NULL) admins = null;

        def representatives = jsonNewData.representatives
        if(representatives == JSONObject.NULL) representatives = null;

        if(users) {
            def projectOldUsers = secUserService.listUsers(project).collect{it.id}.sort() //[a,b,c]
            def projectNewUsers = JSONUtils.getJSONList(jsonNewData.users).collect{Long.parseLong(it+"")}.sort() //[a,b,x]
            def nextAdmins;
            if(admins) {
                nextAdmins = JSONUtils.getJSONList(jsonNewData.admins).collect{Long.parseLong(it+"")}
            } else {
                nextAdmins = secUserService.listAdmins(project).collect{it.id}.sort() //[a,b,c]
            }
            projectNewUsers.addAll(nextAdmins)  //add admin as user too

            projectNewUsers.add(currentUser.id)
            projectNewUsers = projectNewUsers.unique()
            log.info "projectOldUsers=$projectOldUsers"
            log.info "projectNewUsers=$projectNewUsers"
            changeProjectUser(project,projectNewUsers,projectOldUsers,false,task,20)
        }

        if(admins) {
            def projectOldAdmins = secUserService.listAdmins(project).collect{it.id}.sort() //[a,b,c]
            def projectNewAdmins = JSONUtils.getJSONList(jsonNewData.admins).collect{Long.parseLong(it+"")}.sort() //[a,b,x]
            projectNewAdmins.add(currentUser.id)
            projectNewAdmins = projectNewAdmins.unique()
            log.info "projectOldAdmins=$projectOldAdmins"
            log.info "projectNewAdmins=$projectNewAdmins"
            changeProjectUser(project,projectNewAdmins,projectOldAdmins,true,task,60)
        }

        // here, an empty array is a valid argument
        if(representatives != null) {
            def projectOldReprs = projectRepresentativeUserService.listUserByProject(project).collect{it.id}.sort() //[a,b,c]
            def projectNewReprs = JSONUtils.getJSONList(jsonNewData.representatives).collect{Long.parseLong(it+"")}.sort() //[a,b,x]
            projectNewReprs = projectNewReprs.unique()
            log.info "projectOldReprs=$projectOldReprs"
            log.info "projectNewReprs=$projectNewReprs"
            def projectAddReprs = projectNewReprs - projectOldReprs
            def projectDeleteReprs = projectOldReprs - projectNewReprs

            log.info "projectAddUser=$projectAddReprs"
            log.info "projectDeleteUser=$projectDeleteReprs"
            projectAddReprs.each { idUser ->
                SecUser user = SecUser.read(Long.parseLong(idUser+""))
                log.info "projectAddReprs project=${project} user=${user}"
                def json = JSON.parse(new ProjectRepresentativeUser(project:project, user:user).encodeAsJSON());
                projectRepresentativeUserService.add(json);
            }

            projectDeleteReprs.each { idUser ->
                SecUser user = SecUser.read(Long.parseLong(idUser+""))
                ProjectRepresentativeUser repr = ProjectRepresentativeUser.findByUser(user)
                log.info "projectDeleteReprs project=${project} user=${repr}"
                projectRepresentativeUserService.delete(repr)
            }
        }
        if(ontology?.id != jsonNewData.ontology){
            if(ontology) ontologyService.determineRightsForUsers(ontology, secUserService.listUsers(project))
            if(project.ontology) ontologyService.determineRightsForUsers(project.ontology, secUserService.listUsers(project))
        }
        return result
    }

    private changeProjectUser(Project project, def projectNewUsers, def projectOldUsers, boolean admin, Task task, int progressStart) {
        int progress = progressStart
        def projectAddUser = projectNewUsers - projectOldUsers
        def projectDeleteUser = projectOldUsers - projectNewUsers

        log.info "projectAddUser=$projectAddUser"
        log.info "projectDeleteUser=$projectDeleteUser"
        projectAddUser.each { idUser ->
            SecUser user = SecUser.read(Long.parseLong(idUser+""))
            log.info "projectAddUser project=${project} user=${user}"
            secUserService.addUserToProject(user, project, admin)
            progress = progress + (40/projectAddUser.size())
            taskService.updateTask(task,Math.min(100,progress),"User ${user.username} added as ${admin? "Admin" : "User"}")
        }

        projectDeleteUser.each { idUser ->
            SecUser user = SecUser.read(Long.parseLong(idUser+""))
            log.info "projectDeleteUser project=${project} user=${user}"
            secUserService.deleteUserFromProject(user, project, admin)
            progress = progress + (40/projectDeleteUser.size())
            taskService.updateTask(task,Math.min(100,progress),"User ${user.username} added as ${admin? "Admin" : "User"}")
        }
    }

    def getActiveProjects(){

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def xSecondAgo = Utils.getDateMinusSecond(120)

        def result;
        def match = [$match : [ created : [$gte : xSecondAgo]]];
        def group = [$group : [_id : '$project']]

        result = db.persistentProjectConnection.aggregate(
                match,
                group
        )
        return result.results().collect{it["_id"]}
    }

    def getActiveProjectsWithNumberOfUsers() {
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

        def xSecondAgo = Utils.getDateMinusSecond(120)
        def match = [$match : [ created : [$gte : xSecondAgo]]];

        def group1 = [$group : [_id : [project : '$project', user : '$user']]]
        def group2 = [$group : [_id : '$_id.project', "users" :[$sum:1]]]
        def result;

        result = db.lastConnection.aggregate(
                match,
                group1,
                group2
        )

        def tmp = [];
        result.results().each{
            tmp << [project : it["_id"], users : it["users"]]
        }
        def projects = Project.findAllByIdInList(tmp.collect{it.project});

        result = [];
        projects.each{ project ->
            result << [project : project, users : tmp.find{it.project == project.id}.users]
        }

        return result
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Project domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
//        SecUser currentUser = cytomineService.getCurrentUser()
//        securityACLService.check(domain,DELETE)
//        Command c = new DeleteCommand(user: currentUser,transaction:transaction,linkProject: false,refuseUndo:true)
//        return executeCommand(c,domain,null)

        //We don't delete domain, we juste change a flag
        securityACLService.check(domain.container(),ADMINISTRATION)
        securityACLService.checkisNotReadOnly(domain.container())
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    /**
     * Invite an user (not yet existing) in project user
     * @param sender User who send the invitation
     * @param project Project that will be accessed by user
     * @param json the name and the mail of the User to add in project
     * @return Response structure
     */
    def inviteUser(Project project, def json) {

        def guestUser = [username : json.name, firstname : json.firstname?:'firstname',
                         lastname : json.lastname?:'lastname', email : json.mail,
                         password : 'passwordExpired', color : "#FF0000"]

        secUserService.add(JSON.parse(JSONUtils.toJSONString(guestUser)))
        User user = (User) secUserService.findByUsername(guestUser.username)
        SecRole secRole = secRoleService.findByAuthority("ROLE_GUEST")
        secUserSecRoleService.add(JSON.parse(JSONUtils.toJSONString([ user : user.id, role : secRole.id])))
        if (project) {
            secUserService.addUserToProject(user, project, false)
        }

        if (user) {
            user.passwordExpired = true
            user.save()
            ForgotPasswordToken forgotPasswordToken = new ForgotPasswordToken(
                    user : user,
                    tokenKey: UUID.randomUUID().toString(),
                    expiryDate: new Date() + 1
            ).save()
            def sender = cytomineService.currentUser
            notificationService.notifyWelcome(sender, user, forgotPasswordToken)
        } else {
            throw new ObjectNotFoundException("User with username "+guestUser.username+" not found")
        }
        return user

    }



    def afterAdd(Project domain, def response) {
        log.info("Add permission on " + domain + " to " + springSecurityService.authentication.name)
        if(!domain.hasACLPermission(READ)) {
            log.info("force to put it in list")
            permissionService.addPermission(domain, cytomineService.currentUser.username, BasePermission.READ)
        }
        if(!domain.hasACLPermission(ADMINISTRATION)) {
            log.info("force to put it in list")
            permissionService.addPermission(domain, cytomineService.currentUser.username, BasePermission.ADMINISTRATION)
        }
    }




    protected def beforeUpdate(Project domain) {
        domain.countAnnotations = UserAnnotation.countByProjectAndDeletedIsNull(domain)
        domain.countImages = ImageInstance.countByProjectAndDeletedIsNull(domain)
        domain.countJobAnnotations = AlgoAnnotation.countByProjectAndDeletedIsNull(domain)
        domain.countReviewedAnnotations = ReviewedAnnotation.countByProjectAndDeletedIsNull(domain)
    }

    protected def beforeDelete(Project domain) {
        CommandHistory.findAllByProject(domain).each { it.delete() }
        Command.findAllByProject(domain).each {
            it
            UndoStackItem.findAllByCommand(it).each { it.delete()}
            RedoStackItem.findAllByCommand(it).each { it.delete()}
            it.delete()
        }
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    /**
     * Check if retrieval parameter from a project json are ok
     * E.g. if retrieval is disable and project retrieval is not empty, there is a mistake
     * @param json Project json
     */
    private void checkRetrievalConsistency(def json) {

        boolean retrievalDisable =  false
        if(!json.retrievalDisable.toString().equals("null")) {
            retrievalDisable = Boolean.parseBoolean(json.retrievalDisable.toString())
        }

        boolean retrievalAllOntology =  true
        if(!json.retrievalAllOntology.toString().equals("null")) {
            retrievalAllOntology= Boolean.parseBoolean(json.retrievalAllOntology.toString())
        }

        boolean retrievalProjectEmpty = true
        if(!json.retrievalProjects.toString().equals("null")) {
            retrievalProjectEmpty = json.retrievalProjects.isEmpty()
        }

        if(retrievalDisable && retrievalAllOntology) {
            throw new WrongArgumentException("Retrieval cannot be disable of all Projects are selected")
        } else if(retrievalDisable && !retrievalProjectEmpty) {
            throw new WrongArgumentException("Retrieval cannot be disable of some Projects are selected")
        } else if(retrievalAllOntology && !retrievalProjectEmpty) {
            throw new WrongArgumentException("Retrieval cannot be set for all projects if some projects are selected")
        }
    }

//    def deleteDependentImageFilterProject(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${ImageFilterProject.countByProject(project)} links to image filter":"")
//
//        ImageFilterProject.findAllByProject(project).each {
//            imageFilterProjectService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentJob(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${Job.countByProject(project)} jobs":"")
//
//        Job.findAllByProject(project).each {
//            jobService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentSoftwareProject(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${SoftwareProject.countByProject(project)} links to software":"")
//
//        SoftwareProject.findAllByProject(project).each {
//            softwareProjectService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentAlgoAnnotation(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${AlgoAnnotation.countByProject(project)} annotations from algo":"")
//
//        AlgoAnnotation.findAllByProject(project).each {
//            algoAnnotationService.delete(it,transaction, null,false)
//        }
//    }
//
//    def deleteDependentAlgoAnnotationTerm(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${AlgoAnnotationTerm.countByProject(project)} terms for annotation from algo":"")
//
//        AlgoAnnotationTerm.findAllByProject(project).each {
//            algoAnnotationTermService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentAnnotationFilter(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${AnnotationFilter.countByProject(project)} annotations filters":"")
//
//        AnnotationFilter.findAllByProject(project).each {
//            annotationFilterService.delete(it,transaction,null, false)
//        }
//    }

    def deleteDependentImageInstance(Project project, Transaction transaction,Task task=null) {
        taskService.updateTask(task,task? "Delete ${ImageInstance.countByProject(project)} images":"")

        ImageInstance.findAllByProject(project).each {
            imageInstanceService.delete(it,transaction,null, false)
        }
    }

//    def deleteDependentReviewedAnnotation(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${ReviewedAnnotation.countByProject(project)} validate annotation":"")
//
//        ReviewedAnnotation.findAllByProject(project).each {
//            reviewedAnnotationService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentUserAnnotation(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${UserAnnotation.countByProject(project)} annotations created by user":"")
//
//        UserAnnotation.findAllByProject(project).each {
//            userAnnotationService.delete(it,transaction,null, false)
//        }
//    }
//
//    def deleteDependentHasManyProject(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Remove project from other project retrieval list":"")
//
//        //remove Retrieval-project where this project is set
//       def criteria = Project.createCriteria()
//        List<Project> projectsThatUseThisProjectForRetrieval = criteria.list {
//          retrievalProjects {
//              eq('id', project.id)
//          }
//        }
//
//        projectsThatUseThisProjectForRetrieval.each {
//            it.refresh()
//            it.removeFromRetrievalProjects(project)
//            it.save(flush: true)
//        }
//
//
//        project.retrievalProjects?.clear()
//    }
//
////
////    def deleteDependentTask(Project project, Transaction transaction,Task task=null) {
////
////        taskService.updateTask(task,task? "Delete ${Task.countByProject(project)} user position information":"")
////
////        Task.findAllByProjectIdent(project).each {
////            //Task from param will loose project link too...
////            it.project = null
////        }
////    }
//
//    def deleteDependentLastConnection(Project project, Transaction transaction,Task task=null) {
//
//        taskService.updateTask(task,task? "Delete ${LastConnection.countByProject(project)} connection information":"")
//
//        LastConnection.findAllByProject(project).each {
//              it.delete()
//        }
//    }
//
//    def deleteDependentImageGroup(Project project, Transaction transaction, Task task = null) {
//        ImageGroup.findAllByProject(project).each {
//            imageSequenceService.delete(it,transaction,null,false)
//        }
//    }
//
//    def deleteDependentNestedImageInstance(Project project, Transaction transaction,Task task=null) {
//        NestedImageInstance.findAllByProject(project).each {
//            it.delete(flush: true)
//        }
//    }
//    def deleteDependentProjectDefaultLayer(Project project, Transaction transaction, Task task = null) {
//        taskService.updateTask(task,task? "Delete ${ProjectDefaultLayer.countByProject(project)} connection information":"")
//        ProjectDefaultLayer.findAllByProject(project).each {
//            projectDefaultLayerService.delete(it,transaction, null,false)
//        }
//    }
}
