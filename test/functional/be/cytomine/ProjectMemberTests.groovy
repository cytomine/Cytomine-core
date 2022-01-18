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

import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class ProjectMemberTests {

    void testListProjectMember() {
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        result = UserAPI.list(project.id,"project","user",false, false, false, true, (String) null, (String) null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        result = UserAPI.list(-99,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAPI.list(-99,"project","user", Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testListProjectMemberWithInfos() {
        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        assert !(((JSONObject)json.collection[0]).has("lastImage"))
        assert !(((JSONObject)json.collection[0]).has("lastConnection"))
        assert !(((JSONObject)json.collection[0]).has("numberConnections"))

        result = UserAPI.listWithConsultationInformation(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert (((JSONObject)json.collection[0]).has("lastImage"))
        assert (((JSONObject)json.collection[0]).has("lastConnection"))
        assert (((JSONObject)json.collection[0]).has("numberConnections"))
        assert json.collection[0].lastImage instanceof JSONObject.Null
        assert json.collection[0].lastConnection instanceof JSONObject.Null
        assert json.collection[0].numberConnections instanceof JSONObject.Null

        BasicInstanceBuilder.getImageConsultationNotExist(project.id,true)

        result = UserAPI.listWithConsultationInformation(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
        assert !(json.collection[0].lastImage instanceof JSONObject.Null)
        assert !(json.collection[0].lastConnection instanceof JSONObject.Null)
        assert !(json.collection[0].numberConnections instanceof JSONObject.Null)
    }


    void testListProjectAdmin() {
        def project = BasicInstanceBuilder.getProject()
        def result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = UserAPI.list(-99,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddDeleteUserToProject() {
        def project = BasicInstanceBuilder.getProjectNotExist()
        BasicInstanceBuilder.saveDomain(project)
        User u1 = BasicInstanceBuilder.user1

        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)


        //Add project right for user 1
        def resAddUser = ProjectAPI.addUserProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert UserAPI.containsInJSONList(u1.id,json)

        resAddUser = ProjectAPI.deleteUserProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)
    }

    void testAddAndDeleteMultipleMembers(){
        def project = BasicInstanceBuilder.getProjectNotExist()
        BasicInstanceBuilder.saveDomain(project)
        def users = []
        for(int i = 0; i< 10; i++){
            users << BasicInstanceBuilder.getUserNotExist(true)
        }

        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        Long size = json.size

        def userIds = users.collect{it.id}

        //Add
        def resAddUser = ProjectAPI.addUsersProject(project.id, userIds, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()

        resAddUser = ProjectAPI.addUsersProject(project.id, [-99], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 206 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()

        resAddUser = ProjectAPI.addUsersProject(project.id, ["not_long"], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 206 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()

        //Delete
        resAddUser = ProjectAPI.deleteUsersProject(project.id, userIds.subList(0, 2), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()-2

        resAddUser = ProjectAPI.deleteUsersProject(project.id, [-99], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 206 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()-2

        resAddUser = ProjectAPI.deleteUsersProject(project.id, ["not_long"], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 206 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json.size == size + users.size()-2
    }

    void testAddDeleteAdminToProject() {
        def project = BasicInstanceBuilder.getProjectNotExist()
        BasicInstanceBuilder.saveDomain(project)
        User u1 = BasicInstanceBuilder.user1
        def result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)

        //Add project right for user 2
        def resAddUser = ProjectAPI.addAdminProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert UserAPI.containsInJSONList(u1.id,json)

        resAddUser = ProjectAPI.deleteAdminProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)
    }

    void testAddDeleteUserToProjectNoOntology() {
        def project = BasicInstanceBuilder.getProjectNotExist(null, true)
        User u1 = BasicInstanceBuilder.user1
        def result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)

        //Add project right for user 1
        def resAddUser = ProjectAPI.addUserProject(project.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert UserAPI.containsInJSONList(u1.id,json)

        resAddUser = ProjectAPI.deleteUserProject(project.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)
    }

    void testAddDeleteAdminToProjectNoOntology() {
        def project = BasicInstanceBuilder.getProjectNotExist(null, true)
        User u1 = BasicInstanceBuilder.user1
        def result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)

        //Add project right for user 1
        def resAddUser = ProjectAPI.addAdminProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert UserAPI.containsInJSONList(u1.id,json)

        resAddUser = ProjectAPI.deleteAdminProject(project.id, u1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        result = UserAPI.list(project.id,"project","admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert !UserAPI.containsInJSONList(u1.id,json)
    }


}
