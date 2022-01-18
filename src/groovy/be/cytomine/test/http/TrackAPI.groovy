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

import be.cytomine.ontology.Track
import be.cytomine.test.Infos
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class TrackAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/track/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/track.json"
        return doGET(URL, username, password)
    }

    static def listByImageInstance(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/$id/track.json"
        return doGET(URL, username, password)
    }

    static def create(def jsonTrack, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/track.json"
        def result = doPOST(URL,jsonTrack,username,password)
        def json = JSON.parse(result.data)
        if(JSON.parse(jsonTrack) instanceof JSONArray) return [code: result.code]
        Long idTrack = json?.track?.id
        return [data: Track.get(idTrack), code: result.code]
    }

    static def update(def id, def jsonTrack, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/track/" + id + ".json"
        return doPUT(URL,jsonTrack,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/track/" + id + ".json"
        return doDELETE(URL,username,password)
    }

}
