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
import be.cytomine.project.ProjectRepresentativeUser
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.ProjectRepresentativeUserAPI
import be.cytomine.test.http.UserAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by hoyoux on 13.11.14.
 */
class ProjectRepresentativeUserTests {

    void testListAllProjectRepresentativeUserByProjectWithCredential() {
        ProjectRepresentativeUser ref = BasicInstanceBuilder.getProjectRepresentativeUser();
        def result = ProjectRepresentativeUserAPI.list(ref.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListAllProjectRepresentativeUserByProjectWithoutCredential() {
        Project project = BasicInstanceBuilder.getProjectRepresentativeUser().getProject();
        def result = ProjectRepresentativeUserAPI.list(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testShowProjectRepresentativeUserWithCredential() {
        ProjectRepresentativeUser ref = BasicInstanceBuilder.getProjectRepresentativeUser()
        def result = ProjectRepresentativeUserAPI.show(ref.id, ref.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddProjectRepresentativeUserCorrect() {
        def refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()

        def result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        println result
        int idRef = result.data.id

        result = ProjectRepresentativeUserAPI.show(idRef, refToAdd.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.undo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(idRef, refToAdd.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = ProjectRepresentativeUserAPI.redo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(idRef, refToAdd.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddProjectRepresentativeUserAlreadyExist() {
        def refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist();

        def result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code // first creation

        result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code // cannot create if already exist
    }

    void testDeleteProjectRepresentativeUser() {
        def refToDelete = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        assert refToDelete.save(flush: true)!= null
        def id = refToDelete.id
        def idProject = refToDelete.project.id
        def result = ProjectRepresentativeUserAPI.delete(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = ProjectRepresentativeUserAPI.undo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.redo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteProjectRepresentativeUserByProjectAndUser() {
        ProjectRepresentativeUser refToDelete = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        assert refToDelete.save(flush: true)!= null
        def id = refToDelete.id
        def idProject = refToDelete.project.id
        def idUser = refToDelete.user.id
        def result = ProjectRepresentativeUserAPI.deleteByUser(idUser, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = ProjectRepresentativeUserAPI.undo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.redo()
        assert 200 == result.code

        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteDependentUser() {
        //init user creator
        def USERNAME2 = "user2";
        def PASSWORD2 = "password";

        BasicInstanceBuilder.getUser2()
        User user = BasicInstanceBuilder.getUser1()

        //Create new project (user1)
        def resultProject = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME2,PASSWORD2)

        Project project = Project.get(resultProject.data.id)
        def resAddUser = ProjectAPI.addUserProject(project.id,user.id,USERNAME2,PASSWORD2)
        assert 200 == resAddUser.code

        //create new representative
        def representative = new ProjectRepresentativeUser(project: project, user: user)
        def result = ProjectRepresentativeUserAPI.create(representative.encodeAsJSON(), USERNAME2, PASSWORD2)

        //check if create == good HTTP Code
        assert 200 == result.code

        Long id = result.data.id
        Long idProject = result.data.project.id

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAPI.delete(representative.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code



    }

    void testDeleteProjectRepresentativeUserNotExist() {
        def result = ProjectRepresentativeUserAPI.delete(-99, -99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}
