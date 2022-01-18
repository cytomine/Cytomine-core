package be.cytomine.api.project

import be.cytomine.Exception.CytomineException

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

import be.cytomine.api.RestController
import be.cytomine.project.Project
import be.cytomine.project.ProjectRepresentativeUser
import be.cytomine.security.User
import be.cytomine.utils.Task
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for project default layer
 */
@RestApi(name = "Project | representative user services", description = "Controller for project representative user")
class RestProjectRepresentativeUserController extends RestController {

    def projectRepresentativeUserService
    def taskService
    def projectService
    def secUserService

    @RestApiMethod(description="List all representative user of a project", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH, description = "The id of project")
    ])
    def listByProject() {
        Project project = Project.read(params.idProject)
        responseSuccess(projectRepresentativeUserService.listByProject(project))
    }

    @RestApiMethod(description="Get a project_representative_user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project_representative_user id")
    ])
    def show () {
        ProjectRepresentativeUser ref = projectRepresentativeUserService.read(params.long('id'))
        if (ref) {
            responseSuccess(ref)
        } else {
            responseNotFound("ProjectRepresentativeUser", params.id)
        }
    }

    @RestApiMethod(description="Add a project_representative_user")
    def add () {
        add(projectRepresentativeUserService, request.JSON)
    }

    @RestApiMethod(description="Delete a project_representative_user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project_representative_user id"),
            @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH,description = "The project id relative to the representative"),
            @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.QUERY,description = "An user id"),
            @RestApiParam(name="task", type="long", paramType = RestApiParamType.QUERY,description = "(Optional, default:null) The id of the task to update during process"),
    ])
    def delete () {
        try {
            Task task = taskService.read(params.getLong("task"))
            def domain
            if(params.id) domain = projectRepresentativeUserService.retrieve(JSON.parse("{id : $params.id}"))
            if(!domain) {
                Project project = projectService.read(params.getLong("idProject"))
                User user = secUserService.read(params.getLong("user"))

                domain = projectRepresentativeUserService.getByProjectAndUser(project, user)
            }
            def result = projectRepresentativeUserService.delete(domain,transactionService.start(),task)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }
}
