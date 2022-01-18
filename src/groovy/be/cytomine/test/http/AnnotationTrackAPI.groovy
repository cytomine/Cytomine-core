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

import be.cytomine.ontology.AnnotationTrack
import be.cytomine.test.Infos
import grails.converters.JSON

class AnnotationTrackAPI extends DomainAPI {


    static def showAnnotationTrack(Long idAnnotation,Long idTrack, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationtrack/" + idAnnotation + "/"+ idTrack +".json"
        return doGET(URL, username, password)
    }

    static def listAnnotationTrackByAnnotation(Long idAnnotation,String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation/"+idAnnotation+"/annotationtrack.json"
        return doGET(URL, username, password)
    }

    static def listAnnotationTrackByTrack(Long idTrack, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/track/"+idTrack+"/annotationtrack.json"
        return doGET(URL, username, password)
    }

    static def createAnnotationTrack(String jsonAnnotationTrack, String username, String password) {
        def json = JSON.parse(jsonAnnotationTrack);
        String URL = Infos.CYTOMINEURL+"api/annotationtrack.json"
        def result = doPOST(URL,jsonAnnotationTrack,username,password)
        json = JSON.parse(result.data)
        def idAnnotationTrack = json?.annotationtrack?.id
        return [data: AnnotationTrack.get(idAnnotationTrack), code: result.code]
    }

    static def deleteAnnotationTrack(def idAnnotation, def idTrack, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationtrack/" + idAnnotation + "/"+ idTrack +".json"
        return doDELETE(URL,username,password)
    }

}
