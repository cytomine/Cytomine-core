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

import be.cytomine.image.AbstractImage
import be.cytomine.image.AbstractSlice
import be.cytomine.image.UploadedFile
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractSliceAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class AbstractSliceTests {


    void testListByAbstractImage() {
        AbstractImage ai = BasicInstanceBuilder.getAbstractImage()
        def result = AbstractSliceAPI.listByAbstractImage(ai.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListByAbstractImageNoExist() {
        def result = AbstractSliceAPI.listByAbstractImage(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListByAbstractImageWithoutCredential() {
        AbstractImage ai = BasicInstanceBuilder.getAbstractImage()
        def result = AbstractSliceAPI.listByAbstractImage(ai.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testListByUploadedFile() {
        UploadedFile uf = BasicInstanceBuilder.getUploadedFile()
        def result = AbstractSliceAPI.listByUploadedFile(uf.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListByUploadedFileNoExist() {
        def result = AbstractSliceAPI.listByUploadedFile(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testGetByAbstractImageAndCoordinates() {
        AbstractImage image = BasicInstanceBuilder.getAbstractImage()
        AbstractSlice slice = BasicInstanceBuilder.getAbstractSliceNotExist(image, true)
        def result = AbstractSliceAPI.getByAbstractImageAndCoordinates(image.id, slice.channel, slice.zStack, slice.time, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testGetAbstractSliceWithCredential() {
        AbstractSlice slice = BasicInstanceBuilder.getAbstractSlice()
        def result = AbstractSliceAPI.show(slice.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testGetUploaderOfImage() {
        AbstractSlice slice = BasicInstanceBuilder.getAbstractSlice()
        def result = AbstractSliceAPI.showUploaderOfImage(slice.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddSliceCorrect() {
        def sliceToAdd = BasicInstanceBuilder.getAbstractSliceNotExist()
        String json = sliceToAdd.encodeAsJSON()
        println "encodeAsJSON="+json
        def result = AbstractSliceAPI.create(json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddSliceWithUnexistingImage() {
        def sliceToAdd = BasicInstanceBuilder.getAbstractSliceNotExist()
        def json = JSON.parse(sliceToAdd.encodeAsJSON())
        json.image = -99
        println "encodeAsJSON="+json
        def result = AbstractSliceAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testEditSlice() {
        def slice = BasicInstanceBuilder.getAbstractSliceNotExist(true)
        def data = UpdateData.createUpdateSet(
                slice,
                [time:[0,2],
                 channel:[3,9]]
        )

        def result = AbstractSliceAPI.update(slice.id,data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int id = json.abstractslice.id
        def showResult = AbstractSliceAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testDeleteSlice()  {
        def sliceToDelete = BasicInstanceBuilder.getAbstractSlice()
        Long id = sliceToDelete.id
        def result = AbstractSliceAPI.delete(id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        println "testDeleteSlice=" +result
        assert 200 == result.code
        result = AbstractSliceAPI.show(id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteSliceNoExist()  {
        def result = AbstractSliceAPI.delete(-99,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}