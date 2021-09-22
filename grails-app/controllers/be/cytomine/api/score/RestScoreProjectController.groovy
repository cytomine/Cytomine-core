package be.cytomine.api.score

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, score
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.api.RestController
import be.cytomine.score.ScoreProject
import be.cytomine.project.Project
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for score project link
 * A score may be used by some project
 */
@RestApi(name = "Score project services", description = "Methods for managing score inside a project")
class RestScoreProjectController extends RestController{

    def scoreProjectService
    def projectService

    /**
     * List all score project links
     */
    @RestApiMethod(description="List all score project links", listing = true)
    def list() {
        responseSuccess(scoreProjectService.list())
    }

    /**
     * List all score by project
     */
    @RestApiMethod(description="List all score project links by project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(scoreProjectService.list(project))
        } else {
            responseNotFound("Project", params.id)
        }
    }

    /**
     * Get a score project link
     */
    @RestApiMethod(description="Get a score project link")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score project id")
    ])
    def show() {
        ScoreProject parameter = scoreProjectService.read(params.long('id'))
        if (parameter) responseSuccess(parameter)
        else responseNotFound("ScoreProject", params.id)
    }

    /**
     * Add an existing score to a project
     */
    @RestApiMethod(description="Add an existing score to a project")
    def add () {
        add(scoreProjectService, request.JSON)
    }

    /**
     * Delete the score for the project
     */
    @RestApiMethod(description="Remove the score from the project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score project id")
    ])
    def delete() {
        delete(scoreProjectService, JSON.parse("{id : $params.id}"),null)
    }
}
