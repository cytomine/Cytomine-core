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
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.ontology.UserAnnotation
import be.cytomine.sql.ReviewedAnnotationListing
import be.cytomine.utils.GeometryUtils
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryCollection
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "Image | slice instance services", description = "Methods to manage a slice instance.")
class RestSliceInstanceController extends RestController {

    def sliceInstanceService
    def imageServerService
    def abstractImageService
    def imageInstanceService

    @RestApiMethod(description = "Get all slice instances for the given image instance", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The image instance id"),
    ])
    def listByImageInstance() {
        ImageInstance image = imageInstanceService.read(params.long('id'))
        if (image) {
            responseSuccess(sliceInstanceService.list(image))
        }
        else {
            responseNotFound("SliceInstance", "ImageInstance", params.id)
        }
    }

    @RestApiMethod(description="Get an slice instance")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The slice instance id")
    ])
    def show() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long('id'))
        if (sliceInstance) {
            responseSuccess(sliceInstance)
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    @RestApiMethod(description="Get the slice instance for the given coordinates and image instance")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image instance id"),
            @RestApiParam(name="zStack", type="double", paramType = RestApiParamType.PATH, description = "The zStack coordinate"),
            @RestApiParam(name="time", type="double", paramType = RestApiParamType.PATH, description = "The time coordinate"),
            @RestApiParam(name="channel", type="double", paramType = RestApiParamType.PATH, description = "The channel coordinate"),
    ])
    def getByImageInstanceAndCoordinates() {
        ImageInstance image = imageInstanceService.read(params.long('id'))
        if (image) {
            SliceInstance sliceInstance = sliceInstanceService.read(image, params.int('channel'),
                    params.int('zStack'), params.int('time'))
            if (sliceInstance) {
                responseSuccess(sliceInstance)
            } else {
                responseNotFound("SliceInstance", params.id)
            }
        } else {
            responseNotFound("SliceInstance", "ImageInstance", params.id)
        }

    }

    @RestApiMethod(description="Add a new slice instance. See 'upload file service' to upload a slice.")
    def add() {
        add(sliceInstanceService, request.JSON)
    }

    @RestApiMethod(description="Update an slice instance")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The slice instance id")
    ])
    def update() {
        update(sliceInstanceService, request.JSON)
    }

    @RestApiMethod(description="Delete an slice instance)")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The slice instance id")
    ])
    def delete() {
        delete(sliceInstanceService, JSON.parse("{id : $params.id}"),null)
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
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  512)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            parameters.refresh = params.boolean('refresh', false)
            responseByteArray(imageServerService.thumb(sliceInstance, parameters))
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    def crop() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            responseByteArray(imageServerService.crop(sliceInstance, params))
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    def windowUrl() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            String url = imageServerService.window(sliceInstance, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    def window() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            if (params.mask || params.alphaMask || params.alphaMask || params.draw || params.type in ['draw', 'mask', 'alphaMask', 'alphamask'])
                params.location = getWKTGeometry(sliceInstance, params)
            responseByteArray(imageServerService.window(sliceInstance, params, false))
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    def cameraUrl() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            params.withExterior = false
            String url = imageServerService.window(sliceInstance, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    def camera() {
        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
        if (sliceInstance) {
            params.withExterior = false
            responseByteArray(imageServerService.window(sliceInstance, params, false))
        } else {
            responseNotFound("SliceInstance", params.id)
        }
    }

    //todo : move into a service
    def userAnnotationService
    def reviewedAnnotationService
    def termService
    def secUserService
    def annotationListingService
    public String getWKTGeometry(SliceInstance sliceInstance, params) {
        def geometries = []
        if (params.annotations && !params.reviewed) {
            def idAnnotations = params.annotations.split(',')
            idAnnotations.each { idAnnotation ->
                def annot = userAnnotationService.read(idAnnotation)
                if (annot)
                    geometries << annot.location
            }
        }
        else if (params.annotations && params.reviewed) {
            def idAnnotations = params.annotations.split(',')
            idAnnotations.each { idAnnotation ->
                def annot = reviewedAnnotationService.read(idAnnotation)
                if (annot)
                    geometries << annot.location
            }
        }
        else if (!params.annotations) {
            def project = sliceInstance.image.project
            List<Long> termsIDS = params.terms?.split(',')?.collect {
                Long.parseLong(it)
            }
            if (!termsIDS) { //don't filter by term, take everything
                termsIDS = termService.getAllTermId(project)
            }

            List<Long> userIDS = params.users?.split(",")?.collect {
                Long.parseLong(it)
            }
            if (!userIDS) { //don't filter by users, take everything
                userIDS = secUserService.listLayers(project).collect { it.id}
            }
            List<Long> sliceIDS = [sliceInstance.id]

            log.info params
            //Create a geometry corresponding to the ROI of the request (x,y,w,h)
            int x
            int y
            int w
            int h
            try {
                x = params.int('topLeftX')
                y = params.int('topLeftY')
                w = params.int('width')
                h = params.int('height')
            }catch (Exception e) {
                x = params.int('x')
                y = params.int('y')
                w = params.int('w')
                h = params.int('h')
            }
            Geometry roiGeometry = GeometryUtils.createBoundingBox(
                    x,                                      //minX
                    x + w,                                  //maxX
                    sliceInstance.baseSlice.image.height - (y + h),    //minX
                    sliceInstance.baseSlice.image.height - y           //maxY
            )


            //Fetch annotations with the requested term on the request image
            if (params.review) {
                ReviewedAnnotationListing ral = new ReviewedAnnotationListing(
                        project: project.id, terms: termsIDS, reviewUsers: userIDS, slices:sliceIDS, bbox:roiGeometry,
                        columnToPrint:['basic', 'meta', 'wkt', 'term']
                )
                def result = annotationListingService.listGeneric(ral)
                log.info "annotations=${result.size()}"
                geometries = result.collect {
                    new WKTReader().read(it["location"])
                }

            } else {
                log.info "roiGeometry=${roiGeometry}"
                log.info "termsIDS=${termsIDS}"
                log.info "userIDS=${userIDS}"
                Collection<UserAnnotation> annotations = userAnnotationService.list(sliceInstance, roiGeometry, termsIDS, userIDS)
                log.info "annotations=${annotations.size()}"
                geometries = annotations.collect { geometry ->
                    geometry.getLocation()
                }
            }
        }
        GeometryCollection geometryCollection = new GeometryCollection((Geometry[])geometries, new GeometryFactory())
        return new WKTWriter().write(geometryCollection)
    }

//    def download() {
//        SliceInstance sliceInstance = sliceInstanceService.read(params.long("id"))
//        def uf = sliceInstance?.uploadedFile
//        if (uf) {
//            String url = imageServerService.downloadUri(sliceInstance, uf)
//            redirect(url: url)
//        } else {
//            responseNotFound("SliceInstance", params.id)
//        }
//    }
}
