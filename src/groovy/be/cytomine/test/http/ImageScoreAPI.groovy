package be.cytomine.test.http

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

import be.cytomine.score.ScoreProject
import be.cytomine.test.Infos
import grails.converters.JSON

class ImageScoreAPI extends DomainAPI {

    static def show(Long idImageInstance, Long idScore, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/${idImageInstance}/score/${idScore}.json"
        return doGET(URL, username, password)
    }

    static def listByImage(Long idImageInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/${idImageInstance}/image-score.json"
        return doGET(URL, username, password)
    }

    static def listByProject(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/image-score.json"
        return doGET(URL, username, password)
    }


    static def statsReport(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/${idProject}/image-score/stats-report.csv"
        return doGET(URL, username, password)
    }

    static def create(Long idImageInstance, Long idScore, Long idValue, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/${idImageInstance}/score/${idScore}/value/${idValue}.json"
        def result = doPOST(URL,"",username,password)
        result.data = ImageScore.get(JSON.parse(result.data)?.imagescore?.id)
        return result
    }

    static def delete(Long idImageInstance, Long idScore, Long idValue, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/${idImageInstance}/score/${idScore}/value/${idValue}.json"
        return doDELETE(URL,username,password)
    }
}
