package be.cytomine.api.image.multidim

import be.cytomine.Exception.ForbiddenException

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
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.project.Project
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.annotation.RestApiResponseObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 18/05/11
 */
@RestApi(name = "Image | multidim | image group services", description = "[REMOVED] Methods for managing image group, a group of image from the same sample in different dimension (channel, zstack,...)")
class RestImageGroupController extends RestController {

    def imageGroupService
    def imageGroupHDF5Service
    def imageSequenceService
    def projectService

    @RestApiMethod(description="[REMOVED] Get an image group")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image group id")
    ])
    def show() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroup image = imageGroupService.read(params.long('id'))
        if (image) {
            responseSuccess(image)
        } else {
            responseNotFound("ImageGroup", params.id)
        }*/
    }

    @RestApiMethod(description="[REMOVED] Get image group listing by project", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*Project project = projectService.read(params.long('id'))

        if (project)  {
            responseSuccess(imageGroupService.list(project))
        }
        else {
            responseNotFound("ImageGroup", "Project", params.id)
        }*/
    }

    @RestApiMethod(description="[REMOVED] Add a new image group")
    def add () {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        //add(imageGroupService, request.JSON)
    }

    @RestApiMethod(description="[REMOVED] Update an image group")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="int", paramType = RestApiParamType.PATH, description = "The image group id")
    ])
    def update() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        //update(imageGroupService, request.JSON)
    }

    @RestApiMethod(description="[REMOVED] Delete an image group")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image group")
    ])
    def delete() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        //delete(imageGroupService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="[REMOVED] Get the different Characteristics for ImageGroup")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image group")
    ])
    def characteristics() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroup imageGroup = imageGroupService.read(params.long('id'))
        if (imageGroup)  {
            responseSuccess(imageGroupService.characteristics(imageGroup))
        }
        else {
            responseNotFound("ImageGroup", "ImageGroup", params.id)
        }*/
    }

    /**
     * Get image thumb URL
     */
    @RestApiMethod(description="[REMOVED] Get a small image (thumb) for a specific multidimensional image")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image group id")
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def thumb() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*response.setHeader("max-age", "86400")
        int maxSize = params.int('maxSize',  512)
        imageGroupService.thumb(params.long('id'), maxSize)
        responseByteArray(imageGroupService.thumb(params.long('id'), maxSize))*/
    }
}
