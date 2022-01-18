package be.cytomine

import be.cytomine.image.ImageInstance
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

import be.cytomine.security.SecUser
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageConsultationAPI
import be.cytomine.test.http.ImageInstanceAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

class ImageConsultationTests {


    void testAddConsultation() {
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()

        def result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        assert "test" == json.mode
        assert consultation.image == json.image

        //same re-opening image
        consultation = BasicInstanceBuilder.getImageConsultationNotExist()
        result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


    void testLastImageOfUsersByProject() {
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()

        def result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        // project where image is located
        def project = BasicInstanceBuilder.getProject();
        result = ImageConsultationAPI.lastImageOfUsersByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        assert json.collection.size() == 1
        assert json.collection[0].image == consultation.image
    }

    void testGetLastOpenedImage() {
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()

        def result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageInstanceAPI.listLastOpened(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert ImageInstanceAPI.containsInJSONList(consultation.image, json)

    }

    void testGetLastOpenedImageDoNotShowImageRemovedFromProject() {
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()

        def result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageInstanceAPI.delete(ImageInstance.read(consultation.image), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageInstanceAPI.listLastOpened(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert !ImageInstanceAPI.containsInJSONList(consultation.image, json)
    }

    void testListImageConsultationByProjectAndUser(){
        ImageInstance image = BasicInstanceBuilder.getImageInstance()
        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
        def result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        BasicInstanceBuilder.getImageConsultationNotExist(image, true)
        BasicInstanceBuilder.getImageConsultationNotExist(image, true)
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist(image, true)

        result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection[0].id == consultation.id

        def imageId = JSON.parse(result.data).collection[0].image

        result = ImageConsultationAPI.listDistinctImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.findAll{it.image == imageId}.size() == 1

        result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 2
    }

    void testListImageConsultationByProjectAndUserDoNotShowImageRemovedFromProject(){
        ImageInstance image = BasicInstanceBuilder.getImageInstance()
        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
        def result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        BasicInstanceBuilder.getImageConsultationNotExist(image, true)
        BasicInstanceBuilder.getImageConsultationNotExist(image, true)
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist(image, true)

        result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.collect{String.valueOf(it.id)}.contains(String.valueOf(consultation.id))

        result = ImageInstanceAPI.delete(ImageInstance.read(consultation.image), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageConsultationAPI.listImageConsultationByProjectAndUser(image.project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !JSON.parse(result.data).collection.collect{String.valueOf(it.id)}.contains(String.valueOf(consultation.id))
    }

    void testResumeByUserAndProject() {
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def consultation = BasicInstanceBuilder.getImageConsultationNotExist(project.id)

        def result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        result = ImageConsultationAPI.resumeByUserAndProject(user.id, consultation.project, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection.size() == 1
        assert json.collection[0].frequency == 2

        consultation = BasicInstanceBuilder.getImageConsultationNotExist(project.id)

        result = ImageConsultationAPI.create(consultation.image, consultation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageConsultationAPI.resumeByUserAndProject(user.id, consultation.project, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 2
    }

    void testCountAnnotationByProject() {
        def result = ImageConsultationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProjectWithDates() {
        Date startDate = new Date()
        def result = ImageConsultationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }
}
