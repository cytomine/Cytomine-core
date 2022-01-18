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

import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageConsultationAPI
import be.cytomine.test.http.ProjectConnectionAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

class ProjectConnectionTests {


    void testAddConnection() {
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def json = JSON.parse("{project:${project.id}, os:Debian, browser:HttpClient, browserVersion: 1.0}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        result = ProjectConnectionAPI.getConnectionByUserAndProject(user.id, project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 1
        result = ProjectConnectionAPI.lastConnectionInProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 1
        result = ProjectConnectionAPI.numberOfConnectionsByProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 1
        result = ProjectConnectionAPI.numberOfConnectionsByProjectAndUser(project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 1
    }



    /*void testPerf() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def user;
        String username = "user"
        for(int i=0;i<10000;i++){
            user = BasicInstanceBuilder.getUser(username+i, username+i)
            ProjectAPI.addUserProject(project.id,user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
            result = ProjectConnectionAPI.create(project.id, json.toString(),username+i, username+i)
            assert 200 == result.code
        }

        def begin = System.currentTimeMillis()
        result = UserAPI.listUsersWithLastActivity(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
        println "collection.size()"
        println json.collection.size()
        def end = System.currentTimeMillis()
        println "ellapse time : "+(end-begin)
        assert false
    }*/

    void testGetConnectionByUserAndProject() {
        def connection = BasicInstanceBuilder.getProjectConnection()
        def project = Project.read(connection.project)

        def result = ProjectConnectionAPI.create(connection.project, connection.encodeAsJSON() ,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        result = ProjectConnectionAPI.getConnectionByUserAndProject(user.id,project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testLastConnectionInProject() {
        def connection = BasicInstanceBuilder.getProjectConnection();
        def result = ProjectConnectionAPI.create(connection.project, connection.encodeAsJSON(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.lastConnectionInProject(connection.project,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testNumberOfConnectionsByProject() {
        def connection = BasicInstanceBuilder.getProjectConnection();
        def result = ProjectConnectionAPI.create(connection.project, connection.encodeAsJSON(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.numberOfConnectionsByProject(connection.project,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 200 == result.code
    }

    void testNumberOfConnectionsByUserAndProject() {
        def connection = BasicInstanceBuilder.getProjectConnection();
        def result = ProjectConnectionAPI.create(connection.project, connection.encodeAsJSON(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        result = ProjectConnectionAPI.numberOfConnectionsByProjectAndUser(connection.project, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testClosePreviousConnectionSimple(){
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def json = JSON.parse("{project:${project.id}, os:Debian, browser:HttpClient, browserVersion: 1.0}");

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.getConnectionByUserAndProject(user.id, project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 2

        json.collection.each {
            assert it.countViewedImages == 0
            assert it.countCreatedAnnotations == 0
            assert it.time == 0
        }
    }

    void testClosePreviousConnectionWithData(){
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        def json = JSON.parse("{project:${project.id}, os:Debian, browser:HttpClient, browserVersion: 1.0}");

        SecUser user = BasicInstanceBuilder.getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse("{image:${image.id},mode:test}")
        result = ImageConsultationAPI.create(image.id, json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.getConnectionByUserAndProject(user.id, project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 2

        assert json.collection[0].countViewedImages == 1
        assert json.collection[1].countViewedImages == 0
        json.collection.each {
            assert it.countCreatedAnnotations == 0
            assert it.time == 0
        }
    }

    void testCountAnnotationByProject() {
        def result = ProjectConnectionAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProjectWithDates() {
        Date startDate = new Date()
        def result = ProjectConnectionAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }
}