package be.cytomine

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.score.ImageScore
import be.cytomine.score.Score
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
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.score.ScoreValue
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageScoreAPI
import be.cytomine.test.http.ScoreProjectAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray


class ImageScoreTests {

    void testShowScoreImage() {
        ImageScore imageScore = BasicInstanceBuilder.getImageScore()
        println ImageScore.getDataFromDomain(imageScore)
        def result = ImageScoreAPI.show(imageScore.imageInstance.id, imageScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.scoreValue == imageScore.scoreValue.id

    }

    void testShowScoreImageNotExist() {
        ImageScore imageScore = BasicInstanceBuilder.getImageScoreNotExist() // not persisted
        def result = ImageScoreAPI.show(imageScore.imageInstance.id, imageScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddScoreImage() {
        ImageScore imageScore = BasicInstanceBuilder.getImageScoreNotExist()
        def result = ImageScoreAPI.create(imageScore.imageInstance.id, imageScore.scoreValue.score.id, imageScore.scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        println result
    }


    void testListScoreImageByImage() {
        ImageScore imageScore = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getImageScoreNotExist())
        println ImageScore.getDataFromDomain(imageScore)
        def result = ImageScoreAPI.listByImage(imageScore.imageInstance.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 1 == json.collection.size()
        assert imageScore.id == json.collection[0].id
    }

    void testListScoreImageByProject() {
        ImageScore imageScore = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getImageScoreNotExist())
        println ImageScore.getDataFromDomain(imageScore)
        def result = ImageScoreAPI.listByProject(imageScore.imageInstance.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 1 == json.collection.size()
        assert imageScore.id == json.collection[0].id
    }

    void testStatsImageScoreGroupByImageByProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance imageWithAllScoreExeptScore3 = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithSomeScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithNoScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)

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


        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score1Value1, user, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score1Value1, anotherUser, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score2Value1, user, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithSomeScore, score1Value1, anotherUser, true)


        def result = ImageScoreAPI.statsGroupByImage(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 3 == json.collection.size() // 3 image instance
        def imageRowWithAllScores =  json.collection.find{it.id == imageWithAllScoreExeptScore3.id}
        assert imageRowWithAllScores != null
        assert 2 == imageRowWithAllScores[String.valueOf(score1.id)]
        assert 1 == imageRowWithAllScores[String.valueOf(score2.id)]
        assert 0 == imageRowWithAllScores[String.valueOf(score3.id)]

        def imageRowWithSomeScore =  json.collection.find{it.id == imageWithSomeScore.id}
        assert imageRowWithSomeScore != null
        assert 1 == imageRowWithSomeScore[String.valueOf(score1.id)]
        assert 0 == imageRowWithSomeScore[String.valueOf(score2.id)]
        assert 0 == imageRowWithSomeScore[String.valueOf(score3.id)]

        def imageRowWithNoScore =  json.collection.find{it.id == imageWithNoScore.id}
        assert imageRowWithNoScore != null
        assert 0 == imageRowWithNoScore[String.valueOf(score1.id)]
        assert 0 == imageRowWithNoScore[String.valueOf(score2.id)]
        assert 0 == imageRowWithNoScore[String.valueOf(score3.id)]


        result = ImageScoreAPI.statsGroupByImage(project.id, "csv", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        println result
        assert 200 == result.code

    }

    void testStatsImageScoreGroupByUserByProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance anotherImage = BasicInstanceBuilder.getImageInstanceNotExist(project, true)

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

        User userWithAllScoreExeptScore3 = User.findByUsername(Infos.SUPERADMINLOGIN);
        Infos.addUserRight(userWithAllScoreExeptScore3.username,project)
        User userWithSomeScore = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getSuperAdmin(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        Infos.addUserRight(userWithSomeScore.username,project)
        User userWithNoScore = BasicInstanceBuilder.saveDomain(BasicInstanceBuilder.getSuperAdmin(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        Infos.addUserRight(userWithNoScore.username,project)


        BasicInstanceBuilder.getImageScoreNotExist(image, score1Value2, userWithAllScoreExeptScore3, true)
        BasicInstanceBuilder.getImageScoreNotExist(image, score2Value1, userWithAllScoreExeptScore3, true)
        BasicInstanceBuilder.getImageScoreNotExist(anotherImage, score1Value1, userWithAllScoreExeptScore3, true)
        BasicInstanceBuilder.getImageScoreNotExist(anotherImage, score2Value1, userWithAllScoreExeptScore3, true)

        BasicInstanceBuilder.getImageScoreNotExist(image, score1Value2, userWithSomeScore, true)
        BasicInstanceBuilder.getImageScoreNotExist(image, score2Value1, userWithSomeScore, true)
        BasicInstanceBuilder.getImageScoreNotExist(anotherImage, score2Value1, userWithSomeScore, true)
        BasicInstanceBuilder.getImageScoreNotExist(anotherImage, score3Value1, userWithSomeScore, true)


        def result = ImageScoreAPI.statsGroupByUser(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert 3 == json.collection.size() // 3 users

        def userRowWithAllScores =  json.collection.find{it.id == userWithAllScoreExeptScore3.id}
        assert userRowWithAllScores != null
        assert 2 == userRowWithAllScores[String.valueOf(score1.id)]
        assert 2 == userRowWithAllScores[String.valueOf(score2.id)]
        assert 0 == userRowWithAllScores[String.valueOf(score3.id)]

        def userRowWithSomeScore =  json.collection.find{it.id == userWithSomeScore.id}
        assert userRowWithSomeScore != null
        assert 1 == userRowWithSomeScore[String.valueOf(score1.id)]
        assert 2 == userRowWithSomeScore[String.valueOf(score2.id)]
        assert 1 == userRowWithSomeScore[String.valueOf(score3.id)]

        def userRowWithNoScore =  json.collection.find{it.id == userWithNoScore.id}
        assert userRowWithNoScore != null
        assert 0 == userRowWithNoScore[String.valueOf(score1.id)]
        assert 0 == userRowWithNoScore[String.valueOf(score2.id)]
        assert 0 == userRowWithNoScore[String.valueOf(score3.id)]

        result = ImageScoreAPI.statsGroupByUser(project.id, "csv", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        println result
        assert 200 == result.code

    }



    void testStatsReportByProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance imageWithAllScoreExeptScore3 = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithSomeScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        ImageInstance imageWithNoScore = BasicInstanceBuilder.getImageInstanceNotExist(project, true)

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
        Infos.addUserRight(anotherUser, project)

        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score1Value1, user, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score1Value1, anotherUser, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithAllScoreExeptScore3, score2Value1, user, true)
        BasicInstanceBuilder.getImageScoreNotExist(imageWithSomeScore, score1Value1, anotherUser, true)

        def result = ImageScoreAPI.statsReport(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        println result
        assert 200 == result.code
        def rows = result.data.split("\n")
        assert rows.length==6 + 1 // + (3 images * 2 users) + 1 header
    }




    void testAddScoreImageAlreadyExistChangeItsValue() {
        ImageScore imageScore = BasicInstanceBuilder.getImageScoreNotExist()
        imageScore = BasicInstanceBuilder.saveDomain(imageScore)
        ScoreValue anotherValueFromSameScore = BasicInstanceBuilder.getScoreValueNotExist(imageScore.scoreValue.score, true)

        def result = ImageScoreAPI.show(imageScore.imageInstance.id, imageScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.scoreValue == imageScore.scoreValue.id

        result = ImageScoreAPI.create(imageScore.imageInstance.id, imageScore.scoreValue.score.id, anotherValueFromSameScore.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert result.data.scoreValue.id == anotherValueFromSameScore.id
    }

    void testDeleteScoreImage() {
        ImageScore imageScore = BasicInstanceBuilder.getImageScoreNotExist()
        imageScore = BasicInstanceBuilder.saveDomain(imageScore)
        def result = ImageScoreAPI.delete(imageScore.imageInstance.id, imageScore.scoreValue.score.id, imageScore.scoreValue.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageScoreAPI.show(imageScore.imageInstance.id, imageScore.scoreValue.score.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
