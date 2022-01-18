package be.cytomine

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
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationDomainAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.security.*


class AnnotationDomainTests {

    void testSearchAnnotationFromUserAndJob() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation userAnnotation = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        AlgoAnnotation algoAnnotation = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, true)
        ArrayList<Long> users = new ArrayList<>();
        users.add(userAnnotation.user.id)
        users.add(algoAnnotation.user.id)

        def result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,false, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2

        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2

        users = users.reverse()
        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2

        result = AnnotationDomainAPI.delete(userAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 1

        result = AnnotationDomainAPI.delete(algoAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 0
    }

    void testSearchAnnotationFromUsers() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation userAnnotation = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        UserAnnotation userAnnotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        ArrayList<Long> users = new ArrayList<>()
        users.add(userAnnotation.user.id)
        users.add(userAnnotation2.user.id)

        def result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,false, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2

        ArrayList<Long> images = new ArrayList<>()
        images.add(userAnnotation.image.id)
        result = AnnotationDomainAPI.listByImagesAndUsersByPOST(images,users, false, false, false, null, null, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2



        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2


        result = AnnotationDomainAPI.delete(userAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.listByImagesAndUsersByPOST(images,users, false, false, false, null, null, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 1

        //reviewed
        def user = User.findByUsername(Infos.SUPERADMINLOGIN)
        String reviewUsers = "${user.id},${user.id}"
        result = AnnotationDomainAPI.listByImagesAndUsersByPOST(images,users, false, true, true, reviewUsers, "0", Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 0


    }

    void testSearchAnnotationFromJobs() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        AlgoAnnotation algoAnnotation = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, true)
        AlgoAnnotation algoAnnotation2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, true)
        ArrayList<Long> users = new ArrayList<>();
        users.add(algoAnnotation.user.id)
        users.add(algoAnnotation2.user.id)

        def result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,false, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

        //TODO : currently if only algo, we infer automatically the research. Do we want to keep this ?
        assert json.collection.size() == 2

        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 2


        result = AnnotationDomainAPI.delete(algoAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.listByImageAndUsers(image.id,users,true, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.collection.size() == 1

    }


}
