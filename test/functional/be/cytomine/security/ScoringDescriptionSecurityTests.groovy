package be.cytomine.security

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ScoringDescriptionAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.score.ScoringDescription

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class ScoringDescriptionSecurityTests extends SecurityTestsAbstract{


  void testScoringDescriptionSecurityForCytomineAdmin() {

      //Get user1
      User user1 = getUser1()

      //Get admin user
      User admin = getUserAdmin()

      //Create new project (user1)

      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data
      result = ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      ImageInstance imageInstance = result.data

      //Add description instance to project
      ScoringDescription description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance,user1,false)
      description.setDomain(imageInstance)
      //check if admin user can access/update/delete
      result = ScoringDescriptionAPI.create(imageInstance.id,imageInstance.class.name,description.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      description = result.data
      assert (200 == ScoringDescriptionAPI.show(imageInstance.id,imageInstance.class.name,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
  }

  void testScoringDescriptionSecurityForDescriptionCreator() {

      //Get user1
      User user1 = getUser1()

      //Get admin user
      User admin = getUserAdmin()

      //Create new project (user1)
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data
      def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      Infos.printRight(project)
      assert 200 == resAddUser.code
      result = ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      ImageInstance imageInstance = result.data


      //Add description instance to project
      ScoringDescription description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance,user1, false)
      description.setDomain(imageInstance)

      //check if user 2 can access/update/delete
      result = ScoringDescriptionAPI.create(project.id,project.class.name,description.encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      description = result.data
      assert (200 == ScoringDescriptionAPI.show(imageInstance.id,imageInstance.class.name,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code)
      assert (200 == ScoringDescriptionAPI.update(imageInstance.id,imageInstance.class.name,description.encodeAsJSON(),USERNAME1,PASSWORD1).code)
      assert (200 == ScoringDescriptionAPI.delete(imageInstance.id,imageInstance.class.name,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1).code)
  }

  void testScoringDescriptionSecurityForAnotherProjectUser() {

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
      result = ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      ImageInstance imageInstance = result.data

      //Add description instance to project
      ScoringDescription description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, user1,false)
      description.setDomain(imageInstance)

      description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance,user1,false)
      description.setDomain(imageInstance)
      description.save(flush:true)

      // user has no description for this entity
      assert (404 == ScoringDescriptionAPI.show(imageInstance.id,imageInstance.class.name,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
      assert (404 == ScoringDescriptionAPI.update(imageInstance.id,imageInstance.class.name,description.encodeAsJSON(),USERNAME2,PASSWORD2).code)
      assert (404 == ScoringDescriptionAPI.delete(imageInstance.id,imageInstance.class.name,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)

  }

}
