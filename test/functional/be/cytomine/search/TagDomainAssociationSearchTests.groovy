package be.cytomine.search

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

import be.cytomine.meta.TagDomainAssociation
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.RoiAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.ReviewedAnnotationAPI
import be.cytomine.test.http.RoiAnnotationAPI
import be.cytomine.test.http.UserAnnotationAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class TagDomainAssociationSearchTests {

    void testGetSearchProject(){
        Project p1 = BasicInstanceBuilder.getProjectNotExist(true)
        Project p2 = BasicInstanceBuilder.getProjectNotExist(true)
        p2.name = "S"
        p2.save(flush: true)
        p2 = p2.refresh()

        User user = BasicInstanceBuilder.getUser(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        ProjectAPI.addUserProject(p1.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(p2.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        TagDomainAssociation tda = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda.tag = BasicInstanceBuilder.getTagNotExist(true)
        tda.domain = p1
        tda.save(true)


        def searchParameters = [[operator : "in", field : "tag", value:tda.tag.id]]

        def result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert ProjectAPI.containsInJSONList(p1.id,json)

        searchParameters = [[operator : "in", field : "tag", value:tda.tag.id], [operator : "ilike", field : "name", value:p2.name]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0

        searchParameters = [[operator : "in", field : "tag", value:null]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ProjectAPI.containsInJSONList(p2.id,json)

        searchParameters = [[operator : "in", field : "tag", value:"null,"+tda.tag.id]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ProjectAPI.containsInJSONList(p1.id,json)
        assert ProjectAPI.containsInJSONList(p2.id,json)
    }

    void testGetSearchImage(){

        ImageInstance i1 = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true),true)
        ImageInstance i2 = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true),true)

        User user = BasicInstanceBuilder.getUser(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        User superadmin = BasicInstanceBuilder.getSuperAdmin(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        TagDomainAssociation tda = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda.tag = BasicInstanceBuilder.getTagNotExist(true)
        tda.domain = i1
        BasicInstanceBuilder.saveDomain(tda)

        ImageInstance.findAll().each {
            println "image ${it.id} => review user ${it.reviewUser?.id}"
        }

        SecUser.findAll().each {
            println it.id + " " + it.class
        }

        def searchParameters = [[operator : "in", field : "tag", value:tda.tag.id]]

        def result = ImageInstanceAPI.listByUser(superadmin.id, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert ImageInstanceAPI.containsInJSONList(i1.id,json)

        searchParameters = [[operator : "in", field : "tag", value:null]]

        result = ImageInstanceAPI.listByUser(superadmin.id, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ImageInstanceAPI.containsInJSONList(i2.id,json)

        searchParameters = [[operator : "in", field : "tag", value:"null,"+tda.tag.id]]

        result = ImageInstanceAPI.listByUser(superadmin.id, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ImageInstanceAPI.containsInJSONList(i1.id,json)
        assert ImageInstanceAPI.containsInJSONList(i2.id,json)

        searchParameters = [[operator : "in", field : "tag", value:tda.tag.id]]

        result = ImageInstanceAPI.listByUser(user.id, searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0

        ProjectAPI.addUserProject(i1.project.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(i2.project.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = ImageInstanceAPI.listByUser(user.id, searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ImageInstanceAPI.containsInJSONList(i1.id,json)


        result = ImageInstanceAPI.listByProject(i1.project.id, 0,0, searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ImageInstanceAPI.containsInJSONList(i1.id,json)


        ImageInstance i3 = BasicInstanceBuilder.getImageInstanceNotExist(i1.project,true)

        searchParameters = [[operator : "in", field : "tag", value:"null"]]

        result = ImageInstanceAPI.listByProject(i1.project.id, 0,0, searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert !ImageInstanceAPI.containsInJSONList(i1.id,json)
        assert ImageInstanceAPI.containsInJSONList(i3.id,json)

        searchParameters = [[operator : "in", field : "tag", value:"null,"+tda.tag.id]]

        result = ImageInstanceAPI.listByProject(i1.project.id, 0,0, searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ImageInstanceAPI.containsInJSONList(i1.id,json)
        assert ImageInstanceAPI.containsInJSONList(i3.id,json)
    }

    void testGetSearchAnnotation(){

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        UserAnnotation ua1 = BasicInstanceBuilder.getUserAnnotationNotExist(project,true)
        UserAnnotation ua2 = BasicInstanceBuilder.getUserAnnotationNotExist(project,true)

        TagDomainAssociation tda = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda.tag = BasicInstanceBuilder.getTagNotExist(true)
        tda.domain = ua1
        BasicInstanceBuilder.saveDomain(tda)

        def result = UserAnnotationAPI.listByProject(project.id, [tda.tag.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAnnotationAPI.containsInJSONList(ua1.id,json)
        assert !UserAnnotationAPI.containsInJSONList(ua2.id,json)

        result = UserAnnotationAPI.listByProject(project.id, [tda.tag.id], true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
        assert UserAnnotationAPI.containsInJSONList(ua1.id,json)
        assert UserAnnotationAPI.containsInJSONList(ua2.id,json)


        AlgoAnnotation aa1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(project,true)
        AlgoAnnotation aa2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(project,true)
        TagDomainAssociation tda2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda2.tag = tda.tag
        tda2.domain = aa1
        BasicInstanceBuilder.saveDomain(tda2)

        result = AlgoAnnotationAPI.listByProject(project.id, [tda.tag.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2 // return user AND algo annots
        assert AlgoAnnotationAPI.containsInJSONList(ua1.id,json)
        assert AlgoAnnotationAPI.containsInJSONList(aa1.id,json)
        assert !AlgoAnnotationAPI.containsInJSONList(ua2.id,json)
        assert !AlgoAnnotationAPI.containsInJSONList(aa2.id,json)

        result = AlgoAnnotationAPI.listByProject(project.id, [tda.tag.id], true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 4
        assert UserAnnotationAPI.containsInJSONList(ua1.id,json)
        assert UserAnnotationAPI.containsInJSONList(ua2.id,json)
        assert UserAnnotationAPI.containsInJSONList(aa1.id,json)
        assert UserAnnotationAPI.containsInJSONList(aa2.id,json)


        ReviewedAnnotation ra1 = BasicInstanceBuilder.getReviewedAnnotationNotExist(project,true)
        ReviewedAnnotation ra2 = BasicInstanceBuilder.getReviewedAnnotationNotExist(project,true)
        TagDomainAssociation tda3 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda3.tag = tda.tag
        tda3.domain = ra1
        BasicInstanceBuilder.saveDomain(tda3)

        result = ReviewedAnnotationAPI.listByProject(project.id, [tda.tag.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert ReviewedAnnotationAPI.containsInJSONList(ra1.id,json)
        assert !ReviewedAnnotationAPI.containsInJSONList(ra2.id,json)

        result = ReviewedAnnotationAPI.listByProject(project.id, [tda.tag.id], true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
        assert ReviewedAnnotationAPI.containsInJSONList(ra1.id,json)
        assert ReviewedAnnotationAPI.containsInJSONList(ra2.id,json)


        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        RoiAnnotation roa1 = BasicInstanceBuilder.getRoiAnnotationNotExist(image,true)
        RoiAnnotation roa2 = BasicInstanceBuilder.getRoiAnnotationNotExist(image,true)
        TagDomainAssociation tda4 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        tda4.tag = tda.tag
        tda4.domain = roa1
        BasicInstanceBuilder.saveDomain(tda4)

        result = RoiAnnotationAPI.listByProject(project.id, [tda.tag.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert RoiAnnotationAPI.containsInJSONList(roa1.id,json)
        assert !RoiAnnotationAPI.containsInJSONList(roa2.id,json)

        result = RoiAnnotationAPI.listByProject(project.id, [tda.tag.id], true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
        assert RoiAnnotationAPI.containsInJSONList(roa1.id,json)
        assert RoiAnnotationAPI.containsInJSONList(roa2.id,json)
    }
}
