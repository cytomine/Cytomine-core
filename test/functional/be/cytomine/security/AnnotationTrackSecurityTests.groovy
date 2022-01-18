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

import be.cytomine.ontology.AnnotationTrack
import be.cytomine.ontology.Track
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationTrackAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAnnotationAPI
import grails.converters.JSON

class AnnotationTrackSecurityTests extends SecurityTestsAbstract {

    void testAnnotationTrackSecurityForCytomineAdmin() {
        //Get User 1
        User user = getUser1()

        //Get cytomine admin
        User admin = getUserAdmin()

        //Create Annotation with user 1
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation-Term for annotation 1 with cytomine admin
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id

        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        annotationTrack = result.data

        //Get/List annotation-term with cytomine admin
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert (true ==AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        //Delete annotation 2 with cytomine admin
        assert (200 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN).code)


    }

    void testAnnotationTrackSecurityForAnnotationTrackCreator() {
        //Get User 1
        User user = getUser1()

        //Add annotation 1 with cytomine admin
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.addUserRight(user,annotation.project)

        //Add annotation-Term for annotation 1 with cytomine admin
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstanceNotExist(annotation.project, true), true)
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track

        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotationTrack = result.data

        //Get/List annotation-term with cytomine admin
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (true ==AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        //Delete annotation 2 with cytomine admin
        assert (200 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
    }

    void testAnnotationTrackSecurityForProjectUser() {
        // Get User 1
        User user = getUser1()

        // Get User 2
        User user2 = getUser2()

        // Create project and annotation with user 1
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.addUserRight(user, annotation.project.ontology)

        // Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(annotation.project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == resAddUser.code

        //Add annotation-Term for annotation 1 with cytomine admin
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstanceNotExist(annotation.project, true), true)
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track
        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotationTrack = result.data


        // Get/List annotation term with user 2
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true == AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        // Delete annotation term with user 2
        assert (200 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
    }

    void testAnnotationTrackSecurityForProjectUserWhenAnnotationTrackByAnotherUser() {
        // Get User 0
        User userAdmin = getUserAdmin()

        // Get User 1
        User user = getUser1()

        // Get User 2
        User user2 = getUser2()

        // Create project with user 0 ; annotation with user 1
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        annotation.user = user
        annotation.save(true)

        Project project = annotation.project

        // Add project right for user 1
        def resAddUser = ProjectAPI.addUserProject(annotation.project.id, user.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == resAddUser.code
        // Add project right for user 2
        resAddUser = ProjectAPI.addUserProject(annotation.project.id, user2.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == resAddUser.code

        project.mode = Project.EditingMode.READ_ONLY
        project.save(true)

        // Add annotation term with user 2
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstanceNotExist(annotation.project, true), true)
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track
        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        project.save(true)

        result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        project.save(true)

        result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotationTrack = result.data

        project.mode = Project.EditingMode.READ_ONLY
        project.save(true)

        // Get/List annotation term with user 2
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true == AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        // Delete annotation term with user 1: forbidden in read-only mode
        assert (403 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)

        project.mode = Project.EditingMode.RESTRICTED
        project.save(true)

        // Get/List annotation term with user 2
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true == AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        // Delete annotation term with user 2: forbidden in restricted mode
        assert (403 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        // Delete annotation term with user 1: allowed in restricted mode
        assert (200 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)

        project.mode = Project.EditingMode.CLASSIC
        project.save(true)

        // Add annotation term with user 2
        annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track
        result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotationTrack = result.data

        //Get/List annotation term with user 2
        assert (200 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true == AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        // Delete annotation term with user 2: allowed in classic mode
        assert (200 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent, annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
    }

    void testAnnotationTrackSecurityForUser() {
        //Get User 1
        User user = getUser1()

        //Get User 2
        User user2 = getUser2()

        //Create project with user 1
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation-Term for annotation 1 with cytomine admin
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstanceNotExist(annotation.project, true), true)
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track
        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotationTrack = result.data

        //Get/List annotation-term with cytomine admin
        assert (403 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert (403 == result.code)
        //assert (true ==AnnotationTrackAPI.containsInJSONList(annotationTrack.id, JSON.parse(result.data)))
        //Delete annotation 2 with cytomine admin
        assert (403 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
    }

    void testAnnotationTrackSecurityForAnonymous() {
        //Get User 1
        User user = getUser1()

        //Create project with user 1
        UserAnnotation annotation = UserAnnotationAPI.buildBasicUserAnnotation(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation-Term for annotation 1 with cytomine admin
        Track track = BasicInstanceBuilder.getTrackNotExist(BasicInstanceBuilder.getImageInstanceNotExist(annotation.project, true), true)
        AnnotationTrack annotationTrack = BasicInstanceBuilder.getAnnotationTrackNotExist()
        annotationTrack.annotationIdent = annotation.id
        annotationTrack.track = track
        def result = AnnotationTrackAPI.createAnnotationTrack(annotationTrack.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotationTrack = result.data

        //Get/List annotation-term with cytomine admin
        assert (401 == AnnotationTrackAPI.showAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
        assert (401 == AnnotationTrackAPI.listAnnotationTrackByAnnotation(annotationTrack.annotationIdent, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
        assert (401 == AnnotationTrackAPI.deleteAnnotationTrack(annotationTrack.annotationIdent,annotationTrack.track.id, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
    }
}
