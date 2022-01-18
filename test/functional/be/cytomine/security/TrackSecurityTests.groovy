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
import be.cytomine.ontology.Track
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.TrackAPI
import grails.converters.JSON

class TrackSecurityTests extends SecurityTestsAbstract {


    void testTrackSecurityForCytomineAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //Create new track (user1)
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist(image)
        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Track track = result.data
        //check if admin user can access/update/delete
        assert (200 == TrackAPI.show(track.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAMEADMIN,PASSWORDADMIN).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAMEADMIN,PASSWORDADMIN).data))
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == TrackAPI.delete(track.id,USERNAMEADMIN,PASSWORDADMIN).code)
    }

    void testTrackSecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //Create new track (user1)
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist(image)
        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Track track = result.data

        //check if user 2 cannot access/update/delete
        assert (403 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert !TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert !TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (403 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)
    }

    void testTrackSecurityForAnonymous() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //Create new track (user1)
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist(image)
        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Track track = result.data

        //check if not user cannot access/update/delete
        assert (401 == TrackAPI.show(track.id,USERNAMEBAD,PASSWORDBAD).code)
        assert !TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAMEBAD,PASSWORDBAD).data))
        assert !TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAMEBAD,PASSWORDBAD).data))
        assert (401 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == TrackAPI.delete(track.id,USERNAMEBAD,PASSWORDBAD).code)
    }


    void testTrackSecurityForProjectManager() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)
        //Get user2
        User user3 = BasicInstanceBuilder.getUser(USERNAME3,PASSWORD3)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //add user1 and user2 as manager of project
        ProjectAPI.addAdminProject(project.id, user1.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        ProjectAPI.addAdminProject(project.id, user2.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)

        //Create new track (user1)
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist(image)
        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Track track = result.data

        //check if user 2 cannot access/update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        track = result.data

        //check if user 2 cannot access/update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        track = result.data

        //check if user 2 cannot access/update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)

    }

    void testTrackSecurityForProjectUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)
        //Get user2
        User user3 = BasicInstanceBuilder.getUser(USERNAME3,PASSWORD3)

        //Create new project (user3)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //add user1 and user2 as manager of project
        ProjectAPI.addUserProject(project.id, user1.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)

        //Create new track (user1)
        def trackToAdd = BasicInstanceBuilder.getTrackNotExist(image)
        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Track track = result.data

        //check if user 2 cannot access/update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (200 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)


        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        track = result.data

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = TrackAPI.create(trackToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 403 == result.code

        //check if user 2 cannot update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (403 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (403 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        //check if user 2 cannot update/delete
        assert (200 == TrackAPI.show(track.id,USERNAME2,PASSWORD2).code)
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByProject(track.project.id,USERNAME2,PASSWORD2).data))
        assert TrackAPI.containsInJSONList(track.id,JSON.parse(TrackAPI.listByImageInstance(track.image.id,USERNAME2,PASSWORD2).data))
        assert (403 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (403 == TrackAPI.update(track.id,track.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == TrackAPI.delete(track.id,USERNAME2,PASSWORD2).code)

    }
}
