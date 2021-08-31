package be.cytomine.api.image

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
import be.cytomine.image.AbstractImage
import be.cytomine.image.CompanionFile
import be.cytomine.image.UploadedFile
import be.cytomine.security.SecUser
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "Image | companion file services", description = "Methods to manage a companion file.")
class RestCompanionFileController extends RestController {
    
    def companionFileService
    def imageServerService
    def abstractImageService

    @RestApiMethod(description = "Get all companion files for the given abstract image", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The abstract image id")
    ])
    def listByAbstractImage() {
        AbstractImage abstractImage = AbstractImage.read(params.long("id"))
        if (abstractImage) {
            responseSuccess(companionFileService.list(abstractImage))
        }
        else {
            responseNotFound("CompanionFile", "AbstractImage", params.id)
        }
    }
    
    @RestApiMethod(description = "Get all companion files for the given uploaded file", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def listByUploadedFile() {
        UploadedFile uploadedFile = UploadedFile.read(params.long("id"))
        if (uploadedFile) {
            responseSuccess(companionFileService.list(uploadedFile))
        }
        else {
            responseNotFound("CompanionFile", "UploadedFile", params.id)
        }
    }

    @RestApiMethod(description="Get a companion file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The companion file id")
    ])
    def show() {
        CompanionFile companionFile = companionFileService.read(params.long('id'))
        if (companionFile) {
            responseSuccess(companionFile)
        } else {
            responseNotFound("CompanionFile", params.id)
        }
    }

    @RestApiMethod(description="Add a new companion file. See 'upload file service' to upload a slice.")
    def add() {
        def json = request.JSON

        if (json?.type == "HDF5" && json?.uploadedFile == null)
            forward(controller: "restCompanionFile", action: "computeProfile")
        else
            add(companionFileService, request.JSON)
    }

    @RestApiMethod(description="Update a companion file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The companion file id")
    ])
    def update() {
        update(companionFileService, request.JSON)
    }

    @RestApiMethod(description="Delete a companion file)")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The companion file id")
    ])
    def delete() {
        delete(companionFileService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="Show user who uploaded the companion file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The companion file id")
    ])
    def showUploader() {
        SecUser user = companionFileService.getUploader(params.long('id'))
        if (user) {
            responseSuccess(user)
        } else {
            responseNotFound("User", "CompanionFile", params.id)
        }
    }

    def download() {
        CompanionFile companionFile = companionFileService.read(params.long("id"))
        String url = imageServerService.downloadUri(companionFile)
        redirect(url: url)
    }

    @RestApiMethod(description="Ask to compute HDF5 profile for the given image")
    @RestApiParams(params=[
            @RestApiParam(name="image", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id")
    ])
    def computeProfile() {
        def id = params.long("image") ?: request.JSON?.image

        AbstractImage abstractImage = abstractImageService.read(id)
        if (abstractImage) {
            if (abstractImage.dimensions.length() == 3 && !abstractImage.hasProfile()) {
                //TODO: check image is greyscale
                responseSuccess(imageServerService.profile(abstractImage).companionFile)
            }
            else {
                responseError(new InvalidRequestException("Abstract image ${abstractImage.id} already has a profile or cannot have one."))
            }
        }
        else {
            responseNotFound("Image", id)
        }
    }

//    def download() {
//        CompanionFile companionFile = companionFileService.read(params.long("id"))
//        def uf = companionFile?.uploadedFile
//        if (uf) {
//            String url = imageServerService.downloadUri(companionFile, uf)
//            redirect(url: url)
//        } else {
//            responseNotFound("CompanionFile", params.id)
//        }
//    }
}
