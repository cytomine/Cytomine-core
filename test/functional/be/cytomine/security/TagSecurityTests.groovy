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

import be.cytomine.meta.Tag
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import be.cytomine.utils.UpdateData
import grails.converters.JSON


class TagSecurityTests extends SecurityTestsAbstract {

    void testShowTag() {
        //Every one can see a tag
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def result = TagAPI.show(tag.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
        getGuest1()
        result = TagAPI.show(tag.id, GUEST1,GPASSWORD1)
        assert 200 == result.code
    }
    void testListTag() {
        getUser1()
        def result = TagAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        long size = json.size
        result = TagAPI.list(USERNAME1, PASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.size
        getGuest1()
        result = TagAPI.list(GUEST1,GPASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.size
    }
    void testAddTag() {
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        getUser1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        assert tag.user.id == getUser1().id

        getGuest1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), GUEST1,GPASSWORD1)
        assert 403 == result.code
    }
    void testUpdateTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithNoAssociation() {
        getUser1()
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithAssociation() {
        getUser1()
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data

        def project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = project
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME1, PASSWORD1)

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)

        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }

    void testDeleteTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        result = TagAPI.delete(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithNoAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data

        def project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = project
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME1, PASSWORD1)

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)

        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }
}
