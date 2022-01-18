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

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.social.PersistentImageConsultation
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageConsultationAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.ProjectConnectionAPI
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class ImageConsultationSecurityTests extends SecurityTestsAbstract{


  void testImageConsultationSecurityForCytomineAdmin() {

      //Get user1
      User user1 = getUser1()

      //Get admin user
      User admin = getUserAdmin()

      //Create new project (user1)
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data

      //Add image instance to project
      ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
      image.project = project
      //check if admin user can access/update/delete
      result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      image = result.data

      //Add image consultation
      PersistentImageConsultation consultation = BasicInstanceBuilder.getImageConsultationNotExist()
      consultation.project = project.id
      consultation.image = image.id

      //check if admin user can access/update/delete
      result = ProjectConnectionAPI.create(project.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      result = ImageConsultationAPI.create(image.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user1.id, SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      result = ImageConsultationAPI.countByProject(project.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
  }

    void testImageConsultationSecurityForProjectAdmin() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addAdminProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        //check if admin user can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        image = result.data

        //Add image consultation
        PersistentImageConsultation consultation = BasicInstanceBuilder.getImageConsultationNotExist()
        consultation.project = project.id
        consultation.image = image.id

        //check if admin user can access/update/delete
        result = ProjectConnectionAPI.create(project.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = ImageConsultationAPI.create(image.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user1.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = ImageConsultationAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
    }

  void testImageConsultationSecurityForProjectUser() {

      //Get user1
      User user1 = getUser1()
      User user2 = getUser2()

      //Get admin user
      User admin = getUserAdmin()

      //Create new project (user1)
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data
      def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      Infos.printRight(project)
      assert 200 == resAddUser.code

      //Add image instance to project
      ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
      image.project = project
      //check if admin user can access/update/delete
      result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      image = result.data

      //Add image consultation
      PersistentImageConsultation consultation = BasicInstanceBuilder.getImageConsultationNotExist()
      consultation.project = project.id
      consultation.image = image.id

      //check if contributor user can access/update/delete
      result = ProjectConnectionAPI.create(project.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code
      result = ImageConsultationAPI.create(image.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code
      result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user1.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 403 == result.code
      result = ImageConsultationAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code

      result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user2.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json.collection[0].imageName == image.getBlindInstanceFilename()

      project.blindMode = true
      project = BasicInstanceBuilder.saveDomain(project)

      result = ImageConsultationAPI.create(image.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code

      result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user2.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 200 == result.code
      json = JSON.parse(result.data)
      assert json.collection[0].imageName == image.getBlindInstanceFilename()
  }

  void testImageConsultationSecurityForSimpleUser() {

      //Get user1
      User user1 = getUser1()
      User user2 = getUser2()

      //Get admin user
      User admin = getUserAdmin()

      //Create new project (user1)
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data

      //Add image instance to project
      ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
      image.project = project
      //check if admin user can access/update/delete
      result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      image = result.data

      //Add image consultation
      PersistentImageConsultation consultation = BasicInstanceBuilder.getImageConsultationNotExist()
      consultation.project = project.id
      consultation.image = image.id

      //check if user not in project can access/update/delete
      result = ProjectConnectionAPI.create(project.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 403 == result.code
      result = ImageConsultationAPI.create(image.id,consultation.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 403 == result.code
      result = ImageConsultationAPI.listImageConsultationByProjectAndUser(project.id, user1.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
      assert 403 == result.code
      result = ImageConsultationAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
      assert 403 == result.code
  }

}
