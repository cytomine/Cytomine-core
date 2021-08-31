package be.cytomine.api.ontology

import be.cytomine.Exception.InvalidRequestException

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.Track
import be.cytomine.project.Project
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ

@RestApi(name = "Ontology | track services", description = "Methods for managing tracks")
class RestTrackController extends RestController {

    def trackService
    def projectService
    def imageInstanceService
    def securityACLService

    @RestApiMethod(description="Get all track from a project", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id")
    ])
    def listByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(trackService.list(project))
        } else {
            responseNotFound("Track", "Project", params.id)
        }
    }

    @RestApiMethod(description="Get all track from an image instance", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image instance id")
    ])
    def listByImageInstance() {
        ImageInstance image = imageInstanceService.read(params.long('id'))
        if (image) {
            responseSuccess(trackService.list(image))
        } else {
            responseNotFound("Track", "Image", params.id)
        }
    }

    @RestApiMethod(description = "Count the number of tracks in the project")
    @RestApiResponseObject(objectIdentifier = "[total:x]")
    @RestApiParams(params = [
            @RestApiParam(name = "project", type = "long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name = "startDate", type = "long", paramType = RestApiParamType.QUERY, description = "Only count the tracks created after this date (optional)"),
            @RestApiParam(name = "endDate", type = "long", paramType = RestApiParamType.QUERY, description = "Only count the tracks created before this date (optional)")
    ])
    def countByProject() {
        Project project = projectService.read(params.project)
        securityACLService.check(project, READ)
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        responseSuccess([total: trackService.countByProject(project, startDate, endDate)])
    }

    @RestApiMethod(description="Get a track")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The track id")
    ])
    def show() {
        Track track = trackService.read(params.long('id'))
        if (track) {
            responseSuccess(track)
        } else {
            responseNotFound("Track", params.id)
        }
    }

    @RestApiMethod(description="Add a track in an image")
    def add () {
        if(!request.JSON.image) throw new InvalidRequestException("Image instance not set")
        add(trackService, request.JSON)
    }

    @RestApiMethod(description="Update a track")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The track id")
    ])
    def update () {
        update(trackService, request.JSON)
    }

    @RestApiMethod(description="Delete a track")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The track id")
    ])
    def delete () {
        delete(trackService, JSON.parse("{id : $params.id}"),null)
    }

}
