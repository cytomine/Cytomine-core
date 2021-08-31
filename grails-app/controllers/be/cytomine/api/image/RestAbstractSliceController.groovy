package be.cytomine.api.image

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
import be.cytomine.image.AbstractImage
import be.cytomine.image.AbstractSlice
import be.cytomine.image.UploadedFile
import be.cytomine.security.SecUser
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "Image | abstract slice services", description = "Methods to manage an abstract slice. See slice instance service to manage an instance of a slice in a project.")
class RestAbstractSliceController extends RestController {

    def abstractSliceService
    def imageServerService
    def abstractImageService

    @RestApiMethod(description = "Get all abstract slices for the given abstract image", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The abstract image id")
    ])
    def listByAbstractImage() {
        AbstractImage abstractImage = AbstractImage.read(params.long("id"))
        if (abstractImage) {
            responseSuccess(abstractSliceService.list(abstractImage))
        }
        else {
            responseNotFound("AbstractSlice", "AbstractImage", params.id)
        }
    }

    @RestApiMethod(description = "Get all abstract slices for the given uploaded file", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def listByUploadedFile() {
        UploadedFile uploadedFile = UploadedFile.read(params.long("id"))
        if (uploadedFile) {
            responseSuccess(abstractSliceService.list(uploadedFile))
        }
        else {
            responseNotFound("AbstractSlice", "UploadedFile", params.id)
        }
    }
    
    @RestApiMethod(description="Get an abstract slice")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The abstract slice id")
    ])
    def show() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long('id'))
        if (abstractSlice) {
            responseSuccess(abstractSlice)
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    @RestApiMethod(description="Get the abstract slice for the given coordinates and abstract image")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id"),
            @RestApiParam(name="zStack", type="double", paramType = RestApiParamType.PATH, description = "The zStack coordinate"),
            @RestApiParam(name="time", type="double", paramType = RestApiParamType.PATH, description = "The time coordinate"),
            @RestApiParam(name="channel", type="double", paramType = RestApiParamType.PATH, description = "The channel coordinate"),
    ])
    def getByAbstractImageAndCoordinates() {
        AbstractImage image = abstractImageService.read(params.long('id'))
        if (image) {
            AbstractSlice abstractSlice = abstractSliceService.read(image, params.double('channel'), 
                    params.double('zStack'), params.double('time'))
            if (abstractSlice) {
                responseSuccess(abstractSlice)
            } else {
                responseNotFound("AbstractSlice", params.id)
            }
        } else {
            responseNotFound("AbstractSlice", "AbstractImage", params.id)
        }
       
    }
    
    @RestApiMethod(description="Add a new abstract slice. See 'upload file service' to upload a slice.")
    def add() {
        add(abstractSliceService, request.JSON)
    }
    
    @RestApiMethod(description="Update an abstract slice")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The abstract slice id")
    ])
    def update() {
        update(abstractSliceService, request.JSON)
    }
    
    @RestApiMethod(description="Delete an abstract slice)")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The abstract slice id")
    ])
    def delete() {
        delete(abstractSliceService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="Show user who uploaded the slice")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The abstract slice id")
    ])
    def showUploaderOfImage() {
        SecUser user = abstractSliceService.getUploaderOfImage(params.long('id'))
        if (user) {
            responseSuccess(user)
        } else {
            responseNotFound("User", "AbstractSlice", params.id)
        }
    }

    @RestApiMethod(description="Get a small image (thumb) for a specific slice", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The image id"),
            @RestApiParam(name="refresh", type="boolean", paramType=RestApiParamType.QUERY, description="If true, don't take it from cache and regenerate it", required=false),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def thumb() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  512)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            parameters.refresh = params.boolean('refresh', false)
            responseByteArray(imageServerService.thumb(abstractSlice, parameters))
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    def crop() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            responseByteArray(imageServerService.crop(abstractSlice, params))
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    def windowUrl() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            String url = imageServerService.window(abstractSlice, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    def window() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            responseByteArray(imageServerService.window(abstractSlice, params, false))
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    def cameraUrl() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            params.withExterior = false
            String url = imageServerService.window(abstractSlice, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

    def camera() {
        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
        if (abstractSlice) {
            params.withExterior = false
            responseByteArray(imageServerService.window(abstractSlice, params, false))
        } else {
            responseNotFound("AbstractSlice", params.id)
        }
    }

//    def download() {
//        AbstractSlice abstractSlice = abstractSliceService.read(params.long("id"))
//        def uf = abstractSlice?.uploadedFile
//        if (uf) {
//            String url = imageServerService.downloadUri(abstractSlice, uf)
//            redirect(url: url)
//        } else {
//            responseNotFound("AbstractSlice", params.id)
//        }
//    }
}
