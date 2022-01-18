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
import be.cytomine.image.SliceInstance
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.SliceInstanceAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

class SliceInstanceSecurityTests extends SecurityTestsAbstract{


    void testSliceInstanceSecurityForCytomineAdmin() {

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
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image)
        //check if admin user can access/update/delete
        result = SliceInstanceAPI.create(slice.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        slice = result.data
        assert (200 == SliceInstanceAPI.show(slice.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
        result = SliceInstanceAPI.listByImageInstance(image.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert (true ==SliceInstanceAPI.containsInJSONList(slice.id,JSON.parse(result.data)))
        assert (200 == SliceInstanceAPI.update(slice.id,slice.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
        assert (200 == SliceInstanceAPI.delete(slice,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
    }

    void testSliceInstanceSecurityForProjectAdmin() {

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
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image)

        //check if user 2 can access/update/delete
        result = SliceInstanceAPI.create(slice.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        slice = result.data
        assert (200 == SliceInstanceAPI.show(slice.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        result = SliceInstanceAPI.listByImageInstance(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true ==SliceInstanceAPI.containsInJSONList(slice.id,JSON.parse(result.data)))
        //assert (200 == SliceInstanceAPI.update(image,USERNAME2,PASSWORD2).code)
        assert (200 == SliceInstanceAPI.delete(slice,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
    }


    void testSliceInstanceSecurityForProjectUser() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()
        User user3 = getUser3()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project.id,user3.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image)

        //check if user 2 can access/update/delete
        result = SliceInstanceAPI.create(slice.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        slice = result.data
        assert (200 == SliceInstanceAPI.show(slice.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        result = SliceInstanceAPI.listByImageInstance(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true ==SliceInstanceAPI.containsInJSONList(slice.id,JSON.parse(result.data)))
        //assert (200 == SliceInstanceAPI.update(image,USERNAME2,PASSWORD2).code)

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)
        assert (200 == SliceInstanceAPI.delete(slice,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)
        assert (403 == SliceInstanceAPI.delete(slice,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code)
    }

    void testSliceInstanceSecurityForSimpleUser() {

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
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image)

        //check if simple  user can access/update/delete
        result = SliceInstanceAPI.create(slice.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert (403 == result.code)

        slice = BasicInstanceBuilder.getSliceInstance()
        slice.image = image
        slice.save(flush:true)

        assert (403 == SliceInstanceAPI.show(slice.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 ==SliceInstanceAPI.listByImageInstance(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        //assert (403 == SliceInstanceAPI.update(slice,USERNAME2,PASSWORD2).code)
        assert (403 == SliceInstanceAPI.delete(slice,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
    }

}
