package be.cytomine

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationActionAPI
import be.cytomine.test.http.SliceInstanceAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotationActionTests {

    void testAddAction() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse("{annotationIdent:${annotation.id},action:Test}");

        def result = AnnotationActionAPI.create(json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListByImage() {
        def slice = SliceInstanceAPI.buildBasicSlice(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(slice, true)
        def image = slice.image
        def json = JSON.parse("{image:${image.id}, annotationIdent:${annotation.id}, action:Test}")

        def result = AnnotationActionAPI.create(json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImageAndUser(image.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0
    }

    void testListBySlice() {
        def slice = SliceInstanceAPI.buildBasicSlice(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(slice, true)
        def image = slice.image
        def json = JSON.parse("{slice:${slice.id}, annotationIdent:${annotation.id}, action:Test}")

        def result = AnnotationActionAPI.create(json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listBySlice(slice.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listBySliceAndUser(slice.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listBySliceAndUser(slice.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0
    }

    void testListAfterThan() {
        def slice = SliceInstanceAPI.buildBasicSlice(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(slice, true)
        def image = slice.image
        def json = JSON.parse("{image:${image.id}, annotationIdent:${annotation.id}, action:Test}")

        def result = AnnotationActionAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long created = Long.parseLong(JSON.parse(result.data).created)
        Long creator = JSON.parse(result.data).user

        result = AnnotationActionAPI.listByImage(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = AnnotationActionAPI.listByImage(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created + 1)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0

        result = AnnotationActionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created + 1)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0
    }

    void testCountAnnotationActionsByProject() {
        def result = AnnotationActionAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationActionsByProjectWithDates() {
        Date startDate = new Date()
        def result = AnnotationActionAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }
}
