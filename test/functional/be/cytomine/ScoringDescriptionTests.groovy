package be.cytomine

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ScoringDescriptionAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class ScoringDescriptionTests {

  void testListScoringDescriptionWithCredential() {
      def result = ScoringDescriptionAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json.collection instanceof JSONArray
  }

  void testShowScoringDescriptionWithCredential() {
      ImageInstance imageInstance = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true), true)
      def description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD),true)
      description.data = "<TEST>"
      description = BasicInstanceBuilder.saveDomain(description)
      println description.domainIdent
      println description.domainClassName
      def result = ScoringDescriptionAPI.show(description.domainIdent,description.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json instanceof JSONObject
      assert json.data.equals(description.data)
  }

  void testAddScoringDescriptionCorrect() {
      ImageInstance imageInstance = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true), true)
      def descriptionToAdd = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD),false)
      def result = ScoringDescriptionAPI.create(descriptionToAdd.domainIdent,descriptionToAdd.domainClassName,descriptionToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      int idScoringDescription = result.data.id

      result =ScoringDescriptionAPI.show(descriptionToAdd.domainIdent,descriptionToAdd.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code

      result = ScoringDescriptionAPI.undo()
      assert 200 == result.code

      result = ScoringDescriptionAPI.show(descriptionToAdd.domainIdent,descriptionToAdd.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 404 == result.code

      result = ScoringDescriptionAPI.redo()
      assert 200 == result.code

      result =ScoringDescriptionAPI.show(descriptionToAdd.domainIdent,descriptionToAdd.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
  }

  void testAddScoringDescriptionAlreadyExist() {
      ImageInstance imageInstance = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true), true)
      def descriptionToAdd = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD),true)
      def result = ScoringDescriptionAPI.create(descriptionToAdd.domainIdent,descriptionToAdd.domainClassName,descriptionToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 409 == result.code
  }

  void testUpdateScoringDescriptionCorrect() {
      ImageInstance imageInstance = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true), true)
      def description = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD),true)
      def data = UpdateData.createUpdateSet(description,[data: [description.data,"NEWdata"]])
      def result = ScoringDescriptionAPI.update(description.domainIdent, description.domainClassName,data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json instanceof JSONObject
      int idScoringDescription = json.scoringdescription.id

      result =  ScoringDescriptionAPI.show(description.domainIdent,description.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      json = JSON.parse(result.data)
      BasicInstanceBuilder.compare(data.mapNew, json)

      result= ScoringDescriptionAPI.undo()
      assert 200 == result.code
      result=  ScoringDescriptionAPI.show(description.domainIdent,description.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

      result = ScoringDescriptionAPI.redo()
      assert 200 == result.code
      result =  ScoringDescriptionAPI.show(description.domainIdent,description.domainClassName,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))
  }


  void testDeleteScoringDescription() {
      ImageInstance imageInstance = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true), true)
      def descriptionToDelete = BasicInstanceBuilder.getScoringDescriptionNotExist(imageInstance, BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD),true)
      assert descriptionToDelete.save(flush: true)!= null
      def id = descriptionToDelete.id
      def result = ScoringDescriptionAPI.delete(descriptionToDelete.domainIdent,descriptionToDelete.domainClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code

      def showResult = ScoringDescriptionAPI.show(descriptionToDelete.domainIdent,descriptionToDelete.domainClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 404 == showResult.code

      result = ScoringDescriptionAPI.undo()
      assert 200 == result.code

      result = ScoringDescriptionAPI.show(descriptionToDelete.domainIdent,descriptionToDelete.domainClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code

      result = ScoringDescriptionAPI.redo()
      assert 200 == result.code

      result = ScoringDescriptionAPI.show(descriptionToDelete.domainIdent,descriptionToDelete.domainClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 404 == result.code
  }

  void testDeleteScoringDescriptionNotExist() {
      def result = ScoringDescriptionAPI.delete(-99, 'bad.class.name', Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 404 == result.code
  }
}
