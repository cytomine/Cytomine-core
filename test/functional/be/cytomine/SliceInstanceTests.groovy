package be.cytomine

import be.cytomine.image.ImageInstance

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

import be.cytomine.image.SliceInstance
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.SliceInstanceAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class SliceInstanceTests {


    void testListSlicesByImageInstance() {
        def result = SliceInstanceAPI.listByImageInstance(BasicInstanceBuilder.getImageInstance().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = SliceInstanceAPI.listByImageInstance(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testGetSliceInstanceWithCredential() {
        def result = SliceInstanceAPI.show(BasicInstanceBuilder.getSliceInstance().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testGetByImageInstanceAndCoordinates() {
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true))
        BasicInstanceBuilder.saveDomain(image)

        def result = SliceInstanceAPI.getByImageInstanceAndCoordinates(image.id, 0, 0, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        SliceInstance slice = BasicInstanceBuilder.getSliceInstance()

        result = SliceInstanceAPI.getByImageInstanceAndCoordinates(slice.image.id, slice.baseSlice.channel, slice.baseSlice.zStack ,slice.baseSlice.time, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddSliceInstanceCorrect() {

        def result = SliceInstanceAPI.create(BasicInstanceBuilder.getSliceInstanceNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        SliceInstance slice = result.data
        Long idSlice = slice.id

        result = SliceInstanceAPI.show(idSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = SliceInstanceAPI.undo()
        assert 200 == result.code

        result = SliceInstanceAPI.show(idSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = SliceInstanceAPI.redo()
        assert 200 == result.code

        result = SliceInstanceAPI.show(idSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddSliceInstanceAlreadyExist() {
        def sliceToAdd = BasicInstanceBuilder.getSliceInstance()
        def result = SliceInstanceAPI.create(sliceToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testAddSliceInstanceWithUnexistingImageInstance() {
        def sliceToAdd = BasicInstanceBuilder.getSliceInstanceNotExist()
        String jsonSlice = sliceToAdd.encodeAsJSON()
        def updateSlice = JSON.parse(jsonSlice)
        updateSlice.image = -99
        jsonSlice = updateSlice.toString()
        def result = ImageInstanceAPI.create(jsonSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddSliceInstanceWithUnexistingProject() {
        def sliceToAdd = BasicInstanceBuilder.getSliceInstance()
        String jsonSlice = sliceToAdd.encodeAsJSON()
        def updateSlice = JSON.parse(jsonSlice)
        updateSlice.project = -99
        jsonSlice = updateSlice.toString()
        def result = SliceInstanceAPI.create(jsonSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testEditSliceInstance() {


        def slice = BasicInstanceBuilder.getSliceInstance()
        def data = UpdateData.createUpdateSet(slice,[project: [BasicInstanceBuilder.getProject(),BasicInstanceBuilder.getProjectNotExist(true)]])

        def result = SliceInstanceAPI.update(slice.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idSliceInstance = json.sliceinstance.id
        def showResult = SliceInstanceAPI.show(idSliceInstance, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = SliceInstanceAPI.undo()
        assert 200==showResult.code

        showResult = SliceInstanceAPI.show(idSliceInstance, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)

        BasicInstanceBuilder.compare(data.mapOld, json)

        showResult = SliceInstanceAPI.redo()
        assert 200==showResult.code

        showResult = SliceInstanceAPI.show(idSliceInstance, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testEditSliceInstanceWithBadProject() {
        SliceInstance imageToEdit = BasicInstanceBuilder.getSliceInstance()
        def jsonUpdate = JSON.parse(imageToEdit.encodeAsJSON())
        jsonUpdate.project = -99
        def result = SliceInstanceAPI.update(imageToEdit.id, jsonUpdate.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testEditSliceInstanceWithBadImage() {
        SliceInstance imageToEdit = BasicInstanceBuilder.getSliceInstance()
        def jsonImage = imageToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonImage)
        jsonUpdate.image = -99
        def result = SliceInstanceAPI.update(imageToEdit.id, jsonUpdate.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code

    }

    void testDeleteSliceInstance() {
        def sliceInstanceToDelete = BasicInstanceBuilder.getSliceInstanceNotExist()
        assert sliceInstanceToDelete.save(flush: true) != null
        def idSlice = sliceInstanceToDelete.id
        println "Slice=${sliceInstanceToDelete.id} ${sliceInstanceToDelete.deleted}"

        def result = SliceInstanceAPI.delete(sliceInstanceToDelete, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        sliceInstanceToDelete.refresh()
        println "Slice=${sliceInstanceToDelete.id} ${sliceInstanceToDelete.deleted}"
        def showResult = SliceInstanceAPI.show(sliceInstanceToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = SliceInstanceAPI.undo()
        assert 200 == result.code

        result = SliceInstanceAPI.show(idSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = SliceInstanceAPI.redo()
        assert 200 == result.code

        result = SliceInstanceAPI.show(idSlice, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteSliceInstanceNoExist() {
        def sliceInstanceToDelete = BasicInstanceBuilder.getSliceInstanceNotExist()
        def result = SliceInstanceAPI.delete(sliceInstanceToDelete, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
