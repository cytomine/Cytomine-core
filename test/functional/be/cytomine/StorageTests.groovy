package be.cytomine

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.image.server.Storage
import be.cytomine.image.UploadedFile
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AclAPI
import be.cytomine.test.http.StorageAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Cytomine
 * User: stevben
 * Date: 7/02/13
 * Time: 15:05
 */
public class StorageTests {

    void testListStorageWithCredential() {
        def result = StorageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListStorageWithoutCredential() {
        def result = StorageAPI.list(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testShowStorageWithCredential() {
        def result = StorageAPI.show(BasicInstanceBuilder.getStorage().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListStorageRightsAccess() {
        User user = BasicInstanceBuilder.getUser(BasicInstanceBuilder.getRandomString(), "PASSWORD")
        Storage storage = BasicInstanceBuilder.getStorageNotExist(false)
        storage.user = user
        storage = BasicInstanceBuilder.saveDomain(storage)

        Storage storageWhereUserHasReadAccess = BasicInstanceBuilder.getStorageNotExist(true)

        Storage storageWhereUserNoAccess = BasicInstanceBuilder.getStorageNotExist(true)

        assert 200 == AclAPI.create(storage.class.name, storage.id, user.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, user.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, user.id,"ADMINISTRATION",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        assert 200 == AclAPI.create(storage.class.name, storageWhereUserHasReadAccess.id, user.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        def result = StorageAPI.listRights(user.username, "PASSWORD")
        println result
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 2 == json.collection.size()

        def entry1 = json.collection.find {it.id == storage.id}
        assert entry1 != null
        assert entry1.permission == 'ADMINISTRATION'

        def entry2 = json.collection.find {it.id == storageWhereUserHasReadAccess.id}
        assert entry2 != null
        assert entry2.permission == 'READ'

        def entryNotExpected = json.collection.find {it.id == storageWhereUserNoAccess.id}
        assert entryNotExpected == null
    }


    void testListStorageUserStatsEmptyStorage() {

        Storage storage = BasicInstanceBuilder.getStorageNotExist(true)

        def result = StorageAPI.usersStats(storage.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 1 == json.collection.size() // superadmin
    }

    void testListStorageUserStatsStorageWithUsers() {
        User owner = BasicInstanceBuilder.getUserNotExist(true)
        User userWithFiles = BasicInstanceBuilder.getUserNotExist(true)
        User userWithReadOnly = BasicInstanceBuilder.getUserNotExist(true)
        User userNoStorageRight = BasicInstanceBuilder.getUserNotExist(true)

        Storage storage = BasicInstanceBuilder.getStorageNotExist(false)
        storage.user = owner
        storage = BasicInstanceBuilder.saveDomain(storage)

        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"ADMINISTRATION",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        assert 200 == AclAPI.create(storage.class.name, storage.id, userWithFiles.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, userWithFiles.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        assert 200 == AclAPI.create(storage.class.name, storage.id, userWithReadOnly.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code


        UploadedFile uploadedFile = BasicInstanceBuilder.getUploadedFileNotExist(true)
        uploadedFile.storage = storage
        uploadedFile.user = owner
        uploadedFile.size = 100L
        BasicInstanceBuilder.saveDomain(uploadedFile)

        UploadedFile uploadedFileNotInStorage = BasicInstanceBuilder.getUploadedFileNotExist(true)
        uploadedFileNotInStorage.storage = BasicInstanceBuilder.getStorage()
        uploadedFileNotInStorage.user = owner
        uploadedFileNotInStorage.size = 999L
        BasicInstanceBuilder.saveDomain(uploadedFileNotInStorage)

        (0..9).each {
            UploadedFile uploadedFileForUser1 = BasicInstanceBuilder.getUploadedFileNotExist(true)
            uploadedFileForUser1.storage = storage
            uploadedFileForUser1.user = userWithFiles
            uploadedFileForUser1.size = 100L
            BasicInstanceBuilder.saveDomain(uploadedFileForUser1)
        }

        def result = StorageAPI.usersStats(storage.id, 'creaded', 'asc', 0, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 3 == json.collection.size()

        assert owner.username == json.collection[0].username
        assert 1 == json.collection[0].numberOfFiles
        assert 100 == json.collection[0].totalSize
        assert 'ADMINISTRATION' == json.collection[0].role

        assert userWithFiles.username == json.collection[1].username
        assert 10 == json.collection[1].numberOfFiles
        assert 1000 == json.collection[1].totalSize
        assert 'WRITE' == json.collection[1].role

        assert userWithReadOnly.username == json.collection[2].username
        assert 0 == json.collection[2].numberOfFiles
        assert 0 == json.collection[2].totalSize
        assert 'READ' == json.collection[2].role

        result = StorageAPI.usersStats(storage.id, 'creaded', 'desc', 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 1 == json.collection.size()
        // expect to be the one in the middle (offset 1 / 3)
        assert userWithFiles.username == json.collection[0].username

    }

    void testListStorageUserStatsStorageWithUserRemovedFromStorage() {
        User owner = BasicInstanceBuilder.getUserNotExist(true)
        User userWithFiles = BasicInstanceBuilder.getUserNotExist(true)

        Storage storage = BasicInstanceBuilder.getStorageNotExist(false)
        storage.user = owner
        storage = BasicInstanceBuilder.saveDomain(storage)

        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, owner.id,"ADMINISTRATION",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        assert 200 == AclAPI.create(storage.class.name, storage.id, userWithFiles.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.create(storage.class.name, storage.id, userWithFiles.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code


        UploadedFile uploadedFile = BasicInstanceBuilder.getUploadedFileNotExist(true)
        uploadedFile.storage = storage
        uploadedFile.user = userWithFiles
        uploadedFile.size = 100L
        BasicInstanceBuilder.saveDomain(uploadedFile)

        def result = StorageAPI.usersStats(storage.id, 'creaded', 'asc', 0, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 2 == json.collection.size()

        assert owner.username == json.collection[0].username
        assert owner.username == json.collection[0].username
        assert 0 == json.collection[0].numberOfFiles
        assert 'ADMINISTRATION' == json.collection[0].role

        assert userWithFiles.username == json.collection[1].username
        assert 1 == json.collection[1].numberOfFiles
        assert 100 == json.collection[1].totalSize
        assert 'WRITE' == json.collection[1].role

        assert 200 == AclAPI.delete(storage.class.name, storage.id, userWithFiles.id,"WRITE",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AclAPI.delete(storage.class.name, storage.id, userWithFiles.id,"READ",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

        result = StorageAPI.usersStats(storage.id, 'creaded', 'asc', 0, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 2 == json.collection.size()

        assert userWithFiles.username == json.collection[0].username
        assert owner.username == json.collection[0].username
        assert 0 == json.collection[0].numberOfFiles
        assert 'ADMINISTRATION' == json.collection[0].role

        // user has no longer the right on the storage, but expect to be in the listing with no role
        assert userWithFiles.username == json.collection[1].username
        assert 1 == json.collection[1].numberOfFiles
        assert 100 == json.collection[1].totalSize
        assert null == json.collection[1].role
    }

    void testAddStorageCorrect() {
        def storageToAdd = BasicInstanceBuilder.getStorageNotExist()
        storageToAdd.name = "testAddStorageCorrect"
        def json = storageToAdd.encodeAsJSON()

        def result = StorageAPI.create(json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 200 == result.code
        int idStorage = result.data.id

        result = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        /*result = StorageAPI.undo()
        assert 200 == result.code

        result = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = StorageAPI.redo()
        assert 200 == result.code

        result = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code*/
    }

    void testUpdateStorageCorrect() {
        Storage storage = BasicInstanceBuilder.getStorage()

        def data = UpdateData.createUpdateSet(storage,[name: ["OLDNAME","NEWNAME"]])
        def result = StorageAPI.update(storage.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        println "result : $result"
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idStorage = json.storage.id

        def showResult = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = StorageAPI.undo()
        assert 200 == result.code
        showResult = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = StorageAPI.redo()
        assert 200 == result.code
        showResult = StorageAPI.show(idStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testUpdateStorageNotExist() {
        Storage storageWithOldName = BasicInstanceBuilder.getStorage()
        Storage storageWithNewName = BasicInstanceBuilder.getStorageNotExist(true)

        Storage storageToEdit = Storage.get(storageWithNewName.id)
        def jsonStorage = storageToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonStorage)
        jsonUpdate.name = storageWithOldName.name
        jsonUpdate.id = -99
        jsonStorage = jsonUpdate.toString()
        def result = StorageAPI.update(-99, jsonStorage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteStorage() {
        def storageToDelete = BasicInstanceBuilder.getStorageNotExist(true)
        def id = storageToDelete.id
        def result = StorageAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = StorageAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = StorageAPI.undo()
        assert 200 == result.code

        result = StorageAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        /*result = StorageAPI.redo()
        assert 200 == result.code

        result = StorageAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code*/
    }

    void testStorageNotExist() {
        def result = StorageAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
