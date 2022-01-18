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

import be.cytomine.security.User
import be.cytomine.test.Infos
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage User to Cytomine with HTTP request during functional test
 */
class UserAPI extends DomainAPI {

    static def showCurrent(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/current.json"
        return doGET(URL, username, password)
    }

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def show(String id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def keys(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/" + id + "/keys.json"
        return doGET(URL, username, password)
    }

    static def signature(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/signature.json"
        return doGET(URL, username, password)
    }

    static def showUserJob(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/userJob/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        list(null, 0, 0, username, password)
    }

    static def list(String key, String username, String password) {
        list(key, 0, 0, username, password)
    }

    static def list(String key = null, boolean withRoles = false, Long max, Long offset, String username, String password) {
        list(key, withRoles, null, null,  max, offset, username, password)
    }
    static def list(boolean withRoles, String sort, String order, String username, String password) {
        list((String) null, withRoles, sort, order,  0, 0, username, password)
    }
    static def list(String key = null, boolean withRoles = false, String sort, String order, String username, String password) {
        list(key, withRoles, sort, order,  0, 0, username, password)
    }
    static def list(String key = null, String sort, String order, Long max, Long offset, String username, String password) {
        list(key, false, sort, order,  max, offset, username, password)
    }
    static def list(String key = null, boolean withRoles, String sort, String order, Long max, Long offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user.json?max=$max&offset=$offset" + (key ? "&publicKey=$key" : "")
        URL += withRoles ? "&withRoles=$withRoles" : ""
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }

    static def list(Long id, String domain, String type, Long max = 0, Long offset = 0, String username, String password) {
        list(id, domain, type, false, false, false, false, max, offset, username, password)
    }
    static def list(Long id, String domain, String type, boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, String username, String password) {
        list(id, domain, type, false, withLastImage, withLastConsultation, withNumberConsultations, 0, 0, username, password)
    }
    static def list(Long id,String domain,String type, Boolean online = false, boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, String sort, String order, String username, String password) {
        return list(id, domain, type, online, withLastImage, withLastConsultation, withNumberConsultations, sort, order,0, 0, username, password)
    }
    static def list(Long id,String domain,String type, Boolean online = false,  boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, Long max, Long offset, String username, String password) {
        return list(id, domain, type, online, withLastImage, withLastConsultation, withNumberConsultations, null, null, max, offset, username, password)
    }
    static def list(Long id,String domain,String type, Boolean online = false, boolean withLastImage, boolean withLastConnection, boolean withNumberConnections, String sort, String order, Long max, Long offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/${domain}/$id/${type}.json?max=$max&offset=$offset" + (online? "&online=true":"")
        URL += withLastImage ? "&withLastImage=$withLastImage" : ""
        URL += withLastConnection ? "&withLastConnection=$withLastConnection" : ""
        URL += withNumberConnections ? "&withNumberConnections=$withNumberConnections" : ""
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }


    static def searchAndList(ArrayList searchParameters, String username, String password) {
        searchAndList(false, searchParameters,  0, 0, username, password)
    }
    static def searchAndList(Boolean online = false, ArrayList searchParameters, String sort, String order, String username, String password) {
        return searchAndList(online, searchParameters, sort, order,0, 0, username, password)
    }
    static def searchAndList(Boolean online = false, ArrayList searchParameters, Long max, Long offset, String username, String password) {
        return searchAndList(online, searchParameters, null, null, max, offset, username, password)
    }
    static def searchAndList(Boolean online = false, ArrayList searchParameters, String sort, String order, Long max, Long offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user.json?max=$max&offset=$offset&${convertSearchParameters(searchParameters)}" + (online? "&online=true":"")
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }


    static def searchAndList(Long id, String domain, String type, ArrayList searchParameters, String username, String password) {
        searchAndList(id, domain, type, false, false, false, false, searchParameters,  0, 0, username, password)
    }

    static def searchAndList(Long id, String domain, String type, boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, ArrayList searchParameters, String username, String password) {
        searchAndList(id, domain, type, false, withLastImage, withLastConsultation, withNumberConsultations, searchParameters,  0, 0, username, password)
    }
    static def searchAndList(Long id,String domain,String type, Boolean online = false, boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, ArrayList searchParameters, String sort, String order, String username, String password) {
        return searchAndList(id, domain, type, online, withLastImage, withLastConsultation, withNumberConsultations, searchParameters, sort, order,0, 0, username, password)
    }
    static def searchAndList(Long id,String domain,String type, Boolean online = false, boolean withLastImage, boolean withLastConsultation, boolean withNumberConsultations, ArrayList searchParameters, Long max, Long offset, String username, String password) {
        return searchAndList(id, domain, type, online, withLastImage, withLastConsultation, withNumberConsultations, searchParameters, null, null, max, offset, username, password)
    }
    static def searchAndList(Long id,String domain,String type, Boolean online = false, boolean withLastImage, boolean withLastConnection, boolean withNumberConnections, ArrayList searchParameters, String sort, String order, Long max, Long offset, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/${domain}/$id/${type}.json?max=$max&offset=$offset&${convertSearchParameters(searchParameters)}" + (online? "&online=true":"")
        URL += withLastImage ? "&withLastImage=$withLastImage" : ""
        URL += withLastConnection ? "&withLastConnection=$withLastConnection" : ""
        URL += withNumberConnections ? "&withNumberConnections=$withNumberConnections" : ""
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }
    static def listWithConsultationInformation(Long id,String domain,String type, Long max = 0, Long offset = 0, String username, String password) {
        return list(id, domain, type, false, true, true, true, null, null, max, offset, username, password)
    }

    static def listFriends(Long id,def offline, Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/friends.json?offline=" + (offline? "true":"false") + (idProject? "&project=${idProject}":"")
        return doGET(URL, username, password)
    }

    static def listOnline(Long id,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$id/online/user.json"
        return doGET(URL, username, password)
    }

    static def listUserJob(Long id,Boolean tree, Long idImage,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$id/userjob.json?tree="+(tree?"true":false)+(idImage?"&image=$idImage":"")
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        JSONElement jsonWithPassword = JSON.parse(json)
        if(jsonWithPassword.password==null || jsonWithPassword.password.toString()=="null") {
            jsonWithPassword.password = "defaultPassword"
            jsonWithPassword.oldPassword = password
        }
        String URL = Infos.CYTOMINEURL + "api/user.json"
        def result = doPOST(URL,jsonWithPassword.toString(),username,password)
        result.data = User.get(JSON.parse(result.data)?.user?.id)
        return result
    }

    static def update(def id, def jsonUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/" + id + ".json"
        return doPUT(URL,jsonUser,username,password)
    }
    static def resetPassword(def id,def newPassword, String username, String password) {
        String json = ([password: newPassword, oldPassword :password] as JSON).toString()
        String URL = Infos.CYTOMINEURL + "api/user/$id/password.json"
        return doPUT(URL,json,username,password)
    }
    static def checkPassword(String passwordToCheck, String username, String password) {
        String json = ([password: passwordToCheck] as JSON).toString()
        String URL = Infos.CYTOMINEURL + "api/user/security_check.json"
        return doPOST(URL,json,username,password)
    }


    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    static def listLayers(Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/project/$idProject/userlayer.json"
        return doGET(URL, username, password)
    }

    static def switchUser(String usernameToSwitch,String username, String password) {
        String URL = Infos.CYTOMINEURL + "j_spring_security_switch_user"
        return doPOST(URL, 'j_username: '+usernameToSwitch,username, password)
    }

    static def retrieveCustomUI(Long idProject,String username, String password) {
        String URL = Infos.CYTOMINEURL + "custom-ui/config.json" + (idProject? "?project=$idProject" : "")
        return doGET(URL, username, password)
    }



    static def retrieveCustomUIProject(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "custom-ui/project/${idProject}.json"
        return doGET(URL,username, password)
    }

    static def setCustomUIProject(Long idProject, String json,String username, String password) {
        String URL = Infos.CYTOMINEURL + "custom-ui/project/${idProject}.json"
        return doPOST(URL, json,username, password)
    }

//    static def retrieveCustomUIProjectFlag(Long idProject, String username, String password) {
//        String URL = Infos.CYTOMINEURL + "custom-ui/project/${idProject}/flag.json"
//        return doGET(URL,username, password)
//    }

    static def listUsersWithLastActivity(Long idProject, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/${idProject}/usersActivity.json?max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def lock(Long id,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/lock"
        return doPOST(URL, '',username, password)
    }
    static def unlock(Long id,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/lock"
        return doDELETE(URL, username, password)
    }
}
