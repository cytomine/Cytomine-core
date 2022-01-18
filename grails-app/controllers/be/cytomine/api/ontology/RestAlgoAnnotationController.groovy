package be.cytomine.api.ontology

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
import be.cytomine.api.RestController
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.SharedAnnotation
import be.cytomine.ontology.Term
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import grails.converters.JSON
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import static org.springframework.security.acls.domain.BasePermission.READ

/**
 * Controller that handle request on annotation created by software (job)
 * Annotation my be created by humain (RestUserAnnotationController).
 */
@RestApi(name = "Ontology | algo annotation services", description = "Methods for managing an annotation created by a software")
class  RestAlgoAnnotationController extends RestController {

    def algoAnnotationService
    def termService
    def imageInstanceService
    def secUserService
    def projectService
    def cytomineService
    def dataSource
    def algoAnnotationTermService
    def paramsService
    def unionGeometryService
    def annotationIndexService
    def reportService
    def imageServerService

    /**
     * List all annotation (created by algo) visible for the current user
     */
    @RestApiMethod(description="List all software annotation visible for the current user. See 'annotation domain' data for parameters (only show/hide parameters are available for this service). ", listing = true)
    @RestApiResponseObject(objectIdentifier =  "[annotation listing]")
    def list() {
        def annotations = []
        //get all user's project and list all algo annotation
        def projects = projectService.list()
        projects.each {
            annotations.addAll(algoAnnotationService.list(it,paramsService.getPropertyGroupToShow(params)))
        }
        responseSuccess(annotations)
    }

    /**
     * Read a single algo annotation
     */
    @RestApiMethod(description="Get an algo annotation")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The annotation id")
    ])
    def show() {
        AlgoAnnotation annotation = algoAnnotationService.read(params.long('id'))
        if (annotation) {
            responseSuccess(annotation)
        }
        else {
            responseNotFound("Annotation", params.id)
        }
    }

    /**
     * Count annotations
     */
    @RestApiMethod(description="Count the number of annotation in the project")
    @RestApiResponseObject(objectIdentifier = "[total:x]")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY,description = "Only count the annotations created after this date (optional)"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY,description = "Only count the annotations created before this date (optional)")
    ])
    def countByProject() {
        Project project = projectService.read(params.project)
        securityACLService.check(project, READ)
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        responseSuccess([total: algoAnnotationService.countByProject(project, startDate, endDate)])
    }

    /**
     * Add an annotation created by an algo
     * If JSON request params is an object, create a new annotation
     * If its a json array, create multiple annotation
     */
    @RestApiMethod(description="Add one (body is a JSON object) or multiple (body is a JSON array) algo annotations")
    @RestApiParams(params=[
            @RestApiParam(name="POST JSON: project", type="long", paramType = RestApiParamType.PATH, description = "The project id where this annotation belongs"),
            @RestApiParam(name="POST JSON: image", type="long", paramType = RestApiParamType.QUERY, description = "The image instance id where this annotation belongs"),
            @RestApiParam(name="POST JSON: location", type="string", paramType = RestApiParamType.QUERY, description = "The WKT geometrical description of the annotation"),
            @RestApiParam(name="POST JSON: term", type="long", paramType = RestApiParamType.QUERY, required = false, description = "Term id to associate with this annotation"),
            @RestApiParam(name="POST JSON: minPoint", type="int", paramType = RestApiParamType.QUERY, required = false, description = "Minimum number of point that constitute the annotation"),
            @RestApiParam(name="POST JSON: maxPoint", type="int", paramType = RestApiParamType.QUERY, required = false, description = "Maximum number of point that constitute the annotation")
    ])
    def add(){
        if(request.JSON instanceof JSONObject) {
            if(params.minPoint && !(params.minPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.put("minPoint", params.minPoint)
            if(params.maxPoint && !(params.maxPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.put("maxPoint", params.maxPoint)
        } else if(request.JSON instanceof JSONArray) {
            request.JSON.each {println it}
            if(params.minPoint && !(params.minPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.each{it.put("minPoint", params.minPoint)}
            if(params.maxPoint && !(params.maxPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.each{it.put("maxPoint", params.maxPoint)}
        }
        add(algoAnnotationService, request.JSON)
    }


    public Object addOne(def service, def json) {
        json.minPoint = params.getLong('minPoint')
        json.maxPoint = params.getLong('maxPoint')

        return algoAnnotationService.add(json)
    }

    /**
     * Update a single annotation created by algo
     */
    @RestApiMethod(description="Update an algo annotation")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def update() {
        def json = request.JSON
        try {
            //get annotation from DB
            def domain = algoAnnotationService.retrieve(json)
            //update it thanks to JSON in request
            def result = algoAnnotationService.update(domain,json)
            responseResult(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    /**
     * Delete a single annotation created by algo
     */
    @RestApiMethod(description="Delete an algo annotation")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def delete() {
        def json = JSON.parse("{id : $params.id}")
        delete(algoAnnotationService, json,null)
    }

    @RestApiMethod(description="Download a report (pdf, xls,...) with software annotation data from a specific project")
    @RestApiResponseObject(objectIdentifier =  "file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The project id"),
            @RestApiParam(name="terms", type="list", paramType = RestApiParamType.QUERY,description = "The annotation terms id (if empty: all terms)"),
            @RestApiParam(name="users", type="list", paramType = RestApiParamType.QUERY,description = "The annotation users id (if empty: all users)"),
            @RestApiParam(name="images", type="list", paramType = RestApiParamType.QUERY,description = "The annotation images id (if empty: all images)"),
            @RestApiParam(name="afterThan", type="Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created before this date will not be returned"),
            @RestApiParam(name="beforeThan", type="Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created after this date will not be returned"),
            @RestApiParam(name="format", type="string", paramType = RestApiParamType.QUERY,description = "The report format (pdf, xls,...)"),
    ])
    def downloadDocumentByProject() {
        Long afterThan = params.getLong('afterThan')
        Long beforeThan = params.getLong('beforeThan')
        reportService.createAnnotationDocuments(params.long('id'), params.terms, params.boolean("noTerm", false), params.boolean("multipleTerms", false),
                params.users, params.images, afterThan, beforeThan, params.format, response, "ALGOANNOTATION")
    }


    @RestApiMethod(description="Get a crop of an automatic annotation (image area framing annotation)", extensions=["png", "jpg", "tiff"])
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
        AlgoAnnotation annotation = AlgoAnnotation.read(params.long("id"))
        if (annotation) {
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("AlgoAnnotation", params.id)
        }

    }

    @RestApiMethod(description="Get a binary mask of an automatic annotation (image area framing annotation). Equivalent to crop with 'mask' type.", extensions=["png", "jpg", "tiff"])
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
        AlgoAnnotation annotation = AlgoAnnotation.read(params.long("id"))
        if (annotation) {
            params.mask = true
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("AlgoAnnotation", params.id)
        }
    }

    @RestApiMethod(description="Get an alpha mask of an automatic annotation (image area framing annotation). Equivalent to crop with 'alphaMask' type.", extensions=["png"])
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
        AlgoAnnotation annotation = AlgoAnnotation.read(params.long("id"))
        if (annotation) {
            params.alphaMask = true
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("AlgoAnnotation", params.id)
        }
    }

    /**
     * Merge all annotation from the image, user and term that touch with min minIntersectLength size and with a tolerance threshold bufferLength
     * @param image Image
     * @param user User
     * @param term Term
     * @param minIntersectLength  size of the intersection geometry between two annotation to merge them
     * @param bufferLength tolerance threshold for two annotation (if they are very close but not intersect)
     */
    private def unionAnnotations(ImageInstance image, SecUser user, Term term, Integer minIntersectLength, Integer bufferLength, Integer area) {
        unionGeometryService.unionPicture(image,user,term,area,area,bufferLength,minIntersectLength)
    }

    def securityACLService
    def abstractImageService

    def sharedAnnotationService
    @RestApiMethod(description="Add comment on an annotation to other user and send a mail to users")
    @RestApiResponseObject(objectIdentifier = "empty")
    @RestApiParams(params=[
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id"),
            @RestApiParam(name="POST JSON: comment", type="string", paramType = RestApiParamType.QUERY,description = "The comment"),
            @RestApiParam(name="POST JSON: sender", type="long", paramType = RestApiParamType.QUERY,description = "The user id who share the annotation"),
            @RestApiParam(name="POST JSON: subject", type="string", paramType = RestApiParamType.QUERY,description = "The subject of the mail that will be send"),
            @RestApiParam(name="POST JSON: from", type="string", paramType = RestApiParamType.QUERY,description = "The username of the user who send the mail"),
            @RestApiParam(name="POST JSON: receivers", type="list", paramType = RestApiParamType.QUERY,description = "The list of user (id) to send the mail"),
            @RestApiParam(name="POST JSON: emails", type="list", paramType = RestApiParamType.QUERY,required = false, description = "The list of emails to send the mail. Used (and mandatory) if receivers is null"),
            @RestApiParam(name="POST JSON: annotationURL ", type="string", paramType = RestApiParamType.QUERY,description = "The URL of the annotation in the image viewer"),
            @RestApiParam(name="POST JSON: shareAnnotationURL", type="string", paramType = RestApiParamType.QUERY,description = "The URL of the comment"),
    ])
    def addComment() {

        AlgoAnnotation annotation = algoAnnotationService.read(params.getLong('annotation'))
        def result = sharedAnnotationService.add(request.JSON, annotation, params)
        if(result) {
            responseResult(result)
        }
    }

    /**
     * Show a single comment for an annotation
     */
    //TODO : duplicate code from UserAnnotation
    @RestApiMethod(description="Get a specific comment")
    @RestApiParams(params=[
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id"),
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The comment id"),
    ])
    def showComment() {

        AlgoAnnotation annotation = algoAnnotationService.read(params.long('annotation'))
        if (!annotation) {
            responseNotFound("Annotation", params.annotation)
        }
        def sharedAnnotation = SharedAnnotation.findById(params.long('id'))
        if (sharedAnnotation) {
            responseSuccess(sharedAnnotation)
        } else {
            responseNotFound("SharedAnnotation", params.id)
        }
    }

    /**
     * List all comments for an annotation
     */
    @RestApiMethod(description="Get all comments on annotation", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def listComments() {
        AlgoAnnotation annotation = algoAnnotationService.read(params.long('annotation'))
        if (annotation) {
            responseSuccess(sharedAnnotationService.listComments(annotation))
        } else {
            responseNotFound("Annotation", params.id)
        }
    }

}
