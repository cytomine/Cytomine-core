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

import be.cytomine.ontology.Ontology
import be.cytomine.project.Project
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage Project to Cytomine with HTTP request during functional test
 */
class ProjectAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(Long max , Long offset, String username, String password) {
        return list([], null, null,null, max,offset, username, password)
    }

    static def list(Boolean withMembersCount = null, Boolean withLastActivity = null, Boolean withCurrentUserRoles = null, String username, String password) {
        return list([], withMembersCount, withLastActivity,withCurrentUserRoles, username, password)
    }

    static def list(def searchParameters, Boolean withMembersCount = null, Boolean withLastActivity = null, Boolean withCurrentUserRoles = null, String username, String password) {
        return list(searchParameters, withMembersCount, withLastActivity,withCurrentUserRoles, 0,0,username, password)
    }

    static def list(Boolean withMembersCount, Boolean withLastActivity, Boolean withCurrentUserRoles, Long max , Long offset, String username, String password) {
        return list([],withMembersCount, withLastActivity,withCurrentUserRoles, max,offset,username, password)
    }

    static def list(def searchParameters, Boolean withMembersCount = null, Boolean withLastActivity = null, Boolean withCurrentUserRoles = null, Long max , Long offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project.json"
        URL += "?max=$max&offset=$offset&${convertSearchParameters(searchParameters)}"
        if(withMembersCount || withLastActivity || withCurrentUserRoles) {
            URL += "&withMembersCount="+withMembersCount
            URL += "&withLastActivity="+withLastActivity
            URL += "&withCurrentUserRoles="+withCurrentUserRoles
        }
        return doGET(URL, username, password)
    }

    static def listByUser(Long id, Long max  = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/project.json?max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByUserLight(Long id, String type, Long max  = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/project/light.json?$type=true"
        return doGET(URL, username, password)
    }

    static def listUser(Long id, String type, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/${type}.json"
        return doGET(URL, username, password)
    }

    static def listBySoftware(Long id, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/software/$id/project.json"
        return doGET(URL, username, password)
    }

    static def listByOntology(Long id, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology/$id/project.json"
        return doGET(URL, username, password)
    }

    static def listRetrieval(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/retrieval/$id/project.json"
        return doGET(URL, username, password)
    }

    static def create(String jsonProject, String username, String password) {
        def json = JSON.parse(jsonProject)
        if(json.ontology && Ontology.read(json.ontology)) {
            Infos.addUserRight(username,Ontology.read(json.ontology))
        }
        String URL = Infos.CYTOMINEURL + "api/project.json"
        def result = doPOST(URL,jsonProject,username,password)
        result.data = Project.get(JSON.parse(result.data)?.project?.id)
        return result
    }

    static def update(def id, String jsonProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + ".json"
        return doPUT(URL,jsonProject,username,password)
    }

    static def delete(def id, String username, String password, Long task = null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + ".json" + (task ? "?task=${task}" :"")
        return doDELETE(URL,username,password)
    }


    static def addUserProject(def idProject, def idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user/${idUser}.json"
        return doPOST(URL,"",username,password)
    }
    static def addUsersProject(def idProject, def idUsers, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user.json?users="+idUsers.join(",")
        return doPOST(URL,"",username,password)
    }

    static def addAdminProject(def idProject, def idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user/${idUser}/admin.json"
        return doPOST(URL,"",username,password)
    }


    static def deleteUserProject(def idProject, def idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user/${idUser}.json"
        return doDELETE(URL,username,password)
    }
    static def deleteUsersProject(def idProject, def idUsers, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user.json?users="+idUsers.join(",")
        return doDELETE(URL,username,password)
    }

    static def deleteAdminProject(def idProject, def idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/user/${idUser}/admin.json"
        return doDELETE(URL,username,password)
    }

    static def listLastOpened(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/method/lastopened.json"
        return doGET(URL, username, password)
    }

    static def listLastOpened(int max,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/method/lastopened.json?max=$max"
        return doGET(URL, username, password)
    }

    static def doPing(Long idProject,String username, String password) {
        String url = Infos.CYTOMINEURL + "server/ping.json"
        return doPOST(url,'{"project": "' + idProject + '"}',username,password)
    }

    static def listCommandHistory(Long idProject, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/$idProject/commandhistory.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def listCommandHistory(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/commandhistory.json"
        return doGET(URL, username, password)
    }

    static def getBounds(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/bounds/project.json"
        return doGET(URL, username, password)
    }
}
