package be.cytomine.api.processing

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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
import be.cytomine.processing.SoftwareUserRepository
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "Software user repositories services", description = "Methods for managing software user repositories")
class RestSoftwareUserRepositoryController extends RestController {

    def softwareUserRepositoryService

    @RestApiMethod(description = "Get all the software user repositories.", listing = true)
    def list() {
        responseSuccess(softwareUserRepositoryService.list())
    }

    @RestApiMethod(description = "Add a new software user repository.")
    def add() {
        add(softwareUserRepositoryService, request.JSON)
    }

    @RestApiMethod(description = "Get a specific software user repository.")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def show() {
        SoftwareUserRepository softwareUserRepository = softwareUserRepositoryService.read(params.long('id'))
        if (softwareUserRepository) {
            responseSuccess(softwareUserRepository)
        } else {
            responseNotFound("SoftwareUserRepository", params.id)
        }
    }

    @RestApiMethod(description = "Update a software user repository.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software user repository id")
    ])
    def update() {
        update(softwareUserRepositoryService, request.JSON)
    }

    @RestApiMethod(description = "Delete a software user repository.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software user repository id")
    ])
    def delete() {
        delete(softwareUserRepositoryService, JSON.parse("{id : $params.id}"), null)
    }

    @RestApiMethod(description = "Refresh the given software user repository")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software user repository id")
    ])
    def refresh() {
        def repo = softwareUserRepositoryService.read(params.long('id'))
        if (repo) {
            softwareUserRepositoryService.refresh(repo)
            responseSuccess(["message": "Software repositories refreshing has been asked!"])
        }
        else {
            responseNotFound("SoftwareUserRepository", params.id)
        }
    }

    @RestApiMethod(description = "Refresh the software user repositories loaded by the software-router")
    def refreshUserRepositories() {
        softwareUserRepositoryService.refreshRepositories()
        responseSuccess(["message": "Software repositories refreshing has been asked!"])
    }

}
