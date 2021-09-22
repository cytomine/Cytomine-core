package be.cytomine.api.score

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.RestController

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

import be.cytomine.middleware.AmqpQueue
import be.cytomine.processing.Job
import be.cytomine.project.Project
import grails.converters.JSON
import groovy.json.JsonBuilder
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest
import be.cytomine.score.Score

/**
 * Controller for score: application that can be launch (job)
 */
@RestApi(name = "Processing | score services", description = "Methods for managing score, application that can be launch (job)")
class RestScoreController extends RestController {

    def scoreService
    def securityACLService
    def amqpQueueService

    /**
     * List all score available in cytomine
     */
    @RestApiMethod(description="Get all score available in cytomine", listing = true)
    def list() {
        String sort = params.sort ?: 'id'
        if (!['id', 'name'].contains(sort)) sort = 'id'
        String order = params.order ?: 'desc'
        if (!['asc', 'desc'].contains(order)) order = 'desc'
        responseSuccess(scoreService.list(sort, order))
    }

    /**
     * List all score by project
     */
    @RestApiMethod(description="Get all score available in a project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = Project.read(params.long('id'))
        if(project) responseSuccess(scoreService.list(project))
        else responseNotFound("Project", params.id)
    }

    /**
     * Get a specific score
     */
    @RestApiMethod(description="Get a specific score")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def show() {
        Score score = scoreService.read(params.long('id'))
        if (score) {
            responseSuccess(score)
        } else {
            responseNotFound("Score", params.id)
        }
    }

    /**
     * Add a new score to cytomine
     * We must add in other request: parameters, score-project link,...
     */
    @RestApiMethod(description="Add a new score to cytomine. We must add in other request: score parameters, score project link,...")
    def add() {
        add(scoreService, request.JSON)
    }

    /**
     * Update a score info
     */
    @RestApiMethod(description="Update a score.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def update() {
        update(scoreService, request.JSON)
    }

    /**
     * Delete score
     */
    @RestApiMethod(description="Delete a score.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def delete() {
        delete(scoreService, JSON.parse("{id : $params.id}"),null)
    }
}
