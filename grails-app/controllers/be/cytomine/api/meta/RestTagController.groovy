package be.cytomine.api.meta

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
import be.cytomine.meta.Tag
import be.cytomine.security.SecUser
import grails.converters.JSON
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "tag services", description = "Methods for managing tags, flags that can be associated to Cytomine domain")
class RestTagController extends RestController {

    def tagService
    def cytomineService
    def securityACLService

    @RestApiMethod(description="Get all tag available in cytomine", listing = true)
    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        responseSuccess(tagService.list())
    }

    @RestApiMethod(description="Get a specific tag")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The tag id")
    ])
    def show() {
        securityACLService.checkGuest(cytomineService.currentUser)
        Tag tag = tagService.read(params.long('id'))
        if (tag) {
            responseSuccess(tag)
        } else {
            responseNotFound("Tag", params.id)
        }
    }

    @RestApiMethod(description="Add a new tag to cytomine.")
    def add() {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        add(tagService, request.JSON)
    }

    /**
     * Update a tag info
     */
    @RestApiMethod(description="Update a tag.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The tag id")
    ])
    def update() {
        SecUser currentUser = cytomineService.getCurrentUser()
        Tag tag = Tag.read(request.JSON.id)
        securityACLService.checkIsCreator(tag, currentUser)
        update(tagService, request.JSON)
    }

    /**
     * Delete tag
     */
    @RestApiMethod(description="Delete a tag.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The tag id")
    ])
    def delete() {
        SecUser currentUser = cytomineService.getCurrentUser()
        Tag tag = Tag.read(params.id)
        if(tag) securityACLService.checkIsCreator(tag, currentUser )
        delete(tagService, JSON.parse("{id : $params.id}"),null)
    }
}
