package be.cytomine.api.middleware

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
import be.cytomine.middleware.ImageServer
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApi(name="Middleware | image server", description="Methods to manage image servers (IMS)")
class RestImageServerController extends RestController {

    def imageServerService

    @RestApiMethod(description="List image servers", listing=true)
    def list() {
        responseSuccess(imageServerService.list())
    }

    @RestApiMethod(description="Get an image server")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image server id")
    ])
    def show() {
        ImageServer imageServer = imageServerService.read(params.long('id'))
        if (imageServer) {
            responseSuccess(imageServer)
        } else {
            responseNotFound("ImageServer", params.id)
        }
    }
}
