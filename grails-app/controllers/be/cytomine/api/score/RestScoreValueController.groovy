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
import be.cytomine.score.Score
import be.cytomine.score.ScoreValue
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for score value
 * A score may have some value
 */
@RestApi(name = "Score value services", description = "Methods for score values, a score may have some value.")
class RestScoreValueController extends RestController{

    def scoreValueService

    /**
     * List all score value
     */
    @RestApiMethod(description="Get all score value", listing = true)
    def list() {
        responseSuccess(scoreValueService.list())
    }

    /**
     * List all sofwtare value for a single score
     */
    @RestApiMethod(description="Get all score values for a score", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def listByScore() {
        Score score = Score.read(params.long('id'))
        if(score) {
            responseSuccess(scoreValueService.list(score))
        } else {
            responseNotFound("Score", params.id)
        }
    }

    /**
     * Get a score value info
     */
    @RestApiMethod(description="Get a score value info")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def show() {
        ScoreValue value = scoreValueService.read(params.long('id'))
        if (value) {
            responseSuccess(value)
        } else {
            responseNotFound("ScoreValue", params.id)
        }
    }

    /**
     * Add a new score value
     */
    @RestApiMethod(description="Add a new score value")
    def add() {
        add(scoreValueService, request.JSON)
    }

    /**
     * Update a score value
     */
    @RestApiMethod(description="Update a score value")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score value id")
    ])
    def update() {
        update(scoreValueService, request.JSON)
    }

    /**
     * Update a score value
     */
    @RestApiMethod(description="Reorder score values")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score id"),
            @RestApiParam(name="ids", type="list", paramType = RestApiParamType.QUERY, description = "The score ids (commat sep)")
    ])
    def reorder() {
        List<Long> ids = params.get('ids').split(",").collect{Long.parseLong(it)}
        Score score = Score.read(params.long('id'))
        if(score) {
            scoreValueService.reorder(score, ids);
            responseSuccess(scoreValueService.list(score))
        } else {
            responseNotFound("Score", params.id)
        }
    }

    /**
     * Delete a score value
     */
    @RestApiMethod(description="Delete a score value")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The score value id")
    ])
    def delete() {
        delete(scoreValueService, JSON.parse("{id : $params.id}"),null)
    }
}
