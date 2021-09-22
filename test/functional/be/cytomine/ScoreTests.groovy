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
* Unless required by applicable law or agreed to in writing, score
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.JobAPI
import be.cytomine.test.http.ScoreAPI
import be.cytomine.test.http.ScoreValueAPI
import be.cytomine.test.http.ScoreProjectAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.score.Score

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class ScoreTests {

    void testListScoreWithCredential() {
       def result = ScoreAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 200 == result.code
       def json = JSON.parse(result.data)
       assert json.collection instanceof JSONArray
   }
 
   void testListScoreWithoutCredential() {
       def result = ScoreAPI.list(Infos.BADLOGIN, Infos.BADPASSWORD)
       assert 401 == result.code
   }
 
   void testShowScoreWithCredential() {
       def result = ScoreAPI.show(BasicInstanceBuilder.getScore().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 200 == result.code
       def json = JSON.parse(result.data)
       assert json instanceof JSONObject
   }
 
   void testAddScoreCorrect() {
       def scoreToAdd = BasicInstanceBuilder.getScoreNotExist()
       def result = ScoreAPI.create(scoreToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 200 == result.code
       int idScore = result.data.id
 
       result = ScoreAPI.show(idScore, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 200 == result.code
   }
 
   void testAddScoreAlreadyExist() {
       def scoreToAdd = BasicInstanceBuilder.getScore()
       def result = ScoreAPI.create(scoreToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 409 == result.code
   }
 
   void testUpdateScoreCorrect() {
       def score = BasicInstanceBuilder.getScore()
       def data = UpdateData.createUpdateSet(score,[name: ["OLDNAME","NEWNAME"]])
       def resultBase = ScoreAPI.update(score.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 200==resultBase.code
       def json = JSON.parse(resultBase.data)
       assert json instanceof JSONObject
       int idScore = json.score.id
 
       def showResult = ScoreAPI.show(idScore, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       json = JSON.parse(showResult.data)
       BasicInstanceBuilder.compare(data.mapNew, json)
   }
 
   void testUpdateScoreNotExist() {
       Score scoreWithOldName = BasicInstanceBuilder.getScoreNotExist(true)
       Score scoreWithNewName = BasicInstanceBuilder.getScoreNotExist()
       scoreWithNewName.save(flush: true)
       Score scoreToEdit = Score.get(scoreWithNewName.id)
       def jsonScore = scoreToEdit.encodeAsJSON()
       def jsonUpdate = JSON.parse(jsonScore)
       jsonUpdate.name = scoreWithOldName.name
       jsonUpdate.id = -99
       jsonScore = jsonUpdate.toString()
       def result = ScoreAPI.update(-99, jsonScore, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 404 == result.code
   }
 
   void testUpdateScoreWithNameAlreadyExist() {
       Score scoreWithOldName = BasicInstanceBuilder.getScore()
       Score scoreWithNewName = BasicInstanceBuilder.getScoreNotExist(true)
       Score scoreToEdit = Score.get(scoreWithNewName.id)
       def jsonScore = scoreToEdit.encodeAsJSON()
       def jsonUpdate = JSON.parse(jsonScore)
       jsonUpdate.name = scoreWithOldName.name
       jsonScore = jsonUpdate.toString()
       def result = ScoreAPI.update(scoreToEdit.id, jsonScore, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 409 == result.code
   }
     
     void testEditScoreWithBadName() {
         Score scoreToAdd = BasicInstanceBuilder.getScore()
         Score scoreToEdit = Score.get(scoreToAdd.id)
         def jsonScore = scoreToEdit.encodeAsJSON()
         def jsonUpdate = JSON.parse(jsonScore)
         jsonUpdate.name = null
         jsonScore = jsonUpdate.toString()
         def result = ScoreAPI.update(scoreToAdd.id, jsonScore, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 400 == result.code
     }

    void testDeleteScore() {
        def scoreToDelete = BasicInstanceBuilder.getScoreNotExist(true)
        def id = scoreToDelete.id
        def result = ScoreAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = ScoreAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }

    void testDeleteDependencyScore() {
        def scoreToDelete = BasicInstanceBuilder.getScoreNotExist(true)
        def id = scoreToDelete.id

        def scoreValue = BasicInstanceBuilder.getScoreValueNotExist(scoreToDelete, true)

        def result = ScoreValueAPI.show(scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ScoreAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ScoreAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = ScoreAPI.show(scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteScoreNotExist() {
       def result = ScoreAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
       assert 404 == result.code
   }
}
