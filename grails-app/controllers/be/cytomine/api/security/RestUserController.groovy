package be.cytomine.api.security

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
import be.cytomine.Exception.ForbiddenException
import be.cytomine.api.RestController
import be.cytomine.image.ImageInstance
import be.cytomine.image.server.Storage
import be.cytomine.ontology.Ontology
import be.cytomine.project.Project
import be.cytomine.security.Group
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.social.PersistentProjectConnection
import be.cytomine.social.PersistentImageConsultation
import be.cytomine.social.AnnotationAction
import be.cytomine.utils.SecurityUtils
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.READ

/**
 * Handle HTTP Requests for CRUD operations on the User domain class.
 */
@RestApi(name = "Security | user services", description = "Methods for managing a user")
class RestUserController extends RestController {

    def springSecurityService
    def cytomineService
    def secUserService
    def projectService
    def ontologyService
    def imageInstanceService
    def groupService
    def securityACLService
    def mongo
    def noSQLCollectionService
    def reportService
    def projectConnectionService
    def imageConsultationService
    def projectRepresentativeUserService
    def userAnnotationService
    def storageService


    /**
     * Get all project managers
     */
    @RestApiMethod(description="Get all project managers", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def showAdminByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(secUserService.listAdmins(project))
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    @RestApiMethod(description="Get all project representatives", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def showRepresentativeByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(projectRepresentativeUserService.listUserByProject(project))
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    /**
     * Get project creator
     */
    @RestApiMethod(description="Get project creator (Only 1 even if response is list)", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def showCreatorByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess([secUserService.listCreator(project)])
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    /**
     * Get ontology creator
     */
    @RestApiMethod(description="Get ontology creator (Only 1 even if response is list)", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The ontology id")
    ])
    def showCreatorByOntology() {
        Ontology ontology = ontologyService.read(params.long('id'))
        if (ontology) {
            responseSuccess([ontology.user])
        }
        else responseNotFound("User", "Project", params.id)
    }

    /**
     * Get ontology user list
     */
    @RestApiMethod(description="Get all ontology users. Online flag may be set to get only online users", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The ontology id")
    ])
    def showUserByOntology() {
        Ontology ontology = ontologyService.read(params.long('id'))
        if (ontology) {
            responseSuccess(secUserService.listUsers(ontology))
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    /**
     * Get all user layers available for a project
     */
    @RestApiMethod(
            description="Get all user layers available for a project. If image param is set, add user job layers. The result depends on the current user and the project flag (hideUsersLayers,...).",
            listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="image", type="long", paramType = RestApiParamType.PATH, description = "(Optional) The image id, if set add userjob layers"),
    ])
    def showLayerByProject() {
        Project project = projectService.read(params.long('id'))
        ImageInstance image = imageInstanceService.read(params.long('image'))
        if (project) {
            responseSuccess(secUserService.listLayers(project,image))
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    @RestApiMethod(description="Get all storage users.", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id")
    ])
    def showUserByStorage() {
        Storage storage = storageService.read(params.long('id'))
        if (storage) {
            responseSuccess(secUserService.listUsers(storage))
        } else {
            responseNotFound("User", "Storage", params.id)
        }
    }

    def listByGroup() {
        Group group = groupService.read(params.long('id'))
        if (group) {
            responseSuccess(secUserService.listByGroup(group))
        } else {
            responseNotFound("User", "Project", params.id)
        }
    }

    /**
     * Render and returns all Users into the specified format given in the request
     * @return all Users into the specified format
     */
    @RestApiMethod(description="Render and returns all Users",listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="publicKey", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) If set, get only user with the public key in param"),
    ])
    def list() {
        def result

        if (params.publicKey != null) {
            responseSuccess(secUserService.getByPublicKey(params.publicKey))
        } else {
            def extended = [:]
            if(params.getBoolean("withRoles")) extended.put("withRoles",params.withRoles)
            result = secUserService.list(extended, searchParameters, params.sort, params.order, params.long("max",0), params.long("offset",0))
            responseSuccess([collection : result.data, size : result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages])
        }
    }

    /**
     * Render and return an User into the specified format given in the request
     * @param id the user identifier
     * @return user an User into the specified format
     */
    @RestApiMethod(description="Get a user")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long/string", paramType = RestApiParamType.PATH, description = "The user id or the user username")
    ])
    def show() {
        def id = params.long('id')
        SecUser user
        if(id) {
            user = secUserService.read(id)
        } else {
            user = secUserService.findByUsername(params.id)
        }

        if (user) {
            def  maps = JSON.parse(user.encodeAsJSON())
            def  authMaps = secUserService.getAuth(user)
            maps.admin = authMaps.get("admin")
            maps.user = authMaps.get("user")
            maps.guest = authMaps.get("guest")
           responseSuccess(maps)
//            responseSuccess(user)
        } else {
            responseNotFound("User", params.id)
        }
    }

    @RestApiMethod(description="Get the public and private key for a user. Request only available for Admin or if user is the current user")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "(Optional) The user id"),
        @RestApiParam(name="publicKey", type="string", paramType = RestApiParamType.PATH, description = "(Optional) The user key")
    ])
    @RestApiResponseObject(objectIdentifier = "[publicKey:x, privateKey:x]")
    def keys() {
        def publicKey = params.publicKey
        def id = params.long('id')
        SecUser user

        if(publicKey) {
            user = SecUser.findByPublicKey(publicKey)
        } else if(id) {
            user = secUserService.read(id)
        } else {
            user = secUserService.findByUsername(params.id)
        }
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        if (user) {
            responseSuccess([publicKey:user.publicKey,privateKey:user.privateKey])
        } else {
            responseNotFound("User", params.id)
        }
    }

    @RestApiMethod(description="Build a signature string based on params for the current user.")
    @RestApiParams(params=[
        @RestApiParam(name="method", type="string", paramType = RestApiParamType.QUERY, description = "The request method action"),
        @RestApiParam(name="content-MD5", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) The request MD5"),
        @RestApiParam(name="content-type", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) The request content type"),
        @RestApiParam(name="date", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) The request date"),
        @RestApiParam(name="queryString", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) The request query string"),
        @RestApiParam(name="forwardURI", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) The request forward URI")
        ])
    @RestApiResponseObject(objectIdentifier = "[signature:x, publicKey:x]")
    def signature() {
        SecUser user = cytomineService.currentUser

        String method = params.get('method')
        String content_md5 = (params.get("content-MD5") != null) ? params.get("content-MD5") : ""
        String content_type = (params.get("content-type") != null) ? params.get("content-type") : ""
        content_type = (params.get("Content-Type") != null) ? params.get("Content-Type") : content_type
        String date = (params.get("date") != null) ? params.get("date") : ""
        String queryString = (params.get("queryString") != null) ? "?" + params.get("queryString") : ""
        String path = params.get('forwardURI') //original URI Request

        log.info "user=$user"
        log.info "content_md5=$content_md5"
        log.info "content_type=$content_type"
        log.info "date=$date"
        log.info "queryString=$queryString"
        log.info "path=$path"
        log.info "method=$method"

        String signature = SecurityUtils.generateKeys(method,content_md5,content_type,date,queryString,path,user)

        responseSuccess([signature:signature, publicKey:user.getPublicKey()])
    }

    /**
     * Get current user info
     */
    @RestApiMethod(description="Get current user info")
    def showCurrent() {
        SecUser user = secUserService.readCurrentUser()
        def maps = JSON.parse(user.encodeAsJSON())
        if(!user.algo()){
            def authMaps = secUserService.getAuth(user)
            maps.admin = authMaps.get("admin")
            maps.user = authMaps.get("user")
            maps.guest = authMaps.get("guest")
            maps.adminByNow = authMaps.get("adminByNow")
            maps.userByNow = authMaps.get("userByNow")
            maps.guestByNow = authMaps.get("guestByNow")
            maps.isSwitched = SpringSecurityUtils.isSwitched()
            if(maps.isSwitched) {
                maps.realUser = SpringSecurityUtils.switchedUserOriginalUsername
            }

        }
        responseSuccess(maps)
    }



    /**
     * Add a new user
     */
    @RestApiMethod(description="Add a user, by default the sec role 'USER' is set")
    def add() {
        add(secUserService, request.JSON)
    }

    /**
     * Update a user
     */
    @RestApiMethod(description="Edit a user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def update() {
        update(secUserService, request.JSON)
    }

    /**
     * Delete a user
     */
    @RestApiMethod(description="Delete a user")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def delete() {
        delete(secUserService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="Lock an user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def lock() {
        SecUser user = SecUser.get(params.id)
        responseResult(secUserService.lock(user))
    }

    @RestApiMethod(description="Unlock an user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def unlock() {
        SecUser user = SecUser.get(params.id)
        responseResult(secUserService.unlock(user))
    }


    @RestApiMethod(description="Get all project users. Online flag may be set to get only online users", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="online", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Get only online users for this project"),
            @RestApiParam(name="showJob", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Also show the users job for this project"),
            @RestApiParam(name="withLastImage", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Show the last image seen by each user in this project"),
            @RestApiParam(name="withLastConsultation", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Show the last consultation of this project by each user"),
            @RestApiParam(name="withNumberConsultations", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Show the number of consultations of this project by each user"),
    ])
    def showByProject() {

        Project project = projectService.read(params.long('project'))
        securityACLService.check(project,READ)

        def extended = [:]
        if(params.withLastImage) extended.put("withLastImage",params.withLastImage)
        if(params.withLastConnection) extended.put("withLastConnection",params.withLastConnection)
        if(params.withNumberConnections) extended.put("withNumberConnections",params.withNumberConnections)
        if(params.withUserJob) extended.put("withUserJob",params.withUserJob)
        String sortColumn = params.sort ?: "created"
        String sortDirection = params.order ?: "desc"

        def results = secUserService.listUsersExtendedByProject(project, extended, searchParameters, sortColumn, sortDirection, params.long('max',0), params.long('offset',0))

        responseSuccess([collection : results.data, size:results.total, offset: results.offset, perPage: results.perPage, totalPages: results.totalPages])

        //boolean showUserJob = params.boolean('showJob')
    }

    @RestApiMethod(description="Add user in a project as simple 'user'")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="idUsers", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def addUserToProject() {
        Project project = Project.get(params.id)
        SecUser user = SecUser.get(params.idUser)
        log.info "addUserToProject project=${project} user=${user}"
        secUserService.addUserToProject(user, project, false)
        log.info "addUserToProject ok"
        response.status = 200
        def ret = [data: [message: "OK"], status: 200]
        response(ret)
    }

    @RestApiMethod(description="Add users in a project as simple 'user'")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="users", type="array", paramType = RestApiParamType.QUERY, description = "The users ids")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def addUsersToProject() {
        Project project = projectService.read(params.long('project'))
        securityACLService.check(project,ADMINISTRATION)

        def idUsers = params.users.toString().split(",")
        def users = []
        def wrongIds = []

        def errorMessage = ""
        def errors = []

        for(def id : idUsers){
            try{
                users << Long.parseLong(id)
            } catch(NumberFormatException e){
                wrongIds << id
            }
        }

        idUsers = users
        log.info "addUserToProject project=${project} users=${users}"
        users = User.findAllByIdInList(users)

        wrongIds.addAll(idUsers- (users.collect{it.id}))

        users.each { user ->
            def code = secUserService.addUserToProject(user, project, false).status
            if(code != 200 && code != 201) errors << user.id
        }

        if(!errors.isEmpty()) errorMessage += "Cannot add theses users to the project ${project.id} : "+errors.join(",")+". "
        if(!wrongIds.isEmpty()) errorMessage += wrongIds.join(",")+" are not well formatted ids"

        def result = [data: [message: "OK"], status: 200]
        response.status = 200

        if(!errors.isEmpty() || !wrongIds.isEmpty()) {
            result.data.message = errorMessage
            result.status = 206
            response.status = 206
        }

        response(result)
    }

    @RestApiMethod(description="Delete user from a project as simple 'user'")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def deleteUserFromProject() {
        Project project = Project.get(params.id)
        SecUser user = SecUser.get(params.idUser)
        secUserService.deleteUserFromProject(user, project, false)
        response.status = 200
        def ret = [data: [message: "OK"], status: 200]
        response(ret)
    }

    @RestApiMethod(description="Delete users from a project (also delete the manager role if the user was one)")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="users", type="array", paramType = RestApiParamType.QUERY, description = "The users ids")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def deleteUsersFromProject() {

        Project project = projectService.read(params.long('project'))
        securityACLService.check(project,ADMINISTRATION)

        def idUsers = params.users.toString().split(",")
        def users = []
        def wrongIds = []

        def errorMessage = ""
        def errors = []

        for(def id : idUsers){
            try{
                users << Long.parseLong(id)
            } catch(NumberFormatException e){
                wrongIds << id
            }
        }
        idUsers = users
        users = User.findAllByIdInList(users)

        users.each { user ->
            if(project.getPermissionInACL(project, user).contains(ADMINISTRATION.mask)) secUserService.deleteUserFromProject(user, project, true)
            def code = secUserService.deleteUserFromProject(user, project, false).status
            if(code != 200 && code != 201) {
                errors << user.id
            }
        }
        wrongIds.addAll(idUsers- (users.collect{it.id}))

        if(!errors.isEmpty()) errorMessage += "Cannot add theses users to the project ${project.id} : "+errors.join(",")+". "
        if(!wrongIds.isEmpty()) errorMessage += wrongIds.join(",")+" are not well formatted ids"

        def result = [data: [message: "OK"], status: 200]
        response.status = 200

        if(!errors.isEmpty() || !wrongIds.isEmpty()) {
            result.data.message = errorMessage
            result.status = 206
            response.status = 206
        }

        response(result)
    }

    /**
     * Add user in project manager list
     */
    @RestApiMethod(description="Add user in project manager list")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def addUserAdminToProject() {
        Project project = Project.get(params.id)
        User user = User.get(params.idUser)
        secUserService.addUserToProject(user, project, true)
        response.status = 200
        def ret = [data: [message: "OK"], status: 200]
        response(ret)

    }

    /**
     * Delete user from project manager list
     */
    @RestApiMethod(description="Delete user from project manager list")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def deleteUserAdminFromProject() {
        Project project = Project.get(params.id)
        SecUser user = SecUser.get(params.idUser)
        if (cytomineService.currentUser.id!=user.id) {
            securityACLService.check(project,ADMINISTRATION)
        }
        secUserService.deleteUserFromProject(user, project, true)
        response.status = 200
        def ret = [data: [message: "OK"], status: 200]
        response(ret)
    }

    @RestApiMethod(description="Add user in a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
            @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
            @RestApiParam(name="permission", type="string", paramType = RestApiParamType.QUERY, description = "Storage permission")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def addUserToStorage() {
        Storage storage = storageService.read(params.long('id'))
        SecUser user = secUserService.read(params.long('idUser'))

        secUserService.addUserToStorage(user, storage, params.get('permission', "READ"))
        response.status = 200
        response([data: [message: "OK"], status: 200])
    }

    @RestApiMethod(description="Add users in a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
            @RestApiParam(name="users", type="array", paramType = RestApiParamType.QUERY, description = "The users ids"),
            @RestApiParam(name="permission", type="string", paramType = RestApiParamType.QUERY, description = "ACL rights")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def addUsersToStorage() {
        Storage storage = storageService.read(params.long('id'))
        securityACLService.check(storage,ADMINISTRATION)

        def idUsers = params.users.toString().split(",")
        def users = []
        def wrongIds = []

        def errorMessage = ""
        def errors = []

        for(def id : idUsers){
            try{
                users << Long.parseLong(id)
            } catch(NumberFormatException e){
                wrongIds << id
            }
        }

        idUsers = users
        log.info "addUsersToStorage storage=${storage} users=${users}"
        users = User.findAllByIdInList(users)

        wrongIds.addAll(idUsers- (users.collect{it.id}))

        users.each { user ->
            def code = secUserService.addUserToStorage(user, storage, params.get('permission', "READ")).status
            if(code != 200 && code != 201) errors << user.id
        }

        if(!errors.isEmpty()) errorMessage += "Cannot add theses users to the storage ${storage.id} : "+errors.join(",")+". "
        if(!wrongIds.isEmpty()) errorMessage += wrongIds.join(",")+" are not well formatted ids"

        def result = [data: [message: "OK"], status: 200]
        response.status = 200

        if(!errors.isEmpty() || !wrongIds.isEmpty()) {
            result.data.message = errorMessage
            result.status = 206
            response.status = 206
        }

        response(result)
    }
    
    @RestApiMethod(description="Delete user from a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
            @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def deleteUserFromStorage() {
        Storage storage = storageService.read(params.long('id'))
        SecUser user = secUserService.read(params.long('idUser'))
        secUserService.deleteUserFromStorage(user, storage)
        response.status = 200
        response([data: [message: "OK"], status: 200])
    }


    @RestApiMethod(description="Delete users from a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
            @RestApiParam(name="users", type="array", paramType = RestApiParamType.QUERY, description = "The users ids")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def deleteUsersFromStorage() {

        Storage storage = storageService.read(params.long('id'))
        securityACLService.check(storage,ADMINISTRATION)

        def idUsers = params.users.toString().split(",")
        def users = []
        def wrongIds = []

        def errorMessage = ""
        def errors = []

        for(def id : idUsers){
            try{
                users << Long.parseLong(id)
            } catch(NumberFormatException e){
                wrongIds << id
            }
        }
        idUsers = users
        users = User.findAllByIdInList(users)

        users.each { user ->
            def code = secUserService.deleteUserFromStorage(user, storage).status
            if(code != 200 && code != 201) {
                errors << user.id
            }
        }
        wrongIds.addAll(idUsers- (users.collect{it.id}))

        if(!errors.isEmpty()) errorMessage += "Cannot add theses users to the storage ${storage.id} : "+errors.join(",")+". "
        if(!wrongIds.isEmpty()) errorMessage += wrongIds.join(",")+" are not well formatted ids"

        def result = [data: [message: "OK"], status: 200]
        response.status = 200

        if(!errors.isEmpty() || !wrongIds.isEmpty()) {
            result.data.message = errorMessage
            result.status = 206
            response.status = 206
        }

        response(result)
    }


    @RestApiMethod(description="Change user permission in storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
            @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
            @RestApiParam(name="permission", type="string", paramType = RestApiParamType.QUERY, description = "Storage permission")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def changeUserPermission() {
        Storage storage = storageService.read(params.long('id'))
        SecUser user = secUserService.read(params.long('idUser'))

        secUserService.changeUserPermission(user, storage, params.get('permission', "READ"))
        response.status = 200
        response([data: [message: "OK"], status: 200])
    }






    @RestApiMethod(description="Change a user password for a user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
            @RestApiParam(name="password", type="string", paramType = RestApiParamType.QUERY, description = "The new password")
    ])
    def resetPassword () {
        try {
            SecUser user = SecUser.get(params.long('id'))
            String newPassword = request.JSON.password == JSONObject.NULL ? null : request.JSON.password;

            log.info "change password for user $user with new password $newPassword"
            if(user && newPassword) {
                securityACLService.checkIsCreator(user,cytomineService.currentUser)
                user.newPassword = newPassword
                //force to reset password (newPassword is transient => beforeupdate is not called):
                user.password = "bad"
                secUserService.saveDomain(user)
                response(user)
            } else if(!user) {
                responseNotFound("SecUser",params.id)
            }else if(!newPassword) {
                responseNotFound("Password",newPassword)
            }
        }catch(CytomineException e) {
            responseError(e)
        }

    }

    @RestApiMethod(description="Check a user password for the current user")
    @RestApiParams(params=[
            @RestApiParam(name="password", type="string", paramType = RestApiParamType.QUERY, description = "The password")
    ])
    def checkPassword () {
        String password = request.JSON.password
        def result = springSecurityService.encodePassword(password).equals(cytomineService.currentUser.password)

        if(result) {
            responseSuccess([:])
        } else {
            response([success: false, errors: "No matching password"], 401)
        }
    }

    /**
     * Get all user friend (other user that share same project)
     */
    @RestApiMethod(description="Get all user friend (other user that share same project) for a specific user", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
        @RestApiParam(name="project", type="long", paramType = RestApiParamType.QUERY, description = "The project id"),
        @RestApiParam(name="offline", type="boolean", paramType = RestApiParamType.QUERY, description = "(Optional, default false) Get online and offline user")
    ])
    def listFriends() {
        SecUser user = secUserService.get(params.long('id'))
        Project project = null
        if (params.long('project')) {
            project = projectService.read(params.long('project'))
        }
        boolean includeOffline = params.boolean('offline')

        List<SecUser> users
        if (includeOffline) {
            if (project) {
                //get all user project list
                users = secUserService.listUsers(project)
            } else {
                //get all people that share common project with user
                users = secUserService.getAllFriendsUsers(user)
            }
        } else {
            if (project) {
                //get user project online
                users = secUserService.getAllFriendsUsersOnline(user, project)
            } else {
                //get friends online
                users = secUserService.getAllFriendsUsersOnline(user)
            }
        }
        responseSuccess(users)
    }

    /**
     * List people connected now to the same project and get their openned pictures
     */
    @RestApiMethod(description="List people connected now to the same project and get their openned pictures", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    @RestApiResponseObject(objectIdentifier = "List of [id: %idUser%,image: %idImage%, filename: %Image path%, originalFilename:%Image filename%, date: %Last position date%]")
    def listOnlineFriendsWithPosition() {
        Project project = projectService.read(params.long('id'))

        //Get all project user online
        def usersId = secUserService.getAllFriendsUsersOnline(cytomineService.currentUser, project).collect {it.id}

        //Get all user online and their pictures
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        DateTime thirtySecondsAgo = new DateTime().minusSeconds(30)
        def result = db.lastUserPosition.aggregate(
                [$match : [ project : project.id, created:[$gt:thirtySecondsAgo.toDate()]]],
                [$project:[user:1,image:1,slice:1,imageName:1,created:1]],
                [$group : [_id : [ user: '$user', slice: '$slice'], "date":[$max:'$created'], image: [$first:'$image'],imageName: [$first:'$imageName']]],
                [$group : [_id : [ user: '$_id.user'], "position":[$push: [id: '$_id.image',slice: '$_id.slice', image: '$image', filename: '$imageName', originalFilename: '$imageName', date: '$date']]]]
        )

        def usersWithPosition = []
        result.results().each {
            usersWithPosition << [id: it["_id"]["user"], position: it["position"]]
        }
        usersId.remove(usersWithPosition.collect{it.id})

        //user online with no image open
        usersId.each {
            usersWithPosition << [id: it, position: []]
        }
        responseSuccess(usersWithPosition)
//        responseSuccess([])
    }

    @RestApiMethod(description="List all the users of a project with their last activity (opened project & image)", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listUsersWithLastActivity() {

        securityACLService.checkAdmin(cytomineService.currentUser)

        //asc = 1; desc = -1
        int order = 1;
        boolean sorted = false;
        def field = null;
        String _search;

        if(params.datatables) {
            params.max = params["length"] ? params["length"] as int : 10;
            if(params.max < 0) params.max = null;
            params.offset = params["start"] ? params["start"] as int : 0;

            _search = params["search[value]"] ? ".*"+params["search[value]"].toLowerCase()+".*" : ".*"

            def col = params["order[0][column]"];
            def sortArg = params["order[0][dir]"];
            def sortProperty = "columns[$col][data]"

            field = params[sortProperty].toString().toLowerCase()

            if (sortArg.equals("desc")) {
                order = -1;
            }

        }

        if(field == null || field == "null") {
            field = "id"
        }

        Project project = projectService.read(params.long('id'))

        boolean online = params.boolean('onlineOnly');
        boolean adminsOnly = params.boolean('adminsOnly');

        def results = []

        List<SecUser> users;

        if (online) {
            users = secUserService.getAllFriendsUsersOnline(cytomineService.currentUser, project)
        } else {
            users = secUserService.listUsers(project)
        }

        if(_search) {
            users = users.findAll{
                it.lastname.toLowerCase().matches(_search) ||
                        it.firstname.toLowerCase().matches(_search) ||
                        it.username.toLowerCase().matches(_search)}
        }

        if (adminsOnly) {
            List<SecUser> admins;
            admins = secUserService.listAdmins(project)
            users = users.findAll{admins.contains(it)}
        }

        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer max = (params.max != null && params.getInt('max')!=0) ? params.getInt('max') : Integer.MAX_VALUE
        def maxForCollection = Math.min(users.size() - offset, max)
        long collectionSize = users.size()

        if(field && ["id","email","username"].contains(field)) {
            users.sort { a,b->
                if(field.equals("email")) {
                    (order)*(a.email <=>b.email)
                } else if(field.equals("username")) {
                    (order)*(a.username.toLowerCase() <=>b.username.toLowerCase() )
                } else if(field.equals("id")) {
                    (order)*(a.id <=>b.id )
                }
            }
            sorted = true;

            // avoid subList if unwanted ==> work only of we have already sorted on a user field.
            if(offset > 0 || users.size() > maxForCollection) {
                users = users.subList(offset,offset + maxForCollection)
            }
        }

        def connections = projectConnectionService.lastConnectionInProject(project)
        def frequencies = projectConnectionService.numberOfConnectionsByProjectAndUser(project)
        def images = imageConsultationService.lastImageOfUsersByProject(project)
        // can be done in the service ?
        connections.sort {it.user}
        frequencies.sort {it.user}
        images.sort {it.user}

        // we sorted to apply binary search instead of a simple "find" method. => performance
        def binSearchI = { aList, property, target ->
            def a = aList
            def offSet = 0
            while (!a.empty) {
                def n = a.size()
                def m = n.intdiv(2)
                if(a[m]."$property" > target) {
                    a = a[0..<m]
                } else if (a[m]."$property" < target) {
                    a = a[(m + 1)..<n]
                    offSet += m + 1
                } else {
                    return (offSet + m)
                }
            }
            return -1
        }

        for(SecUser user : users) {

            int index = binSearchI(connections, "user", user.id)
            def connection = index >= 0 ? connections[index]:null
            index = binSearchI(frequencies, "user", user.id)
            def frequency = index >= 0 ? frequencies[index]:null
            index = binSearchI(images, "user", user.id)
            def image = index >= 0 ? images[index]:null


            def userInfo = [id : user.id, username : user.username, firstname : user.firstname, lastname : user.lastname, email: user.email,
                        lastImageId : image?.image, lastImageName : image?.imageName,
                        lastConnection : connection?.created, frequency : frequency?.frequency?: 0]
            results << userInfo
        }

        // sort if not already done
        if(field && !sorted) {
            results.sort { a,b->
                if(field.equals("lastconnection")) {
                    (order)*(a.lastConnection <=>b.lastConnection)
                } else if(field.equals("frequency")) {
                    (order)*(a.frequency <=>b.frequency )
                } else {
                    a.id <=>b.id
                }
            }
            sorted = true;
        }

        // to fit the pagination system
        if(collectionSize > results.size()) {
            def filler = Arrays.asList(new Object[collectionSize-results.size()-offset]);
            results = Arrays.asList(new Object[offset]) + results + filler
        }

        responseSuccess(results)
    }

    @RestApiMethod(description="Download a report (pdf, xls,...) with user listing from a specific project")
    @RestApiResponseObject(objectIdentifier =  "file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
            @RestApiParam(name="terms", type="list", paramType = RestApiParamType.QUERY,description = "The annotation terms id (if empty: all terms)"),
            @RestApiParam(name="users", type="list", paramType = RestApiParamType.QUERY,description = "The annotation users id (if empty: all users)"),
            @RestApiParam(name="images", type="list", paramType = RestApiParamType.QUERY,description = "The annotation images id (if empty: all images)"),
            @RestApiParam(name="format", type="string", paramType = RestApiParamType.QUERY,description = "The report format (pdf, xls,...)")
    ])
    def downloadUserListingLightByProject() {
        reportService.createUserListingLightDocuments(params.long('id'),params.format,response)
    }

    @RestApiMethod(description="Return a resume of the activities of a user into a project")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def resumeUserActivity() {
        def result = [:]

        SecUser user = secUserService.get(params.long('user'))
        Project project = projectService.read(params.long('project'))
        try {
            securityACLService.checkIsSameUser(user, cytomineService.currentUser)
        } catch(ForbiddenException) {
            securityACLService.checkAdmin(cytomineService.currentUser)
        }

        result["firstConnection"] = PersistentProjectConnection.findAllByUserAndProject(user.id, project.id, [sort: 'created', order: 'asc', max: 1])[0]?.created
        result["lastConnection"] = PersistentProjectConnection.findAllByUserAndProject(user.id, project.id, [sort: 'created', order: 'desc', max: 1])[0]?.created
        result["totalAnnotations"] = userAnnotationService.count(user, project)
        result["totalConnections"] = PersistentProjectConnection.countByUserAndProject(user.id, project.id)
        result["totalConsultations"] = PersistentImageConsultation.countByUserAndProject(user.id, project.id)
        result["totalAnnotationSelections"] = AnnotationAction.countByUserAndProjectAndAction(user.id, project.id, "select")

        responseSuccess(result)
    }

    @Override
    protected boolean isFilterResponseEnabled() {
        try{
            securityACLService.checkAdmin(cytomineService.currentUser)
            return false
        } catch(ForbiddenException e){}
        return true
    }

    @Override
    protected void filterOneElement(JSONObject element){
        if(element['id'] != cytomineService.currentUser.id)
        element['email'] = null
    }

}
