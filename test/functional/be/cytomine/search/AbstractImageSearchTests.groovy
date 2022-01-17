package be.cytomine.search

import be.cytomine.image.AbstractImage
import be.cytomine.image.hv.HVMetadata
import be.cytomine.meta.Tag
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.security.SecurityTestsAbstract

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractImageAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class AbstractImageSearchTests {


    //search
    void testGetSearch(){
        AbstractImage img = BasicInstanceBuilder.getAbstractImageNotExist(true)
        img.width = 499
        img.save(flush: true)
        img = img.refresh()
        AbstractImage img2 = BasicInstanceBuilder.getAbstractImageNotExist(true)

        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size >= 2


        def searchParameters = [[operator : "lte", field : "width", value:500]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByDeletedIsNullAndWidthLessThanEquals(500)

        searchParameters = [[operator : "gte", field : "width", value:600]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.findAllByDeletedIsNullAndWidthGreaterThanEquals(600).size()

        searchParameters = [[operator : "lte", field : "width", value:Integer.MAX_VALUE]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByDeletedIsNull()

        searchParameters = [[operator : "lte", field : "width", value:100]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0

    }

    void testGetSearchByMetadata(){

        AbstractImage img = BasicInstanceBuilder.getAbstractImageNotExist(true)
        AbstractImage img2 = BasicInstanceBuilder.getAbstractImageNotExist(true)
        TagDomainAssociation tda = BasicInstanceBuilder.getTagDomainAssociationNotExist(img, true)
        TagDomainAssociation tda2 = BasicInstanceBuilder.getTagDomainAssociationNotExist(img2, true)
        Tag tag1 = tda.tag
        Tag tag2 = tda2.tag

        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size >= 2


        def searchParameters = [[operator : "in", field : "tag", value: tag1.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert json.collection[0].id == img.id

        searchParameters = [[operator : "in", field : "tag", value: tag2.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert json.collection[0].id == img2.id

        searchParameters = [[operator : "in", field : "tag", value: ""+tag1.id+","+tag2.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
    }

    //search hv-ikt
    void testGetSearchHVIKT(){

        HVMetadata staining = new HVMetadata(value:BasicInstanceBuilder.getRandomInteger(0,5555), type:HVMetadata.Type.STAINING).save()
        HVMetadata antibody = new HVMetadata(value:BasicInstanceBuilder.getRandomInteger(0,5555), type:HVMetadata.Type.ANTIBODY).save()


        AbstractImage img = BasicInstanceBuilder.getAbstractImageNotExist(true)
        img.originalFilename = "test"
        img.staining = staining
        img.save(flush: true)
        img.refresh()
        AbstractImage img2 = BasicInstanceBuilder.getAbstractImageNotExist(true)
        img2.originalFilename = "space"
        img2.antibody = antibody
        img2.save(flush: true)
        img2.refresh()

        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size >= 2


        def searchParameters = [[operator : "ilike", field : "searchText", value:"test with space"]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.findAllByOriginalFilenameIlike("test")
                                .findAll {it.originalFilename.contains("with")}
                                .findAll {it.originalFilename.contains("space")}.size()

        searchParameters = [[operator : "ilike", field : "searchText", value:"test  space"]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
            assert json.size == AbstractImage.findAllByOriginalFilenameIlike("test")
                    .findAll {it.originalFilename.contains("space")}.size()

        searchParameters = [[operator : "ilike", field : "searchText", value:"test_without_space"]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByOriginalFilename("test_without_space")
        assert json.size == 0

        searchParameters = [[operator : "ilike", field : "searchText", value:"sp*ce"]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByOriginalFilenameIlike("space")


        searchParameters = [[operator : "in", field : "staining", value:staining.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert json.collection[0].originalFilename == "test"


        searchParameters = [[operator : "in", field : "antibody", value:antibody.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert json.collection[0].originalFilename == "space"


        searchParameters = [[operator : "in", field : "staining", value:staining.id], [operator : "in", field : "antibody", value:antibody.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0


        //test a "not admin"
        def user1 = BasicInstanceBuilder.getUser(SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        BasicInstanceBuilder.getStorageNotExist(user1, true)
        searchParameters = [[operator : "in", field : "staining", value:staining.id], [operator : "in", field : "antibody", value:antibody.id]]

        result = AbstractImageAPI.search(0,0, searchParameters, SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0

    }

    //pagination
    void testListImagesInstanceByProject() {
        BasicInstanceBuilder.getAbstractImage()
        BasicInstanceBuilder.getAbstractImageNotExist(true)
        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = AbstractImageAPI.list(1,0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = AbstractImageAPI.list(1,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

}
