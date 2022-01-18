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

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.test.http.AnnotationTermAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.UpdateData
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject


/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class UserAnnotationTests  {

    void testGetUserAnnotationWithCredential() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListUserAnnotationWithCredential() {
        BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testCountAnnotationWithCredential() {
        def result = UserAnnotationAPI.countByUser(BasicInstanceBuilder.getUser1().id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProject() {
        def result = UserAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProjectWithDates() {
        Date startDate = new Date()
        def result = UserAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testDownloadUserAnnotationDocument() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        def result = UserAnnotationAPI.downloadDocumentByProject(annotationTerm.userAnnotation.project.id,annotationTerm.userAnnotation.user.id,annotationTerm.term.id, annotationTerm.userAnnotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListUserAnnotationTermsWithCredential() {
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def result = UserAnnotationAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert annotation.terms().size() == 0

        def annotationTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotation, true)

        assert annotation.terms().size() == 1

        result = AnnotationTermAPI.deleteAnnotationTerm(annotationTerm.userAnnotation.id,annotationTerm.term.id,User.findByUsername(Infos.SUPERADMINLOGIN).id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert annotation.terms().size() == 0
    }

    void testAddBigUserAnnotationWithMaxNumberOfPoint() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationToAdd.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/very_big_annotation.txt').text)
        int maxPoints = 100
        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), 0, maxPoints, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id
        assert result.data.location.numPoints <= maxPoints

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testAddTooLittleUserAnnotation() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, 226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, 225.73582220103702 306.89723126347087))"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }
    void testAddUserAnnotationMultiLine() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "LINESTRING(181.05636403199998 324.87936288,208.31216076799996 303.464094016)"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrect() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        Long commandId = result.command

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code


        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        //200 because the undoed annotation was not this one
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationMultipleCorrect() {
        def annotationToAdd1 = BasicInstanceBuilder.getUserAnnotation()
        def annotationToAdd2 = BasicInstanceBuilder.getUserAnnotation()
        def annotations = []
        annotations << JSON.parse(annotationToAdd1.encodeAsJSON())
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrectWithoutProject() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.project = null
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrectWithTerm() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        Term term1 = BasicInstanceBuilder.getTerm()
        Term term2 = BasicInstanceBuilder.getAnotherBasicTerm()
        term2.ontology = term1.ontology
        annotationToAdd.project.ontology = term1.ontology
        BasicInstanceBuilder.saveDomain(annotationToAdd.project)
        BasicInstanceBuilder.saveDomain(term2)


        def annotationWithTerm = JSON.parse((String)annotationToAdd.encodeAsJSON())
        annotationWithTerm.term = [term1.id, term2.id]

        log.info annotationToAdd.project.ontology.id

        def result = UserAnnotationAPI.create(annotationWithTerm.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)
        assert json.term.size() == 2


        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationBadGeom() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POINT(BAD GEOMETRY)'

        Long idTerm1 = BasicInstanceBuilder.getTerm().id
        Long idTerm2 = BasicInstanceBuilder.getAnotherBasicTerm().id
        updateAnnotation.term = [idTerm1, idTerm2]

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationOutOfBoundsGeom() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        ImageInstance im = annotationToAdd.image

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "POLYGON((-1 -1,-1 $im.baseImage.height,${im.baseImage.width+5} $im.baseImage.height,$im.baseImage.width 0,-1 -1))"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert result.data.location.toString() == "POLYGON ((0 $im.baseImage.height, $im.baseImage.width $im.baseImage.height, $im.baseImage.width 0, 0 0, 0 $im.baseImage.height))"
    }

    void testAddUserAnnotationBadGeomEmpty() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POLYGON EMPTY'
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationBadGeomNull() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = null
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationSliceNotExist() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = -99
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationSliceNull() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = null
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code //referenceSlice is taken
    }

    void testAddUserAnnotationImageNotExist() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = null
        updateAnnotation.image = -99
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testEditUserAnnotation() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def data = UpdateData.createUpdateSet(
                BasicInstanceBuilder.getUserAnnotation(),
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )

        def result = UserAnnotationAPI.update(annotationToAdd.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        Long commandId = JSON.parse(result.data).command

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

        result = UserAnnotationAPI.redo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.redo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))
    }

    void testEditUserAnnotationOutOfBoundsGeom() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        ImageInstance im = annotation.image

        def updateAnnotation = JSON.parse((String)annotation.encodeAsJSON())
        updateAnnotation.location = "POLYGON((-1 -1,-1 $im.baseImage.height,${im.baseImage.width+5} $im.baseImage.height,$im.baseImage.width 0,-1 -1))"

        def result = UserAnnotationAPI.update(annotation.id, updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)

        assert json.annotation.location.toString() == "POLYGON ((0 $im.baseImage.height, $im.baseImage.width $im.baseImage.height, $im.baseImage.width 0, 0 0, 0 $im.baseImage.height))"
    }

    void testEditUserAnnotationNotExist() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        UserAnnotation annotationToEdit = UserAnnotation.get(annotationToAdd.id)
        def jsonAnnotation = JSON.parse((String)annotationToEdit.encodeAsJSON())
        jsonAnnotation.id = "-99"
        def result = UserAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testEditUserAnnotationWithBadGeometry() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def jsonAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        jsonAnnotation.location = "POINT (BAD GEOMETRY)"
        def result = UserAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testDeleteUserAnnotation() {
        def annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        def id = annotationToDelete.id
        def result = UserAnnotationAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        Long commandId = JSON.parse(result.data).command

        assert 200 == result.code

        def showResult = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        //404 because the undoed annotation was not this one
        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code


        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteUserAnnotationNotExist() {
        def result = UserAnnotationAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteUserAnnotationWithTerm() {
        def annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def result = UserAnnotationAPI.delete(annotationToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def user = BasicInstanceBuilder.getUser(Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)

        annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def annotTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotationToDelete)
        annotTerm.user = user

        result = AnnotationTermAPI.createAnnotationTerm(annotTerm.encodeAsJSON(), Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.delete(annotationToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }




    void testListAlgoAnnotationByImageAndUser() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        UserAnnotation annotationWith2Term = BasicInstanceBuilder.getUserAnnotation()
        AnnotationTerm aat = BasicInstanceBuilder.getAnnotationTermNotExist(annotationWith2Term,true)


        def result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        //very small bbox, hight annotation number
        String bbox = "1,1,100,100"
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,2,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,3,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        result = UserAnnotationAPI.listByImageAndUser(-99, annotation.user.id, bbox, false,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, -99, bbox, false,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}
