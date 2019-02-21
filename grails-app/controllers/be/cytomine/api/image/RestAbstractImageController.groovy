package be.cytomine.api.image

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.RestController
import be.cytomine.image.AbstractImage
import be.cytomine.image.ImageInstance
import be.cytomine.image.UploadedFile
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageSequence
import be.cytomine.image.server.ImageServer
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.test.HttpClient
import grails.converters.JSON
import groovyx.net.http.HTTPBuilder
import org.apache.commons.io.IOUtils
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import java.awt.image.BufferedImage

/**
 * Controller for abstract image
 * An abstract image can be add in n projects
 */
@RestApi(name = "Image | abstract image services", description = "Methods for managing an image. See image instance service to manage an instance of image in a project.")
class RestAbstractImageController extends RestController {

    def imagePropertiesService
    def abstractImageService
    def cytomineService
    def projectService
    def imageSequenceService
    def dataTablesService
    def imageServerProxyService
    def uploadedFileService

    /**
     * List all abstract image available on cytomine
     */
    //TODO:APIDOC

    @RestApiMethod(description="Get all image available for the current user", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="project", type="long", paramType = RestApiParamType.QUERY, description = "If set, check if image is in project or not", required=false),
        @RestApiParam(name="sortColumn", type="string", paramType = RestApiParamType.QUERY, description = "Column sort (created by default)", required=false),
        @RestApiParam(name="sortDirection", type="string", paramType = RestApiParamType.QUERY, description = "Sort direction (desc by default)", required=false),
        @RestApiParam(name="search", type="string", paramType = RestApiParamType.QUERY, description = "Original filename search filter (all by default)", required=false),
        @RestApiParam(name="datatables", type="boolean", paramType=RestApiParamType.QUERY, description="", required=false),
    ])
    def list() {
        SecUser user = cytomineService.getCurrentUser()
        if (params.datatables) {
            Project project = projectService.read(params.long("project"))
            responseSuccess(dataTablesService.process(params, AbstractImage, null, [],project))
        }  else {
            responseSuccess(abstractImageService.list(user))
        }
    }

    /**
     * List all abstract images for a project
     */
    @RestApiMethod(description="Get all image having an instance in a project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = Project.read(params.id)
        if (project) {
            responseSuccess(abstractImageService.list(project))
        } else {
            responseNotFound("Image", "Project", params.id)
        }
    }

    @RestApiMethod(description="Get an abstract image from its uploaded file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def getByAbstractImage () {
        UploadedFile uf = uploadedFileService.read(params.long('id'))
        AbstractImage image = AbstractImage.findByUploadedFile(uf)
        if (image) {
            responseSuccess(image)
        } else {
            responseNotFound("AbstractImage", params.id)
        }
    }

    /**
     * Get a single image
     */
    @RestApiMethod(description="Get an image")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id")
    ])
    def show() {
        AbstractImage image = abstractImageService.read(params.long('id'))
        if (image) {
            responseSuccess(image)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    /**
     * Add a new image
     */
    @RestApiMethod(description="Add a new image in the software. See 'upload file service' to upload an image")
    def add() {
        add(abstractImageService, request.JSON)
    }

    /**
     * Update a new image
     */
    @RestApiMethod(description="Update an image in the software")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image sequence id")
    ])
    def update() {
        update(abstractImageService, request.JSON)
    }

    /**
     * Delete a new image
     */
    @RestApiMethod(description="Delete an abstract image)")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image sequence id")
    ])
    def delete() {
        delete(abstractImageService, JSON.parse("{id : $params.id}"),null)
    }


    @RestApiMethod(description="Get all unused images available for the current user", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.QUERY, description = "The id of abstract image"),
    ])
    def listUnused() {
        SecUser user = cytomineService.getCurrentUser()
        def result = abstractImageService.listUnused(user);
        responseSuccess(result);
    }

    @RestApiMethod(description="Show user who uploaded an image")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id")
    ])
    def showUploaderOfImage() {
        SecUser user = abstractImageService.getUploaderOfImage(params.long('id'))
        if (user) {
            responseSuccess(user)
        } else {
            responseNotFound("User", "Image", params.id)
        }
    }

    @RestApiMethod(description="Get a small image (thumb) for a specific image", extensions=["png", "jpg"])
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
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  512)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            parameters.refresh = params.boolean('refresh', false)
            responseBufferedImage(imageServerProxyService.thumb(abstractImage.referenceSlice, parameters))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get an image (preview) for a specific image", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The image id"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier ="image (bytes)")
    def preview() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  1024)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            responseBufferedImage(imageServerProxyService.thumb(abstractImage.referenceSlice, parameters))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get available associated images", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id")
    ])
    @RestApiResponseObject(objectIdentifier ="associated image labels")
    def associated() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def associated = imageServerProxyService.associated(abstractImage)
            responseSuccess(associated)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get an associated image of a abstract image (e.g. label, macro, thumbnail)", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id"),
            @RestApiParam(name="label", type="string", paramType = RestApiParamType.PATH,description = "The associated image label"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def label() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def parameters = [:]
            parameters.format = params.format
            parameters.label = params.label
            parameters.maxSize = params.int('maxSize', 256)
            def associatedImage = imageServerProxyService.label(abstractImage, parameters)
            responseBufferedImage(associatedImage)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def crop() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            responseBufferedImage(imageServerProxyService.crop(abstractImage.referenceSlice, params))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def windowUrl() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            String url = imageServerProxyService.window(abstractImage.referenceSlice, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def window() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            responseBufferedImage(imageServerProxyService.window(abstractImage.referenceSlice, params, false))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def cameraUrl() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            params.withExterior = false
            String url = imageServerProxyService.window(abstractImage.referenceSlice, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def camera() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage && abstractImage.referenceSlice) {
            params.withExterior = false
            responseBufferedImage(imageServerProxyService.window(abstractImage.referenceSlice, params, false))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def download() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        def uf = abstractImage?.uploadedFile
        if (uf) {
            String url = imageServerProxyService.downloadUri(abstractImage, uf)
            redirect(url: url)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    /**
     * Get all image servers URL for an image
     */
    @RestApiMethod(description="Get all image servers URL for an image")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id"),
    ])
    @RestApiResponseObject(objectIdentifier = "URL list")
    def imageServers() {
        try {
            def id = params.long('id')
            responseSuccess(abstractImageService.imageServers(id))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    @RestApiMethod(description="Delete all previously saved image properties")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def clearProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.clear(abstractImage)
        responseSuccess([:])
    }

    @RestApiMethod(description="Get all image properties (metadata) from underlying file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def populateProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.populate(abstractImage)
        responseSuccess([:])
    }

    @RestApiMethod(description="Fill main image field from image properties")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id")
    ])
    @RestApiResponseObject(objectIdentifier = "empty")
    def extractProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.extractUseful(abstractImage)
        responseSuccess([:])
    }

}
