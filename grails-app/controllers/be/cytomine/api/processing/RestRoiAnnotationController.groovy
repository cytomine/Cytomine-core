package be.cytomine.api.processing

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.RestController
import be.cytomine.image.ImageInstance
import be.cytomine.processing.RoiAnnotation
import grails.converters.JSON
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for annotation created by user
 */
@RestApi(name = "Processing | roi annotation services", description = "Methods for managing an region of interest annotation")
class RestRoiAnnotationController extends RestController {

    def roiAnnotationService
    def termService
    def imageInstanceService
    def secUserService
    def projectService
    def cytomineService
    def annotationListingService
    def abstractImageService
    def imageServerService

    /**
     * Get a single annotation
     */
    @RestApiMethod(description="Get a roi annotation")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def show() {
        RoiAnnotation annotation = roiAnnotationService.read(params.long('id'))
        if (annotation) {
            responseSuccess(annotation)
        }
        else responseNotFound("Annotation", params.id)
    }

    /**
     * Add annotation created by user
     */
    @RestApiMethod(description="Add an annotation created by user")
    def add(){
        add(roiAnnotationService, request.JSON)
    }


    public Object addOne(def service, def json) {
        if (!json.project || json.isNull('project')) {
            ImageInstance image = ImageInstance.read(json.image)
            if (image) json.project = image.project.id
        }
        if (json.isNull('project')) {
            throw new WrongArgumentException("Annotation must have a valid project:" + json.project)
        }
        if (json.isNull('location')) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + json.location)
        }
        def minPoint = params.getLong('minPoint')
        def maxPoint = params.getLong('maxPoint')

        def result = roiAnnotationService.add(json,minPoint,maxPoint)
        return result
    }


    /**
     * Update annotation created by user
     */
    @RestApiMethod(description="Update an annotation")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def update() {
        def json = request.JSON
        try {
            def domain = roiAnnotationService.retrieve(json)
            def result = roiAnnotationService.update(domain,json)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Delete annotation created by user
     */
    @RestApiMethod(description="Delete an annotation")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def delete() {
        def json = JSON.parse("{id : $params.id}")
        delete(roiAnnotationService, json,null)
    }


    @RestApiMethod(description="Get a crop of a roi annotation (image area framing annotation)", extensions=["png", "jpg", "tiff"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The annotation id"),
            @RestApiParam(name="type", type="String", paramType=RestApiParamType.QUERY, description="Type of crop. Allowed values are 'crop' (default behavior if not set), 'draw' (the shape is drawn in the crop), 'mask' (annotation binary mask), 'alphaMask (part of crop outside annotation is transparent, requires png format)", required=false),
            @RestApiParam(name="draw", type="boolean", paramType=RestApiParamType.QUERY, description="Equivalent to set type='draw'", required=false),
            @RestApiParam(name="mask", type="boolean", paramType=RestApiParamType.QUERY, description="Equivalent to set type='mask'", required=false),
            @RestApiParam(name="alphaMask", type="boolean", paramType=RestApiParamType.QUERY, description="Equivalent to set type='alphaMask'", required=false),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY, description="Maximum crop size in width and height", required = false),
            @RestApiParam(name="zoom", type="int", paramType=RestApiParamType.QUERY, description="Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name="increaseArea", type="double", paramType=RestApiParamType.QUERY, description="Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name="complete", type="boolean", paramType = RestApiParamType.QUERY,description = "Do not simplify the annotation shape.", required=false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier ="image (bytes)")
    def crop() {
        RoiAnnotation annotation = RoiAnnotation.read(params.long("id"))
        if (annotation) {
            params.geometry = annotation.location
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("RoiAnnotation", params.id)
        }

    }

    @RestApiMethod(description="Get a binary mask of a roi annotation (image area framing annotation). Equivalent to crop with 'mask' type.", extensions=["png", "jpg", "tiff"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The annotation id"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY, description="Maximum crop size in width and height", required = false),
            @RestApiParam(name="zoom", type="int", paramType=RestApiParamType.QUERY, description="Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name="increaseArea", type="double", paramType=RestApiParamType.QUERY, description="Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name="complete", type="boolean", paramType = RestApiParamType.QUERY,description = "Do not simplify the annotation shape.", required=false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier ="image (bytes)")
    def cropMask () {
        RoiAnnotation annotation = RoiAnnotation.read(params.long("id"))
        if (annotation) {
            params.mask = true
            params.geometry = annotation.location
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("RoiAnnotation", params.id)
        }
    }

    @RestApiMethod(description="Get an alpha mask of a roi annotation (image area framing annotation). Equivalent to crop with 'alphaMask' type.", extensions=["png"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The annotation id"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY, description="Maximum crop size in width and height", required = false),
            @RestApiParam(name="zoom", type="int", paramType=RestApiParamType.QUERY, description="Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name="increaseArea", type="double", paramType=RestApiParamType.QUERY, description="Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name="complete", type="boolean", paramType = RestApiParamType.QUERY,description = "Do not simplify the annotation shape.", required=false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier ="image (bytes)")
    def cropAlphaMask () {
        RoiAnnotation annotation = RoiAnnotation.read(params.long("id"))
        if (annotation) {
            params.alphaMask = true
            params.geometry = annotation.location
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("RoiAnnotation", params.id)
        }
    }


}
