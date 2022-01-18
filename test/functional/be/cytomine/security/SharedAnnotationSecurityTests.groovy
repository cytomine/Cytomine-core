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
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.test.http.AnnotationCommentAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAnnotationAPI
import grails.converters.JSON

class SharedAnnotationSecurityTests extends SecurityTestsAbstract {

    //TODO test algo too

    // initialization block to record in DB the 3 users used in these tests
    {
        getUser1()
        getUser2()
        getUser3()
        getUserAdmin()
    }


    void testSharedAnnotationSecurityForClassicalProject() {
        testSharedAnnotationSecurity(Project.EditingMode.CLASSIC);
    }
    void testSharedAnnotationSecurityForRestrictedProject() {
        testSharedAnnotationSecurity(Project.EditingMode.RESTRICTED);
    }
    void testSharedAnnotationSecurityForReadOnlyProject() {
        testSharedAnnotationSecurity(Project.EditingMode.READ_ONLY);
    }

    private void testSharedAnnotationSecurity(Project.EditingMode mode) {

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add contributor to project
        ProjectAPI.addUserProject(project.id, getUser2().id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation 1 with a contributor
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user2)
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotation1 = result.data


        /*//Add annotation 2 with project admin
        UserAnnotation annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user1)
        result = UserAnnotationAPI.create(annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation2 = result.data*/
        //Add annotation 2 with a job from project admin
        Job job = BasicInstanceBuilder.getJobNotExist(project)
        job = BasicInstanceBuilder.saveDomain(job)
        UserJob userJob = BasicInstanceBuilder.getUserJobNotExist(job, user1);
        userJob = BasicInstanceBuilder.saveDomain(userJob)
        AlgoAnnotation annotation2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(userJob.job, userJob, image)
        result = AlgoAnnotationAPI.create(annotation2.encodeAsJSON(), userJob.username, "PasswordUserJob")
        assert 200 == result.code
        annotation2 = result.data

        project.mode = mode
        BasicInstanceBuilder.saveDomain(project)

        Integer expectedResult;
        /*-------STEP 1 : Create SharedAnnotation & test show
            On Annot1 (made by user2), Comments : [admin->user1, user1->user2, user2->user1]
            On Annot2 (made by user1), Comments : [user2->admin]
         */

        ProjectAPI.addUserProject(project.id, getUser2().id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        ProjectAPI.addUserProject(project.id, getUser3().id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        sharedAnnotation.receivers = [getUser1()]
        sharedAnnotation.sender = getUserAdmin()

        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"

        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME3, SecurityTestsAbstract.PASSWORD3)
        if(mode == Project.EditingMode.CLASSIC) {
            expectedResult = 200;
        } else{
            expectedResult = 403;
        }
        //Cytomine admin cannot create on other annotation on non classical project ???
        //in classical oui ?
        assert expectedResult == result.code
        Long idSharedAnnotationUser;

        if(mode == Project.EditingMode.CLASSIC) {
            idSharedAnnotationUser = result.data.id

            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
            assert 200 == result.code
        }

        //contributor can create on its own annotation
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        sharedAnnotation.receivers = [getUser1()]
        sharedAnnotation.sender = getUser2()

        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        if(mode == Project.EditingMode.READ_ONLY) {
            expectedResult = 403;
        } else{
            expectedResult = 200;
        }
        assert expectedResult == result.code
        if(mode != Project.EditingMode.READ_ONLY) {
            idSharedAnnotationUser = result.data.id

            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
            assert 200 == result.code
        }

        //project admin can create on other annotation
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        sharedAnnotation.sender = getUser1()
        sharedAnnotation.receivers = [getUser2()]

        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        idSharedAnnotationUser = result.data.id

        result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        sharedAnnotation.receivers = [getUserAdmin()]
        sharedAnnotation.sender = getUser2()

        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        //a contributor can create on other annotation only in classical mode
        if(mode == Project.EditingMode.CLASSIC) {
            expectedResult = 200;
        } else{
            expectedResult = 403;
        }
        assert expectedResult == result.code

        if(mode == Project.EditingMode.CLASSIC) {
            idSharedAnnotationUser = result.data.id

            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
            assert 200 == result.code
            result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, idSharedAnnotationUser, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
            assert 200 == result.code
        }

        /*-------STEP 2 : test list
            On Annot1 (made by user2), Comments : [admin->user1, user1->user2, user2->user1]
            On Annot2 (made by user1), Comments : [user2->admin]
         */

        Integer expectedSize;

        result = AnnotationCommentAPI.list(annotation1.id, annotation1.class.name, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data)

        expectedSize = 0
        if(mode == Project.EditingMode.CLASSIC) {
            expectedSize = 1
        }
        assert json.collection.size() == expectedSize

        result = AnnotationCommentAPI.list(annotation2.id, annotation2.class.name, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data)
        expectedSize = 0
        if(mode == Project.EditingMode.CLASSIC) {
            expectedSize = 1
        }
        assert json.collection.size() == expectedSize


        result = AnnotationCommentAPI.list(annotation1.id, annotation1.class.name, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        switch (mode) {
            case Project.EditingMode.CLASSIC:
                expectedSize = 3;
                break;
            case Project.EditingMode.RESTRICTED:
                expectedSize = 2;
                break;
            case Project.EditingMode.READ_ONLY:
                expectedSize = 1;
                break;
        }
        assert json.collection.size() == expectedSize

        result = AnnotationCommentAPI.list(annotation2.id, annotation2.class.name, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection.size() == 0


        result = AnnotationCommentAPI.list(annotation1.id, annotation1.class.name, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        json = JSON.parse(result.data)
        expectedSize = 2
        if(mode == Project.EditingMode.READ_ONLY) {
            expectedSize = 1
        }
        assert json.collection.size() == expectedSize

        result = AnnotationCommentAPI.list(annotation2.id, annotation2.class.name, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        json = JSON.parse(result.data)
        expectedSize = 0
        if(mode == Project.EditingMode.CLASSIC) {
            expectedSize = 1
        }
        assert json.collection.size() == expectedSize
    }
}
