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
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.AlgoAnnotationTerm
import be.cytomine.ontology.Term
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.UpdateData
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class AlgoAnnotationTests  {

    void testGetAlgoAnnotationWithCredential() {
        def annotation = BasicInstanceBuilder.getAlgoAnnotation()
        def result = AlgoAnnotationAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }


    void testDownloadAlgoAnnotationDocument() {
        AlgoAnnotationTerm annotationTerm = BasicInstanceBuilder.getAlgoAnnotationTerm(true)
        def result = AlgoAnnotationAPI.downloadDocumentByProject(annotationTerm.retrieveAnnotationDomain().project.id,annotationTerm.retrieveAnnotationDomain().user.id,annotationTerm.term.id, annotationTerm.retrieveAnnotationDomain().image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddAlgoAnnotationCorrect() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user
        try {Infos.addUserRight(user.user.username,annotationToAdd.project)} catch(Exception e) {println e}
        def result = AlgoAnnotationAPI.create(annotationToAdd.encodeAsJSON(),user.username, 'PasswordUserJob')
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 404 == result.code

        result = AlgoAnnotationAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddAlgoAnnotationMultipleCorrect() {
        UserJob userJob = BasicInstanceBuilder.getUserJobNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(userJob.job.project,true)
        def annotationToAdd1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(userJob.job, userJob , image)
        def annotationToAdd2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(userJob.job, userJob , image)

        Long idTerm1 = BasicInstanceBuilder.getTermNotExist(image.project.ontology,true).id
        Long idTerm2 = BasicInstanceBuilder.getTermNotExist(image.project.ontology,true).id
        def annotationWithTerm = JSON.parse((String)annotationToAdd1.encodeAsJSON())
        annotationWithTerm.term = [idTerm1, idTerm2]

        UserJob user1 = annotationToAdd1.user
        def annotations = []
        annotations << JSON.parse(JSONUtils.toJSONString(annotationWithTerm))
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = AlgoAnnotationAPI.create(JSONUtils.toJSONString(annotations), user1.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddMultipleIncorrectAlgoAnnotations() {
        def annotationToAdd1 = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        def annotationToAdd2 = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        annotationToAdd2.image =  annotationToAdd1.image
        annotationToAdd2.project =  annotationToAdd1.project
        annotationToAdd1.location = new WKTReader().read("POLYGON ((1983 2168, 2083 2268, 2183 2368, 1983 2168))")
        annotationToAdd2.location = new WKTReader().read("POLYGON ((1983 2168, 2083 2268, 2183 2368, 1983 2168))")

        UserJob user1 = annotationToAdd1.user
        def annotations = []
        annotations << JSON.parse(annotationToAdd1.encodeAsJSON())
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = AlgoAnnotationAPI.create(JSONUtils.toJSONString(annotations), user1.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testAddMultipleAlgoAnnotationMsWithIncorrectValues() {
        def annotationToAdd1 = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        def annotationToAdd2 = BasicInstanceBuilder.getAlgoAnnotation()
        annotationToAdd2.image =  annotationToAdd1.image
        annotationToAdd2.project =  annotationToAdd1.project
        annotationToAdd1.location = new WKTReader().read("POLYGON ((1983 2168, 2083 2268, 2183 2368, 1983 2168))")

        UserJob user1 = annotationToAdd1.user
        def annotations = []
        annotations << JSON.parse(annotationToAdd1.encodeAsJSON())
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = AlgoAnnotationAPI.create(JSONUtils.toJSONString(annotations), user1.username, 'PasswordUserJob')
        assert 206 == result.code
    }

    void testAddAlgoAnnotationCorrectWithoutProject() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.project = null
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddAlgoAnnotationCorrectWithTerm() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        Term term1 = BasicInstanceBuilder.getTerm()
        Term term2 = BasicInstanceBuilder.getAnotherBasicTerm()
        term2.ontology = term1.ontology
        annotationToAdd.project.ontology = term1.ontology
        BasicInstanceBuilder.saveDomain(annotationToAdd.project)
        BasicInstanceBuilder.saveDomain(term2)

        UserJob user = annotationToAdd.user


        def annotationWithTerm = JSON.parse((String)annotationToAdd.encodeAsJSON())
        annotationWithTerm.term = [term1.id, term2.id]

        def result = AlgoAnnotationAPI.create(JSONUtils.toJSONString(annotationWithTerm), user.username, 'PasswordUserJob')
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code

        def json = JSON.parse(result.data)
        assert json.term.size() == 2

        result = AlgoAnnotationAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 404 == result.code

        result = AlgoAnnotationAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddAlgoAnnotationWithoutProject() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.project = null

        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddAlgoAnnotationBadGeom() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POINT(BAD GEOMETRY)'

        Long idTerm1 = BasicInstanceBuilder.getTerm().id
        Long idTerm2 = BasicInstanceBuilder.getAnotherBasicTerm().id
        updateAnnotation.term = [idTerm1, idTerm2]

        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testAddAlgoAnnotationOutOfBoundsGeom() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        ImageInstance im = annotationToAdd.image

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "POLYGON((-1 -1,-1 $im.baseImage.height,${im.baseImage.width+5} $im.baseImage.height,$im.baseImage.width 0,-1 -1))"

        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 200 == result.code
        assert result.data.location.toString() == "POLYGON ((0 $im.baseImage.height, $im.baseImage.width $im.baseImage.height, $im.baseImage.width 0, 0 0, 0 $im.baseImage.height))"

    }

    void testAddAlgoAnnotationBadGeomEmpty() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POLYGON EMPTY'
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testAddAlgoAnnotationBadGeomNull() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = null
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testAddAlgoAnnotationSliceNotExist() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = -99
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddAlgoAnnotationSliceNull() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = null
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 200 == result.code //referenceSlice is taken
    }

    void testAddAlgoAnnotationImageNotExist() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.slice = null
        updateAnnotation.image = -99
        def result = AlgoAnnotationAPI.create(updateAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testEditAlgoAnnotation() {
        def aa = BasicInstanceBuilder.getAlgoAnnotation()
        def data = UpdateData.createUpdateSet(
                aa,
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )
        UserJob user = aa.user

        def result = AlgoAnnotationAPI.update(aa.id, data.postData,user.username, 'PasswordUserJob')
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        def showResult = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = AlgoAnnotationAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code
        showResult = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = AlgoAnnotationAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code
        showResult = AlgoAnnotationAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testEditAlgoAnnotationNotExist() {
        AlgoAnnotation annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        AlgoAnnotation annotationToEdit = AlgoAnnotation.get(annotationToAdd.id)
        def jsonAnnotation = JSON.parse((String)annotationToEdit.encodeAsJSON())
        jsonAnnotation.id = "-99"
        def result = AlgoAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), user.username,'PasswordUserJob')
        assert 404 == result.code
    }

    void testEditAlgoAnnotationWithBadGeometry() {
        AlgoAnnotation annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def jsonAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        jsonAnnotation.location = "POINT (BAD GEOMETRY)"
        def result = AlgoAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), user.username, 'PasswordUserJob')
        assert 400 == result.code
    }

    void testDeleteAlgoAnnotation() {
        def annotationToDelete = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        UserJob user = annotationToDelete.user

        def id = annotationToDelete.id
        def result = AlgoAnnotationAPI.delete(id, user.username, 'PasswordUserJob')
        assert 200 == result.code

        def showResult = AlgoAnnotationAPI.show(id, user.username,'PasswordUserJob')
        assert 404 == showResult.code

        result = AlgoAnnotationAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(id, user.username,'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AlgoAnnotationAPI.show(id, user.username,'PasswordUserJob')
        assert 404 == result.code
    }

    void testDeleteAlgoAnnotationNotExist() {
        def result = AlgoAnnotationAPI.delete(-99, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteAlgoAnnotationWithData() {
        def annotTerm = BasicInstanceBuilder.getAlgoAnnotationTerm(true)
        UserJob user = annotTerm.retrieveAnnotationDomain().user

        def annotationToDelete = annotTerm.retrieveAnnotationDomain()
        def result = AlgoAnnotationAPI.delete(annotationToDelete.id,user.username,'PasswordUserJob')
        assert 200 == result.code
    }

    void testCountAnnotationByProject() {
        def result = AlgoAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProjectWithDates() {
        Date startDate = new Date()
        def result = AlgoAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

}
