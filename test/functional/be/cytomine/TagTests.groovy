package be.cytomine

import be.cytomine.image.ImageInstance
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.TagAPI
import be.cytomine.test.http.TagDomainAssociationAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TagTests {

    //Test Tags
    void testShowTag() {
        def tag = BasicInstanceBuilder.getTag()
        def result = TagAPI.show(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.id == tag.id
    }
    void testListTag() {
        def result = TagAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection instanceof JSONArray
    }
    void testAddTag() {
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        tag = result.data
        result = TagAPI.show(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testAddTagSameName() {
        def tagOrigin = BasicInstanceBuilder.getTag()
        def tag = BasicInstanceBuilder.getTagNotExist()
        tag.name = tagOrigin.name
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
        tag.name = tagOrigin.name.toUpperCase()
        result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
        tag.name = tagOrigin.name.toLowerCase()
        result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }
    void testUpdateTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, "NEWNAME"]])
        def result = TagAPI.update(tag.id, data.postData, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idTag = json.tag.id
        result = TagAPI.show(idTag, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }
    void testDeleteTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.save(flush: true)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.save(flush: true)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.save(flush: true)

        def searchParameters = [[operator : "in", field : "tag", value:tag.id]]

        def result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 3

        result = TagAPI.delete(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.tag.id == tag.id
        result = TagAPI.show(json.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 0
    }

    void testDeleteNotExistingTag() {
        def result = TagAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    //Test Tag Associations
    void testListTagDomainAssociationByDomain() {
        def domain = BasicInstanceBuilder.getProjectNotExist(true)
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.setDomain(domain)
        association.save(flush: true)
        def result = TagDomainAssociationAPI.listByDomain(domain, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 1
    }

    void testListTagDomainAssociationByTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.save(flush: true)

        def searchParameters = [[operator : "in", field : "tag", value: tag.id]]
        def result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 1
        assert json.collection[0].tag == tag.id
    }

    void testListTagDomainAssociationByTagAndDomain() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def tag2 = BasicInstanceBuilder.getTagNotExist(true)
        def tag3 = BasicInstanceBuilder.getTagNotExist(true)

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(true)


        def association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        association1.save(flush: true)

        def association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = tag2
        association2.domain = image
        association2.save(flush: true)

        def association3 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association3.tag = tag3
        association3.domain = annot
        association3.save(flush: true)

        def association4 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association4.tag = tag3
        association4.domain = image
        association4.save(flush: true)

        def searchParameters = [[operator : "in", field : "tag", value: tag.id+","+tag2.id], [operator : "in", field : "domainIdent", value: project.id+","+image.id]]
        def result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 2
        assert TagDomainAssociationAPI.containsInJSONList(association1.id,json)

        searchParameters = [[operator : "in", field : "tag", value: tag.id+","+tag2.id], [operator : "in", field : "domainIdent", value: image.id+","+annot.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 1
        assert TagDomainAssociationAPI.containsInJSONList(association2.id,json)

        searchParameters = [[operator : "in", field : "tag", value: tag2.id+","+tag3.id], [operator : "in", field : "domainIdent", value: project.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 0

        searchParameters = [[operator : "in", field : "tag", value: tag2.id+","+tag3.id], [operator : "in", field : "domainIdent", value: image.id+","+annot.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.collection.size() == 3
    }

    void testAddTagDomainAssociation() {
        TagDomainAssociation association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        def result = TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        association = result.data
        result = TagDomainAssociationAPI.show(association.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testAddSameTagDomainAssociation() {
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist(true)
        def association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = association.tag
        association2.domain = association.retrieveCytomineDomain()
        def result = TagDomainAssociationAPI.create(association2.encodeAsJSON(), association2.domainClassName, association2.domainIdent, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }
    void testDeleteTagDomainAssociation() {
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist(true)
        def result = TagDomainAssociationAPI.delete(association.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert result.code == 200
        result = TagDomainAssociationAPI.show(association.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}
