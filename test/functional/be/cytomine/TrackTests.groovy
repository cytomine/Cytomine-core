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

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.Track
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.TrackAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TrackTests {


    void testListTrackByImageInstanceWithCredential() {
        ImageInstance image = BasicInstanceBuilder.getImageInstance()
        Track track = BasicInstanceBuilder.getTrackNotExist(image, true)
        def result = TrackAPI.listByImageInstance(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int size = json.collection.size()

        TrackAPI.delete(track.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        result = TrackAPI.listByImageInstance(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.collection.size()+1
    }

    void testListTrackByImageInstanceWithImageNotExist() {
        def result = TrackAPI.listByImageInstance(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListTrackByProjectWithCredential() {
        Project project = BasicInstanceBuilder.getProject()
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstance(), true)
        def result = TrackAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int size = json.collection.size()

        TrackAPI.delete(track.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        result = TrackAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.collection.size()+1
    }

    void testListTrackByProjectWithProjectNotExist() {
        def result = TrackAPI.listByProject(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testShowTrackWithCredential() {
        def result = TrackAPI.show(BasicInstanceBuilder.getTrack().id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddTrackCorrect() {
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist()
        def result = TrackAPI.create(trackToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idTrack = result.data.id

        result = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = TrackAPI.undo()
        assert 200 == result.code

        result = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = TrackAPI.redo()
        assert 200 == result.code

        result = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddTrackAlreadyExist() {
        def trackToAdd = BasicInstanceBuilder.getTrack()
        def data = (trackToAdd as JSON).toString()
        def result = TrackAPI.create((trackToAdd as JSON).toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        assert 409 == result.code
    }

    void testUpdateTrackCorrect() {
        Track track = BasicInstanceBuilder.getTrack()
        def data = UpdateData.createUpdateSet(track,[name: ["OLDNAME","NEWNAME"]])
        def result = TrackAPI.update(track.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idTrack = json.track.id

        def showResult = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = TrackAPI.undo()
        assert 200 == showResult.code
        showResult = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = TrackAPI.redo()
        assert 200 == showResult.code
        showResult = TrackAPI.show(idTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testUpdateTrackNotExist() {
        Track trackWithOldName = BasicInstanceBuilder.getTrack()
        Track trackWithNewName = BasicInstanceBuilder.getTrackNotExist()
        trackWithNewName.save(flush: true)
        Track trackToEdit = Track.get(trackWithNewName.id)
        def jsonTrack = trackToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonTrack)
        jsonUpdate.name = trackWithOldName.name
        jsonUpdate.id = -99
        jsonTrack = jsonUpdate.toString()
        def result = TrackAPI.update(-99, jsonTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testUpdateTrackWithNameAlreadyExist() {
        Track trackWithOldName = BasicInstanceBuilder.getTrack()
        Track trackWithNewName = BasicInstanceBuilder.getTrackNotExist()
        trackWithNewName.image = trackWithOldName.image
        trackWithNewName.save(flush: true)
        Track trackToEdit = Track.get(trackWithNewName.id)
        def jsonTrack = trackToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonTrack)
        jsonUpdate.name = trackWithOldName.name
        jsonTrack = jsonUpdate.toString()
        def result = TrackAPI.update(trackToEdit.id, jsonTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testEditTrackWithBadName() {
        Track trackToAdd = BasicInstanceBuilder.getTrack()
        Track trackToEdit = Track.get(trackToAdd.id)
        def jsonTrack = trackToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonTrack)
        jsonUpdate.name = null
        jsonTrack = jsonUpdate.toString()
        def result = TrackAPI.update(trackToAdd.id, jsonTrack, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testDeleteTrack() {
        def trackToDelete = BasicInstanceBuilder.getTrackNotExist()
        assert trackToDelete.save(flush: true)!= null
        def id = trackToDelete.id
        def result = TrackAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = TrackAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = TrackAPI.undo()
        assert 200 == result.code

        result = TrackAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = TrackAPI.redo()
        assert 200 == result.code

        result = TrackAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteTrackNotExist() {
        def result = TrackAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
