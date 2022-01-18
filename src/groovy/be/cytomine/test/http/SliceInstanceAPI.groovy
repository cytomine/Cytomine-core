package be.cytomine.test.http

import be.cytomine.image.ImageInstance

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

import be.cytomine.image.SliceInstance
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import grails.converters.JSON

class SliceInstanceAPI extends DomainAPI {

    static def create(String jsonSliceInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/sliceinstance.json"
        def result = doPOST(URL,jsonSliceInstance,username,password)
        result.data = SliceInstance.get(JSON.parse(result.data)?.sliceinstance?.id)
        return result
    }

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/sliceinstance/" + id + ".json"
        return doGET(URL,username,password)
    }

    static def update(def id, def jsonSliceInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/sliceinstance/" + id + ".json"
        return doPUT(URL,jsonSliceInstance,username,password)
    }

    static def delete(SliceInstance image, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/sliceinstance/" + image.id + ".json"
        return doDELETE(URL,username,password)
    }


    static def listByImageInstance(Long imageInstanceId, Integer max = 0, Integer offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/$imageInstanceId/sliceinstance.json&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def getByImageInstanceAndCoordinates(Long imageInstanceId, Integer channel, Integer zStack, Integer time, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/$imageInstanceId/$channel/$zStack/$time/sliceinstance.json"
        return doGET(URL, username, password)
    }


    static SliceInstance buildBasicSlice(String username, String password) {
        ImageInstance image = ImageInstanceAPI.buildBasicImage(username, password)
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image)
        def result = SliceInstanceAPI.create(slice.encodeAsJSON(), username, password)
        assert 200==result.code
        slice = result.data
        return slice
    }
}
