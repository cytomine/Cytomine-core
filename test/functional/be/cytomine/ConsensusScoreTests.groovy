package be.cytomine

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.score.ImageScore
import be.cytomine.score.Score
import be.cytomine.score.ScoreProject
import be.cytomine.score.ScoreValue

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

import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ConsensusScoreAPI
import be.cytomine.test.http.ImageScoreAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import be.cytomine.score.ConsensusScore

class ConsensusScoreTests {

    void testShowScoreConsensus() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScore()
        println ConsensusScore.getDataFromDomain(consensusScore)
        def result = ConsensusScoreAPI.show(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.scoreValue == consensusScore.scoreValue.id

    }

    void testShowScoreConsensusNotExist() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist() // not persisted
        def result = ConsensusScoreAPI.show(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddScoreConsensus() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist()
        def result = ConsensusScoreAPI.create(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, consensusScore.scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        println result
    }

    void testListScoreConsensusByImage() {
        ConsensusScore consensusScore = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getConsensusScoreNotExist())
        println ConsensusScore.getDataFromDomain(consensusScore)
        def result = ConsensusScoreAPI.listByImage(consensusScore.imageInstance.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 1 == json.collection.size()
        assert consensusScore.id == json.collection[0].id
    }


    void testStatsConsensusScoreGroupByImageByProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance imageWithAllScoreExeptScore3 = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithSomeScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithNoScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageDeleted = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        imageDeleted.setDeleted(new Date());
        imageDeleted = BasicInstanceBuilder.saveDomain(imageDeleted)

        Score score1 = BasicInstanceBuilder.getScoreNotExist(true)
        ScoreValue score1Value1 = BasicInstanceBuilder.getScoreValueNotExist(score1, true)
        ScoreValue score1Value2 = BasicInstanceBuilder.getScoreValueNotExist(score1, true)
        Score score2 = BasicInstanceBuilder.getScoreNotExist(true)
        ScoreValue score2Value1 = BasicInstanceBuilder.getScoreValueNotExist(score2, true)
        Score score3 = BasicInstanceBuilder.getScoreNotExist(true)
        ScoreValue score3Value1 = BasicInstanceBuilder.getScoreValueNotExist(score3, true)

        ScoreProject score1Project = BasicInstanceBuilder.getScoreProjectNotExist(score1, project, true)
        ScoreProject score2Project = BasicInstanceBuilder.getScoreProjectNotExist(score2, project, true)
        ScoreProject score3Project = BasicInstanceBuilder.getScoreProjectNotExist(score3, project, true)

        User user = User.findByUsername(Infos.SUPERADMINLOGIN);
        User anotherUser = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getSuperAdmin(UUID.randomUUID().toString(), UUID.randomUUID().toString()));


        BasicInstanceBuilder.getConsensusScoreNotExist(imageWithAllScoreExeptScore3, score1Value1, user, true)
        BasicInstanceBuilder.getConsensusScoreNotExist(imageWithAllScoreExeptScore3, score2Value1, user, true)
        BasicInstanceBuilder.getConsensusScoreNotExist(imageWithSomeScore, score1Value1, anotherUser, true)


        def result = ConsensusScoreAPI.statsGroupByImage(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 3 == json.collection.size() // 4 image instance ; but 1 is deleted
        def imageRowWithAllScores =  json.collection.find{it.id == imageWithAllScoreExeptScore3.id}
        assert imageRowWithAllScores != null

        assert !imageRowWithAllScores.isNull(String.valueOf(score1.id))
        assert !imageRowWithAllScores.isNull(String.valueOf(score2.id))
        assert imageRowWithAllScores.isNull(String.valueOf(score3.id))
        assert score1Value1.id == imageRowWithAllScores[String.valueOf(score1.id)]
        assert score2Value1.id == imageRowWithAllScores[String.valueOf(score2.id)]


        def imageRowWithSomeScore =  json.collection.find{it.id == imageWithSomeScore.id}
        assert imageRowWithSomeScore != null
        assert !imageRowWithSomeScore.isNull(String.valueOf(score1.id))
        assert imageRowWithSomeScore.isNull(String.valueOf(score2.id))
        assert imageRowWithSomeScore.isNull(String.valueOf(score3.id))
        assert score1Value1.id == imageRowWithSomeScore[String.valueOf(score1.id)]

        def imageRowWithNoScore =  json.collection.find{it.id == imageWithNoScore.id}
        assert imageRowWithNoScore != null
        assert imageRowWithNoScore.isNull(String.valueOf(score1.id))
        assert imageRowWithNoScore.isNull(String.valueOf(score2.id))
        assert imageRowWithNoScore.isNull(String.valueOf(score3.id))
    }

    void testAddScoreConsensusAlreadyExistChangeItsValue() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist()
        consensusScore = BasicInstanceBuilder.saveDomain(consensusScore)
        ScoreValue anotherValueFromSameScore = BasicInstanceBuilder.getScoreValueNotExist(consensusScore.scoreValue.score, true)

        def result = ConsensusScoreAPI.show(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.scoreValue == consensusScore.scoreValue.id

        result = ConsensusScoreAPI.create(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, anotherValueFromSameScore.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert result.data.scoreValue.id == anotherValueFromSameScore.id
    }

    void testDeleteScoreConsensus() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist()
        consensusScore = BasicInstanceBuilder.saveDomain(consensusScore)
        def result = ConsensusScoreAPI.delete(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ConsensusScoreAPI.show(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddConsensusScoreInLockedProject() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist()
        ProjectAPI.lock(consensusScore.imageInstance.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def result = ConsensusScoreAPI.create(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, consensusScore.scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code
    }

    void testDeleteConsensusScoreInLockedProject() {
        ConsensusScore consensusScore = BasicInstanceBuilder.getConsensusScoreNotExist()
        consensusScore = BasicInstanceBuilder.saveDomain(consensusScore)
        ProjectAPI.lock(consensusScore.imageInstance.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def result = ConsensusScoreAPI.delete(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code

        result = ConsensusScoreAPI.show(consensusScore.imageInstance.id, consensusScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

}
