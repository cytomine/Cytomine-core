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
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.JobAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class UserTests  {

    void testListUserWithCredential() {
        def result = UserAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListUserWithKey() {
        def result = UserAPI.list(BasicInstanceBuilder.user1.publicKey,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        println json
        assert json.id==BasicInstanceBuilder.user1.id
    }
  
    void testListUserWithoutCredential() {
        def result = UserAPI.list(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }


    void testListFriends() {
        def user = BasicInstanceBuilder.user1
        def project = BasicInstanceBuilder.getProject()
        Infos.addUserRight(user.username,project)
        def result = UserAPI.listFriends(user.id,false,project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAPI.listFriends(user.id,true,project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = UserAPI.listFriends(user.id,false,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAPI.listFriends(user.id,true,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListOnlineFriendsWithOpenedImages() {
        def project = BasicInstanceBuilder.getProject()
        def result = UserAPI.listOnline(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testShowUserWithId() {
        def result = UserAPI.show(BasicInstanceBuilder.getUser().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testShowUserWithUsername() {
        def result = UserAPI.show(BasicInstanceBuilder.getUser().username, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testShowKeysWithUsername() {
        def user = BasicInstanceBuilder.getUser()
        def result = UserAPI.keys(user.username, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.publicKey.equals(user.publicKey)
        assert json.privateKey.equals(user.privateKey)

        result = UserAPI.keys(user.username, user.username, "password")
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.publicKey.equals(user.publicKey)
        assert json.privateKey.equals(user.privateKey)
    }

    void testShowKeysWithId() {
        def user = BasicInstanceBuilder.getUser()
        def result = UserAPI.keys(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.publicKey.equals(user.publicKey)
        assert json.privateKey.equals(user.privateKey)

        result = UserAPI.keys(user.id, user.username, "password")
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.publicKey.equals(user.publicKey)
        assert json.privateKey.equals(user.privateKey)
    }


    void testShowCurrentUser() {
        def result = UserAPI.showCurrent(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }


    void testListProjectCreator() {
        def project = BasicInstanceBuilder.getProject()
        def result = UserAPI.list(project.id,"project","creator",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = UserAPI.list(-99,"project","creator",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListProjectLayer() {
        def project = BasicInstanceBuilder.getProject()
        def result = UserAPI.list(project.id,"project","userlayer",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = UserAPI.list(-99,"project","userlayer",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddUserCorrect() {
        User userToAdd = BasicInstanceBuilder.getUserNotExist()
        def jsonUser = new JSONObject(userToAdd.encodeAsJSON()).put("password", "password").toString()
        println "jsonUser =" + jsonUser
        def result = UserAPI.create(jsonUser.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUser = result.data.id
  
        result = UserAPI.show(idUser, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
  
    void testAddUserAlreadyExist() {
        def userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.username = BasicInstanceBuilder.getUser().username
        def result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testAddUserNameAlreadyExist() {
        def user = BasicInstanceBuilder.getUser()
        def userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.username = user.username
        def result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code

        userToAdd.username = ""
        int i = Math.abs(((new Random()).nextInt())%(user.username.size()))

        userToAdd.username += user.username.substring(0,i)
        if(user.username.charAt(i).toUpperCase() == user.username.charAt(i)) {
            userToAdd.username += user.username.charAt(i).toLowerCase()
        } else {
            userToAdd.username += user.username.charAt(i).toUpperCase()
        }
        userToAdd.username += user.username.substring(i+1)

        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code

    }

    void testAddUserInvalidPassword() {
        def userToAdd = BasicInstanceBuilder.getUserNotExist()
        JSONElement jsonWithPassword = JSON.parse(userToAdd.encodeAsJSON())
        jsonWithPassword.password = "123456"

        def result = UserAPI.create(jsonWithPassword.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code

        userToAdd = BasicInstanceBuilder.getUserNotExist()
        jsonWithPassword = JSON.parse(userToAdd.encodeAsJSON())
        jsonWithPassword.password = "12345678"

        result = UserAPI.create(jsonWithPassword.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAPI.checkPassword("12345678",userToAdd.username,"12345678")
        assert 200 == result .code
    }

    void testAddUserInvalidUsername() {
        def userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.username = " invalid "
        def result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code

        userToAdd.username = "invalid "
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code

        userToAdd.username = " invalid"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code

        userToAdd.username = "is valid"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        userToAdd.username = "valid.92_06"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        userToAdd.username = "ù%Ôë.ã6."
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        userToAdd.username = "Jean-Charles-Marc-Édouard"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserInvalidEmail() {
        def user = BasicInstanceBuilder.getUser()
        def userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.email = "invalid@email"
        def result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
        userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.email = "somperson@someagency.agency"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        userToAdd = BasicInstanceBuilder.getUserNotExist()
        userToAdd.email = "somperson@someschool.school"
        result = UserAPI.create(userToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testUpdateUserCorrect() {
        def user = BasicInstanceBuilder.getUserNotExist(true)
        def data = UpdateData.createUpdateSet(user,[firstname: ["OLDNAME","NEWNAME"], email:["old@email.com","new@email.com"]])

        def result = UserAPI.update(user.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idUser = json.user.id
  
        def showResult = UserAPI.show(idUser, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }
  
    void testUpdateUserNotExist() {
        User userWithOldName = BasicInstanceBuilder.getUser()
        User userWithNewName = BasicInstanceBuilder.getUserNotExist()
        userWithNewName.save(flush: true)
        User userToEdit = User.get(userWithNewName.id)
        def jsonUser = userToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonUser)
        jsonUpdate.username = "-99"
        jsonUpdate.id = -99
        jsonUser = jsonUpdate.toString()
        def result = UserAPI.update(-99, jsonUser, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
  
    void testUpdateUserWithNameAlreadyExist() {
        User userWithOldName = BasicInstanceBuilder.getUser()
        User userWithNewName = BasicInstanceBuilder.getUserNotExist()
        userWithNewName.save(flush: true)
        User userToEdit = User.get(userWithNewName.id)
        def jsonUser = userToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonUser)
        jsonUpdate.username = userWithOldName.username
        jsonUser = jsonUpdate.toString()
        def result = UserAPI.update(userToEdit.id, jsonUser, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testDeleteUser() {
        def userToDelete = BasicInstanceBuilder.getUserNotExist()
        assert userToDelete.save(flush: true)!= null
        def id = userToDelete.id
        def result = UserAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
  
        def showResult = UserAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }
  
    void testDeleteUserNotExist() {
        def result = UserAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
    
    void testDeleteMe() {
        def result = UserAPI.delete(User.findByUsername(Infos.SUPERADMINLOGIN).id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403==result.code
    }

    // SHOW USER JOB

    void testShowUserJob() {
        def userJob = BasicInstanceBuilder.getUserJob()
        def result = UserAPI.showUserJob(userJob.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = UserAPI.showUserJob(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListUserJob() {
        def userJob = BasicInstanceBuilder.getUserJob()
        def result = UserAPI.listUserJob(userJob.job.project.id,false,null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testListUserJobTree() {
        def userJob = BasicInstanceBuilder.getUserJob()
        def result = UserAPI.listUserJob(userJob.job.project.id,true,null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testListUserJobByImages() {
        def userJob = BasicInstanceBuilder.getUserJob()
        def result = UserAPI.listUserJob(userJob.job.project.id,false,BasicInstanceBuilder.getImageInstance().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }



    void testAddUserJobCorrect() {
        log.info("create user")
        def parent = User.findByUsername(Infos.SUPERADMINLOGIN);
        def json = "{parent:"+ parent.id +", username:"+ Math.random()+", software: ${BasicInstanceBuilder.getSoftware().id}, project: ${BasicInstanceBuilder.getProject().id}}";

        log.info("post user child")
        def response = JobAPI.createUserJob(json.toString(), Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)

        log.info("check response")
        assert 200==response.code
    }

    void testAddTwoUserJobAtTheSameTime() {
        log.info("create user")
        def parent = User.findByUsername(Infos.SUPERADMINLOGIN);
        def json = "{parent:"+ parent.id +", username:"+ Math.random()+", software: ${BasicInstanceBuilder.getSoftware().id}, project: ${BasicInstanceBuilder.getProject().id}}";

        log.info("post user child")
        def response = JobAPI.createUserJob(json.toString(), Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)

        log.info("check response")
        assert 200==response.code

        log.info("post user child")
        response = JobAPI.createUserJob(json.toString(), Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)

        log.info("check response")
        assert 200==response.code
    }



    void testListLayerAllLayers() {
        def simpleUsername1 = "simpleUserListLayer1"
        def simpleUsername2 = "simpleUserListLayer2"
        def adminUsername = "adminRO"
        def password = "password"
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        //by default all is visible!
        assert !project.hideUsersLayers
        assert !project.hideAdminsLayers

        //Add a simple project user
        User simpleUser1 = BasicInstanceBuilder.getUser(simpleUsername1,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a simple project user
        User simpleUser2 = BasicInstanceBuilder.getUser(simpleUsername2,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //a admin must see all layers
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser1.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser2.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),admin.id)
        //a simple user must see all layers too
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser1.id)
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser2.id)
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),admin.id)
    }

    void testListLayerAllLayersHideAdminLayers() {
        def simpleUsername1 = "simpleUserListLayer1"
        def simpleUsername2 = "simpleUserListLayer2"
        def adminUsername = "adminRO"
        def password = "password"
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        project.hideAdminsLayers = true
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser1 = BasicInstanceBuilder.getUser(simpleUsername1,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a simple project user
        User simpleUser2 = BasicInstanceBuilder.getUser(simpleUsername2,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //a admin must see all layers
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser1.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser2.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),admin.id)
        //a simple user must see all layers exept admins layer
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser1.id)
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser2.id)
        assert !checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),admin.id)
    }

    void testListLayerAllLayersHideUserLayers() {
        def simpleUsername1 = "simpleUserListLayer1"
        def simpleUsername2 = "simpleUserListLayer2"
        def adminUsername = "adminRO"
        def password = "password"
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        project.hideUsersLayers = true
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser1 = BasicInstanceBuilder.getUser(simpleUsername1,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a simple project user
        User simpleUser2 = BasicInstanceBuilder.getUser(simpleUsername2,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //a admin must see all layers
        def layers = UserAPI.listLayers(project.id,adminUsername,password)
        assert checkIfContains(layers,simpleUser1.id)
        assert checkIfContains(layers,simpleUser2.id)
        assert checkIfContains(layers,admin.id)
        //a simple user must see all admin layers and his layer
        layers = UserAPI.listLayers(project.id,simpleUsername1,password)
        assert checkIfContains(layers,simpleUser1.id)
        assert !checkIfContains(layers,simpleUser2.id)
        assert checkIfContains(layers,admin.id)
    }

    void testListLayerAllLayersHideUserLayersAndHideAdminLayers() {
        def simpleUsername1 = "simpleUserListLayer1"
        def simpleUsername2 = "simpleUserListLayer2"
        def adminUsername = "adminRO"
        def password = "password"
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        project.hideUsersLayers = true
        project.hideAdminsLayers = true
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser1 = BasicInstanceBuilder.getUser(simpleUsername1,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a simple project user
        User simpleUser2 = BasicInstanceBuilder.getUser(simpleUsername2,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //a admin must see all layers
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser1.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),simpleUser2.id)
        assert checkIfContains(UserAPI.listLayers(project.id,adminUsername,password),admin.id)
        //a simple user must see only its own layer
        assert checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser1.id)
        assert !checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),simpleUser2.id)
        assert !checkIfContains(UserAPI.listLayers(project.id,simpleUsername1,password),admin.id)
    }

    static boolean checkIfContains(def result, def id) {
        assert 200 == result.code
        def json = JSON.parse(result.data)
        return UserAPI.containsInJSONList(id,json)
    }


    void testAPIGetSignature() {
         ///api/signature
         assert 200 == UserAPI.signature(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
     }


    void testResetPassword() {
        User user = BasicInstanceBuilder.getUserNotExist(true)

        //just call a simple service to check
        assert 200 == UserAPI.signature(user.username,"password").code

        //change password
        def response = UserAPI.resetPassword(user.id,"newpassword",Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == response.code
        println response
        //just call a simple service to check if password  has changed
        assert 200 == UserAPI.signature(user.username,"newpassword").code
        assert 401 == UserAPI.signature(user.username,"password").code

    }

    void testCheckPassword() {

        def response = UserAPI.checkPassword(Infos.SUPERADMINPASSWORD,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == response.code

        response = UserAPI.checkPassword("test",Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 401 == response.code
    }

    void testResetPasswordWithBadUser() {
        //change password
        assert 404 == UserAPI.resetPassword(-99,"newpassword",Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
    }

    void testResetPasswordWithBadPassword() {
        User user = BasicInstanceBuilder.getUserNotExist(true)
        assert 404 == UserAPI.resetPassword(user.id,null,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
        assert 400 == UserAPI.resetPassword(user.id,"bad",Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
    }


    void testResetPasswordWithUnauthUser() {
        User user = BasicInstanceBuilder.getUserNotExist(true)
        User anotherUser = BasicInstanceBuilder.getUserNotExist(true)

        //just call a simple service to check
        assert 200 == UserAPI.signature(user.username,"password").code

        //change password
        assert 200 == UserAPI.resetPassword(user.id,"newpassword",user.username,"password").code

        //just call a simple service to check if password  has changed
        assert 200 == UserAPI.signature(user.username,"newpassword").code
        assert 401 == UserAPI.signature(user.username,"password").code

        //change password with another user credential
        assert 403 == UserAPI.resetPassword(user.id,"unauthpassword",anotherUser.username,"password").code
        //just call a simple service to check if password  has changed
        assert 401 == UserAPI.signature(user.username,"unauthpassword").code
        assert 200 == UserAPI.signature(user.username,"newpassword").code
    }

    void testListUsersWithLastActivity(){
        def connection = BasicInstanceBuilder.getProjectConnection(true)

        def result = UserAPI.listUsersWithLastActivity(connection.project, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
    }

    void testLockUser() {
        User user = BasicInstanceBuilder.getUserNotExist(true);
        def result = UserAPI.lock(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAPI.show(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.enabled == false

        result = UserAPI.lock(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code


        result = UserAPI.unlock(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        result = UserAPI.show(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.enabled == true

        result = UserAPI.unlock(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

}
