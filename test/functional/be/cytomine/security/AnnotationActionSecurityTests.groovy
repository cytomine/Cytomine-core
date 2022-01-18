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

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.social.AnnotationAction
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationActionAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

class AnnotationActionSecurityTests extends SecurityTestsAbstract{


    void testAnnotationActionSecurityForCytomineAdmin() {

        //Get user1
        User user1 = getUser1()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        //Add annotation action
        AnnotationAction action = BasicInstanceBuilder.getAnnotationActionNotExist(annot)

        //check if admin user can access/update/delete
        result = AnnotationActionAPI.create(action.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id,SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.countByProject(project.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert JSON.parse(result.data).total == 1
    }

    void testAnnotationActionSecurityForProjectAdmin() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addAdminProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        //Add annotation action
        AnnotationAction action = BasicInstanceBuilder.getAnnotationActionNotExist(annot)

        //check if admin user can access/update/delete
        result = AnnotationActionAPI.create(action.encodeAsJSON(),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id,SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert JSON.parse(result.data).total == 1
    }

    void testAnnotationActionSecurityForProjectUser() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        //Add annotation action
        AnnotationAction action = BasicInstanceBuilder.getAnnotationActionNotExist(annot)

        //check if admin user can access/update/delete
        result = AnnotationActionAPI.create(action.encodeAsJSON(),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id,SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AnnotationActionAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
    }

    void testAnnotationActionSecurityForSimpleUser() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, true)
        //Add annotation action
        AnnotationAction action = BasicInstanceBuilder.getAnnotationActionNotExist(annot)

        //check if admin user can access/update/delete
        result = AnnotationActionAPI.create(action.encodeAsJSON(),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id,SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AnnotationActionAPI.countByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
    }
}
