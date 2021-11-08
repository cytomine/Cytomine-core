package be.cytomine.api.score

import be.cytomine.api.RestController
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project

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

import be.cytomine.score.ConsensusScore
import be.cytomine.score.Score
import be.cytomine.score.ScoreProject
import be.cytomine.security.SecUser
import be.cytomine.security.User
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import java.text.SimpleDateFormat

/**
 * Controller for score project link
 * A score may be used by some project
 */
@RestApi(name = "Score image services", description = "Methods for managing score for an image")
class RestConsensusScoreController extends RestController{

    def consensusScoreService
    def projectService
    def cytomineService
    def imageInstanceService
    def scoreService
    def exportService

    
    @RestApiMethod(description="Get a consensus score")
    @RestApiParams(params=[
        @RestApiParam(name="imageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image id"),
        @RestApiParam(name="score", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def show() {
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        Score score = scoreService.read(params.getLong("score"))
        println "Look for score ${score?.id} image ${imageInstance?.id}"
        ConsensusScore consensusScore = consensusScoreService.read(imageInstance, score)
        if (consensusScore) responseSuccess(consensusScore)
        else responseNotFound("ConsensusScore", null)
    }

    /**
     * List scores values for an image
     */
    @RestApiMethod(description="List scores values for an image")
    @RestApiParams(params=[
            @RestApiParam(name="imageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image id"),
    ])
    def listByImageInstance() {
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        responseSuccess(consensusScoreService.listByImageInstance(imageInstance));
    }

    def consensusGroupByImageInstances() {
        String sortColumn = params.sort ? params.sort : "created"
        String sortDirection = params.order ? params.order : "desc"
        Project project = projectService.read(params.getLong("project"))
        def searchString = params['name[ilike]']
        def stats = consensusScoreService.statsGroupByImageInstances(project, sortColumn, sortDirection, searchString)
        responseSuccess(stats)
    }

    /**
     * Add an existing score to a project
     */
    @RestApiMethod(description="Add an existing score to a project")
    def add () {
        def json = JSON.parse("{imageInstance: ${params.getLong("imageInstance")}, scoreValue: ${params.getLong("value")}, user: ${cytomineService.currentUser.id}}")
        add(consensusScoreService, json)
    }

    /**
     * Delete the score for the project
     */
    @RestApiMethod(description="Remove the score from the project")
    def delete() {
        SecUser user = cytomineService.currentUser
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        Score score = scoreService.read(params.getLong("score"))
        ConsensusScore consensusScore = consensusScoreService.read(imageInstance, score)
        delete(consensusScoreService, JSON.parse("{id : $consensusScore.id}"),null)
    }
}
