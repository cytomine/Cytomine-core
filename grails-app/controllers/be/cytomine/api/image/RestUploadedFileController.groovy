package be.cytomine.api.image

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

import be.cytomine.api.RestController
import be.cytomine.image.UploadedFile
import be.cytomine.security.User
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller that handle request on file uploading (when a file is uploaded, list uploaded files...)
 */
@RestApi(name = "Image | uploaded file services", description = "Methods for managing an uploaded image file.")
class RestUploadedFileController extends RestController {

    def cytomineService
    def projectService
    def storageService
    def grailsApplication
    def uploadedFileService
    def imageInstanceService
    def abstractImageService
    def notificationService
    def securityACLService
    def secUserService
    def imageServerService

    @RestApiMethod(description = "Get all uploaded files made by the current user")
    @RestApiParams(params = [
            @RestApiParam(name = "onlyRootsWithDetails", type = "boolean", paramType = RestApiParamType.QUERY, description = "If set, only return uploaded files which are roots (no parent) with supplementary details such global size."),
            @RestApiParam(name = "onlyRoots", type = "boolean", paramType = RestApiParamType.QUERY, description = "If set, only return uploaded files which are roots (no parent)."),
            @RestApiParam(name = "parent", type = "long", paramType = RestApiParamType.QUERY, description = "If set, only return uploaded files having the given parent."),
            @RestApiParam(name = "root", type = "long", paramType = RestApiParamType.QUERY, description = "If set, only return uploaded files which are children of the given root. Returned attributes are a subset of uploaded files attributes."),
            @RestApiParam(name = "all", type = "boolean", paramType = RestApiParamType.QUERY, description = "True to list uploaded files for all users the current user has access to")
    ])
    def list() {
        Long root
        def uploadedFiles
        if (params.root) {
            root = params.long('root')
            uploadedFiles = uploadedFileService.listHierarchicalTree((User) cytomineService.getCurrentUser(), root)
        } else if (params.onlyRootsWithDetails) {
            def allowedSearchParameters = [
                    [field: "originalFilename", allowedOperators: equalsAndLikeAndIlikeOperators],
                    [field: "storage", allowedOperators: 'in'],
                    [field: "user", allowedOperators: 'in']
            ]
            uploadedFiles = uploadedFileService.listWithDetails((User) cytomineService.getCurrentUser(), getSearchParameters(allowedSearchParameters), params.sort, params.order)
        } else if (params.all) {
            def result = uploadedFileService.list()
            uploadedFiles = [collection : result.data, size : result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages]
        } else {
            Boolean onlyRoots = params.boolean('onlyRoots', false)
            Long parent = params.long('parent', null)
            def result = uploadedFileService.list((User)secUserService.getUser(cytomineService.getCurrentUser().id), parent, onlyRoots, params.sort, params.order, params.long('max'), params.long('offset'))
            uploadedFiles = [collection : result.data, size : result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages]
        }

        responseSuccess(uploadedFiles)
    }

    @RestApiMethod(description = "Get an uploaded file")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def show() {
        UploadedFile up = uploadedFileService.read(params.long('id'))
        if (up) {
            responseSuccess(up)
        } else {
            responseNotFound("UploadedFile", params.id)
        }
    }

    @RestApiMethod(description = "Add a new uploaded file. This DOES NOT upload the file, just create the domain.")
    def add() {
        add(uploadedFileService, request.JSON)
    }

    @RestApiMethod(description = "Edit an uploaded file domain (mainly to edit status during upload)")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def update() {
        update(uploadedFileService, request.JSON)
    }

    @RestApiMethod(description = "Delete an uploaded file domain. This do not delete the file on disk.")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def delete() {
        delete(uploadedFileService, JSON.parse("{id : $params.id}"), null)
    }

    @RestApiMethod(description = "Download the uploaded file")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def download() {
        UploadedFile up = uploadedFileService.read(params.long('id'));
        if (up) {
            String url = imageServerService.downloadUri(up)
            log.info "redirect url"
            redirect(url: url)
        } else {
            responseNotFound("UploadedFile", params.id)
        }
    }
}
