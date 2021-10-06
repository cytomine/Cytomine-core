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
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for a description (big text data/with html format) on a specific domain
 */
@RestApi(name = "Utils | scoring description by user services", description = "Methods for managing scoring description on a specific domain")
class RestScoringDescriptionController extends RestController {

    def springSecurityService
    def scoringDescriptionService
    def cytomineService

    @RestApiMethod(description="List all description available", listing=true)
    def list() {
        responseSuccess(scoringDescriptionService.list())
    }

    @RestApiMethod(description="Get a description for a specific domain (id and class)")
    @RestApiParams(params=[
        @RestApiParam(name="domainIdent", type="long", paramType = RestApiParamType.PATH, description = "The domain id"),
        @RestApiParam(name="domainClassName", type="string", paramType = RestApiParamType.PATH, description = "The domain class")
    ])
    def showByDomain() {
        def id = params.long('domainIdent')
        def className = params.get('domainClassName')
        def user = cytomineService.getCurrentUser()

        if(className && id) {
            def desc = scoringDescriptionService.get(id,className.replace("_","."),user)
            if(desc) {
                responseSuccess(desc)
            } else {
                responseNotFound("Description with Project","$id and className=$className")
            }
        } else {
            log.error "classname $className or id $id is missing"
        }
    }

    /**
     * Add a new description to a domain
     */
    @RestApiMethod(description="Add a new description to a domain")
    def add() {
        request.JSON.user = cytomineService.getCurrentUser()?.id
        add(scoringDescriptionService, request.JSON)
    }

    /**
     * Update a description
     */
    @RestApiMethod(description="Update a description")
    @RestApiParams(params=[
        @RestApiParam(name="domainIdent", type="long", paramType = RestApiParamType.PATH, description = "The domain id"),
        @RestApiParam(name="domainClassName", type="string", paramType = RestApiParamType.PATH, description = "The domain class")
    ])
    def update() {
        request.JSON.user = cytomineService.getCurrentUser()?.id
        update(scoringDescriptionService, request.JSON)
    }

    /**
     * Delete a description
     */
    @RestApiMethod(description="Delete a description")
    @RestApiParams(params=[
        @RestApiParam(name="domainIdent", type="long", paramType = RestApiParamType.PATH, description = "The domain id"),
        @RestApiParam(name="domainClassName", type="string", paramType = RestApiParamType.PATH, description = "The domain class")
    ])
    def delete() {
        try {
            def json = [domainIdent:params.long('domainIdent'),domainClassName:params.get('domainClassName').replace("_","."),user:cytomineService.getCurrentUser()?.id]
            def domain = scoringDescriptionService.retrieve(json)
            def result = scoringDescriptionService.delete(domain,transactionService.start(),null,true)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }
}

