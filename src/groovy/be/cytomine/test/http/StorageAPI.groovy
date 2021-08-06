package be.cytomine.test.http

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

import be.cytomine.image.server.Storage
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * Cytomine
 * User: stevben
 * Date: 7/02/13
 * Time: 15:06
 */
class StorageAPI  extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage.json"
        return doGET(URL, username, password)
    }

    static def listAll(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage.json?all=true"
        return doGET(URL, username, password)
    }

    static def usersStats(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/" + id + "/usersstats.json"
        return doGET(URL, username, password)
    }

    static def usersStats(Long id, String sortColumn, String sortDirection, Long offset, Long max, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/" + id + "/usersstats.json?sort=$sortColumn&order=$sortDirection&offset=$offset&max=$max"
        return doGET(URL, username, password)
    }

    static def listRights(String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/storage/access.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage.json"
        def result = doPOST(URL,json,username,password)
        result.data = Storage.get(JSON.parse(result.data)?.storage?.id)
        return result
    }

    static def update(def id, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/" + id + ".json"
        return doPUT(URL,json,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    static def addUserToStorage(def idStorage, def idUser, def permission, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/${idStorage}/user/${idUser}.json?permission=${permission}"
        return doPOST(URL,"",username,password)
    }

    static def addUsersToStorage(def idStorage, def idUsers, def permission, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/${idStorage}/user.json?permission=${permission}&users=${idUsers.join(",")}"
        return doPOST(URL,"",username,password)
    }

    static def deleteUserFromStorage(def idStorage, def idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/${idStorage}/user/${idUser}.json"
        return doDELETE(URL,username,password)
    }

    static def deleteUsersFromStorage(def idStorage, def idUsers, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/storage/${idStorage}/user.json?users=${idUsers.join(",")}"
        return doDELETE(URL,username,password)
    }
}