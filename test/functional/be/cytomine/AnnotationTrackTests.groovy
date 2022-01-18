package be.cytomine

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

import be.cytomine.ontology.AnnotationTrack
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationTrackAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotationTrackTests {

    void testGetAnnotationTrackWithCredential() {
        AnnotationTrack annotationTrackToAdd = BasicInstanceBuilder.getAnnotationTrack()
        def result = AnnotationTrackAPI.showAnnotationTrack(annotationTrackToAdd.annotationIdent,annotationTrackToAdd.track.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListAnnotationTrackByAnnotationWithCredential() {
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrack()
        def result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int size = json.collection.size()
        AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.collection.size()+1
    }

    void testListAnnotationTrackByUserNotWithCredential() {
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrack()
        def result = AnnotationTrackAPI.listAnnotationTrackByTrack(annotationTrack.track.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int size = json.collection.size()
        AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        result = AnnotationTrackAPI.listAnnotationTrackByTrack(annotationTrack.track.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.collection.size()+1
    }

    void testAddAnnotationTrackCorrect() {
        User currentUser = User.findByUsername(Infos.SUPERADMINLOGIN)
        def annotationTrackToAdd = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrackToAdd.discard()
        String jsonAnnotationTrack = annotationTrackToAdd.encodeAsJSON()
        def result = AnnotationTrackAPI.createAnnotationTrack(jsonAnnotationTrack,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        AnnotationTrack annotationTrack = result.data
        Long idAnnotation = annotationTrack.annotationIdent
        Long idTerm = annotationTrack.track.id
        log.info("check if object "+ annotationTrack.annotationIdent +"/"+ annotationTrack.track.id +"exist in DB")

        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationTrackAPI.undo()
        assert 200 == result.code

        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = AnnotationTrackAPI.redo()
        assert 200 == result.code

        log.info("check if object "+ idAnnotation +"/"+ idTerm +" exist in DB")
        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

    }

    void testAddAnnotationTrackAlreadyExist() {
        def annotationTrackToAdd = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrackToAdd.save(flush:true)
        //annotationTrackToAdd is in database, we will try to add it twice
        String jsonAnnotationTrack = annotationTrackToAdd.encodeAsJSON()
        def result = AnnotationTrackAPI.createAnnotationTrack(jsonAnnotationTrack,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testAddAnnotationTrackOnSameTrackAndAnnotation() {
        def annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.save(flush:true)

        def annotationTrackToAdd = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrackToAdd.annotationIdent = annotationTrack.annotationIdent
        annotationTrackToAdd.annotationClassName = annotationTrack.annotationClassName
        annotationTrackToAdd.track = annotationTrack.track
        String jsonAnnotationTrack = annotationTrackToAdd.encodeAsJSON()
        def result = AnnotationTrackAPI.createAnnotationTrack(jsonAnnotationTrack,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testAddAnnotationTrackWithAnnotationNotExist() {
        def annotationTrackAdd = BasicInstanceBuilder.getAnnotationTrackNotExist()
        String jsonAnnotationTrack = annotationTrackAdd.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonAnnotationTrack)
        jsonUpdate.annotationIdent = -99
        jsonAnnotationTrack = jsonUpdate.toString()
        def result = AnnotationTrackAPI.createAnnotationTrack(jsonAnnotationTrack,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddAnnotationTrackWithTrackNotExist() {
        def annotationTrackAdd = BasicInstanceBuilder.getAnnotationTrackNotExist()
        String jsonAnnotationTrack = annotationTrackAdd.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonAnnotationTrack)
        jsonUpdate.track = -99
        jsonAnnotationTrack = jsonUpdate.toString()
        def result = AnnotationTrackAPI.createAnnotationTrack(jsonAnnotationTrack,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testDeleteAnnotationTrack() {
        def annotationTrackToDelete = BasicInstanceBuilder.getAnnotationTrackNotExist(true)
        int idAnnotation = annotationTrackToDelete.annotationIdent
        int idTerm = annotationTrackToDelete.track.id

        def result = AnnotationTrackAPI.deleteAnnotationTrack(idAnnotation,idTerm,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = AnnotationTrackAPI.undo()
        assert 200 == result.code

        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationTrackAPI.redo()
        assert 200 == result.code

        result = AnnotationTrackAPI.showAnnotationTrack(idAnnotation,idTerm,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

    }

    void testDeleteAnnotationTrackNotExist() {
        def result = AnnotationTrackAPI.deleteAnnotationTrack(-99,-99,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
