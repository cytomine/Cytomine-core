package be.cytomine.search

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

import be.cytomine.image.UploadedFile
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.UploadedFileAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class UploadedFileSearchTests {

    void testListUploadedFilePagination() {

        //creation
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        def result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFile = result.data.id

        UploadedFile uploadedfileChildToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd.parent = UploadedFile.get(idUploadedFile)
        UploadedFileAPI.create(uploadedfileChildToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        UploadedFile uploadedfileChildToAdd2 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd2.parent = UploadedFile.get(idUploadedFile)
        UploadedFileAPI.create(uploadedfileChildToAdd2.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        UploadedFile uploadedfileToAdd2 = BasicInstanceBuilder.getUploadedFileNotExist()
        UploadedFileAPI.create(uploadedfileToAdd2.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        //fetching with pagination

        result = UploadedFileAPI.listOnlyRoots(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = UploadedFileAPI.listOnlyRoots(1,0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UploadedFileAPI.listOnlyRoots(1,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        result = UploadedFileAPI.listChilds(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size > 1
        id1 = json.collection[0].id
        id2 = json.collection[1].id

        result = UploadedFileAPI.listChilds(idUploadedFile, 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UploadedFileAPI.listChilds(idUploadedFile, 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        result = UploadedFileAPI.hierarchicalList(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size > 1
        id1 = json.collection[0].id
        id2 = json.collection[1].id

        result = UploadedFileAPI.hierarchicalList(idUploadedFile, 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UploadedFileAPI.hierarchicalList(idUploadedFile, 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        result = UploadedFileAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size > 1
        id1 = json.collection[0].id
        id2 = json.collection[1].id

        result = UploadedFileAPI.list(1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UploadedFileAPI.list(1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == size
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

    void testSearchUploadedFileByName() {
        String name = "test"
        def result = UploadedFileAPI.searchWithName(name, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int previousSize = json.collection.size()

        UploadedFile uf = BasicInstanceBuilder.getUploadedFileNotExist()
        uf.originalFilename = "test"
        uf.save(true)
        uf = BasicInstanceBuilder.getUploadedFileNotExist()
        uf.originalFilename = "random"
        uf.save(true)

        result = UploadedFileAPI.searchWithName(name, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == previousSize + 1
    }

    //sort
    void testSortUploadedFile() {

        //creation
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        def result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFile = result.data.id

        UploadedFile uploadedfileChildToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd.parent = UploadedFile.get(idUploadedFile)
        UploadedFileAPI.create(uploadedfileChildToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        UploadedFile uploadedfileChildToAdd2 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd2.parent = UploadedFile.get(idUploadedFile)
        uploadedfileChildToAdd2.size += 200
        uploadedfileChildToAdd2.originalFilename += "s"
        uploadedfileChildToAdd2.status = 9
        UploadedFileAPI.create(uploadedfileChildToAdd2.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        UploadedFile uploadedfileToAdd2 = BasicInstanceBuilder.getUploadedFileNotExist()
        UploadedFileAPI.create(uploadedfileToAdd2.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        //sorts

        result = UploadedFileAPI.listOnlyRoots("created", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        Long id1 = json.collection[0].id
        Long id2 = json.collection[-1].id

        result = UploadedFileAPI.listOnlyRoots("created", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert json.collection[0].id == id2
        assert json.collection[-1].id == id1


        result = UploadedFileAPI.listChilds(idUploadedFile, "originalFilename", "asc" , Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        id1 = json.collection[0].id
        id2 = json.collection[-1].id

        result = UploadedFileAPI.listChilds(idUploadedFile, "originalFilename", "desc" , Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert json.collection[0].id == id2
        assert json.collection[-1].id == id1


        result = UploadedFileAPI.list("size", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1


        long sizeU = json.collection[0].size

        result = UploadedFileAPI.list("size", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert sizeU != json.collection[0].size


        result = UploadedFileAPI.list("contentType", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1

        result = UploadedFileAPI.list("contentType", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        result = UploadedFileAPI.list(true, "globalSize", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        long greaterSizeId = json.collection[0].id

        result = UploadedFileAPI.list(true, "globalSize", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert greaterSizeId != json.collection[0].id


        long status
        result = UploadedFileAPI.list("status", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        status = json.collection[0].status

        result = UploadedFileAPI.list("status", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert status != json.collection[0].status


        result = UploadedFileAPI.list("parentFilename", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1

        result = UploadedFileAPI.list("parentFilename", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

}
