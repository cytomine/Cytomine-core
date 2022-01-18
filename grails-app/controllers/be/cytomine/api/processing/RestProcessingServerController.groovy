package be.cytomine.api.processing

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
import be.cytomine.processing.ProcessingServer
import grails.converters.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApiObject(name = "Processing server services", description = "Methods for managing processing servers")
class RestProcessingServerController extends RestController {

    def processingServerService

    @RestApiMethod(description = "Get all the processing servers available in Cytomine")
    def list() {
        responseSuccess(processingServerService.list())
    }

    @RestApiMethod(description = "Get a specific processing server")
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def show() {
        ProcessingServer processingServer = processingServerService.read(params.long('id'))
        if (processingServer) {
            responseSuccess(processingServer)
        } else {
            responseNotFound("ProcessingServer", params.id)
        }
    }

    @RestApiMethod(description = "Add a new processing server to Cytomine")
    def add() {
        add(processingServerService, request.JSON)
    }

    @RestApiMethod(description = "Update a processing server available in Cytomine")
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def update() {
        update(processingServerService, request.JSON)
    }

    @RestApiMethod(description = "Delete a processing server", listing = true)
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def delete() {
        delete(processingServerService, JSON.parse("{id : $params.id}"), null)
    }

}
