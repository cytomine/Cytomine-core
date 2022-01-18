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
import be.cytomine.image.ImageInstance
import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractImageAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class AbstractImageSecurityTests extends SecurityTestsAbstract{

  void testAbstractImageSecurityForCytomineAdmin() {
      //Get user1
      User user1 = getUser1()

      //Get admin user
      User admin = getUserAdmin()

      Storage storage = BasicInstanceBuilder.getStorageNotExist(true)

      Infos.addUserRight(user1.username,storage)

      //Add an image
      AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(true)
      AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(true)

      //Create project
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data

      result = AbstractImageAPI.list(SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert 2<=json.collection.size() //may be more image because all images are available for admin (images from previous test)
      assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))
      assert (true ==AbstractImageAPI.containsInJSONList(image2.id,json))
      assert !json.collection.find{it.id==image1.id}.inProject
      assert !json.collection.find{it.id==image2.id}.inProject

      //Add image instance to project
      ImageInstance image = new ImageInstance(project:project,baseImage: image1, user: user1)
      //check if user 2 can access/update/delete
      result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code

      result = AbstractImageAPI.list(SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      assert 2<=JSON.parse(result.data).collection.size()
      assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))

      assert 200 == AbstractImageAPI.show(image1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
      assert 200 == AbstractImageAPI.create(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code

      // image1 has instances in active projects ==> 403
      assert 403 == AbstractImageAPI.delete(image1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
  }

    void testAbstractImageSecurityForCytomineUser() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser("testListACLUser",PASSWORD1)

        //Get admin user
        User admin = getUserAdmin()

        Storage storage = BasicInstanceBuilder.getStorageNotExist(true)
        Infos.addUserRight(user1,storage)
        Storage storageForbiden = BasicInstanceBuilder.getStorageNotExist(true)
        //don't add acl to this storage

        UploadedFile uploadedFile1 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedFile1.storage = storage
        uploadedFile1 = uploadedFile1.save()
        UploadedFile uploadedFile2 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedFile2.storage = storageForbiden
        uploadedFile2 = uploadedFile2.save()

        //Add an image
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(uploadedFile1, true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(uploadedFile2, true)

        //Create project
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),user1.username,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        result = AbstractImageAPI.list(user1.username,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert 1==json.collection.size()

        assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))
        assert (false ==AbstractImageAPI.containsInJSONList(image2.id,json))

        result = AbstractImageAPI.list(user1.username, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))

        assert 200 == AbstractImageAPI.show(image1.id,user1.username,SecurityTestsAbstract.PASSWORD1).code

        UploadedFile uploadedFile3 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedFile3.storage = storage
        uploadedFile3 = uploadedFile3.save()
        json = JSON.parse(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON())
        json.uploadedFile = uploadedFile3.id
        assert 200 == AbstractImageAPI.create(json.toString(), user1.username,SecurityTestsAbstract.PASSWORD1).code
        assert 200 == AbstractImageAPI.delete(image1.id,user1.username,SecurityTestsAbstract.PASSWORD1).code

        result = AbstractImageAPI.list(user1.username, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (false ==AbstractImageAPI.containsInJSONList(image2.id,json))
        assert 403 == AbstractImageAPI.show(image2.id,user1.username,SecurityTestsAbstract.PASSWORD1).code

        UploadedFile uploadedFile4 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedFile4.storage = storageForbiden
        uploadedFile4 = uploadedFile4.save()
        json = JSON.parse(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON())
        json.uploadedFile = uploadedFile4.id
        assert 403 == AbstractImageAPI.create(json.toString(), user1.username,SecurityTestsAbstract.PASSWORD1).code
        assert 403 == AbstractImageAPI.delete(image2.id,user1.username,SecurityTestsAbstract.PASSWORD1).code
    }


}
