package be.cytomine

import be.cytomine.project.Project
import be.cytomine.score.ScoreProject

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, score
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ScoreValueAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.score.Score
import be.cytomine.score.ScoreValue

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class ScoreValueTests {

    void testListScoreValueWithCredential() {
          def result = ScoreValueAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json.collection instanceof JSONArray
      }
  
      void testListScoreValueByScore() {
          ScoreValue scorevalue = BasicInstanceBuilder.getScoreValue()
          def result = ScoreValueAPI.listByScore(scorevalue.score.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json.collection instanceof JSONArray
          result = ScoreValueAPI.listByScore(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }
  
      void testShowScoreValueWithCredential() {
          def result = ScoreValueAPI.show(BasicInstanceBuilder.getScoreValue().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json instanceof JSONObject
      }
  
      void testAddScoreValueCorrect() {
          def scorevalueToAdd = BasicInstanceBuilder.getScoreValueNotExist()
          def result = ScoreValueAPI.create(scorevalueToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          int idScoreValue = result.data.id
    
          result = ScoreValueAPI.show(idScoreValue, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
      }

      void testUpdateScoreValueCorrect() {
          def sp = BasicInstanceBuilder.getScoreValue()
          def data = UpdateData.createUpdateSet(sp,[value: ["OLDVALUE","NEWVALUE"]])
          def result = ScoreValueAPI.update(sp.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
          def json = JSON.parse(result.data)
          assert json instanceof JSONObject
      }
  
      void testUpdateScoreValueNotExist() {
          ScoreValue scorevalueWithNewName = BasicInstanceBuilder.getScoreValueNotExist()
          scorevalueWithNewName.save(flush: true)
          ScoreValue scorevalueToEdit = ScoreValue.get(scorevalueWithNewName.id)
          def jsonScoreValue = scorevalueToEdit.encodeAsJSON()
          def jsonUpdate = JSON.parse(jsonScoreValue)
          jsonUpdate.id = -99
          jsonScoreValue = jsonUpdate.toString()
          def result = ScoreValueAPI.update(-99, jsonScoreValue, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }
  
      void testUpdateScoreValueWithBadScore() {
          ScoreValue scorevalueToAdd = BasicInstanceBuilder.getScoreValue()
          ScoreValue scorevalueToEdit = ScoreValue.get(scorevalueToAdd.id)
          def jsonScoreValue = scorevalueToEdit.encodeAsJSON()
          def jsonUpdate = JSON.parse(jsonScoreValue)
          jsonUpdate.score = -99
          jsonScoreValue = jsonUpdate.toString()
          def result = ScoreValueAPI.update(scorevalueToAdd.id, jsonScoreValue, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 400 == result.code
      }
  
      void testDeleteScoreValue() {
          def scorevalueToDelete = BasicInstanceBuilder.getScoreValueNotExist()
          assert scorevalueToDelete.save(flush: true)!= null
          def id = scorevalueToDelete.id
          def result = ScoreValueAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 200 == result.code
  
          def showResult = ScoreValueAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == showResult.code
      }
  
      void testDeleteScoreValueNotExist() {
          def result = ScoreValueAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
          assert 404 == result.code
      }

    void testReorderScoreValue() {
        Score score = BasicInstanceBuilder.getScoreNotExist(true)
        ScoreValue score1 = BasicInstanceBuilder.getScoreValueNotExist(score, true)
        ScoreValue score2 = BasicInstanceBuilder.getScoreValueNotExist(score, true)

        def result = ScoreValueAPI.reorder(score.id, score1.getId() + "," + score2.getId(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ScoreValueAPI.listByScore(score.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].id == score1.id
        assert json.collection[1].id == score2.id

        result = ScoreValueAPI.reorder(score.id, score2.getId() + "," + score1.getId(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ScoreValueAPI.listByScore(score.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].id == score2.id
        assert json.collection[1].id == score1.id
    }

    void testReorderScoreValueWithBadList() {
        ScoreValue scoreValue = BasicInstanceBuilder.getScoreValueNotExist(BasicInstanceBuilder.getScoreNotExist(true), true)

        def result = ScoreValueAPI.update(scoreValue.score.id, "-1,-2", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
