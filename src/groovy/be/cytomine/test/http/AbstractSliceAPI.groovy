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

import be.cytomine.image.AbstractImage
import be.cytomine.test.Infos
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class AbstractSliceAPI extends DomainAPI {


    static def listByAbstractImage(Long idAbstractImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractimage/$idAbstractImage/abstractslice.json"
        return doGET(URL, username, password)
    }

    static def listByUploadedFile(Long idAbstractImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile/$idAbstractImage/abstractslice.json"
        return doGET(URL, username, password)
    }

    static def getByAbstractImageAndCoordinates(Long idAbstractImage, Integer channel, Integer zStack, Integer time, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractimage/$idAbstractImage/$channel/$zStack/$time/abstractslice.json"
        return doGET(URL, username, password)
    }

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractslice/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def showUploaderOfImage(Long idAbstractSlice, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/abstractslice/$idAbstractSlice/user.json"
        return doGET(URL, username, password)
    }

    static def create(String jsonAbstractImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractslice.json"
        def result = doPOST(URL, jsonAbstractImage,username, password)
        if(JSON.parse(jsonAbstractImage) instanceof JSONArray) return result
        result.data = AbstractImage.read(JSON.parse(result.data)?.abstractslice?.id)
        return result
    }

    static def update(def id, def jsonAbstractImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractslice/" + id + ".json"
        return doPUT(URL,jsonAbstractImage,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/abstractslice/" + id + ".json"
        return doDELETE(URL,username,password)
    }

}
