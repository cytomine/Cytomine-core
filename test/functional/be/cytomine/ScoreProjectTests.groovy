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

import be.cytomine.processing.Job
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ScoreProjectAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import be.cytomine.score.ScoreProject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class ScoreProjectTests {

    void testListScoreProjectWithCredential() {
         def result = ScoreProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 200 == result.code
         def json = JSON.parse(result.data)
         assert json.collection instanceof JSONArray
     }
 
     void testListScoreByProject() {
         ScoreProject scoreProject = BasicInstanceBuilder.getScoreProject()
         def result = ScoreProjectAPI.listByProject(scoreProject.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 200 == result.code
         def json = JSON.parse(result.data)
         assert json.collection instanceof JSONArray

         result = ScoreProjectAPI.listByProject(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 404 == result.code
     }

    void testListScoreProjectByProject() {
        ScoreProject scoreProject = BasicInstanceBuilder.getScoreProject()
        def result = ScoreProjectAPI.listScoreProjectByProject(scoreProject.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = ScoreProjectAPI.listScoreProjectByProject(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

     void testAddScoreProjectCorrect() {
         def ScoreProjectToAdd = BasicInstanceBuilder.getScoreProjectNotExist()
         def result = ScoreProjectAPI.create(ScoreProjectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 200 == result.code
         int idScoreProject = result.data.id
   
         result = ScoreProjectAPI.show(idScoreProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 200 == result.code
     }
 
     void testAddScoreProjectWithBadScore() {
         ScoreProject ScoreProjectToAdd = BasicInstanceBuilder.getScoreProject()
         ScoreProject ScoreProjectToEdit = ScoreProject.get(ScoreProjectToAdd.id)
         def jsonScoreProject = ScoreProjectToEdit.encodeAsJSON()
         def jsonUpdate = JSON.parse(jsonScoreProject)
         jsonUpdate.score = -99
         jsonScoreProject = jsonUpdate.toString()
         def result = ScoreProjectAPI.create(jsonScoreProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 400 == result.code
     }

    void testAddScoreProjectWithBadProject() {
        ScoreProject ScoreProjectToAdd = BasicInstanceBuilder.getScoreProject()
        ScoreProject ScoreProjectToEdit = ScoreProject.get(ScoreProjectToAdd.id)
        def jsonScoreProject = ScoreProjectToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonScoreProject)
        jsonUpdate.project = -99
        jsonScoreProject = jsonUpdate.toString()
        def result = ScoreProjectAPI.create(jsonScoreProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddScoreAlreadyLinkedToProject() {
        ScoreProject ScoreProjectToAdd = BasicInstanceBuilder.getScoreProject()
        ScoreProjectToAdd = BasicInstanceBuilder.saveDomain(ScoreProjectToAdd)
        def result = ScoreProjectAPI.create(ScoreProjectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

     void testDeleteScoreProject() {
         def ScoreProjectToDelete = BasicInstanceBuilder.getScoreProjectNotExist(BasicInstanceBuilder.getScoreNotExist(true),BasicInstanceBuilder.getProjectNotExist(true),true)
         def id = ScoreProjectToDelete.id
         def result = ScoreProjectAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 200 == result.code
 
         def showResult = ScoreProjectAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 404 == showResult.code
     }
 
     void testDeleteScoreProjectNotExist() {
         def result = ScoreProjectAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         assert 404 == result.code
     }
}
