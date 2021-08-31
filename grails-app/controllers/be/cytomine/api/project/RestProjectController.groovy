package be.cytomine.api.project

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

import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import be.cytomine.ontology.Ontology
import be.cytomine.processing.Software
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.Sql
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for project domain
 * A project has some images and a set of annotation
 * Users can access to project with Spring security Acl plugin
 */
@RestApi(name = "Project | project services", description = "Methods for managing projects")
class RestProjectController extends RestController {

    def springSecurityService
    def projectService
    def ontologyService
    def cytomineService
    def imageInstanceService
    def taskService
    def secUserService
    def dataSource
    def currentRoleServiceProxy
    def securityACLService
    def statsService

    /**
     * List all project available for the current user
     */
    @RestApiMethod(description="Get project listing, according to your access", listing=true)
    def list() {
        SecUser user = cytomineService.currentUser
        Boolean withMembersCount = params.boolean("withMembersCount")
        Boolean withLastActivity = params.boolean("withLastActivity")
        Boolean withDescription = params.boolean("withDescription")
        Boolean withCurrentUserRoles = params.boolean("withCurrentUserRoles")

        def result
        if(currentRoleServiceProxy.isAdminByNow(user)) {
            //if user is admin, we print all available project
            user = null
        } else {
            securityACLService.checkGuest(user)
        }
        def extended = [:]
        if(withMembersCount) extended.put("withMembersCount",withMembersCount)
        if(withLastActivity) extended.put("withLastActivity",withLastActivity)
        if (withDescription) extended.put("withDescription", withDescription)
        if(withCurrentUserRoles) extended.put("withCurrentUserRoles",withCurrentUserRoles)
        result = projectService.list(user, extended, searchParameters, params.sort, params.order, params.long('max'), params.long('offset'))
        responseSuccess([collection : result.data, size : result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages])
    }

    /**
     * Get a project
     */
    @RestApiMethod(description="Get a project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def show () {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(project)
        } else {
            responseNotFound("Project", params.id)
        }
    }

    /**
     * Add a new project to cytomine
     */
    @RestApiMethod(description="Add a new project")
    def add() {
        log.info "Add project = $request.JSON"
        try {
            Task task = taskService.read(params.getLong("task"))
            log.info "task ${task} is find for id = ${params.getLong("task")}"
            def result = projectService.add(request.JSON,task)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Update a project
     */
    @RestApiMethod(description="Update a project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="int", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def update () {
        try {
            Task task = taskService.read(params.getLong("task"))
            log.info "task ${task} is find for id = ${params.getLong("task")}"
            def domain = projectService.retrieve(request.JSON)
            def result = projectService.update(domain,request.JSON,task)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg, errorValues: e.values], e.code)
        }
    }


    /**
     * Delete a project
     */
    @RestApiMethod(description="Delete a project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id")
    ])
    def delete () {
        try {
            Task task = taskService.read(params.getLong("task"))
            log.info "task ${task} is find for id = ${params.getLong("task")}"
            def domain = projectService.retrieve(JSON.parse("{id : $params.id}"))
            log.info "project = ${domain}"
            def result = projectService.delete(domain,transactionService.start(),task)
            //delete container in retrieval
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Get last action done on a specific project
     * ex: "user x add a new annotation on image y",...
     */
    @RestApiMethod(description="Get the last action for a project", listing = true)
    @RestApiResponseObject(objectIdentifier="command history")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id")
    ])
    def lastAction() {
        Project project = projectService.read(params.long('id'))
        int max = Integer.parseInt(params.max);

        if (project) {
            responseSuccess(projectService.lastAction(project, max))
        } else {
            responseNotFound("Project", params.id)
        }
    }

    @RestApiMethod(description="Get the last opened projects for the current user", listing = true)
    def listLastOpened() {
        SecUser user = cytomineService.currentUser
        responseSuccess(projectService.listLastOpened(user, params.long('max')))
    }

    /**
     * List all project available for this user, that can use a software
     */
    @RestApiMethod(description="Get projects available for the current user that can use a specific software", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The software id")
    ])
    def listBySoftware() {
        Software software = Software.read(params.long('id'))
        if(software) {
            responseSuccess(projectService.listBySoftware(software))
        } else {
            responseNotFound("Software", params.id)
        }
    }

    /**
     * List all project available for this user, that use a ontology
     */
    @RestApiMethod(description="Get projects available for the current user that can use a specific ontology", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The ontology id")
    ])
    def listByOntology() {
        Ontology ontology = ontologyService.read(params.long('id'));
        if (ontology != null) {
            responseSuccess(projectService.listByOntology(ontology))
        } else {
            responseNotFound("Project", "Ontology", params.id)
        }
    }

    /**
     * List all project available for the current user, that can be used by a user
     */
    @RestApiMethod(description="Get projects available for the current user and available for a specific user", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The user id")
    ])
    def listByUser() {
        User user = User.read(params.long('id'))
        if(user) {
            def result = projectService.list(user,params.long('max',0),params.long('offset',0))
            responseSuccess([collection : result.data, size : result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages])
        } else {
            responseNotFound("User", params.id)
        }
    }

    /**
     * List all project available for the current user
     */
    @RestApiMethod(description="Get projects available for the current user and available for a specific user in a specific role (user, admin, creator). ", listing = true)
    @RestApiResponseObject(objectIdentifier="project (light)")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The user id"),
        @RestApiParam(name="creator", type="boolean", paramType = RestApiParamType.QUERY,description = "filter by creator"),
        @RestApiParam(name="admin", type="boolean", paramType = RestApiParamType.QUERY,description = "filter by admin"),
        @RestApiParam(name="user", type="boolean", paramType = RestApiParamType.QUERY,description = "filter by user")
    ])
    def listLightByUser() {
        User user = secUserService.read(params.long('id'))
        boolean creator = params.getBoolean('creator')
        boolean admins = params.getBoolean('admin')
        boolean users = params.getBoolean('user')
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        if(!user) {
            responseNotFound("User", params.id)
        } else if(creator) {
            responseSuccess(projectService.listByCreator(user))
        } else if(admins) {
            responseSuccess(projectService.listByAdmin(user))
        } else if(users) {
            responseSuccess(projectService.listByUser(user))
        }  else {
            responseSuccess(projectService.listByUser(user))
        }
    }

    /**
     * List all retrieval-project for a specific project
     * The suggested term can use data from other project (with same ontology).
     */
    @RestApiMethod(description="List all retrieval-project for a specific project. The suggested term can use data from other project (with same ontology).", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
    ])
    def listRetrieval() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            def projects = project.retrievalProjects.findAll{!it.deleted}
            responseSuccess(projects)
        } else {
            responseNotFound("Project", params.id)
        }
    }

    @RestApiMethod(description="Get the last action for a user in a project or in all projects available for the current user", listing = true)
    @RestApiResponseObject(objectIdentifier="commandHistory")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id (if null: all projects)"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.QUERY,description = "The user id"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY,description = "Will return actions created after this date. (Optional)"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Will return actions created before this date. (Optional)"),
            @RestApiParam(name="fullData", type="boolean", paramType = RestApiParamType.QUERY,description = "Flag to include the full JSON of the data field on each command history. Not recommended for long listing.")
    ])

    def listCommandHistory() {
        Project project = projectService.read(params.long('id'))
        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer max = (params.max != null && params.getInt('max')!=0) ? params.getInt('max') : Integer.MAX_VALUE
        SecUser user = secUserService.read(params.long('user'))
        Boolean fullData = params.getBoolean('fullData')
        Long startDate = params.long("startDate")
        Long endDate = params.long("endDate")
        List<Project> projects = project ? [project] : projectService.list(cytomineService.currentUser)
        response(findCommandHistory(projects, user, max, offset, fullData, startDate, endDate))
    }

    @RestApiMethod(description="Count the number of project visits")
    @RestApiResponseObject(objectIdentifier = "[total:x]")
    def countByUser() {
        responseSuccess([total:reviewedAnnotationService.count(cytomineService.currentUser)])
    }

    @RestApiMethod(description="Invite a not yer existing user to the project")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
            @RestApiParam(name="json", type="string", paramType = RestApiParamType.QUERY,description = "The user name and email of the invited user"),
    ])

    def inviteNewUser() {
        Project project = projectService.read(params.long('id'))

        try {
            def result = projectService.inviteUser(project, request.JSON);
            responseSuccess(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    def bounds() {
        def projects
        SecUser user = cytomineService.currentUser

        if(currentRoleServiceProxy.isAdminByNow(user)) {
            //if user is admin, we print all available project
            user = null
        } else {
            securityACLService.checkGuest(user)
        }

        def extended = [:]
        if(params.boolean('withMembersCount')) extended.put("withMembersCount",params.boolean('withMembersCount'))
        projects = projectService.list(user, extended).data

        def bounds = statsService.bounds(Project, projects)

        ["numberOfAnnotations", "numberOfJobAnnotations", "numberOfReviewedAnnotations", "numberOfImages"].each { field ->
            bounds.put(field, [min : projects.min{it[field]}?."${field}", max : projects.max{it[field]}?."${field}"])
        }
        if(!extended.isEmpty()) {
            bounds.put("members", [min : projects.min{it.membersCount}?.membersCount, max : projects.max{it.membersCount}?.membersCount])
        }

        responseSuccess(bounds)
    }


    private def findCommandHistory(List<Project> projects, SecUser user, Integer max, Integer offset,
                                   Boolean fullData, Long startDate, Long endDate) {
        String select = "SELECT ch.id as id, ch.created as created, ch.message as message, " +
                "ch.prefix_action as prefixAction, ch.user_id as user, ch.project_id as project "
        String from = "FROM command_history ch "
        String where = "WHERE true " +
                (projects? "AND ch.project_id IN (${projects.collect{it.id}.join(",")}) " : " ") +
                (user? "AND ch.user_id =  ${user.id} " : " ") +
                (startDate ? "AND ch.created > '${new Date(startDate)}' " : "") +
                (endDate ? "AND ch.created < '${new Date(endDate)}' " : "")
        String orderBy = "ORDER BY ch.created desc LIMIT $max OFFSET $offset"

        if(fullData) {
            select += ", c.data as data,c.service_name as serviceName, " +
                    "c.class as className, c.action_message as actionMessage, u.username as username "
            from += "LEFT JOIN command c ON ch.command_id = c.id " +
                    "LEFT JOIN sec_user u ON u.id = ch.user_id "
        }

        def result = doGenericRequest(select + from + where + orderBy, fullData)
        return result
    }

    private def doGenericRequest(String request,Boolean fullData) {
        def data = []
        Long start = System.currentTimeMillis()

        def sql = new Sql(dataSource)
         sql.eachRow(request) {
            if(data.isEmpty()) {
                start = System.currentTimeMillis()
            }
            def line = [id:it.id,created:it.created,message:it.message,prefix:it.prefixAction,prefixAction:it.prefixAction,user:it.user,project:it.project]
            if(fullData) {
                line.data = it.data
                line.serviceName = it.serviceName
                line.className = it.className
                line.action = it.actionMessage + " by " + it.username
            }
            data << line

        }
        sql.close()
        data
    }

}

