package be.cytomine.security

import be.cytomine.image.ImageInstance

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

import be.cytomine.image.SliceInstance
import be.cytomine.meta.Description
import be.cytomine.meta.Property
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.JobData
import be.cytomine.processing.Software
import be.cytomine.image.server.Storage
import be.cytomine.image.UploadedFile
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import grails.converters.JSON

class StorageSecurityTests extends SecurityTestsAbstract {

    void testStorageSecurityForCytomineAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data
        Infos.printRight(storage)
        Infos.printUserRight(user1)
        Infos.printUserRight(admin)
        //check if admin user can access/update/delete
        assert (200 == StorageAPI.show(storage.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.listAll(USERNAMEADMIN,PASSWORDADMIN).data)))
        assert (200 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)

        assert (200 == StorageAPI.delete(storage.id,USERNAMEADMIN,PASSWORDADMIN).code)


        result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        storage = result.data
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAMEADMIN, PASSWORDADMIN)
        assert 200 == result.code

    }

    void testStorageSecurityForStorageCreator() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data

        println "STORAGE="+storage.deleted

        //check if user 1 can access/update/delete
        assert (200 == StorageAPI.show(storage.id,USERNAME1,PASSWORD1).code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME1,PASSWORD1).data)))
        assert (200 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == StorageAPI.delete(storage.id,USERNAME1,PASSWORD1).code)

        result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        storage = result.data
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testStorageSecurityForStorageAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUserToStorage(storage.id,user2.id,"ADMINISTRATION",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(storage)
        //check if user 2 can access/update/delete
        assert (200 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        assert (200 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)

        result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        storage = result.data
        resAddUser = StorageAPI.addUsersToStorage(storage.id,[user2.id],"ADMINISTRATION",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 200 == result.code

    }

    void testStorageSecurityForStorageWriteUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUserToStorage(storage.id,user2.id,"WRITE",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(storage)
        //check if user 2 can access/update/delete
        assert (403 == StorageAPI.addUserToStorage(storage.id,user1.id,"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.addUsersToStorage(storage.id,[user1.id, user2.id],"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUserFromStorage(storage.id,user1.id,USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUsersFromStorage(storage.id,[user1.id, user2.id],USERNAME2,PASSWORD2).code)
        assert (200 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)

        //remove right to user2
        resAddUser = StorageAPI.deleteUserFromStorage(storage.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(storage)
        //check if user 2 cannot access/update/delete
        assert (403 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (false == StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)

        result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        storage = result.data
        resAddUser = StorageAPI.addUserToStorage(storage.id,user2.id,"WRITE",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 200 == result.code
    }

    void testStorageSecurityForStorageReadOnlyUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUserToStorage(storage.id,user2.id,"READ",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(storage)
        //check if user 2 can access/update/delete
        assert (403 == StorageAPI.addUserToStorage(storage.id,user1.id,"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.addUsersToStorage(storage.id,[user1.id, user2.id],"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUserFromStorage(storage.id,user1.id,USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUsersFromStorage(storage.id,[user1.id, user2.id],USERNAME2,PASSWORD2).code)
        assert (200 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)


        //remove right to user2
        resAddUser = StorageAPI.deleteUserFromStorage(storage.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(storage)
        //check if user 2 cannot access/update/delete
        assert (403 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (false == StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)

        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 403 == result.code
    }

    void testStorageSecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data
        Infos.printRight(storage)
        //check if user 2 cannot access/update/delete
        assert (403 == StorageAPI.addUserToStorage(storage.id,user1.id,"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.addUsersToStorage(storage.id,[user1.id, user2.id],"ADMINISTRATION",USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUserFromStorage(storage.id,user1.id,USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.deleteUsersFromStorage(storage.id,[user1.id, user2.id],USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert(false==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list(USERNAME2,PASSWORD2).data)))
        Infos.printRight(storage)
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.delete(storage.id,USERNAME2,PASSWORD2).code)

        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 403 == result.code

    }

    void testStorageSecurityForGhestUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get ghest
        User ghest = BasicInstanceBuilder.getGhest("GHESTONTOLOGY","PASSWORD")

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUserToStorage(storage.id,ghest.id,"READ",USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(storage)
        //check if user 2 can access/update/delete
        assert (200 == StorageAPI.show(storage.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (true ==StorageAPI.containsInJSONList(storage.id,JSON.parse(StorageAPI.list("GHESTONTOLOGY","PASSWORD").data)))
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == StorageAPI.delete(storage.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
    }




    void testStorageSecurityForAnonymous() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Storage storage = result.data
        Infos.printRight(storage)
        //check if user 2 cannot access/update/delete
        assert (401 == StorageAPI.show(storage.id,USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == StorageAPI.list(USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == StorageAPI.delete(storage.id,USERNAMEBAD,PASSWORDBAD).code)
    }


    void testStorageMultipleAddRead() {
        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUsersToStorage(storage.id,[user1.id,user2.id],"READ",USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == resAddUser.code

        assert (200 == StorageAPI.show(storage.id,USERNAME1,PASSWORD1).code)
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME1,PASSWORD1).code)

        assert (200 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)

        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 403 == result.code

        uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 403 == result.code

    }


    void testStorageMultipleAddWrite() {
        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUsersToStorage(storage.id,[user1.id,user2.id],"WRITE",USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == resAddUser.code

        assert (200 == StorageAPI.show(storage.id,USERNAME1,PASSWORD1).code)
        assert (200 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)

        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code

        uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileToAdd.storage = storage
        result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert 200 == result.code

    }

    void testStorageMultipleRemoveUsers() {
        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new storage (user1)
        def result = StorageAPI.create(BasicInstanceBuilder.getStorageNotExist().encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == result.code
        Storage storage = result.data

        //Add right to user2
        def resAddUser = StorageAPI.addUsersToStorage(storage.id,[user1.id,user2.id],"WRITE",USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == resAddUser.code

        def resDelUser = StorageAPI.deleteUsersFromStorage(storage.id,[user1.id,user2.id],USERNAMEADMIN,PASSWORDADMIN)
        assert 200 == resDelUser.code

        assert (403 == StorageAPI.show(storage.id,USERNAME1,PASSWORD1).code)
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME1,PASSWORD1).code)

        assert (403 == StorageAPI.show(storage.id,USERNAME2,PASSWORD2).code)
        assert (403 == StorageAPI.update(storage.id,storage.encodeAsJSON(),USERNAME2,PASSWORD2).code)



    }
}
