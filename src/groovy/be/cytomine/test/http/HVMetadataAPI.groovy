package be.cytomine.test.http

import be.cytomine.image.hv.HVMetadata

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

import be.cytomine.ontology.Ontology
import be.cytomine.test.Infos
import grails.converters.JSON

class HVMetadataAPI extends DomainAPI {

    /*static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology/" + id + ".json"
        return doGET(URL, username, password)
    }*/

    static def listStaining(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/staining.json"
        return doGET(URL, username, password)
    }

    static def createStaining(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/staining.json"
        def result = doPOST(URL,json,username,password)
        result.data = HVMetadata.get(JSON.parse(result.data)?.ontology?.id)
        return result
    }

    static def updateStaining(def id, def jsonOntology, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/staining/" + id + ".json"
        return doPUT(URL,jsonOntology,username,password)
    }

    static def deleteStaining(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/staining/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    /*static def listLaboratory(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/laboratory.json"
        return doGET(URL, username, password)
    }

    static def listDetection(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/detection.json"
        return doGET(URL, username, password)
    }

    static def listDilution(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/dilution.json"
        return doGET(URL, username, password)
    }

    static def listInstrument(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/instrument.json"
        return doGET(URL, username, password)
    }

    static def listAntibody(String username, String password) {
        String URL = Infos.CYTOMINEURL +  "api/antibody.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology.json"
        def result = doPOST(URL,json,username,password)
        result.data = Ontology.get(JSON.parse(result.data)?.ontology?.id)
        return result
    }

    static def update(def id, def jsonOntology, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology/" + id + ".json"
        return doPUT(URL,jsonOntology,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology/" + id + ".json"
        return doDELETE(URL,username,password)
    }*/
}
