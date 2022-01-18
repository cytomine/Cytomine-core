package be.cytomine.search

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

import be.cytomine.security.User
import be.cytomine.social.LastConnection
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class UserSearchTests {


    //search
    void testUsersSearch(){
        User u1 = BasicInstanceBuilder.getUserNotExist(true)
        User u2 = BasicInstanceBuilder.getUserNotExist(true)
        u1.lastname = BasicInstanceBuilder.getRandomString()
        u1.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u2.lastname = BasicInstanceBuilder.getRandomString()
        u2.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u1.save(failOnError: true)
        u2.save(failOnError: true)

        def project = BasicInstanceBuilder.getProjectNotExist(true)

        u1 = u1.refresh()
        u2 = u2.refresh()

        def result = UserAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() >= 2

        ArrayList searchParameters = [[operator : "ilike", field : "fullName", value:u1.lastname]]

        result = UserAPI.searchAndList(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u1.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u1.email]]

        result = UserAPI.searchAndList(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u1.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u2.lastname]]

        result = UserAPI.searchAndList(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u2.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u2.username]]

        result = UserAPI.searchAndList(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u2.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u2.email]]

        result = UserAPI.searchAndList( searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u2.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:"NOT_PRESENT"]]

        result = UserAPI.searchAndList( searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0
    }

    void testProjectUsersSearch(){
        User u1 = BasicInstanceBuilder.getUserNotExist(true)
        User u2 = BasicInstanceBuilder.getUserNotExist(true)
        u1.lastname = BasicInstanceBuilder.getRandomString()
        u1.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u2.lastname = BasicInstanceBuilder.getRandomString()
        u2.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u1.save(failOnError: true)
        u2.save(failOnError: true)
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id, u1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addAdminProject(project.id, u2.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()
        consultation.project = project.id
        consultation.user = u1.id
        consultation.insert(flush: true, failOnError:true)

        def connection = BasicInstanceBuilder.getProjectConnection()
        connection.project = project.id
        connection.insert(flush: true, failOnError:true)

        u1 = u1.refresh()
        u2 = u2.refresh()

        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 3

        ArrayList searchParameters = [[operator : "ilike", field : "fullName", value:u1.lastname]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u1.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u1.email]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u1.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u2.lastname]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u2.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:u2.email]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u2.id,json)

        searchParameters = [[operator : "ilike", field : "fullName", value:"NOT_PRESENT"]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0


        searchParameters = [[operator : "in", field : "status", value:"online"]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0


        new LastConnection(user: u1, project:project).insert(flush: true, failOnError: true)

        searchParameters = [[operator : "in", field : "status", value:"online"]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert UserAPI.containsInJSONList(u1.id,json)


        searchParameters = [[operator : "in", field : "projectRole", value: "manager"]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].containsKey("role")
        assert json.size == 2


        searchParameters = [[operator : "in", field : "projectRole", value: "contributor"]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].containsKey("role")
        assert json.size == 1


        searchParameters = [[operator : "in", field : "projectRole", value: "representative"]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0


        def pur = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        pur.project = project
        pur.user = u2
        BasicInstanceBuilder.saveDomain(pur)
        searchParameters = [[operator : "in", field : "projectRole", value: "representative"]]

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].containsKey("role")
        assert json.size == 1
    }


    //pagination
    void testListUser() {
        BasicInstanceBuilder.getUser()
        BasicInstanceBuilder.getUser1()
        BasicInstanceBuilder.getUser2()

        def result = UserAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        long size = json.size
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = UserAPI.list(1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.size == size
        assert json.collection[0].id == id1

        result = UserAPI.list(1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.size == size
        assert json.collection[0].id == id2
    }

    void testListProjectUser() {
        User u1 = BasicInstanceBuilder.getUser1()
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id, u1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id
        long size = json.size

        result = UserAPI.list(project.id,"project","user", 1,0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.size == size
        assert json.collection[0].id == id1

        result = UserAPI.list(project.id,"project","user", 1,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.size == size
        assert json.collection[0].id == id2



        result = UserAPI.listWithConsultationInformation(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2
        id1 = json.collection[0].id
        id2 = json.collection[1].id


        result = UserAPI.listWithConsultationInformation(project.id,"project","user",1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1


        result = UserAPI.listWithConsultationInformation(project.id,"project","user",1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2

    }

    void testListUsersWithLastActivity(){
        User u1 = BasicInstanceBuilder.getUser1()
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id, u1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        def connection = BasicInstanceBuilder.getProjectConnection()
        connection.project = project.id
        connection.user = u1.id

        def result = UserAPI.listUsersWithLastActivity(connection.project, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = UserAPI.listUsersWithLastActivity(connection.project, 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.listUsersWithLastActivity(connection.project, 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert json.collection[0].id == id2

    }

    //sort
    void testUserSort(){
        User u1 = BasicInstanceBuilder.getUserNotExist(true)
        User u2 = BasicInstanceBuilder.getUserNotExist(true)
        User u3 = BasicInstanceBuilder.getUserNotExist(true)
        u1.lastname = BasicInstanceBuilder.getRandomString()
        u1.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u2.lastname = BasicInstanceBuilder.getRandomString()
        u2.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u3.lastname = BasicInstanceBuilder.getRandomString()
        u3.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u1.save(failOnError: true)
        u2.save(failOnError: true)
        u3.save(failOnError: true)
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id, u1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addAdminProject(project.id, u2.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(project.id, u3.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        u1 = u1.refresh()
        u2 = u2.refresh()
        u3 = u3.refresh()


        def result = UserAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        Long totalSize = json.size
        assert totalSize >= 4


        //---------------- Sort by role


        result = UserAPI.list(true,"role", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size <= totalSize // totalSize - users without role (creating in test)
        assert json.collection.size() <= totalSize // totalSize - users without role (creating in test)
        Long id1 = json.collection[0].id
        Long id2 = json.collection[-1].id

        result = UserAPI.list( true,"role", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size <= totalSize // totalSize - users without role (creating in test)
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.list(true,"role", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size <= totalSize // totalSize - users without role (creating in test)
        assert json.collection.size() == 1
        assert json.collection[0].id != id1
        assert json.collection[0].role == "ROLE_SUPER_ADMIN"


        //---------------- Sort by origin


        result = UserAPI.list("origin", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        def size = totalSize
        assert json.collection.size() == size
        String origin1 = json.collection[0].origin
        String origin2 = json.collection[-1].origin

        result = UserAPI.list( "origin", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        assert json.collection.size() == 1
        assert json.collection[0].origin == origin1

        result = UserAPI.list("origin", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        assert json.collection.size() == 1
        assert json.collection[0].origin == origin2
    }

    void testProjectUserSort(){
        User u1 = BasicInstanceBuilder.getUserNotExist(true)
        User u2 = BasicInstanceBuilder.getUserNotExist(true)
        User u3 = BasicInstanceBuilder.getUserNotExist(true)
        u1.lastname = BasicInstanceBuilder.getRandomString()
        u1.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u2.lastname = BasicInstanceBuilder.getRandomString()
        u2.email = u1.email
        u3.lastname = BasicInstanceBuilder.getRandomString()
        u3.email = BasicInstanceBuilder.getRandomString()+"@test.be"
        u1.save(failOnError: true)
        u2.save(failOnError: true)
        u3.save(failOnError: true)
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id, u1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addAdminProject(project.id, u2.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(project.id, u3.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        def consultation = BasicInstanceBuilder.getImageConsultationNotExist()
        consultation.project = project.id
        consultation.user = u1.id
        consultation.insert(flush: true, failOnError:true)

        def connection = BasicInstanceBuilder.getProjectConnection()
        connection.user = u1.id
        connection.project = project.id
        connection.insert(flush: true, failOnError:true)

        u1 = u1.refresh()
        u2 = u2.refresh()
        u3 = u3.refresh()


        def result = UserAPI.list(project.id,"project","user", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        Long totalSize = json.size
        assert totalSize == 4

        //---------------- Sort by lastImageName

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastImageName", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        Long size = json.size
        assert json.collection.size() == size
        Long id1 = json.collection[0].id
        Long id2 = json.collection[-1].id
        assert json.collection[0].lastImage instanceof JSONObject.Null
        assert !(json.collection[-1].lastImage instanceof JSONObject.Null)

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastImageName", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastImageName", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2





        ArrayList searchParameters = [[operator : "ilike", field : "fullName", value:u1.email]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size == 2
        assert json.collection.size() == size

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "lastImageName", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id
        assert json.collection[0].lastImage instanceof JSONObject.Null
        assert !(json.collection[-1].lastImage instanceof JSONObject.Null)
        assert json.collection[0].containsKey("lastConnection")

        result = UserAPI.searchAndList(project.id,"project","user", true, false, true, searchParameters, "lastImageName", "asc", 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1
        assert !json.collection[0].containsKey("lastConnection")

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "lastImageName", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2



        //---------------- Sort by lastConnection

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastConnection", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        size = totalSize
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id
        assert json.collection[0].lastConnection instanceof JSONObject.Null
        assert !(json.collection[-1].lastConnection instanceof JSONObject.Null)

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastConnection", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.list(project.id,"project","user", true, true, true, "lastConnection", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2



        searchParameters = [[operator : "ilike", field : "fullName", value:u1.email]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size == 2
        assert json.collection.size() == size

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "lastConnection", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id
        assert json.collection[0].lastConnection instanceof JSONObject.Null
        assert !(json.collection[-1].lastConnection instanceof JSONObject.Null)

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "lastConnection", "asc", 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "lastConnection", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        //---------------- Sort by frequency


        result = UserAPI.list(project.id,"project","user", true, true, true, "frequency", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        size = totalSize
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id
        assert json.collection[0].numberConnections instanceof JSONObject.Null
        assert !(json.collection[-1].numberConnections instanceof JSONObject.Null)

        result = UserAPI.list(project.id,"project","user", true, true, true, "frequency", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.list(project.id,"project","user", true, true, true, "frequency", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2



        searchParameters = [[operator : "ilike", field : "fullName", value:u1.email]]

        result = UserAPI.searchAndList(project.id,"project","user", searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size == 2
        assert json.collection.size() == size

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "frequency", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id
        assert json.collection[0].numberConnections instanceof JSONObject.Null
        assert !(json.collection[-1].numberConnections instanceof JSONObject.Null)

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "frequency", "asc", 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.searchAndList(project.id,"project","user", true, true, true, searchParameters, "frequency", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        //---------------- Sort by projectRole


        result = UserAPI.list(project.id,"project","user", true, true, true, "projectRole", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == totalSize
        size = totalSize
        assert json.collection.size() == size
        id1 = json.collection[0].id
        id2 = json.collection[-1].id

        result = UserAPI.list(project.id,"project","user", true, true, true, "projectRole", "asc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = UserAPI.list(project.id,"project","user", true, true, true, "projectRole", "desc", 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id != id1
    }

}
