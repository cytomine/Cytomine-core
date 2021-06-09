package be.cytomine.security

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.CytomineMethodNotYetImplementedException
import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.*
import be.cytomine.image.ImageInstance
import be.cytomine.image.NestedImageInstance
import be.cytomine.image.UploadedFile
import be.cytomine.image.server.ImageServerStorage
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.ontology.*
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.project.ProjectDefaultLayer
import be.cytomine.project.ProjectRepresentativeUser
import be.cytomine.social.LastConnection
import be.cytomine.utils.ModelService
import be.cytomine.utils.News
import be.cytomine.utils.SearchUtils
import be.cytomine.utils.Task
import be.cytomine.utils.Utils
import grails.converters.JSON
import grails.plugin.springsecurity.acl.AclSid
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.apache.commons.collections.ListUtils
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.util.ReflectionUtils

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.READ

class SecUserService extends ModelService {

    static transactional = true

    def springSecurityService
    def transactionService
    def cytomineService
    def commandService
    def modelService
    def userGroupService
    def dataSource
    def permissionService
    def algoAnnotationService
    def algoAnnotationTermService
    def annotationFilterService
    def annotationTermService
    def imageInstanceService
    def ontologyService
    def reviewedAnnotationService
    def secUserSecRoleService
    def userAnnotationService
    def currentRoleServiceProxy
    def securityACLService
    def projectDefaultLayerService
    def storageService
    def projectRepresentativeUserService
    def imageConsultationService
    def projectService
    def projectConnectionService

    def currentDomain() {
        User
    }

    def get(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        SecUser.get(id)
    }

    User getUser(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        SecUser user = SecUser.get(id)
        if(user instanceof UserJob) user = ((UserJob)user).user
        return ((User) user)
    }

    def findByUsername(def username) {
        if(!username) return null
        securityACLService.checkGuest(cytomineService.currentUser)
        SecUser.findByUsernameIlike(username)
    }

    def findByEmail(def email) {
        if(!email) return null
        securityACLService.checkGuest(cytomineService.currentUser)
        User.findByEmail(email)
    }

    SecUser getByPublicKey(String key) {
        //securityACLService.checkGuest(cytomineService.currentUser)
        SecUser.findByPublicKey(key)
    }

    def read(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        SecUser.read(id)
    }

    def getAuth(SecUser user) {
        def data = [:]
        data['admin'] = currentRoleServiceProxy.isAdmin(user)
        data['user'] = !data['admin'] && currentRoleServiceProxy.isUser(user)
        data['guest'] = !data['admin'] && !data['user'] && currentRoleServiceProxy.isGuest(user)

        data['adminByNow'] = currentRoleServiceProxy.isAdminByNow(user)
        data['userByNow'] = !data['adminByNow'] && currentRoleServiceProxy.isUserByNow(user)
        data['guestByNow'] = !data['adminByNow'] && !data['userByNow'] && currentRoleServiceProxy.isGuestByNow(user)
        return data
    }

    def readCurrentUser() {
        securityACLService.checkGuest(cytomineService.currentUser)
        cytomineService.getCurrentUser()
    }

    def list(def extended = [:], def searchParameters = [], Long max = 0, Long offset = 0) {
        list(extended, searchParameters, null, null, max, offset)
    }
    def list(def extended, def searchParameters = [], String sortColumn, String sortDirection, Long max = 0, Long offset = 0) {
        securityACLService.checkGuest(cytomineService.currentUser)

        if (sortColumn.equals("role") && !extended.withRoles) throw new WrongArgumentException("Cannot sort on user role without argument withRoles")

        if(!sortColumn) sortColumn = "username"
        if(!sortDirection) sortDirection = "asc"

        if(!ReflectionUtils.findField(User, sortColumn) && !(["role", "fullName"].contains(sortColumn)))
            throw new CytomineMethodNotYetImplementedException("User list sorted by $sortColumn is not implemented")

        def multiSearch = searchParameters.find{it.field == "fullName"}

        String select = "SELECT u.* "
        String from = "FROM sec_user u "
        String where ="WHERE job_id IS NULL "
        String search = ""
        String groupBy = ""
        String sort


        def mapParams = [:]
        if(multiSearch) {
            String value = ((String) multiSearch.values).toLowerCase()
            value = "%$value%"
            where += " and (u.firstname ILIKE :name OR u.lastname ILIKE :name OR u.email ILIKE :name OR u.username ILIKE :name) "
            mapParams.put("name", value)
        }
        if(extended.withRoles){
            select += ", MAX(x.order_number) as role "
            from +=", sec_user_sec_role sur, sec_role r " +
                    "JOIN (VALUES ('ROLE_GUEST', 1), ('ROLE_USER' ,2), ('ROLE_ADMIN', 3), ('ROLE_SUPER_ADMIN', 4)) as x(value, order_number) ON r.authority = x.value "
            where += "and u.id = sur.sec_user_id and sur.sec_role_id = r.id "
            groupBy = "GROUP BY u.id "
        }

        if(sortColumn == "role") {
            sort = "ORDER BY $sortColumn $sortDirection, u.id ASC "
        } else if(sortColumn == "fullName"){
            sort = "ORDER BY u.firstname $sortDirection "
        } else sort = "ORDER BY u.$sortColumn $sortDirection "

        String request = select + from + where + search + groupBy + sort
        if(max > 0) request += " LIMIT $max "
        if(offset > 0) request += " OFFSET $offset "


        def sql = new Sql(dataSource)
        def data = []

        if(mapParams.size() == 0) mapParams= []

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1; i<=((GroovyResultSet) it).getMetaData().getColumnCount(); i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() } )
                map.putAt(objectKey, it[key])
            }

            // I mock methods and fields to pass through getDataFromDomain of SecUser
            map["class"] = User.class
            map.getMetaClass().algo = { return false }
            map["language"] = User.Language.valueOf(map["language"])

            def line = User.getDataFromDomain(map)

            if(extended.withRoles){
                String role
                switch (map.role){
                    case 1:
                        role = 'ROLE_GUEST'
                        break;
                    case 2:
                        role = 'ROLE_USER'
                        break;
                    case 3:
                        role = 'ROLE_ADMIN'
                        break;
                    case 4:
                        role = 'ROLE_SUPER_ADMIN'
                        break;
                }
                line.putAt("role", role)
            }
            data << line

        }

        def size
        request = "SELECT COUNT(DISTINCT u.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        return [data:data, total:size]
    }

    def lock(SecUser user){
        securityACLService.checkAdmin(cytomineService.currentUser)
        if(!user.enabled) throw new InvalidRequestException("User already locked !")
        def json = user.encodeAsJSON()
        json = new JSONObject(json)
        json.enabled = false

        return executeCommand(new EditCommand(user: cytomineService.currentUser),user, json)

    }
    def unlock(SecUser user){
        securityACLService.checkAdmin(cytomineService.currentUser)
        if(user.enabled) throw new InvalidRequestException("User already unlocked !")
        def json = user.encodeAsJSON()
        json = new JSONObject(json)
        json.enabled = true

        return executeCommand(new EditCommand(user: cytomineService.currentUser),user, json)
    }

    def listWithRoles() {
        securityACLService.checkAdmin(cytomineService.currentUser)

        def data = User.executeQuery("select u,r from User u, SecUserSecRole sur, SecRole r where u = sur.secUser and sur.secRole = r.id order by LOWER(u.username)").groupBy {it[0].id}

        def getHigherAuth = {item ->
            def result;
            for(String role : ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST"]) {
                result = item.find{it[1].authority.equals(role)}
                if(result) return result
            }
        }

        def result = []
        def tmp, json;
        data.each { item ->
            tmp = getHigherAuth(item.value)
            json = (tmp[0] as JSON)
            json = JSON.parse(json.toString())
            json.putAt("role", tmp[1].authority)
            result << json
        }
        return result
    }

    def list(Project project, List ids) {
        securityACLService.check(project,READ)
        SecUser.findAllByIdInList(ids)
    }

    def listAll(Project project) {
        def data = []
        data.addAll(listUsers(project))
        //TODO: could be optim!!!
        data.addAll(UserJob.findAllByJobInList(Job.findAllByProject(project)))
        data
    }

    def listUsersExtendedByProject(Project project, def extended, def searchParameters = [], String sortColumn, String sortDirection, Long max = 0, Long offset = 0) {

        if(!ReflectionUtils.findField(User, sortColumn) && !(["projectRole", "fullName", "lastImageName","lastConnection","frequency"].contains(sortColumn)))
            throw new CytomineMethodNotYetImplementedException("User list sorted by $sortColumn is not implemented")

        if (sortColumn.equals("lastImageName") && !extended.withLastImage) throw new WrongArgumentException("Cannot sort on lastImageName without argument withLastImage")
        if (sortColumn.equals("lastConnection") && !extended.withLastConnection) throw new WrongArgumentException("Cannot sort on lastConnection without argument withLastConnection")
        if (sortColumn.equals("frequency") && !extended.withNumberConnections) throw new WrongArgumentException("Cannot sort on frequency without argument withNumberConnections")

        if (!extended || extended.isEmpty()) return listUsersByProject(project, true, searchParameters, sortColumn, sortDirection, max, offset)

        def results = []
        def users = []
        def images = []
        def connections = []
        def frequencies = []

        def binSearchI = SearchUtils.binarySearch()

        boolean usersFetched = false
        boolean consultationsFetched = false
        boolean connectionsFetched = false
        boolean frequenciessFetched = false

        def userIds
        long total

        if (ReflectionUtils.findField(User, sortColumn) || sortColumn.equals("projectRole")) {
            users = this.listUsersByProject(project, true, searchParameters, sortColumn, sortDirection, max, offset)
            total = users.total
            users = users.data
            results = users
            usersFetched = true
        } else {
            users = this.listUsersByProject(project, true, searchParameters, "id", "asc").data
            total = users.size()
        }
        userIds = users.collect { it.id }


        switch (sortColumn) {
            case "lastImageName":
                images = imageConsultationService.lastImageOfGivenUsersByProject(project, userIds, "name", sortDirection, max, offset)
                results = images.collect { i -> [id: i.user, lastImage: i.image] }
                userIds = results.collect { it.id }
                consultationsFetched = true
                break
            case "lastConnection":
                connections = projectConnectionService.lastConnectionOfGivenUsersInProject(project, userIds, "created", sortDirection, max, offset)
                results = connections.collect { c -> [id: c.user, lastConnection: c.created] }
                userIds = results.collect { it.id }
                connectionsFetched = true
                break
            case "frequency":
                frequencies = projectConnectionService.numberOfConnectionsOfGivenByProject(project, userIds, "frequency", sortDirection, max, offset)
                results = frequencies.collect { f -> [id: f.user, numberConnections: f.frequency] }
                userIds = results.collect { it.id }
                frequenciessFetched = true
                break
        }

        if(!usersFetched){
            for (def entry : results) {
                int index = binSearchI(users, "id", entry.id)
                def user = users[index]
                entry << user
            }
        }
        if (!consultationsFetched && extended.withLastImage) {
            images = imageConsultationService.lastImageOfUsersByProject(project, [[operator: "in", property: "user", value: userIds]], "id", "asc")
            for (def user : results) {
                int index = binSearchI(images, "user", user.id)
                def image = index >= 0 ? images[index] : null
                user.lastImage = image?.image
            }
            consultationsFetched = true
        }
        if (!connectionsFetched && extended.withLastConnection) {
            connections = projectConnectionService.lastConnectionInProject(project, [[operator: "in", property: "user", value: userIds]], "id", "asc")
            for (def user : results) {
                int index = binSearchI(connections, "user", user.id)
                def connection = index >= 0 ? connections[index] : null
                user.lastConnection = connection?.created
            }
            connectionsFetched = true
        }
        if (!frequenciessFetched && extended.withNumberConnections) {
            frequencies = projectConnectionService.numberOfConnectionsByProjectAndUser(project, [[operator: "in", property: "user", value: userIds]], "id", "asc")
            for (def user : results) {
                int index = binSearchI(frequencies, "user", user.id)
                def frequency = index >= 0 ? frequencies[index] : null
                user.numberConnections = frequency?.frequency
            }
            frequenciessFetched = true
        }

        return [data: results, total: total]
    }

    def listUsersByProject(Project project, boolean withProjectRole = true, def searchParameters = [], String sortColumn, String sortDirection, Long max = 0, Long offset = 0){

        def onlineUserSearch = searchParameters.find{it.field == "status" && it.values == "online"}
        def multiSearch = searchParameters.find{it.field == "fullName"}
        def projectRoleSearch = searchParameters.find{it.field == "projectRole"}

        String select = "select distinct secUser "
        String from  = "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, User as secUser "
        if(withProjectRole) {
            from = "from ProjectRepresentativeUser r right outer join r.user secUser WITH (r.project.id = ${project.id}), " +
                    "AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid "
        }
        String where = "where aclObjectId.objectId = "+project.id+" " +
                "and aclEntry.aclObjectIdentity = aclObjectId.id " +
                "and aclEntry.sid = aclSid.id " +
                "and aclSid.sid = secUser.username " +
                "and secUser.class = 'be.cytomine.security.User' "
        String groupBy = ""
        String order = ""
        String having = ""

        if(multiSearch) {
            String value = ((String) multiSearch.values).toLowerCase()
            where += " and (lower(secUser.firstname) like '%$value%' or lower(secUser.lastname) like '%$value%' or lower(secUser.email) like '%$value%') "
        }
        if(onlineUserSearch) {
            def onlineUsers = getAllOnlineUserIds(project)
            if(onlineUsers.isEmpty())
                return [data: [], total: 0]

            where += " and secUser.id in ("+getAllOnlineUserIds(project).join(",")+") "
        }

        if (withProjectRole && projectRoleSearch) {
            def roles = (projectRoleSearch?.values instanceof String) ? [projectRoleSearch?.values] : projectRoleSearch?.values
            having += " HAVING MAX(CASE WHEN r.id IS NOT NULL THEN 'representative' " +
                    "WHEN aclEntry.mask = 16 THEN 'manager' " +
                    "ELSE 'contributor' END) IN (" + roles.collect { it -> "'$it'"}?.join(',') + ")"
        }

        //for (def t : searchParameters) {
            // TODO parameters to HQL constraints
        //}

        if(withProjectRole){
            //works because 'contributor' < 'manager' < 'representative'
            select += ", MAX( CASE WHEN r.id IS NOT NULL THEN 'representative'\n" +
                    "     WHEN aclEntry.mask = 16 THEN 'manager'\n" +
                    "     ELSE 'contributor'\n" +
                    " END) as role "
            groupBy = "GROUP BY secUser.id "
        }
        if(sortColumn == "projectRole"){
            sortColumn = "role"
        } else if(sortColumn == "fullName"){
            sortColumn = "secUser.firstname"
        } else {
            sortColumn = "secUser.$sortColumn"
        }
        order = "order by $sortColumn $sortDirection "

        String request = select + from + where + groupBy + having + order

        def users = User.executeQuery(request, [offset:offset, max:max])

        long total;
        if(max == 0 && offset == 0) total = users.size()
        else {
            def list = User.executeQuery("select count(distinct secUser.id) " + from + where  )
            total = list[0]
        }

        users = users.collect{item ->
            if(!withProjectRole) return item
            item[0] = User.getDataFromDomain(item[0])
            item[0].role = item[1]
            return item[0]
        }

        return [data: users, total: total]
    }

    def listUsers(Project project, boolean showUserJob = false) {
        List<User> users = User.executeQuery("select distinct secUser " +
                "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, User as secUser "+
                "where aclObjectId.objectId = "+project.id+" " +
                "and aclEntry.aclObjectIdentity = aclObjectId.id " +
                "and aclEntry.sid = aclSid.id " +
                "and aclSid.sid = secUser.username " +
                "and secUser.class = 'be.cytomine.security.User'")
        if(showUserJob) {
            //TODO:: should be optim (see method head comment)
            List<Job> allJobs = Job.findAllByProject(project, [sort: 'created', order: 'desc'])

            allJobs.each { job ->
                def userJob = UserJob.findByJob(job);
                if (userJob) {
                    userJob.username = job.software.name + " " + job.created
                    users << userJob
                }
            }
        }
        return users
    }

    def listCreator(Project project) {
        securityACLService.check(project,READ)
        List<User> users = SecUser.executeQuery("select secUser from AclObjectIdentity as aclObjectId, AclSid as aclSid, SecUser as secUser where aclObjectId.objectId = "+project.id+" and aclObjectId.owner = aclSid.id and aclSid.sid = secUser.username and secUser.class = 'be.cytomine.security.User'")
        User user = users.isEmpty() ? null : users.first()
        return user
    }




    def listAdmins(Project project) {
        def users = SecUser.executeQuery("select distinct secUser from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, SecUser as secUser "+
                "where aclObjectId.objectId = "+project.id+" and aclEntry.aclObjectIdentity = aclObjectId.id and aclEntry.mask = 16 and aclEntry.sid = aclSid.id and aclSid.sid = secUser.username and secUser.class = 'be.cytomine.security.User'")
        return users
    }

    def listUsers(Ontology ontology) {
        securityACLService.check(ontology,READ)
        //TODO:: Not optim code a single SQL request will be very faster
        def users = []
        def projects = Project.findAllByOntology(ontology)
        projects.each { project ->
            users.addAll(listUsers(project))
        }
        users.unique()
    }

    def listByGroup(Group group) {
        securityACLService.checkAdmin(cytomineService.currentUser)
        UserGroup.findAllByGroup(group).collect{it.user}
    }

    /**
     * Get all allowed user id for a specific domain instance
     * E.g; get all user id for a project
     */
    List<Long> getAllowedUserIdList(CytomineDomain domain) {
        String request = "SELECT DISTINCT sec_user.id \n" +
                " FROM acl_object_identity, acl_entry,acl_sid, sec_user \n" +
                " WHERE acl_object_identity.object_id_identity = $domain.id\n" +
                " AND acl_entry.acl_object_identity=acl_object_identity.id\n" +
                " AND acl_entry.sid = acl_sid.id " +
                " AND acl_sid.sid = sec_user.username " +
                " AND sec_user.class = 'be.cytomine.security.User' "
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            data << it[0]
        }
        try {
            sql.close()
        }catch (Exception e) {}
        return data
    }


    private def getUserJobImage(ImageInstance image) {

        String request = "SELECT u.id as id, u.username as username, s.name as softwareName, s.software_version as softwareVersion, j.created as created \n" +
                "FROM annotation_index ai, sec_user u, job j, software s\n" +
                "WHERE ai.image_id = ${image.id}\n" +
                "AND ai.user_id = u.id\n" +
                "AND u.job_id = j.id\n" +
                "AND j.software_id = s.id\n" +
                "ORDER BY j.created"
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            def item = [:]
            item.id = it.id
            item.username = it.username
            item.softwareName = (it.softwareVersion?.trim()) ? "${it.softwareName} (${it.softwareVersion})" : it.softwareName

            item.created = it.created
            item.algo = true
            data << item
        }
        try {
            sql.close()
        }catch (Exception e) {}
        data
    }


    /**
     * List all layers from a project
     * Each user has its own layer
     * If project has private layer, just get current user layer
     */
    def listLayers(Project project, ImageInstance image = null) {
        securityACLService.check(project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        def users = listUsers(project)

        if(image) {
            def jobs = getUserJobImage(image)
            users.addAll(jobs)
        }
        def admins = listAdmins(project)


        if(project.checkPermission(ADMINISTRATION,currentRoleServiceProxy.isAdminByNow(currentUser))) {
            return users
        } else if(project.hideAdminsLayers && project.hideUsersLayers && users.contains(currentUser)) {
            return [currentUser]
        } else if(project.hideAdminsLayers && !project.hideUsersLayers && users.contains(currentUser)) {
            users.removeAll(admins)
            return users
        } else if(!project.hideAdminsLayers && project.hideUsersLayers && users.contains(currentUser)) {
            admins.add(currentUser)
            return admins
         }else if(!project.hideAdminsLayers && !project.hideUsersLayers && users.contains(currentUser)) {
            return users
         }else { //should no arrive but possible if user is admin and not in project
             []
         }

//
//        //if user is admin of the project, show all layers
//        if (!project.checkPermission(ADMINISTRATION) && project.privateLayer && users.contains(currentUser)) {
//            return [currentUser]
//        } else if (!project.privateLayer || project.checkPermission(ADMINISTRATION)) {
//            return  users
//        } else { //should no arrive but possible if user is admin and not in project
//            []
//        }
    }

    /**
     * Get all online user
     */
    List<SecUser> getAllOnlineUsers() {
        securityACLService.checkGuest(cytomineService.currentUser)
        //get date with -X secondes
        def xSecondAgo = Utils.getDateMinusSecond(300)
        def results = LastConnection.withCriteria {
            ge('created', xSecondAgo)
        }
        return User.getAll(results.collect{it.user.id}.unique())
    }

    /**
     * Get all online userIds for a project
     */
    List<Long> getAllOnlineUserIds(Project project) {
        securityACLService.check(project,READ)
        //if(!project) return getAllOnlineUsers()
        def xSecondAgo = Utils.getDateMinusSecond(300)
        def results = LastConnection.withCriteria {
            if(project) eq('project',project)
            ge('created', xSecondAgo)
            distinct('user')
        }
        return results.collect{it.user.id}
    }
    /**
     * Get all online user for a project
     */
    List<SecUser> getAllOnlineUsers(Project project) {
        securityACLService.check(project,READ)
        return User.getAll(getAllOnlineUserIds(project))
    }

    /**
     * Get all user that share at least a same project as user from argument
     */
    List<SecUser> getAllFriendsUsers(SecUser user) {
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        AclSid sid = AclSid.findBySid(user.username)
        List<SecUser> users = SecUser.executeQuery(
                "select distinct secUser from AclSid as aclSid, AclEntry as aclEntry, SecUser as secUser "+
                        "where aclEntry.aclObjectIdentity in (select  aclEntry.aclObjectIdentity from aclEntry where sid = ${sid.id}) and aclEntry.sid = aclSid.id and aclSid.sid = secUser.username and aclSid.id!=${sid.id}")

        return users
    }

    /**
     * Get all online user that share at least a same project as user from argument
     */
    List<SecUser> getAllFriendsUsersOnline(SecUser user) {
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        return ListUtils.intersection(getAllFriendsUsers(user),getAllOnlineUsers())
    }

    /**
     * Get all user that share at least a same project as user from argument and
     */
    List<SecUser> getAllFriendsUsersOnline(SecUser user, Project project) {
        securityACLService.check(project,READ)
        //no need to make insterect because getAllOnlineUsers(project) contains only friends users
        return getAllOnlineUsers(project)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(JSONObject json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        if(json.user == null) {
            json.user = cytomineService.getCurrentUser().id
            if(json.origin == null) json.origin = "ADMINISTRATOR"
        }
        def result = executeCommand(new AddCommand(user: currentUser),null,json)

        def user = User.findById(result?.data?.user?.id)

        //add all workplace params
        def params = json.workplaces;
        if (params) {
            params.each { param ->
                log.info "add workplace = " + param
                Workplace w = Workplace.findByName(param)
                if(!w) {
                    w = new Workplace(name : param)
                    w = w.save(flush: true, failOnError: true)
                }
                UserWorkplace.create(user, w, true)
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
    def update(SecUser user, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsCreator(user,currentUser)
        return executeCommand(new EditCommand(user: currentUser),user, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(SecUser domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        if(domain.algo()) {
            Job job = ((UserJob)domain).job
            securityACLService.check(job?.container(),READ)
            securityACLService.checkFullOrRestrictedForOwner(job, ((UserJob)domain).user)
        } else {
            securityACLService.checkAdmin(currentUser)
            securityACLService.checkIsNotSameUser(domain,currentUser)
        }
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    /**
     * Add a user in project user or admin list
     * @param user User to add in project
     * @param project Project that will be accessed by user
     * @param admin Flaf if user will become a simple user or a project manager
     * @return Response structure
     */
    def addUserToProject(SecUser user, Project project, boolean admin) {
        securityACLService.check(project,ADMINISTRATION)
        log.info "service.addUserToProject"
        if (project) {
            log.info "addUserToProject project=" + project + " username=" + user?.username + " ADMIN=" + admin
            synchronized (this.getClass()) {
                if(admin) {
                    permissionService.addPermission(project, user.username, ADMINISTRATION)
                }
                permissionService.addPermission(project, user.username, READ)
                if(project.ontology) {
                    log.info "addUserToProject ontology=" + project.ontology + " username=" + user?.username + " ADMIN=" + admin
                    permissionService.addPermission(project.ontology, user.username, READ)
                    if (admin) {
                        permissionService.addPermission(project.ontology, user.username, ADMINISTRATION)
                    }
                }
            }
        }
        [data: [message: "OK"], status: 201]
    }

    /**
     * Delete a user from a project user or admin list
     * @param user User to remove from project
     * @param project Project that will not longer be accessed by user
     * @param admin Flaf if user will become a simple user or a project manager
     * @return Response structure
     */
    def deleteUserFromProject(SecUser user, Project project, boolean admin) {
        if (cytomineService.currentUser.id!=user.id) {
            securityACLService.check(project,ADMINISTRATION)
        }
        if (project) {
            log.info "deleteUserFromProject project=" + project?.id + " username=" + user?.username + " ADMIN=" + admin
            if(project.ontology) {
                removeOntologyRightIfNecessary(project, user, admin)
            }
            if(admin) {
                permissionService.deletePermission(project, user.username, ADMINISTRATION)
            }
            else {
                permissionService.deletePermission(project, user.username, READ)
            }
            ProjectRepresentativeUser representative = ProjectRepresentativeUser.findByUserAndProject(user, project)
            if(representative) {
                projectRepresentativeUserService.delete(representative)
            }
        }
        [data: [message: "OK"], status: 201]
    }

    private void removeOntologyRightIfNecessary(Project project, User user, boolean admin = false) {
        //we remove the right ONLY if user has no other project with this ontology
        List<Project> projects = securityACLService.getProjectList(user,project.ontology)
        List<Project> otherProjects = projects.findAll{it.id!=project.id}

        if(otherProjects.isEmpty()) {
            //user has no other project with this ontology, remove the right!
            permissionService.deletePermission(project.ontology,user.username,READ)
            permissionService.deletePermission(project.ontology,user.username,ADMINISTRATION)
        } else if(admin) {
            def managedProjectList = projectService.listByAdmin(user).collect { it.id }
            if (!otherProjects.any { managedProjectList.contains(it.id) }) {
                permissionService.deletePermission(project.ontology, user.username, ADMINISTRATION)
            }
        }

    }

    def beforeDelete(def domain) {
        Command.findAllByUser(domain).each {
            UndoStackItem.findAllByCommand(it).each { it.delete()}
            RedoStackItem.findAllByCommand(it).each { it.delete()}
            CommandHistory.findAllByCommand(it).each {it.delete()}
            it.delete()
        }
    }

    def afterAdd(def domain, def response) {
        SecUserSecRole.create(domain,SecRole.findByAuthority("ROLE_USER"),true)
        if(domain instanceof User) {
            storageService.initUserStorage(domain)
        }
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.username]
    }

    /**
      * Retrieve domain thanks to a JSON object
      * WE MUST OVERRIDE THIS METHOD TO READ USER AND USERJOB (ALL SECUSER)
      * @param json JSON with new domain info
      * @return domain retrieve thanks to json
      */
    def retrieve(Map json) {
        SecUser user = SecUser.get(json.id)
        if (!user) throw new ObjectNotFoundException("User " + json.id + " not found")
        return user
    }



    def deleteDependentAlgoAnnotation(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof UserJob) {
            AlgoAnnotation.findAllByUser((UserJob)user).each {
                algoAnnotationService.delete(it,transaction,null,false)
            }
        }
    }

    def deleteDependentAlgoAnnotationTerm(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof UserJob) {
            AlgoAnnotationTerm.findAllByUserJob((UserJob)user).each {
                algoAnnotationTermService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentAnnotationFilter(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            AnnotationFilter.findAllByUser(user).each {
                annotationFilterService.delete(it,transaction, null,false)
            }
        }
    }

    def deleteDependentAnnotationTerm(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            AnnotationTerm.findAllByUser(user).each {
                annotationTermService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentImageInstance(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            ImageInstance.findAllByUser(user).each {
                imageInstanceService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentOntology(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            Ontology.findAllByUser(user).each {
                ontologyService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentForgotPasswordToken(SecUser secUser, Transaction transaction, Task task = null) {
          if (secUser instanceof User) {
              User user = (User) secUser
              ForgotPasswordToken.findAllByUser(user).each {
                  it.delete()
              }
          }

    }

    def deleteDependentReviewedAnnotation(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            ReviewedAnnotation.findAllByUser(user).each {
                reviewedAnnotationService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentSecUserSecRole(SecUser user, Transaction transaction, Task task = null) {
        SecUserSecRole.findAllBySecUser(user).each {
            secUserSecRoleService.delete(it,transaction,null, false)
        }
    }

    def deleteDependentAbstractImage(SecUser user, Transaction transaction, Task task = null) {
        //:to do implemented this ? allow this or not ?
    }

    def deleteDependentUserAnnotation(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            UserAnnotation.findAllByUser(user).each {
                userAnnotationService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentUserGroup(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            UserGroup.findAllByUser((User)user).each {
                userGroupService.delete(it,transaction,null, false)
            }
        }
    }

    def deleteDependentUserJob(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            UserJob.findAllByUser((User)user).each {
                delete(it,transaction,null,false)
            }
        }
    }

    def deleteDependentUploadedFile(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            UploadedFile.findAllByUser((User)user).each {
                it.delete()
            }
        }
    }

    def deleteDependentNews(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            News.findAllByUser((User)user).each {
                it.delete()
            }
        }
    }

    def deleteDependentHasManyAnnotationFilter(SecUser user, Transaction transaction, Task task = null) {
        def criteria = AnnotationFilter.createCriteria()
        def results = criteria.list {
            users {
                inList("id", user.id)
            }
        }
        results.each {
            it.removeFromUsers(user)
            it.save()
        }
    }

    def deleteDependentStorage(SecUser user,Transaction transaction, Task task = null) {
        for (storage in Storage.findAllByUser(user)) {
            if (StorageAbstractImage.countByStorage(storage) > 0) {
                throw new ConstraintException("Storage contains data, cannot delete user. Remove or assign storage to an another user first")
            } else {
                ImageServerStorage.findAllByStorage(storage).each {
                    it.delete()
                }
                storage.delete()
            }
        }
    }

    def deleteDependentSharedAnnotation(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            //TODO:: implement cascade deleteting/update for shared annotation
            if(SharedAnnotation.findAllBySender(user)) {
                throw new ConstraintException("This user has send/receive annotation comments. We cannot delete it! ")
            }
        }
    }

    def deleteDependentHasManySharedAnnotation(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            //TODO:: implement cascade deleteting/update for shared annotation
            def criteria = SharedAnnotation.createCriteria()
            def results = criteria.list {
                receivers {
                    inList("id", user.id)
                }
            }

            if(!results.isEmpty()) {
                throw new ConstraintException("This user has send/receive annotation comments. We cannot delete it! ")
            }
        }
    }

    def deleteDependentAnnotationIndex(SecUser user,Transaction transaction, Task task = null) {
        AnnotationIndex.findAllByUser(user).each {
            it.delete()
         }
    }

    def deleteDependentNestedImageInstance(SecUser user, Transaction transaction,Task task=null) {
        NestedImageInstance.findAllByUser(user).each {
            it.delete(flush: true)
        }
    }

    def deleteDependentProjectDefaultLayer(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            ProjectDefaultLayer.findAllByUser(user).each {
                projectDefaultLayerService.delete(it,transaction, null,false)
            }
        }
    }

    def deleteDependentProjectRepresentativeUser(SecUser user, Transaction transaction, Task task = null) {
        if(user instanceof User) {
            ProjectRepresentativeUser.findAllByUser(user).each {
                projectRepresentativeUserService.delete(it,transaction, null,false)
            }
        }
    }

    def deleteDependentMessageBrokerServer(SecUser user, Transaction transaction, Task task = null) {

    }
}
