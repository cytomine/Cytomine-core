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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.RestController
import be.cytomine.ontology.SharedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ

/**
 * Controller for annotation created by user
 */
@RestApi(name = "Ontology | user annotation services", description = "Methods for managing an annotation created by a human user")
class RestUserAnnotationController extends RestController {

    def userAnnotationService
    def termService
    def imageInstanceService
    def secUserService
    def projectService
    def cytomineService
    def annotationListingService
    def reportService
    def securityACLService
    def abstractImageService
    def imageServerService

    /**
     * List all annotation with light format
     */
    @RestApiMethod(description = "List all annotation (very light format)", listing = true)
    def list() {
        responseSuccess(userAnnotationService.listLight())
    }

    @RestApiMethod(description = "Count the number of annotation for the current user")
    @RestApiResponseObject(objectIdentifier = "[total:x]")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The user id (mandatory)"),
            @RestApiParam(name = "project", type = "long", paramType = RestApiParamType.PATH, description = "The project id"),
    ])
    def countByUser() {
        SecUser user = secUserService.read(params.id)
        Project project = projectService.read(params.project)

        if (params.project && !project) throw new ObjectNotFoundException("ACL error: domain is null! Unable to process project auth checking")

        if (project) {
            securityACLService.checkIsSameUserOrAdminContainer(project, user, cytomineService.currentUser)
        } else {
            securityACLService.checkIsSameUser(user, cytomineService.currentUser)
        }
        responseSuccess([total: userAnnotationService.count(user, project)])
    }

    @RestApiMethod(description = "Count the number of annotation in the project")
    @RestApiResponseObject(objectIdentifier = "[total:x]")
    @RestApiParams(params = [
            @RestApiParam(name = "project", type = "long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name = "startDate", type = "long", paramType = RestApiParamType.QUERY, description = "Only count the annotations created after this date (optional)"),
            @RestApiParam(name = "endDate", type = "long", paramType = RestApiParamType.QUERY, description = "Only count the annotations created before this date (optional)")
    ])
    def countByProject() {
        Project project = projectService.read(params.project)
        securityACLService.check(project, READ)
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        responseSuccess([total: userAnnotationService.countByProject(project, startDate, endDate)])
    }

    /**
     * Download report with annotation
     */
    @RestApiMethod(description = "Download a report (pdf, xls,...) with user annotation data from a specific project")
    @RestApiResponseObject(objectIdentifier = "file")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name = "terms", type = "list", paramType = RestApiParamType.QUERY, description = "The annotation terms id (if empty: all terms)"),
            @RestApiParam(name = "users", type = "list", paramType = RestApiParamType.QUERY, description = "The annotation users id (if empty: all users)"),
            @RestApiParam(name = "images", type = "list", paramType = RestApiParamType.QUERY, description = "The annotation images id (if empty: all images)"),
            @RestApiParam(name = "afterThan", type = "Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created before this date will not be returned"),
            @RestApiParam(name = "beforeThan", type = "Long", paramType = RestApiParamType.QUERY, description = "(Optional) Annotations created after this date will not be returned"),
            @RestApiParam(name = "format", type = "string", paramType = RestApiParamType.QUERY, description = "The report format (pdf, xls,...)")
    ])
    def downloadDocumentByProject() {
        Long afterThan = params.getLong('afterThan')
        Long beforeThan = params.getLong('beforeThan')
        reportService.createAnnotationDocuments(params.long('id'), params.terms, params.boolean("noTerm", false), params.boolean("multipleTerms", false),
                params.users, params.images, afterThan, beforeThan, params.format, response, "USERANNOTATION")
    }

    def sharedAnnotationService
    /**
     * Add comment on an annotation to other user
     */
    @RestApiMethod(description = "Add comment on an annotation to other user and send a mail to users")
    @RestApiResponseObject(objectIdentifier = "empty")
    @RestApiParams(params = [
            @RestApiParam(name = "annotation", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name = "POST JSON: comment", type = "string", paramType = RestApiParamType.QUERY, description = "The comment"),
            @RestApiParam(name = "POST JSON: sender", type = "long", paramType = RestApiParamType.QUERY, description = "The user id who share the annotation"),
            @RestApiParam(name = "POST JSON: subject", type = "string", paramType = RestApiParamType.QUERY, description = "The subject of the mail that will be send"),
            @RestApiParam(name = "POST JSON: from", type = "string", paramType = RestApiParamType.QUERY, description = "The username of the user who send the mail"),
            @RestApiParam(name = "POST JSON: receivers", type = "list", paramType = RestApiParamType.QUERY, description = "The list of user (id) to send the mail"),
            @RestApiParam(name = "POST JSON: emails", type = "list", paramType = RestApiParamType.QUERY, required = false, description = "The list of emails to send the mail. Used if receivers is null"),
            @RestApiParam(name = "POST JSON: annotationURL ", type = "string", paramType = RestApiParamType.QUERY, description = "The URL of the annotation in the image viewer"),
            @RestApiParam(name = "POST JSON: shareAnnotationURL", type = "string", paramType = RestApiParamType.QUERY, description = "The URL of the comment"),
    ])
    def addComment() {

        UserAnnotation annotation = userAnnotationService.read(params.getLong('annotation'))
        def result = sharedAnnotationService.add(request.JSON, annotation, params)
        if (result) {
            responseResult(result)
        }
    }

    /**
     * Show a single comment for an annotation
     */
    //TODO : duplicated code in AlgoAnnotation
    @RestApiMethod(description = "Get a specific comment")
    @RestApiParams(params = [
            @RestApiParam(name = "annotation", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The comment id"),
    ])
    def showComment() {
        UserAnnotation annotation = userAnnotationService.read(params.long('annotation'))
        if (!annotation) {
            responseNotFound("Annotation", params.annotation)
        }
        def sharedAnnotation = sharedAnnotationService.read(params.long('id'))
        if (sharedAnnotation) {
            responseSuccess(sharedAnnotation)
        } else {
            responseNotFound("SharedAnnotation", params.id)
        }
    }

    /**
     * List all comments for an annotation
     */
    @RestApiMethod(description = "Get all comments on annotation", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "annotation", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id")
    ])
    def listComments() {
        UserAnnotation annotation = userAnnotationService.read(params.long('annotation'))
        if (annotation) {
            responseSuccess(sharedAnnotationService.listComments(annotation))
        } else {
            responseNotFound("Annotation", params.id)
        }
    }

    /**
     * Get a single annotation
     */
    @RestApiMethod(description = "Get a user annotation")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id")
    ])
    def show() {
        UserAnnotation annotation = userAnnotationService.read(params.long('id'))
        if (annotation) {
            responseSuccess(annotation)
        } else responseNotFound("Annotation", params.id)
    }


    /**
     * Add annotation created by user
     */
    @RestApiMethod(description = "Add an annotation created by user")
    @RestApiParams(params = [
            @RestApiParam(name = "POST JSON: project", type = "long", paramType = RestApiParamType.PATH, description = "The project id where this annotation belongs"),
            @RestApiParam(name = "POST JSON: image", type = "long", paramType = RestApiParamType.QUERY, description = "The image instance id where this annotation belongs"),
            @RestApiParam(name = "POST JSON: location", type = "string", paramType = RestApiParamType.QUERY, description = "The WKT geometrical description of the annotation"),
            @RestApiParam(name = "POST JSON: term", type = "long", paramType = RestApiParamType.QUERY, required = false, description = "Term id to associate with this annotation"),
            @RestApiParam(name = "POST JSON: minPoint", type = "int", paramType = RestApiParamType.QUERY, required = false, description = "Minimum number of point that constitute the annotation"),
            @RestApiParam(name = "POST JSON: maxPoint", type = "int", paramType = RestApiParamType.QUERY, required = false, description = "Maximum number of point that constitute the annotation")
    ])
    def add() {
        if(request.JSON instanceof JSONObject) {
            if(params.minPoint && !(params.minPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.put("minPoint", params.minPoint)
            if(params.maxPoint && !(params.maxPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.put("maxPoint", params.maxPoint)
        } else if(request.JSON instanceof JSONArray) {
            request.JSON.each {println it}
            if(params.minPoint && !(params.minPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.each{it.put("minPoint", params.minPoint)}
            if(params.maxPoint && !(params.maxPoint instanceof org.codehaus.groovy.runtime.NullObject)) request.JSON.each{it.put("maxPoint", params.maxPoint)}
        }
        add(userAnnotationService, request.JSON)
    }

    Object addOne(def service, def json) {
        if (json.isNull('location')) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + json.location)
        }

        def minPoint = json.minPoint
        def maxPoint = json.maxPoint

        def result = userAnnotationService.add(json, minPoint, maxPoint)
        return result
    }

    /**
     * Update annotation created by user
     */
    @RestApiMethod(description = "Update an annotation")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id")
    ])
    def update() {
        update(userAnnotationService, request.JSON)
    }

    /**
     * Delete annotation created by user
     */
    @RestApiMethod(description = "Delete an annotation")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id")
    ])
    def delete() {
        delete(userAnnotationService, JSON.parse("{id : $params.id}"), null)
    }

    def repeat() {
        UserAnnotation annotation = userAnnotationService.read(params.long("id"))
        if (annotation) {
            def repeat = JSONUtils.getJSONAttrInteger(request.JSON,'repeat',1)
            def slice = JSONUtils.getJSONAttrInteger(request.JSON, 'slice', null)
            responseSuccess(userAnnotationService.repeat(annotation, slice, repeat))
        } else {
            responseNotFound("UserAnnotation", params.id)
        }
    }

    @RestApiMethod(description = "Get a crop of a user annotation (image area framing annotation)", extensions = ["png", "jpg", "tiff"])
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name = "type", type = "String", paramType = RestApiParamType.QUERY, description = "Type of crop. Allowed values are 'crop' (default behavior if not set), 'draw' (the shape is drawn in the crop), 'mask' (annotation binary mask), 'alphaMask (part of crop outside annotation is transparent, requires png format)", required = false),
            @RestApiParam(name = "draw", type = "boolean", paramType = RestApiParamType.QUERY, description = "Equivalent to set type='draw'", required = false),
            @RestApiParam(name = "mask", type = "boolean", paramType = RestApiParamType.QUERY, description = "Equivalent to set type='mask'", required = false),
            @RestApiParam(name = "alphaMask", type = "boolean", paramType = RestApiParamType.QUERY, description = "Equivalent to set type='alphaMask'", required = false),
            @RestApiParam(name = "maxSize", type = "int", paramType = RestApiParamType.QUERY, description = "Maximum crop size in width and height", required = false),
            @RestApiParam(name = "zoom", type = "int", paramType = RestApiParamType.QUERY, description = "Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name = "increaseArea", type = "double", paramType = RestApiParamType.QUERY, description = "Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name = "complete", type = "boolean", paramType = RestApiParamType.QUERY, description = "Do not simplify the annotation shape.", required = false),
            @RestApiParam(name = "colormap", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required = false),
            @RestApiParam(name = "inverse", type = "int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required = false),
            @RestApiParam(name = "contrast", type = "float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required = false),
            @RestApiParam(name = "gamma", type = "float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required = false),
            @RestApiParam(name = "bits", type = "int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required = false)
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def crop() {
        UserAnnotation annotation = UserAnnotation.read(params.long("id"))
        if (annotation) {
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("UserAnnotation", params.id)
        }

    }

    @RestApiMethod(description = "Get a binary mask of a user annotation (image area framing annotation). Equivalent to crop with 'mask' type.", extensions = ["png", "jpg", "tiff"])
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name = "maxSize", type = "int", paramType = RestApiParamType.QUERY, description = "Maximum crop size in width and height", required = false),
            @RestApiParam(name = "zoom", type = "int", paramType = RestApiParamType.QUERY, description = "Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name = "increaseArea", type = "double", paramType = RestApiParamType.QUERY, description = "Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name = "complete", type = "boolean", paramType = RestApiParamType.QUERY, description = "Do not simplify the annotation shape.", required = false),
            @RestApiParam(name = "colormap", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required = false),
            @RestApiParam(name = "inverse", type = "int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required = false),
            @RestApiParam(name = "contrast", type = "float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required = false),
            @RestApiParam(name = "gamma", type = "float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required = false),
            @RestApiParam(name = "bits", type = "int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required = false)
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def cropMask() {
        UserAnnotation annotation = UserAnnotation.read(params.long("id"))
        if (annotation) {
            params.mask = true
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("UserAnnotation", params.id)
        }
    }

    @RestApiMethod(description = "Get an alpha mask of a user annotation (image area framing annotation). Equivalent to crop with 'alphaMask' type.", extensions = ["png"])
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name = "maxSize", type = "int", paramType = RestApiParamType.QUERY, description = "Maximum crop size in width and height", required = false),
            @RestApiParam(name = "zoom", type = "int", paramType = RestApiParamType.QUERY, description = "Zoom level in which crop is extracted. Ignored if maxSize is set.", required = false),
            @RestApiParam(name = "increaseArea", type = "double", paramType = RestApiParamType.QUERY, description = "Increase crop area by multiplying original crop size by this factor.", required = false),
            @RestApiParam(name = "complete", type = "boolean", paramType = RestApiParamType.QUERY, description = "Do not simplify the annotation shape.", required = false),
            @RestApiParam(name = "colormap", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required = false),
            @RestApiParam(name = "inverse", type = "int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required = false),
            @RestApiParam(name = "contrast", type = "float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required = false),
            @RestApiParam(name = "gamma", type = "float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required = false),
            @RestApiParam(name = "bits", type = "int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required = false)
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def cropAlphaMask() {
        UserAnnotation annotation = UserAnnotation.read(params.long("id"))
        if (annotation) {
            params.alphaMask = true
            responseByteArray(imageServerService.crop(annotation, params))
        } else {
            responseNotFound("UserAnnotation", params.id)
        }
    }
}
