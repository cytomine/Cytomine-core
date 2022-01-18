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
import be.cytomine.meta.AttachedFile
import be.cytomine.meta.Property
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.JobData
import be.cytomine.processing.Software
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import be.cytomine.meta.Description
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class ProjectSecurityTests extends SecurityTestsAbstract {


    void testProjectSecurityForCytomineAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)


        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        Infos.printUserRight(user1)
        Infos.printUserRight(admin)
        //check if admin user can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAMEADMIN,PASSWORDADMIN).data)))
        assert (200 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == ProjectAPI.delete(project.id,USERNAMEADMIN,PASSWORDADMIN).code)
    }

    void testProjectSecurityForProjectCreator() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        println "PROJECT="+project.deleted

        //check if user 1 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAME1,PASSWORD1).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME1,PASSWORD1).data)))
        assert (200 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == ProjectAPI.delete(project.id,USERNAME1,PASSWORD1).code)
    }

    void testProjectSecurityForProjectManager() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add admin right to user2 then remove them to test if old admin has no more right
        def resAddUser = ProjectAPI.addAdminProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.deleteAdminProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(project)
        //check if user 2 can access/update/delete he is still a contributor
        assert (200 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)

        //Add admin right to user2
        resAddUser = ProjectAPI.addAdminProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(project)
        //check if user 2 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (200 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (200 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)
    }

    void testProjectSecurityForProjectContributor() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add right to user2
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(project)
        //check if user 2 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)


        //remove right to user2
        resAddUser = ProjectAPI.deleteUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (403 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (false == ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)
    }

    void testProjectSecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (403 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert(false==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        Infos.printRight(project)
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)

    }

    void testProjectSecurityForGuestUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get ghest
        User ghest = BasicInstanceBuilder.getGhest("GHESTONTOLOGY","PASSWORD")

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add right to user2
        def resAddUser = ProjectAPI.addUserProject(project.id,ghest.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(project)
        //check if user 2 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list("GHESTONTOLOGY","PASSWORD").data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == ProjectAPI.delete(project.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
    }




    void testProjectSecurityForAnonymous() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (401 == ProjectAPI.show(project.id,USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.list(USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.delete(project.id,USERNAMEBAD,PASSWORDBAD).code)
    }

    void testAddProjectGrantAdminUndoRedo() {
        //not implemented (no undo/redo for project)
    }




    void testClassicProject() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"
        def testUsername = "testUser"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //create user test
        User test = BasicInstanceBuilder.getUser(testUsername,password)


        // Test as a project admin

        //update project
        assert 200 == ProjectAPI.update(project.id,project.encodeAsJSON(),adminUsername, password).code
        // add & delete users
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteUserProject(project.id,test.id,adminUsername,password).code
        // add & delete admins
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteAdminProject(project.id,test.id,adminUsername,password).code
        // add & delete representatives
        def refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        def result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password)
        assert 200 == result.code
        int idRef = result.data.id
        assert 200 == ProjectRepresentativeUserAPI.delete(idRef, project.id, adminUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code

        // Test as simple user

        //update project
        assert 403 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // add & delete users
        assert 403 == ProjectAPI.addUserProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteUserProject(project.id,test.id,simpleUsername,password).code
        // add & delete admins
        assert 403 == ProjectAPI.addAdminProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteAdminProject(project.id,test.id,simpleUsername,password).code
        // add & delete representatives
        refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        assert 403 == ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), simpleUsername,password).code
        idRef = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password).data.id
        assert 403 == ProjectRepresentativeUserAPI.delete(idRef, project.id, simpleUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code


        //Description stick to Edition mode, other metadata not. To fix
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),simpleUsername, password).code
        assert 409 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),adminUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        assert 403 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),adminUsername,password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code
    }

    void testClassicProjectWithImageDataAsContributor() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,simpleUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,simpleUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,simpleUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete tag domain association (simple user data)
        assert 200 == TagDomainAssociationAPI.delete(tdaUser.id,simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete tag domain association (admin data)
        assert 200 == TagDomainAssociationAPI.delete(tdaAdmin.id, simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete tag domain association (super admin data)
        assert 200 == TagDomainAssociationAPI.delete(tda.id, simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 200 == AttachedFileAPI.delete(attachedFileUser.id,simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (admin data)
        assert 200 == AttachedFileAPI.delete(attachedFileAdmin.id, simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 200 == AttachedFileAPI.delete(attachedFile.id, simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,simpleUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,simpleUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,simpleUsername, password).code

        //add annotation on my layer
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),simpleUsername, password).code
        //add annotation on other layers
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),simpleUsername, password).code
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete annotation (simple user data)
        assert 200 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser.id,simpleUsername, password).code

        //update, delete annotation (admin data)
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin.id,simpleUsername, password).code

        //update, delete annotation (super admin data)
        assert 200 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation.id,simpleUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, simpleUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, simpleUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, simpleUsername, password).code
    }

    void testClassicProjectWithImageDataAsManager() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)


        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", adminUsername, password).code

        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete tag domain association (simple user data)
        assert 200 == TagDomainAssociationAPI.delete(tdaUser.id,adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == TagDomainAssociationAPI.delete(tdaAdmin.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == TagDomainAssociationAPI.delete(tda.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 200 == AttachedFileAPI.delete(attachedFileUser.id,adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (admin data)
        assert 200 == AttachedFileAPI.delete(attachedFileAdmin.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 200 == AttachedFileAPI.delete(attachedFile.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code


        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,adminUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,adminUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,adminUsername, password).code



        //add annotation on my layer
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),adminUsername, password).code
        //add annotation on other layers
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),adminUsername, password).code
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),adminUsername, password).code

        //update, delete annotation (simple user data)
        assert 200 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser.id,adminUsername, password).code

        //update, delete annotation (admin data)
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin.id,adminUsername, password).code

        //update, delete annotation (super admin data)
        assert 200 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation.id,adminUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),adminUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, adminUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, adminUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, adminUsername, password).code
    }

    void testClassicProjectWithJobData() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        // data
        Software software = BasicInstanceBuilder.getSoftwareNotExist(true);
        Job job = BasicInstanceBuilder.getJobNotExist(true, software, project)
        JobData jobData = BasicInstanceBuilder.getJobDataNotExist(job)
        BasicInstanceBuilder.saveDomain(jobData)


        // Now Test as simple user


        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], simpleUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, simpleUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), simpleUsername, password).code

        def result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == JobDataAPI.delete(result.data.id, simpleUsername, password).code

        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), simpleUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == JobAPI.delete(result.data.id, simpleUsername, password).code

        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == SoftwareProjectAPI.delete(result.data.id, simpleUsername, password).code


        // Now run test as a project admin

        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, adminUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), adminUsername, password).code

        result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == JobDataAPI.delete(result.data.id, adminUsername, password).code

        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), adminUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == JobAPI.delete(result.data.id, adminUsername, password).code

        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == SoftwareProjectAPI.delete(result.data.id, adminUsername, password).code

    }

    void testRestrictedProject() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"
        def testUsername = "testUser"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //create user test
        User test = BasicInstanceBuilder.getUser(testUsername,password)


        // Test as a project admin

        //update project
        assert 200 == ProjectAPI.update(project.id,project.encodeAsJSON(),adminUsername, password).code
        // add & delete users
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteUserProject(project.id,test.id,adminUsername,password).code
        // add & delete admins
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteAdminProject(project.id,test.id,adminUsername,password).code
        // add & delete representatives
        def refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        def result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password)
        assert 200 == result.code
        int idRef = result.data.id
        assert 200 == ProjectRepresentativeUserAPI.delete(idRef, project.id, adminUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code

        // Test as simple user

        //update project
        assert 403 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // add & delete users
        assert 403 == ProjectAPI.addUserProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteUserProject(project.id,test.id,simpleUsername,password).code
        // add & delete admins
        assert 403 == ProjectAPI.addAdminProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteAdminProject(project.id,test.id,simpleUsername,password).code
        // add & delete representatives
        refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        assert 403 == ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), simpleUsername,password).code
        idRef = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password).data.id
        assert 403 == ProjectRepresentativeUserAPI.delete(idRef, project.id, simpleUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code

        //Description check if not readonly mode, other metadata stick to Write permission. To fix
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),simpleUsername, password).code
        assert 409 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),adminUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        assert 403 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),adminUsername,password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code
    }

    void testRestrictedProjectWithImageDataAsContributor() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (admin data)
        assert 403 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (superadmin data)
        assert 403 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,simpleUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),simpleUsername, password).code

        //TODO description doesn't have a user or creator field. Doesn't check neither if admin or not so all is 200
        //add, update, delete description (admin data)
        /*assert 403 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,simpleUsername, password).code
        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 403 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,simpleUsername, password).code
        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),simpleUsername, password).code*/


        //add, update, delete tag domain association (simple user data)
        assert 200 == TagDomainAssociationAPI.delete(tdaUser.id,simpleUsername, password).code //TODO fix should be 403
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete tag domain association (admin data)
        assert 403 == TagDomainAssociationAPI.delete(tdaAdmin.id, simpleUsername, password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete tag domain association (super admin data)
        assert 403 == TagDomainAssociationAPI.delete(tda.id, simpleUsername, password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 200 == AttachedFileAPI.delete(attachedFileUser.id,simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (admin data)
        assert 403 == AttachedFileAPI.delete(attachedFileAdmin.id, simpleUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 403 == AttachedFileAPI.delete(attachedFile.id, simpleUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code

        //TODO check these
        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,simpleUsername, password).code
        //start reviewing image (admin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,simpleUsername, password).code
        //start reviewing image (superadmin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(image.id,simpleUsername, password).code

        //add annotation on my layer
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),simpleUsername, password).code
        //add annotation on other layers
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),simpleUsername, password).code
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete annotation (simple user data)
        assert 200 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser.id,simpleUsername, password).code

        //update, delete annotation (admin data)
        assert 403 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationAdmin.id,simpleUsername, password).code

        //update, delete annotation (super admin data)
        assert 403 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotation.id,simpleUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, simpleUsername, password).code

        //update, delete image instance (admin data)
        assert 403 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageAdmin, simpleUsername, password).code

        //update, delete image instance (superadmin data)
        assert 403 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(image, simpleUsername, password).code
    }

    void testRestrictedProjectWithImageDataAsManager() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)


        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", adminUsername, password).code

        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),adminUsername, password).code


        //add, update, delete tag domain association (simple user data)
        assert 200 == TagDomainAssociationAPI.delete(tdaUser.id,adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete tag domain association (admin data)
        assert 200 == TagDomainAssociationAPI.delete(tdaAdmin.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete tag domain association (super admin data)
        assert 200 == TagDomainAssociationAPI.delete(tda.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 200 == AttachedFileAPI.delete(attachedFileUser.id,adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (admin data)
        assert 200 == AttachedFileAPI.delete(attachedFileAdmin.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 200 == AttachedFileAPI.delete(attachedFile.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,adminUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,adminUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,adminUsername, password).code



        //add annotation on my layer
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),adminUsername, password).code
        //add annotation on other layers
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),adminUsername, password).code
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),adminUsername, password).code

        //update, delete annotation (simple user data)
        assert 200 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser.id,adminUsername, password).code

        //update, delete annotation (admin data)
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin.id,adminUsername, password).code

        //update, delete annotation (super admin data)
        assert 200 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation.id,adminUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),adminUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, adminUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, adminUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, adminUsername, password).code
    }


    // TODO check these
    void testRestrictedProjectWithJobData() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.RESTRICTED
        project = BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        // data
        Software software = BasicInstanceBuilder.getSoftwareNotExist(true);
        Job job = BasicInstanceBuilder.getJobNotExist(true, software, project)
        JobData jobData = BasicInstanceBuilder.getJobDataNotExist(job)
        jobData = BasicInstanceBuilder.saveDomain(jobData)


        // Now Test as simple user


        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], simpleUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, simpleUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), simpleUsername, password).code

        def result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == JobDataAPI.delete(result.data.id, simpleUsername, password).code

        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), simpleUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == JobAPI.delete(result.data.id, simpleUsername, password).code

        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),simpleUsername, password)
        assert 200 == result.code
        assert 200 == SoftwareProjectAPI.delete(result.data.id, simpleUsername, password).code


        // Now run test as a project admin

        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, adminUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), adminUsername, password).code

        result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == JobDataAPI.delete(result.data.id, adminUsername, password).code

        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), adminUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code

        // begin test for simple user
        assert 403 == JobAPI.delete(result.data.id, simpleUsername, password).code
        assert 200 == JobAPI.show(result.data.id, simpleUsername, password).code
        // end test for simple user

        assert 200 == JobAPI.delete(result.data.id, adminUsername, password).code

        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == SoftwareProjectAPI.delete(result.data.id, adminUsername, password).code

    }

    void testReadOnlyProject() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"
        def testUsername = "testUser"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //create user test
        User test = BasicInstanceBuilder.getUser(testUsername,password)


        // Test as a project admin

        //update project
        assert 200 == ProjectAPI.update(project.id,project.encodeAsJSON(),adminUsername, password).code
        // add & delete users
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteUserProject(project.id,test.id,adminUsername,password).code
        // add & delete admins
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 200 == ProjectAPI.deleteAdminProject(project.id,test.id,adminUsername,password).code
        // add & delete representatives
        def refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        def result = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password)
        assert 200 == result.code
        int idRef = result.data.id
        assert 200 == ProjectRepresentativeUserAPI.delete(idRef, project.id, adminUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code

        // Test as simple user

        //update project
        assert 403 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // add & delete users
        assert 403 == ProjectAPI.addUserProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addUserProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteUserProject(project.id,test.id,simpleUsername,password).code
        // add & delete admins
        assert 403 == ProjectAPI.addAdminProject(project.id,test.id,simpleUsername,password).code
        assert 200 == ProjectAPI.addAdminProject(project.id,test.id,adminUsername,password).code
        assert 403 == ProjectAPI.deleteAdminProject(project.id,test.id,simpleUsername,password).code
        // add & delete representatives
        refToAdd = BasicInstanceBuilder.getProjectRepresentativeUserNotExist()
        refToAdd.project = project
        assert 403 == ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), simpleUsername,password).code
        idRef = ProjectRepresentativeUserAPI.create(refToAdd.encodeAsJSON(), adminUsername,password).data.id
        assert 403 == ProjectRepresentativeUserAPI.delete(idRef, project.id, simpleUsername,password).code

        assert 200 == AnnotationDomainAPI.downloadDocumentByProject(project.id, simpleUser.id, null, null, adminUsername,password).code

        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(project, false).encodeAsJSON(),adminUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", project.class.name,project.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        assert 403 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.create(project.id, "project" ,BasicInstanceBuilder.getProjectPropertyNotExist(project,false).encodeAsJSON(),adminUsername,password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(project, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code
    }

    void testReadOnlyProjectWithImageDataAsContributor() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 403 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (admin data)
        assert 403 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (superadmin data)
        assert 403 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add, update, delete description (simple user data)
        assert 403 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,simpleUsername, password).code
        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete description (admin data)
        assert 403 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,simpleUsername, password).code
        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 403 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,simpleUsername, password).code
        assert 403 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),simpleUsername, password).code

        //add, update, delete tag domain association (simple user data)
        assert 403 == TagDomainAssociationAPI.delete(tdaUser.id,simpleUsername, password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete description (admin data)
        assert 403 == TagDomainAssociationAPI.delete(tdaAdmin.id, simpleUsername, password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 403 == TagDomainAssociationAPI.delete(tda.id, simpleUsername, password).code
        assert 403 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, simpleUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 403 == AttachedFileAPI.delete(attachedFileUser.id,simpleUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (admin data)
        assert 403 == AttachedFileAPI.delete(attachedFileAdmin.id, simpleUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 403 == AttachedFileAPI.delete(attachedFile.id, simpleUsername, password).code
        assert 403 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),simpleUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageUser.id,simpleUsername, password).code
        //start reviewing image (admin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,simpleUsername, password).code
        //start reviewing image (superadmin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(image.id,simpleUsername, password).code

        //add annotation on my layer
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),simpleUsername, password).code
        //add annotation on other layers
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),simpleUsername, password).code
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete annotation (simple user data)
        assert 403 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationUser.id,simpleUsername, password).code

        //update, delete annotation (admin data)
        assert 403 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationAdmin.id,simpleUsername, password).code

        //update, delete annotation (super admin data)
        assert 403 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotation.id,simpleUsername, password).code

        //add image instance
        assert 403 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete image instance (simple user data)
        assert 403 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageUser, simpleUsername, password).code

        //update, delete image instance (admin data)
        assert 403 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageAdmin, simpleUsername, password).code

        //update, delete image instance (superadmin data)
        assert 403 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(image, simpleUsername, password).code
    }

    void testReadOnlyProjectWithImageDataAsManager() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)


        def data = initProjectDataSet(project, simpleUsername, adminUsername, password)

        //super admin data
        ImageInstance image = data.image
        UserAnnotation annotation = data.annotation
        Description description = data.description
        Property property = data.property
        AttachedFile attachedFile = data.attachedFile
        TagDomainAssociation tda = data.tagDomainAssociation

        //admin data
        ImageInstance imageAdmin = data.imageAdmin
        UserAnnotation annotationAdmin = data.annotationAdmin
        Description descriptionAdmin = data.descriptionAdmin
        Property propertyAdmin = data.propertyAdmin
        AttachedFile attachedFileAdmin = data.attachedFileAdmin
        TagDomainAssociation tdaAdmin = data.tagDomainAssociationAdmin

        //simple user data
        ImageInstance imageUser = data.imageUser
        UserAnnotation annotationUser = data.annotationUser
        Description descriptionUser = data.descriptionUser
        Property propertyUser = data.propertyUser
        AttachedFile attachedFileUser = data.attachedFileUser
        TagDomainAssociation tdaUser = data.tagDomainAssociationUser


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, propertyUser.domainIdent, "annotation" ,propertyUser.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, propertyUser.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, propertyAdmin.domainIdent, "annotation" ,propertyAdmin.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, propertyAdmin.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", adminUsername, password).code

        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationUser, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin, false).encodeAsJSON(),adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,adminUsername, password).code
        assert 200 == DescriptionAPI.create(project.id,project.class.name,BasicInstanceBuilder.getDescriptionNotExist(annotation, false).encodeAsJSON(),adminUsername, password).code


        //add, update, delete tag domain association (simple user data)
        assert 200 == TagDomainAssociationAPI.delete(tdaUser.id,adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete tag domain association (admin data)
        assert 200 == TagDomainAssociationAPI.delete(tdaAdmin.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete tag domain association (super admin data)
        assert 200 == TagDomainAssociationAPI.delete(tda.id, adminUsername, password).code
        assert 200 == TagDomainAssociationAPI.create(BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation, false).encodeAsJSON(),project.class.name, project.id, adminUsername, password).code

        //add, update, delete attached file (simple user data)
        assert 200 == AttachedFileAPI.delete(attachedFileUser.id,adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationUser.class.name,annotationUser.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (admin data)
        assert 200 == AttachedFileAPI.delete(attachedFileAdmin.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotationAdmin.class.name,annotationAdmin.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code
        //add, update, delete attached file (super admin data)
        assert 200 == AttachedFileAPI.delete(attachedFile.id, adminUsername, password).code
        assert 200 == AttachedFileAPI.upload("test", annotation.class.name,annotation.id,new File("test/functional/be/cytomine/utils/simpleFile.txt"),adminUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,adminUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,adminUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,adminUsername, password).code



        //add annotation on my layer
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,imageAdmin.user,false).encodeAsJSON(),adminUsername, password).code
        //add annotation on other layers
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,imageUser.user,false).encodeAsJSON(),adminUsername, password).code
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,image.user,false).encodeAsJSON(),adminUsername, password).code

        //update, delete annotation (simple user data)
        assert 200 == UserAnnotationAPI.update(annotationUser.id, annotationUser.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser.id,adminUsername, password).code

        //update, delete annotation (admin data)
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, annotationAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin.id,adminUsername, password).code

        //update, delete annotation (super admin data)
        assert 200 == UserAnnotationAPI.update(annotation.id, annotation.encodeAsJSON(), adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation.id,adminUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),adminUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, adminUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, adminUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, adminUsername, password).code
    }

    void testReadOnlyProjectWithJobData() {
        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        // data
        Software software = BasicInstanceBuilder.getSoftwareNotExist(true);
        Job job = BasicInstanceBuilder.getJobNotExist(true, software, project)
        JobData jobData = BasicInstanceBuilder.getJobDataNotExist(job)
        BasicInstanceBuilder.saveDomain(jobData)


        // Now Test as simple user


        assert 403 == JobDataAPI.upload(jobData.id, new byte[5], simpleUsername, password).code
        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, simpleUsername, password).code
        assert 403 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), simpleUsername, password).code

        assert 403 == JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),simpleUsername, password).code
        def result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),adminUsername, password)
        assert 403 == JobDataAPI.delete(result.data.id, simpleUsername, password).code

        assert 403 == JobAPI.update(job.id, job.encodeAsJSON(), simpleUsername, password).code
        assert 403 == JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),simpleUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),adminUsername, password)
        assert 403 == JobAPI.delete(result.data.id, simpleUsername, password).code

        assert 403 == SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),simpleUsername, password).code
        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(software, project, false).encodeAsJSON(),adminUsername, password)
        assert 403 == SoftwareProjectAPI.delete(result.data.id, simpleUsername, password).code


        // Now run test as a project admin

        assert 200 == JobDataAPI.upload(jobData.id, new byte[5], adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, adminUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), adminUsername, password).code

        result = JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist(job).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == JobDataAPI.delete(result.data.id, adminUsername, password).code

        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), adminUsername, password).code
        result = JobAPI.create(BasicInstanceBuilder.getJobNotExist(false, software, project).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == JobAPI.delete(result.data.id, adminUsername, password).code

        result = SoftwareProjectAPI.create(BasicInstanceBuilder.getSoftwareProjectNotExist(BasicInstanceBuilder.getSoftwareNotExist(true), project, false).encodeAsJSON(),adminUsername, password)
        assert 200 == result.code
        assert 200 == SoftwareProjectAPI.delete(result.data.id, adminUsername, password).code

    }


    private def initProjectDataSet(Project project, String simpleUsername, String adminUsername, String password){

        def result = [:]
        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code


        /*super admin data*/
        //Create an annotation (by superadmin)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project,true)
        SliceInstance slice = BasicInstanceBuilder.getSliceInstanceNotExist(image,true)
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project,image,true)
        //Create a description
        Description description = BasicInstanceBuilder.getDescriptionNotExist(annotation,true)
        //Create a property
        Property property = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,true)
        //Create an attached file
        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist(annotation,true)
        //Create a tag
        TagDomainAssociation tda = BasicInstanceBuilder.getTagDomainAssociationNotExist(annotation,true)

        result.image = image
        result.annotation = annotation
        result.description = description
        result.property = property
        result.attachedFile = attachedFile
        result.tagDomainAssociation = tda

        /*admin data*/
        //Create an annotation (by admin)
        ImageInstance imageAdmin = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(imageAdmin)
        slice = BasicInstanceBuilder.getSliceInstanceNotExist(imageAdmin,true)
        UserAnnotation annotationAdmin = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,admin,false)
        annotationAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(annotationAdmin)
        //Create a description
        Description descriptionAdmin = BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin,true)
        //Create a property
        Property propertyAdmin = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,true)
        //Create an attached file
        AttachedFile attachedFileAdmin = BasicInstanceBuilder.getAttachedFileNotExist(annotationAdmin,true)
        //Create a tag
        TagDomainAssociation tdaAdmin = BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationAdmin,true)

        result.imageAdmin = imageAdmin
        result.annotationAdmin = annotationAdmin
        result.descriptionAdmin = descriptionAdmin
        result.propertyAdmin = propertyAdmin
        result.attachedFileAdmin = attachedFileAdmin
        result.tagDomainAssociationAdmin = tdaAdmin

        /*simple user data*/
        //Create an annotation (by user)
        ImageInstance imageUser = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageUser.user = simpleUser;
        BasicInstanceBuilder.saveDomain(imageUser)
        slice = BasicInstanceBuilder.getSliceInstanceNotExist(imageUser,true)
        UserAnnotation annotationUser = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,simpleUser,false)
        annotationUser.user = simpleUser;
        BasicInstanceBuilder.saveDomain(annotationUser)
        //Create a description
        Description descriptionUser = BasicInstanceBuilder.getDescriptionNotExist(annotationUser,true)
        //Create a property
        Property propertyUser = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,true)
        //Create an attached file
        AttachedFile attachedFileUser = BasicInstanceBuilder.getAttachedFileNotExist(annotationUser,true)
        //Create a tag
        TagDomainAssociation tdaUser = BasicInstanceBuilder.getTagDomainAssociationNotExist(annotationUser,true)

        result.imageUser = imageUser
        result.annotationUser = annotationUser
        result.descriptionUser = descriptionUser
        result.propertyUser = propertyUser
        result.attachedFileUser = attachedFileUser
        result.tagDomainAssociationUser = tdaUser

        return result
    }

}
