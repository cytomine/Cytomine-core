package be.cytomine.api.image.server

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

import be.cytomine.Exception.AlreadyExistException
import be.cytomine.api.RestController
import be.cytomine.image.server.Storage
import be.cytomine.security.SecUser
import be.cytomine.security.User
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApi(name="Image | server | storage services", description="Methods to manage storages")
class RestStorageController extends RestController {

    def cytomineService
    def storageService
    def secUserService

    @RestApiMethod(description="List all storages", listing=true)
    @RestApiParams(params = [
            @RestApiParam(name = "all", type = "boolean", paramType = RestApiParamType.QUERY, description = "True to list storages for all users the current user has access to")
    ])
    def list() {
        def storages
        if (params.boolean("all", false)) {
            storages = storageService.list()
        }
        else {
            storages = storageService.list(cytomineService.getCurrentUser())
        }
        responseSuccess(storages)
    }

    @RestApiMethod(description="Get a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id")
    ])
    def show() {
        Storage storage = storageService.read(params.long('id'))
        if (storage) {
            responseSuccess(storage)
        } else {
            responseNotFound("Storage", params.id)
        }
    }

    @RestApiMethod(description="Add a new storage")
    def add() {
        add(storageService, request.JSON)
    }

    @RestApiMethod(description="Update a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id")
    ])
    def update() {
        update(storageService, request.JSON)
    }

    @RestApiMethod(description="Delete a storage")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id")
    ])
    def delete() {
        delete(storageService, JSON.parse("{id : $params.id}"), null)
    }

    @RestApiMethod(description="List user details for a storage", listing=true)
    @RestApiParams(params = [
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The storage id"),
    ])
    def statsPerUser() {
        Storage storage = storageService.read(params.long('id'))
        if (storage) {
            String sortColumn = params.sort ?: "created"
            String sortDirection = params.order ?: "desc"
            responseSuccess(storageService.usersStats(storage, sortColumn, sortDirection, params.long('max',0), params.long('offset',0)))
        } else {
            responseNotFound("Storage", params.id)
        }
    }

    @RestApiMethod(description="List storages access for current user", listing=true)
    @RestApiParams(params = [
    ])
    def storageAccess() {
        responseSuccess(storageService.userAccess(cytomineService.getCurrentUser()))
    }

    /**
     * Create a storage for user with default parameters
     */
//    @RestApiMethod(description="Create a storage for a user with default parameter values")
//    @RestApiParams(params=[
//            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The human user id")
//    ])
//    def create() {
//        def id = params.long('user')
//        SecUser user = secUserService.read(id)
//        if (user instanceof User) {
//            if (Storage.findByUser(user)) {
//                new AlreadyExistException("A storage already exists for user $user.username")
//            } else {
//                storageService.initUserStorage((User)user)
//                responseSuccess(Storage.findByUser(user))
//            }
//        }
//    }
}
