package be.cytomine

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
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.HVMetadataAPI
import be.cytomine.test.http.OntologyAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class HVMetadataTests {

    /*void testShowOntologyWithCredential() {
        def result = OntologyAPI.show(BasicInstanceBuilder.getOntology().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }*/

    void testListStainingWithCredential() {
        def result = HVMetadataAPI.listStaining(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }
  
    void testListStainingWithoutCredential() {
        def result = HVMetadataAPI.listStaining(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testAddStainingCorrect() {
        HVMetadata staining = new HVMetadata(value:BasicInstanceBuilder.getRandomString(), type:HVMetadata.Type.STAINING)
        BasicInstanceBuilder.checkDomain(staining)

        def result = HVMetadataAPI.createStaining(staining.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        //int idOntology = result.data.id
  
        /*result = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
  
        result = OntologyAPI.undo()
        assert 200 == result.code
  
        result = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
  
        result = OntologyAPI.redo()
        assert 200 == result.code
  
        result = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code*/
    }
  
    void testAddStainingAlreadyExist() {
        HVMetadata staining = new HVMetadata(value:BasicInstanceBuilder.getRandomString(), type:HVMetadata.Type.STAINING)
        BasicInstanceBuilder.saveDomain(staining)

        def result = HVMetadataAPI.createStaining(staining.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }
  
    /*void testUpdateStainingCorrect() {
        Ontology ontologyToAdd = BasicInstanceBuilder.getOntologyNotExist(true)
        def data = UpdateData.createUpdateSet(ontologyToAdd,[name: ["OLDNAME","NEWNAME"], user:[BasicInstanceBuilder.user1,BasicInstanceBuilder.user2]])
        def result = OntologyAPI.update(ontologyToAdd.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idOntology = json.ontology.id
  
        def showResult = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
  
        showResult = OntologyAPI.undo()
        assert 200 == result.code
        showResult = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))
  
        showResult = OntologyAPI.redo()
        assert 200 == result.code
        showResult = OntologyAPI.show(idOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }
  
    void testUpdateStainingNotExist() {
        Ontology ontologyWithOldName = BasicInstanceBuilder.getOntology()
        Ontology ontologyWithNewName = BasicInstanceBuilder.getOntologyNotExist()
        ontologyWithNewName.save(flush: true)
        Ontology ontologyToEdit = Ontology.get(ontologyWithNewName.id)
        def jsonOntology = ontologyToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonOntology)
        jsonUpdate.name = ontologyWithOldName.name
        jsonUpdate.id = -99
        jsonOntology = jsonUpdate.toString()
        def result = OntologyAPI.update(-99, jsonOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
  
    void testUpdateStainingWithNameAlreadyExist() {
        Ontology ontologyWithOldName = BasicInstanceBuilder.getOntology()
        Ontology ontologyWithNewName = BasicInstanceBuilder.getOntologyNotExist()
        ontologyWithNewName.save(flush: true)
        Ontology ontologyToEdit = Ontology.get(ontologyWithNewName.id)
        def jsonOntology = ontologyToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonOntology)
        jsonUpdate.name = ontologyWithOldName.name
        jsonOntology = jsonUpdate.toString()
        def result = OntologyAPI.update(ontologyToEdit.id, jsonOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testEditStainingWithBadName() {
        Ontology ontologyToAdd = BasicInstanceBuilder.getOntology()
        Ontology ontologyToEdit = Ontology.get(ontologyToAdd.id)
        def jsonOntology = ontologyToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonOntology)
        jsonUpdate.name = null
        jsonOntology = jsonUpdate.toString()
        def result = OntologyAPI.update(ontologyToAdd.id, jsonOntology, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }
*/
    void testDeleteStaining() {

        HVMetadata staining = new HVMetadata(value:BasicInstanceBuilder.getRandomString(), type:HVMetadata.Type.STAINING)
        BasicInstanceBuilder.saveDomain(staining)
        def id = staining.id

        def result = HVMetadataAPI.listStaining(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        int countOntology = JSON.parse(result.data).collection.size()

        result = HVMetadataAPI.deleteStaining(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
  
        //def showResult = OntologyAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        //assert 404 == showResult.code
        result = HVMetadataAPI.listStaining(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection.size() == countOntology -1

    }
  
    void testDeleteStainingNotExist() {
        def result = HVMetadataAPI.deleteStaining(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteAssociatedStaining() {

        HVMetadata staining = new HVMetadata(value:BasicInstanceBuilder.getRandomString(), type:HVMetadata.Type.STAINING)
        BasicInstanceBuilder.saveDomain(staining)
        def id = staining.id

        def result = HVMetadataAPI.listStaining(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        int countOntology = JSON.parse(result.data).collection.size()

        result = HVMetadataAPI.deleteStaining(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert false
        assert 200 == result.code

        //def showResult = OntologyAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        //assert 404 == showResult.code
        result = HVMetadataAPI.listStaining(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection.size() == countOntology -1

    }


}
