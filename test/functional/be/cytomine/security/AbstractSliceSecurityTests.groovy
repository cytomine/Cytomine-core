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

import be.cytomine.image.AbstractImage
import be.cytomine.image.AbstractSlice
import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractSliceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UploadedFileAPI
import grails.converters.JSON

class AbstractSliceSecurityTests extends SecurityTestsAbstract{

    //Rule : the only ones who can interact are the ones with link with the storage where the related abstractimage -> uploadedfile are located

    void testAbstractSliceeSecurityForCytomineAdmin() {
        //Get user1
        User user1 = getUser1()

        //Get admin user
        User admin = getUserAdmin()

        //Add an image
        UploadedFile uf1 = UploadedFileAPI.buildBasicUploadedFile(SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(uf1,true)
        UploadedFile uf2 = BasicInstanceBuilder.getUploadedFileNotExist(true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(uf2,true)

        //Add abstract slices
        AbstractSlice slice1 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice2 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice3 = BasicInstanceBuilder.getAbstractSliceNotExist(image2,true)

        def result = AbstractSliceAPI.listByAbstractImage(image1.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert (AbstractSliceAPI.containsInJSONList(slice1.id,json))
        assert (AbstractSliceAPI.containsInJSONList(slice2.id,json))
        assert !(AbstractSliceAPI.containsInJSONList(slice3.id,json))
        assert 2<=json.size() //may be more image because all images are available for admin (images from previous test)

        result = AbstractSliceAPI.listByAbstractImage(image2.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert !(AbstractSliceAPI.containsInJSONList(slice1.id,json))
        assert !(AbstractSliceAPI.containsInJSONList(slice2.id,json))
        assert (AbstractSliceAPI.containsInJSONList(slice3.id,json))
        assert 1<=json.size() //may be more image because all images are available for admin (images from previous test)

        assert 200 == AbstractSliceAPI.show(slice1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AbstractSliceAPI.show(slice2.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AbstractSliceAPI.show(slice3.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AbstractSliceAPI.create(BasicInstanceBuilder.getAbstractSliceNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
        assert 200 == AbstractSliceAPI.delete(slice1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
    }

    void testAbstractSliceSecurityForCytomineUser() {
        //Get user1
        User user1 = getUser1()

        //Get admin user
        User admin = getUserAdmin()

        //Add an image
        UploadedFile uf1 = UploadedFileAPI.buildBasicUploadedFile(SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(uf1,true)
        UploadedFile uf2 = BasicInstanceBuilder.getUploadedFileNotExist(true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(uf2,true)

        //Add abstract slices
        AbstractSlice slice1 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice2 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice3 = BasicInstanceBuilder.getAbstractSliceNotExist(image2,true)
        def result = AbstractSliceAPI.listByAbstractImage(image1.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert (AbstractSliceAPI.containsInJSONList(slice1.id,json))
        assert (AbstractSliceAPI.containsInJSONList(slice2.id,json))
        assert !(AbstractSliceAPI.containsInJSONList(slice3.id,json))
        assert 2<=json.size() //may be more image because all images are available for admin (images from previous test)

        result = AbstractSliceAPI.listByAbstractImage(image2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 403 == result.code

        assert 200 == AbstractSliceAPI.show(slice1.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code
        assert 200 == AbstractSliceAPI.show(slice2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code
        assert 403 == AbstractSliceAPI.show(slice3.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code
        assert 200 == AbstractSliceAPI.create(BasicInstanceBuilder.getAbstractSliceNotExist().encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code
        assert 200 == AbstractSliceAPI.delete(slice1.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code

    }


    void testAbstractSliceSecurityForProjectManager() {
        //Get user1
        User user1 = getUser1()
        //Get user1
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Add an image
        UploadedFile uf1 = UploadedFileAPI.buildBasicUploadedFile(SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(uf1,true)
        UploadedFile uf2 = BasicInstanceBuilder.getUploadedFileNotExist(true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(uf2,true)

        //Add abstract slices
        AbstractSlice slice1 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice2 = BasicInstanceBuilder.getAbstractSliceNotExist(image2,true)

        //Create project
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Project project = result.data
        ProjectAPI.addUserProject(project.id, user1.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)

        result = AbstractSliceAPI.listByAbstractImage(image1.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
        result = AbstractSliceAPI.listByAbstractImage(image2.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        assert 403 == AbstractSliceAPI.show(slice1.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code
        assert 200 == AbstractSliceAPI.create(BasicInstanceBuilder.getAbstractSliceNotExist().encodeAsJSON(), SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code
        assert 403 == AbstractSliceAPI.delete(slice1.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code

    }

    void testAbstractSliceSecurityForProjectContributer() {
        //Get user1
        User user1 = getUser1()
        //Get user2
        User user2 = getUser2()
        //Get user3
        User user3 = getUser3()

        //Get admin user
        User admin = getUserAdmin()

        //Add an image
        UploadedFile uf1 = UploadedFileAPI.buildBasicUploadedFile(SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(uf1,true)
        UploadedFile uf2 = BasicInstanceBuilder.getUploadedFileNotExist(true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(uf2,true)

        //Add abstract slices
        AbstractSlice slice1 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice2 = BasicInstanceBuilder.getAbstractSliceNotExist(image1,true)
        AbstractSlice slice3 = BasicInstanceBuilder.getAbstractSliceNotExist(image2,true)

        //Create project
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Project project = result.data
        ProjectAPI.addUserProject(project.id, user3.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)

        result = AbstractSliceAPI.listByAbstractImage(image1.id,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert 403 == result.code
        result = AbstractSliceAPI.listByAbstractImage(image2.id,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert 403 == result.code

        assert 403 == AbstractSliceAPI.show(slice1.id,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code
        assert 200 == AbstractSliceAPI.create(BasicInstanceBuilder.getAbstractSliceNotExist().encodeAsJSON(), SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code
        assert 403 == AbstractSliceAPI.delete(slice1.id,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code

    }


}
