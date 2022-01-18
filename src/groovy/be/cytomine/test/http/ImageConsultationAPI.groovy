package be.cytomine.test.http

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

import be.cytomine.test.Infos

class ImageConsultationAPI extends DomainAPI {

    static def lastImageOfUsersByProject(Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/lastImages.json"
        return doGET(URL, username, password)
    }

    static def create(Long idImage, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/imageinstance/$idImage/consultation.json"
        def result = doPOST(URL,json,username,password)
        return result
    }

    static def listImageConsultationByProjectAndUser(Long idProject, Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/user/$idUser/imageconsultation.json"
        return doGET(URL, username, password)
    }

    static def listImageConsultationByProjectAndUser(Long idProject, Long idUser, boolean offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/user/$idUser/imageconsultation.json?offset=1&max=2"
        return doGET(URL, username, password)
    }

    static def listDistinctImageConsultationByProjectAndUser(Long idProject, Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/user/$idUser/imageconsultation.json?distinctImages=true"
        return doGET(URL, username, password)
    }

    static def resumeByUserAndProject(Long idUser, Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/imageconsultation/resume.json?user=$idUser&project=$idProject"
        return doGET(URL, username, password)
    }

    static def countByProject(Long idProject, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/imageconsultation/count.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }
}
