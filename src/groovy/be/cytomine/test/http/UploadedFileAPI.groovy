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

import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage Discipline to Cytomine with HTTP request during functional test
 */
class UploadedFileAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(Long max = 0, Long offset = 0, String username, String password) {
        return list(false, null, null, max, offset, username, password)
    }
    static def list(boolean detailed, Long max = 0, Long offset = 0, String username, String password) {
        return list(detailed, null, null, max, offset, username, password)
    }
    static def list(boolean detailed = false, String sort, String order, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?max=$max&offset=$offset"
        URL += detailed ? "&detailed=true" : ""
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }

    static def listOnlyRoots(Long max , Long offset, String username, String password) {
        return listOnlyRoots(null, null, max , offset, username, password)
    }

    static def listOnlyRoots(String sort = null, String order = null, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?onlyRoots=true&max=$max&offset=$offset"
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }

    static def listChilds(Long parentId, Long max = 0, Long offset = 0, String username, String password) {
        return listChilds(parentId, null, null, max, offset, username, password)
    }
    static def listChilds(Long parentId, String sort, String order, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?parent=$parentId&max=$max&offset=$offset"
        URL += sort ? "&sort=$sort" : ""
        URL += order ? "&order=$order" : ""
        return doGET(URL, username, password)
    }

    static def hierarchicalList(Long rootId, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?root=$rootId&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }
    static def searchWithName(String name, String username, String password) {
        def searchParameters = [[operator : "ilike", field : "originalFilename", value:name]]
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?onlyRootsWithDetails=true&${convertSearchParameters(searchParameters)}"
        return doGET(URL, username, password)
    }

    static def searchByStorage(Storage storage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json?onlyRootsWithDetails=true&storage[in]="+storage.id + ",123"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile.json"
        def result = doPOST(URL, json,username, password)
        Long idUploadedFile = JSON.parse(result.data)?.uploadedfile?.id
        return [data: UploadedFile.get(idUploadedFile), code: result.code]
    }

    static def update(def id, def jsonUploadedFile, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile/" + id + ".json"
        return doPUT(URL,jsonUploadedFile,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/uploadedfile/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    static def createImage(def uploadedFile,String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/uploadedfile/$uploadedFile/image.jpg"
        return doPOST(URL, "",username, password)
    }


    static def clearAbstractImageProperties(Long idImage,String username, String password) throws Exception {
        return doPOST(Infos.CYTOMINEURL+"/api/abstractimage/"+idImage+"/properties/clear.json","",username,password);
    }
    static def populateAbstractImageProperties(Long idImage,String username, String password) throws Exception {
        return doPOST(Infos.CYTOMINEURL+"/api/abstractimage/"+idImage+"/properties/populate.json","",username,password);
    }
    static def extractUsefulAbstractImageProperties(Long idImage,String username, String password) throws Exception {
        return doPOST(Infos.CYTOMINEURL+"/api/abstractimage/"+idImage+"/properties/extract.json","",username,password);
    }

    static UploadedFile buildBasicUploadedFile(String username, String password) {
        User user = BasicInstanceBuilder.getUser(username, password)
        UploadedFile uploadedFile = BasicInstanceBuilder.getUploadedFileNotExist(user)
        def result = UploadedFileAPI.create(uploadedFile.encodeAsJSON(), username, password)
        assert 200 == result.code
        uploadedFile = result.data
        return uploadedFile
    }

}
