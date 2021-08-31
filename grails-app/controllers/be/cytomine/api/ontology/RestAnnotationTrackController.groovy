package be.cytomine.api.ontology

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

import be.cytomine.AnnotationDomain
import be.cytomine.api.RestController
import be.cytomine.ontology.AnnotationTrack
import be.cytomine.ontology.Track
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ

@RestApi(name = "Ontology | annotation track services", description = "Methods for managing annotation tracks.")
class RestAnnotationTrackController extends RestController {

    def termService
    def annotationTrackService
    def userAnnotationService
    def algoAnnotationService
    def cytomineService
    def reviewedAnnotationService
    def securityACLService
    def trackService

    @RestApiMethod(description="Get all annotationTrack for a track", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The track id"),
    ])
    def listByTrack() {
        Track track = trackService.read(params.long('id'))
        if (track) {
            responseSuccess(annotationTrackService.list(track))
        } else {
            responseNotFound("AnnotationTrack", "Track", params.id)
        }
    }

    @RestApiMethod(description="Get all annotationTrack for an annotation", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The annotation id"),
    ])
    def listByAnnotation() {
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(params.long("id"))
        if (annotation) {
            responseSuccess(annotationTrackService.list(annotation))
        } else {
            responseNotFound("AnnotationTrack", "Annotation", params.id)
        }
    }


    @RestApiMethod(description="Get an annotation track")
    @RestApiParams(params=[
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name="track", type="long", paramType = RestApiParamType.PATH, description = "The track id"),
    ])
    def show() {
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(params.long('annotation'))
        Track track = trackService.read(params.long('track'))

        if (!annotation) {
            responseNotFound("Annotation", params.idannotation)
        }

        if (!track) {
            responseNotFound("Term", params.idterm)
        }

        AnnotationTrack at = annotationTrackService.read(annotation, track)
        if (at) {
            responseSuccess(at)
        }
        else {
            responseNotFound("AnnotationTrack", "Annotation", "Track", annotation.id, track.id)
        }
    }
    
    @RestApiMethod(description="Get an annotation-track relation")
    def add() {
        add(annotationTrackService, request.JSON)
    }
    
    
    @RestApiMethod(description="Remove an annotation from a track")
    @RestApiParams(params=[
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.PATH, description = "The annotation id"),
            @RestApiParam(name="track", type="long", paramType = RestApiParamType.PATH, description = "The track id")
    ])
    def delete() {
        def json = JSON.parse("{annotationIdent: $params.annotation, track: $params.track}")
        delete(annotationTrackService, json,null)
    }
}