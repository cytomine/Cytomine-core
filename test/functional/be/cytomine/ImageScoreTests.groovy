package be.cytomine

import be.cytomine.score.ImageScore

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
