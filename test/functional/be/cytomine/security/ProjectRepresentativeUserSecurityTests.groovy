package be.cytomine.security

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
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.ProjectRepresentativeUserAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * Created by hoyoux on 13.11.14.
 */
class ProjectRepresentativeUserSecurityTests extends SecurityTestsAbstract {

    void testSecurityForCytomineAdmin() {

        ProjectRepresentativeUser ref = BasicInstanceBuilder.getProjectRepresentativeUserNotExist();
        def result = ProjectRepresentativeUserAPI.create(ref.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        //check if create == good HTTP Code
        assert 200 == result.code
        Long id = result.data.id
        Long idProject = result.data.project.id

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //check if delete == good HTTP Code
        result = ProjectRepresentativeUserAPI.delete(id, idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testSecurityForProjectCreator() {
        //init user
        def USERNAME1 = "user1";
        def PASSWORD1 = "password";
        def USERNAME2 = "user2";
        def PASSWORD2 = "password";


        //Get user1 crator of project and user 2 representative of this project
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data


        //create new representative
        def ref = new ProjectRepresentativeUser(project: project, user: user1)
        result = ProjectRepresentativeUserAPI.create(ref.encodeAsJSON(), USERNAME1, PASSWORD1)
        //check if create == good HTTP Code
        assert 200 == result.code

        Long id = result.data.id
        Long idProject = result.data.project.id

        ProjectRepresentativeUser representative = result.data;

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, USERNAME1, PASSWORD1)
        assert 200 == result.code

        //check if delete == good HTTP Code
        result = ProjectRepresentativeUserAPI.delete(id, idProject, USERNAME1, PASSWORD1)
        assert 200 == result.code



        //create new representative not yet added to the project
        ref = new ProjectRepresentativeUser(project: project, user: user2)
        result = ProjectRepresentativeUserAPI.create(ref.encodeAsJSON(), USERNAME1, PASSWORD1)
        //check if create == good HTTP Code
        assert 400 == result.code


        // than add to the project then retry
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        result = ProjectRepresentativeUserAPI.create(ref.encodeAsJSON(), USERNAME1, PASSWORD1)
        //check if create == good HTTP Code
        assert 200 == result.code

        id = result.data.id
        idProject = result.data.project.id

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, USERNAME1, PASSWORD1)
        assert 200 == result.code

        //check if delete == good HTTP Code
        result = ProjectRepresentativeUserAPI.delete(id, idProject, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testSecurityForSimpleUser() {
        //init user creator
        def USERNAME2 = "user2";
        def PASSWORD2 = "password";

        User creator = BasicInstanceBuilder.getUser2()
        //init user tester
        def USERNAME1 = "user1";
        def PASSWORD1 = "password";

        User user = BasicInstanceBuilder.getUser1()

        //Create new project (user1)
        def resultProject = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME2,PASSWORD2)

        Project project = Project.get(resultProject.data.id)
        def resAddUser = ProjectAPI.addUserProject(project.id,user.id,USERNAME2,PASSWORD2)
        assert 200 == resAddUser.code

        //create new representative
        def representative = new ProjectRepresentativeUser(project: project, user: user)
        def result = ProjectRepresentativeUserAPI.create(representative.encodeAsJSON(), USERNAME1, PASSWORD1)

        //check if create == good HTTP Code
        assert 403 == result.code

        //create new representative by the project owner
        result = ProjectRepresentativeUserAPI.create(representative.encodeAsJSON(), USERNAME2, PASSWORD2)

        //check if create == good HTTP Code
        assert 200 == result.code

        Long id = result.data.id
        Long idProject = result.data.project.id

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, USERNAME1, PASSWORD1)
        assert 200 == result.code

        //check if delete == good HTTP Code
        result = ProjectRepresentativeUserAPI.delete(id, idProject, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }

    void testSecurityForAnonymous() {
        //create new representative
        def representative = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        def result = ProjectRepresentativeUserAPI.create(representative.encodeAsJSON(), Infos.BADLOGIN, Infos.BADPASSWORD)

        //check if create == good HTTP Code
        assert 401 == result.code

        //create a representative for the end of the test
        representative = BasicInstanceBuilder.getProjectRepresentativeUser()

        Long id = representative.id
        Long idProject = representative.project.id

        //check if show == good HTTP Code
        result = ProjectRepresentativeUserAPI.show(id, idProject, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code

        //check if delete == good HTTP Code
        result = ProjectRepresentativeUserAPI.delete(id, idProject, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }
}