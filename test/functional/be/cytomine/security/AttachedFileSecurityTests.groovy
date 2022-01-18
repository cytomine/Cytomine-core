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
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.http.AbstractImageAPI
import be.cytomine.test.http.AttachedFileAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.JobAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.meta.AttachedFile
import com.mongodb.util.JSON

class AttachedFileSecurityTests extends SecurityTestsAbstract{


    void testJobAttachedFileForDomainOwner() {

        //Get user1
        User user1 = getUser1()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add job instance to project
        Job job = BasicInstanceBuilder.getJobNotExist()
        job.project = project

        result = JobAPI.create(job.encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        job = result.data

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = job
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

    }

    void testJobAttachedFileForNotDomainOwner() {

        //Get user1
        User user1 = getUser1()

        //Get user2
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add job instance to project
        Job job = BasicInstanceBuilder.getJobNotExist()
        job.project = project
        job.save(flush: true)

        result = JobAPI.create(job.encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        job = result.data

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = job

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code


        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code


        project.mode = Project.EditingMode.CLASSIC
        project = BasicInstanceBuilder.saveDomain(project)
        println "project mode = ${project.mode}"

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
    }

    void testProjectAttachedFileForDomainOwner() {

        //Get user1
        User user1 = getUser1()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = project
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

    }

    void testProjectAttachedFileForNotDomainOwner() {

        //Get user1
        User user1 = getUser1()

        //Get user2
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = project
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

    }

    void testImageInstanceAttachedFileForProjectOwner() {

        //Get user1
        User user1 = getUser1()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        result = ImageInstanceAPI.create(image.encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        image = result.data

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = image
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

    }

    void testImageInstanceAttachedFileForDomainOwner() {

        User user1 = getUser1()
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        result = ImageInstanceAPI.create(image.encodeAsJSON(), SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        image = result.data

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = image
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        idAttachedFile = JSON.parse(result.data).id

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        idAttachedFile = JSON.parse(result.data).id
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        idAttachedFile = JSON.parse(result.data).id

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
    }

    void testImageInstanceAttachedFileForNotDomainOwner() {

        //Get user1
        User user1 = getUser1()

        //Get user2
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        result = ImageInstanceAPI.create(image.encodeAsJSON(), SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        image = result.data

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = image
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        //check all by project settings until the end
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

    }


    void testAnnotationAttachedFileForDomainOwner() {

        User user1 = getUser1()
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        //Add annotation to project
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project)
        annot.project = project
        annot.user = user2

        result = UserAnnotationAPI.create(annot.encodeAsJSON(), SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annot = result.data



        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = annot
        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        idAttachedFile = JSON.parse(result.data).id
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        idAttachedFile = JSON.parse(result.data).id
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

    }

    void testAnnotationAttachedFileForNotDomainOwner() {

        //Get user1
        User user1 = getUser1()

        //Get user2
        User user2 = getUser2()

        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)

        //Add annotation to project
        UserAnnotation annot = BasicInstanceBuilder.getUserAnnotationNotExist(project)
        annot.project = project

        result = UserAnnotationAPI.create(annot.encodeAsJSON(), SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annot = result.data

        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist()
        attachedFile.domain = annot

        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        project = BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.upload(attachedFile.domainClassName,attachedFile.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        Long idAttachedFile = JSON.parse(result.data).id

        //check all by project settings until the end
        project.mode = Project.EditingMode.READ_ONLY
        project = BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        result = AttachedFileAPI.show(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.download(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        result = AttachedFileAPI.delete(idAttachedFile,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

    }

    void testAbstractImageAttachedFile() {
        //Get user1
        User user1 = getUser1()

        //Get user2
        User user2 = getUser2()

        def abstractImage = BasicInstanceBuilder.getAbstractImageNotExist(true)

        def attachedFileToAdd = BasicInstanceBuilder.getAttachedFileNotExist(false)
        attachedFileToAdd.domainClassName = abstractImage.class.name
        attachedFileToAdd.domainIdent = abstractImage.id

        def result = AttachedFileAPI.upload(attachedFileToAdd.domainClassName,attachedFileToAdd.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),USERNAME2, PASSWORD2)
        assert 403 == result.code

        result = AttachedFileAPI.upload(attachedFileToAdd.domainClassName,attachedFileToAdd.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),USERNAME1, PASSWORD1)
        assert 403 == result.code

        abstractImage = AbstractImageAPI.buildBasicAbstractImage(USERNAME1, PASSWORD1)
        attachedFileToAdd.domainClassName = abstractImage.class.name
        attachedFileToAdd.domainIdent = abstractImage.id

        result = AttachedFileAPI.upload(attachedFileToAdd.domainClassName,attachedFileToAdd.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

}
