package be.cytomine.security

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
import be.cytomine.test.http.UploadedFileAPI

class UploadedFileSecurityTests extends SecurityTestsAbstract {


    void testUploadedFileSecurityForCytomineAdmin() {

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create new uploadedFile (user1)
        def result = UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist(user1).encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        UploadedFile uploadedFile = result.data
        //check if admin user can access/update/delete
        assert (200 == UploadedFileAPI.show(uploadedFile.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == UploadedFileAPI.update(uploadedFile.id,uploadedFile.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == UploadedFileAPI.delete(uploadedFile.id,USERNAMEADMIN,PASSWORDADMIN).code)
    }

    void testUploadedFileSecurityForUploadedFileCreator() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new UploadedFile (user1)
        UploadedFile uf = BasicInstanceBuilder.getUploadedFileNotExist(user1);
        uf.user = user1
        def result = UploadedFileAPI.create(uf.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        UploadedFile uploadedFile = result.data

        //check if user 1 can access/update/delete
        assert (200 == UploadedFileAPI.show(uploadedFile.id,USERNAME1,PASSWORD1).code)
        assert (200 == UploadedFileAPI.update(uploadedFile.id,uploadedFile.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == UploadedFileAPI.delete(uploadedFile.id,USERNAME1,PASSWORD1).code)
    }

    void testUploadedFileSecurityForGhest() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User ghest = BasicInstanceBuilder.getGhest("GHEST","PASSWORD")

        //Create new UploadedFile (user2)
        def result = UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist(ghest).encodeAsJSON(),"GHEST","PASSWORD")
        assert 403 == result.code

        //Create new UploadedFile (user1)
        result = UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist(user1).encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        UploadedFile uploadedFile = result.data
        //check if user 2 cannot access/update/delete
        assert (403 == UploadedFileAPI.show(uploadedFile.id,"GHEST","PASSWORD").code)
        assert (403 == UploadedFileAPI.update(uploadedFile.id,uploadedFile.encodeAsJSON(),"GHEST","PASSWORD").code)
        assert (403 == UploadedFileAPI.delete(uploadedFile.id,"GHEST","PASSWORD").code)
        assert (403 == UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist().encodeAsJSON(),"GHEST","PASSWORD").code)

    }


    void testUploadedFileSecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new UploadedFile (user1)
        def result = UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist(user1).encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        UploadedFile uploadedFile = result.data
        //check if user 2 cannot access/update/delete
        assert (403 == UploadedFileAPI.show(uploadedFile.id,USERNAME2,PASSWORD2).code)
        assert (403 == UploadedFileAPI.update(uploadedFile.id,uploadedFile.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == UploadedFileAPI.delete(uploadedFile.id,USERNAME2,PASSWORD2).code)

    }

    void testUploadedFileSecurityForAnonymous() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User ghest = BasicInstanceBuilder.getGhest("GHEST","PASSWORD")

        //Create new UploadedFile (user1)
        def result = UploadedFileAPI.create(BasicInstanceBuilder.getUploadedFileNotExist(user1).encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        UploadedFile uploadedFile = result.data
        //check if user 2 cannot access/update/delete
        assert (401 == UploadedFileAPI.show(uploadedFile.id,USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == UploadedFileAPI.list(USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == UploadedFileAPI.update(uploadedFile.id,uploadedFile.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == UploadedFileAPI.delete(uploadedFile.id,USERNAMEBAD,PASSWORDBAD).code)
    }
}
