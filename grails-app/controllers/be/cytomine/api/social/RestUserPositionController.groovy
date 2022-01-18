package be.cytomine.api.social

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
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.security.SecUser
import be.cytomine.security.User
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
@RestApi(name = "Social| user position services", description = "Methods for managing user positions in an image")
class RestUserPositionController extends RestController {

    def cytomineService
    def imageInstanceService
    def secUserService
    def dataSource
    def projectService
    def mongo
    def userPositionService
    def sliceInstanceService

    @RestApiMethod(description="Record the position of the current user on an image.")
    @RestApiParams(params=[
            @RestApiParam(name="image", type="long", paramType = RestApiParamType.PATH, description = "The image id (Mandatory if slice not used)"),
            @RestApiParam(name="slice", type="long", paramType = RestApiParamType.PATH, description = "The slice id (Mandatory if image not used)"),
            @RestApiParam(name="topLeftX", type="double", paramType = RestApiParamType.QUERY, description = "Top Left X coordinate of the user viewport"),
            @RestApiParam(name="topRightX", type="double", paramType = RestApiParamType.QUERY, description = "Top Right X coordinate of the user viewport"),
            @RestApiParam(name="bottomLeftX", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Left X coordinate of the user viewport"),
            @RestApiParam(name="bottomRightX", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Right X coordinate of the user viewport"),
            @RestApiParam(name="topLeftY", type="double", paramType = RestApiParamType.QUERY, description = "Top Left Y coordinate of the user viewport"),
            @RestApiParam(name="topRightY", type="double", paramType = RestApiParamType.QUERY, description = "Top Right Y coordinate of the user viewport"),
            @RestApiParam(name="bottomLeftY", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Left Y coordinate of the user viewport"),
            @RestApiParam(name="bottomRightY", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Right Y coordinate of the user viewport"),
            @RestApiParam(name="zoom", type="integer", paramType = RestApiParamType.QUERY, description = "Zoom level in the user viewport"),
            @RestApiParam(name="rotation", type="double", paramType = RestApiParamType.QUERY, description = "Rotation level in the user viewport"),
            @RestApiParam(name="broadcast", type="boolean", paramType = RestApiParamType.QUERY, description = "Whether or not the user is broadcasting his/her position")
    ])
    def add() {
        try {
            responseSuccess(userPositionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }


    @RestApiMethod(description="Get the last position for a user and an image.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id (Mandatory)"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id (Mandatory)"),
            @RestApiParam(name="slice", type="long", paramType = RestApiParamType.QUERY, description = "The slice id", required=false),
            @RestApiParam(name="broadcast", type="boolean", paramType = RestApiParamType.PATH, description = "If set to true, the last position broadcasted by the user will be returned"),
    ])
    def lastPositionByUser() {
        ImageInstance image = imageInstanceService.read(params.id)
        SecUser user = secUserService.read(params.user)
        boolean broadcast = params.getBoolean("broadcast")
        SliceInstance slice = (params.slice) ? sliceInstanceService.read(params.slice) : null
        responseSuccess(userPositionService.lastPositionByUser(image, user, broadcast, slice))
    }

    @RestApiMethod(description="Summarize the UserPosition entries.")
    @RestApiParams(params=[
            @RestApiParam(name="image", type="long", paramType = RestApiParamType.PATH, description = "The image id"),
            @RestApiParam(name="slice", type="long", paramType = RestApiParamType.QUERY, description = "The slice id", required=false),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.QUERY, description = "The user id", required=false),
            @RestApiParam(name="afterThan", type="long", paramType = RestApiParamType.QUERY, description = "A date. Will select all the entries created after this date.", required=false),
            @RestApiParam(name="beforeThan", type="long", paramType = RestApiParamType.QUERY, description = "A date. Will select all the entries created before this date.", required=false),
            @RestApiParam(name="showDetails", type="boolean", paramType = RestApiParamType.QUERY, description = "Optional. If true, will give the complete list", required=false),
    ])
    def list() {
        ImageInstance image = imageInstanceService.read(params.image)
        User user = secUserService.read(params.user)
        if(params.user != null && user == null) throw new ObjectNotFoundException("Invalid user")

        SliceInstance slice = (params.slice) ? sliceInstanceService.read(params.slice) : null
        Long afterThan = params.long("afterThan")
        Long beforeThan = params.long("beforeThan")
        if(params.getBoolean("showDetails")){
            Long max = params.long('max')
            Long offset = params.long('offset')
            params.max = 0
            params.offset = 0
            responseSuccess(userPositionService.list(image, user, slice, afterThan, beforeThan, max, offset))
        } else {
            responseSuccess(userPositionService.summarize(image, user, slice, afterThan, beforeThan))
        }
    }

    @RestApiMethod(description="Get users that have opened an image recently.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id (Mandatory)"),
            @RestApiParam(name="slice", type="long", paramType = RestApiParamType.QUERY, description = "The slice id", required=false),
            @RestApiParam(name="broadcast", type="boolean", paramType = RestApiParamType.QUERY, description = "If set to true, only users broadcasting their position will be returned"),
    ])
    def listOnlineUsersByImage() {
        boolean broadcast = params.getBoolean("broadcast")
        ImageInstance image = imageInstanceService.read(params.id)
        SliceInstance slice = (params.slice) ? sliceInstanceService.read(params.slice) : null
        responseSuccess(userPositionService.listOnlineUsersByImage(image, broadcast, slice))
    }
}
