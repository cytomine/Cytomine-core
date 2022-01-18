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

class ProjectConnectionAPI extends DomainAPI {

    static def getConnectionByUserAndProject(Long idUser, Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/userconnection/${idUser}.json"
        return doGET(URL, username, password)
    }

    static def lastConnectionInProject(Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/lastConnection.json"
        return doGET(URL, username, password)
    }

    static def numberOfConnectionsByProject(Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/connectionFrequency.json"
        return doGET(URL, username, password)
    }

    static def numberOfConnectionsByProjectAndUser(Long idProject,Long idUser,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/connectionFrequency/${idUser}.json"
        return doGET(URL, username, password)
    }

    static def create(Long idProject, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/userconnection.json"
        def result = doPOST(URL,json,username,password)
        return result
    }

    static def countByProject(Long idProject, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/userconnection/count.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }
}
