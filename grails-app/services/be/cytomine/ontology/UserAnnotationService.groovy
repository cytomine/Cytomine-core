package be.cytomine.ontology

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

import be.cytomine.AnnotationDomain
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.command.*
import be.cytomine.meta.Property
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.image.server.RetrievalServer
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.sql.AnnotationListing
import be.cytomine.sql.UserAnnotationListing
import be.cytomine.utils.GeometryUtils
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import grails.converters.JSON
import grails.transaction.Transactional
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.FetchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.spatial.criterion.SpatialRestrictions


import static org.springframework.security.acls.domain.BasePermission.READ

//import org.hibernatespatial.criterion.SpatialRestrictions
@Transactional
class UserAnnotationService extends ModelService {

    static transactional = true
    def cytomineService
    def transactionService
    def annotationTermService
    def imageRetrievalService
    def algoAnnotationTermService
    def modelService
    def simplifyGeometryService
    def dataSource
    def reviewedAnnotationService
    def propertyService
    def kmeansGeometryService
    def annotationListingService
    def securityACLService
    def currentRoleServiceProxy
    def sharedAnnotationService
    def imageInstanceService
    def abstractImageService
    def sliceInstanceService

    def currentDomain() {
        return UserAnnotation
    }

    UserAnnotation read(def id) {
        def annotation = UserAnnotation.read(id)
        if (annotation) {
            securityACLService.check(annotation.container(), READ)
            checkDeleted(annotation)
        }
        annotation
    }

    def list(Project project, def propertiesToShow = null) {
        securityACLService.check(project.container(), READ)
        annotationListingService.executeRequest(new UserAnnotationListing(
                project: project.id,
                columnToPrint: propertiesToShow
        ))
    }

    def listIncluded(ImageInstance image, String geometry, SecUser user, List<Long> terms, AnnotationDomain annotation = null,
                     def propertiesToShow = null) {
        securityACLService.check(image.container(), READ)
        annotationListingService.executeRequest(new UserAnnotationListing(
                columnToPrint: propertiesToShow,
                image: image.id,
                user: user.id,
                terms: terms,
                excludedAnnotation: annotation?.id,
                bbox: geometry
        ))
    }

    /**
     * List annotation where a user from 'userList' has added term 'realTerm' and for which a specific job has predicted 'suggestedTerm'
     * @param project Annotation project
     * @param userList Annotation user list filter
     * @param realTerm Annotation term (add by user)
     * @param suggestedTerm Annotation predicted term (from job)
     * @param job Job that make prediction
     * @return
     */
    def list(Project project, List<Long> userList, Term realTerm, Term suggestedTerm, Job job,
             def propertiesToShow = null) {
        securityACLService.check(project.container(), READ)
        if (userList.isEmpty()) {
            return []
        }
        annotationListingService.executeRequest(new UserAnnotationListing(
                columnToPrint: propertiesToShow,
                project: project.id,
                users: userList,
                term: realTerm.id,
                suggestedTerm: suggestedTerm.id,
                userForTermAlgo: UserJob.findByJob(job)
        ))
    }

    /**
     * List annotations according to some filters parameters (rem : use list light if you only need the response, not
     * the objects)
     * @param image the image instance
     * @param bbox Geometry restricted Area
     * @param termsIDS filter terms ids
     * @param userIDS filter user ids
     * @return Annotation listing
     */
    def list(ImageInstance image, Geometry bbox, List<Long> termsIDS, List<Long> userIDS) {
        //:to do use listlight and parse WKT instead ?
        Collection<UserAnnotation> annotations = UserAnnotation.createCriteria()
                .add(Restrictions.isNull("deleted"))
                .add(Restrictions.in("user.id", userIDS))
                .add(Restrictions.eq("image.id", image.id))
                .add(SpatialRestrictions.intersects("location", bbox))
                .list()

        if (!annotations.isEmpty() && termsIDS.size() > 0) {
            annotations = (Collection<UserAnnotation>) AnnotationTerm.createCriteria().list {
                isNull("deleted")
                inList("term.id", termsIDS)
                join("userAnnotation")
                createAlias("userAnnotation", "a")
                projections {
                    inList("a.id", annotations.collect { it.id })
                    groupProperty("userAnnotation")
                }
            }
        }

        return annotations
    }

    def list(SliceInstance slice, Geometry bbox, List<Long> termsIDS, List<Long> userIDS) {
        //:to do use listlight and parse WKT instead ?
        Collection<UserAnnotation> annotations = UserAnnotation.createCriteria()
                .add(Restrictions.isNull("deleted"))
                .add(Restrictions.in("user.id", userIDS))
                .add(Restrictions.eq("slice.id", slice.id))
                .add(SpatialRestrictions.intersects("location", bbox))
                .list()

        if (!annotations.isEmpty() && termsIDS.size() > 0) {
            annotations = (Collection<UserAnnotation>) AnnotationTerm.createCriteria().list {
                isNull("deleted")
                inList("term.id", termsIDS)
                join("userAnnotation")
                createAlias("userAnnotation", "a")
                projections {
                    inList("a.id", annotations.collect { it.id })
                    groupProperty("userAnnotation")
                }
            }
        }

        return annotations
    }

    def count(User user, Project project = null) {
        if (project) return UserAnnotation.countByUserAndProject(user, project)
        return UserAnnotation.countByUser(user)
    }

    def countByProject(Project project, Date startDate, Date endDate) {
        String request = "SELECT COUNT(*) FROM UserAnnotation WHERE project = $project.id " +
                (startDate ? "AND created > '$startDate' " : "") +
                (endDate ? "AND created < '$endDate' " : "")
        def result = UserAnnotation.executeQuery(request)
        return result[0]
    }

    /**
     * List all annotation with a very light structure: id, project and crop url
     */
    def listLight() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        String request = "" +
                "SELECT a.id as id, a.project_id as project FROM user_annotation a, image_instance ii, abstract_image ai WHERE a.image_id = ii.id AND ii.base_image_id = ai.id AND ai.original_filename not like '%ndpi%svs%' AND GeometryType(a.location) != 'POINT' AND st_area(a.location) < 1500000 ORDER BY st_area(a.location) DESC"
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request) {

            long idAnnotation = it[0]
            long idContainer = it[1]
            def url = UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize(idAnnotation)
            data << [id: idAnnotation, container: idContainer, url: url]
        }
        sql.close()
        data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json, def minPoint = null, def maxPoint = null) {
        SliceInstance slice = null
        ImageInstance image = null
        Project project = null

        if (json.slice) {
            slice = sliceInstanceService.read(json.slice)
            image = slice?.image
            project = slice?.project
        } else if (json.image) {
            image = imageInstanceService.read(json.image)
            slice = image?.referenceSlice
            project = image?.project
        }

        if (!project) {
            throw new WrongArgumentException("Annotation does not have a valid project.")
        }

        if (json.isNull('location')) {
            throw new WrongArgumentException("Annotation must have a valid geometry:" + json.location)
        }

        json.slice = slice.id
        json.image = image.id
        json.project = project.id

        securityACLService.check(project, READ)
        securityACLService.checkisNotReadOnly(project)

        SecUser currentUser = cytomineService.getCurrentUser()
        if (!json.user) {
            json.user = currentUser.id
        } else if (json.user != currentUser.id) {
            securityACLService.checkFullOrRestrictedForOwner(project)
        }

        Geometry annotationShape
        try {
            annotationShape = new WKTReader().read(json.location)
        }
        catch (Exception ignored) {
            throw new WrongArgumentException("Annotation location is not valid")
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid")
        }

        def envelope = annotationShape.getEnvelopeInternal()
        if (envelope.minX < 0 ||
                envelope.minY < 0 ||
                envelope.maxX > image.baseImage.width ||
                envelope.maxY > image.baseImage.height) {
            double maxX = Math.min(envelope.maxX, image.baseImage.width)
            double maxY = Math.min(envelope.maxY, image.baseImage.height)
            Geometry insideBounds = new WKTReader().read("POLYGON((0 0,0 $maxY,$maxX $maxY,$maxX 0,0 0))")
            annotationShape = annotationShape.intersection(insideBounds)
        }

        if(!(annotationShape.geometryType.equals("LineString"))) {
            def boundaries = GeometryUtils.getGeometryBoundaries(annotationShape)
            if (boundaries == null || boundaries.width == 0 || boundaries.height == 0) {
                throw new WrongArgumentException("Annotation dimension not valid")
            }
        }

        //simplify annotation
        try {
            if(minPoint != null) minPoint = Long.parseLong(minPoint)
            if(maxPoint != null) maxPoint = Long.parseLong(maxPoint)
            def data = simplifyGeometryService.simplifyPolygon(annotationShape, minPoint, maxPoint)
            json.location = data.geometry
            json.geometryCompression = data.rate
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e)
        }

        if (!json.location) {
            json.location = annotationShape
            json.geometryCompression = 0.0d
        }

        //Start transaction
        Transaction transaction = transactionService.start()
        def result = executeCommand(new AddCommand(user: currentUser, transaction: transaction), null, json)
        def addedAnnotation = result?.object

        if (!addedAnnotation) {
            return result
        }

        // Add annotation-term if any
        def termIds = JSONUtils.getJSONList(json.term) + JSONUtils.getJSONList(json.terms)
        def terms = termIds.collect { id ->
            def r = annotationTermService.addAnnotationTerm(addedAnnotation.id, id, null, currentUser.id, currentUser, transaction)
            return r?.data?.annotationterm?.term
        }
        result.data.annotation.term = terms

        // Add properties if any
        def properties = JSONUtils.getJSONList(json.property) + JSONUtils.getJSONList(json.properties)
        properties.each {
            def key = it.key as String
            def value = it.value as String
            propertyService.addProperty("be.cytomine.ontology.UserAnnotation", addedAnnotation.id, key, value, currentUser, transaction)
        }

        // Add annotation-track if any
        def tracksIds = JSONUtils.getJSONList(json.track) + JSONUtils.getJSONList(json.tracks)
        def annotationTracks = tracksIds.collect { id ->
            def r = annotationTrackService.addAnnotationTrack("be.cytomine.ontology.UserAnnotation", addedAnnotation.id, id, addedAnnotation.slice.id, currentUser, transaction)
            return r?.data?.annotationtrack
        }
        result.data.annotation.annotationTrack = annotationTracks
        result.data.annotation.track = annotationTracks.collect { it -> it.track }

        return result
    }

    def afterAdd(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('userannotation')
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    def update(UserAnnotation annotation, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        //securityACLService.checkIsSameUserOrAdminContainer(annotation,annotation.user,currentUser)
        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user)

        // TODO: what about image/project ??

        Geometry annotationShape
        try {
            annotationShape = new WKTReader().read(jsonNewData.location)
        }
        catch (ParseException ignored) {
            throw new WrongArgumentException("Annotation location is not valid")
        }

        if (!annotationShape.isValid()) {
            throw new WrongArgumentException("Annotation location is not valid")
        }

        ImageInstance im = imageInstanceService.read(jsonNewData.image)
        if(!im){
            throw new WrongArgumentException("Annotation not associated with a valid image")
        }
        def envelope = annotationShape.getEnvelopeInternal()
        if (envelope.minX < 0 ||
                envelope.minY < 0 ||
                envelope.maxX > im.baseImage.width ||
                envelope.maxY > im.baseImage.height) {
            double maxX = Math.min(annotationShape.getEnvelopeInternal().maxX, im.baseImage.width)
            double maxY = Math.min(annotationShape.getEnvelopeInternal().maxY, im.baseImage.height)
            Geometry insideBounds = new WKTReader().read("POLYGON((0 0,0 $maxY,$maxX $maxY,$maxX 0,0 0))")
            annotationShape = annotationShape.intersection(insideBounds)
        }

        //simplify annotation
        try {
            double rate
            if(jsonNewData.geometryCompression != null && !jsonNewData.geometryCompression instanceof org.codehaus.groovy.grails.web.json.JSONObject.Null)
                rate = Double.parseDouble(jsonNewData.geometryCompression)
            else if(jsonNewData.geometryCompression instanceof org.codehaus.groovy.grails.web.json.JSONObject.Null)
                rate = 0d

            def data = simplifyGeometryService.simplifyPolygon(annotationShape, rate)

            jsonNewData.location = data.geometry
            jsonNewData.geometryCompression = data.rate
        } catch (Exception e) {
            log.error("Cannot simplify annotation location:" + e)
        }

        def result = executeCommand(new EditCommand(user: currentUser), annotation, jsonNewData)
        if (result.success) {
            Long id = result.userannotation.id
            try {
                updateRetrievalAnnotation(id)
            } catch (Exception e) {
                log.error "Cannot update in retrieval:" + e.toString()
            }
        }

        return result
    }

    def afterUpdate(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('userannotation')
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(UserAnnotation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkFullOrRestrictedForOwner(domain, domain.user)
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c, domain, jsonNewData)
    }

    def afterDelete(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('userannotation')
    }

    def repeat(def userAnnotation, def baseSliceId, def repeat) {
        SliceInstance currentSlice = sliceInstanceService.read(baseSliceId)

        def slices = SliceInstance.createCriteria().list {
            createAlias("baseSlice", "as")
            eq("image", userAnnotation.image)
            order("as.time", "asc")
            order("as.zStack", "asc")
            order("as.channel", "asc")
            fetchMode("baseSlice", FetchMode.JOIN)
            ge("as.time", currentSlice.baseSlice.time)
            ge("as.zStack", currentSlice.baseSlice.zStack)
            ge("as.channel", currentSlice.baseSlice.channel)
            ne("id", userAnnotation.slice.id)
            maxResults(repeat)
        }

        def collection = []
        slices.each { slice ->
            collection << add(new JSONObject([
                    slice   : slice.id,
                    location: userAnnotation.location.toString(),
                    terms   : userAnnotation.termsId(),
                    tracks  : userAnnotation.tracksId()
            ]))
        }

        return [collection: collection]
    }

    def getStringParamsI18n(def domain) {
        return [cytomineService.getCurrentUser().toString(), domain.image?.getBlindInstanceFilename(), domain.user.toString()]
    }


    def deleteDependentAlgoAnnotationTerm(UserAnnotation ua, Transaction transaction, Task task = null) {
        AlgoAnnotationTerm.findAllByAnnotationIdent(ua.id).each {
            algoAnnotationTermService.delete(it, transaction, null, false)
        }
    }

    def deleteDependentAnnotationTerm(UserAnnotation ua, Transaction transaction, Task task = null) {
        AnnotationTerm.findAllByUserAnnotation(ua).each {
            try {
                annotationTermService.delete(it, transaction, null, false)
            } catch (ForbiddenException fe) {
                throw new ForbiddenException("This annotation has been linked to the term " + it.term + " by " + it.userDomainCreator() + ". " + it.userDomainCreator() + " must unlink its term before you can delete this annotation.")
            }
        }
    }

    def deleteDependentReviewedAnnotation(UserAnnotation ua, Transaction transaction, Task task = null) {
//        ReviewedAnnotation.findAllByParentIdent(ua.id).each {
//            reviewedAnnotationService.delete(it,transaction,null,false)
//        }
    }

    def deleteDependentSharedAnnotation(UserAnnotation ua, Transaction transaction, Task task = null) {
        //TODO: we should implement a full service for sharedannotation and delete them if annotation is deleted
//        if(SharedAnnotation.findByUserAnnotation(ua)) {
//            throw new ConstraintException("There are some comments on this annotation. Cannot delete it!")
//        }

        SharedAnnotation.findAllByAnnotationClassNameAndAnnotationIdent(ua.class.name, ua.id).each {
            sharedAnnotationService.delete(it, transaction, null, false)
        }

    }

    def annotationTrackService

    def deleteDependentAnnotationTrack(UserAnnotation ua, Transaction transaction, Task task = null) {
        AnnotationTrack.findAllByAnnotationIdent(ua.id).each {
            annotationTrackService.delete(it, transaction, task)
        }
    }
}
