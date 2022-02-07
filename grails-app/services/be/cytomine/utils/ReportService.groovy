package be.cytomine.utils

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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.api.UrlApi
import be.cytomine.ontology.Term
import be.cytomine.project.Project
import be.cytomine.sql.AnnotationListing
import be.cytomine.sql.ReviewedAnnotationListing
import be.cytomine.sql.UserAnnotationListing
import be.cytomine.sql.AlgoAnnotationListing

import java.text.SimpleDateFormat

/**
 * Cytomine
 * User: stevben
 * Date: 13/03/13
 * Time: 11:48
 */
class ReportService {

    def projectService
    def paramsService
    def grailsApplication
    def annotationListingService
    def exportService
    def secUserService

    def createAnnotationDocuments(Long idProject, def termsParam, boolean noTerm, boolean multipleTerms, def usersParam, def imagesParam, Long afterThan, Long beforeThan, def format,def response, String type) {

        Project project = projectService.read(idProject)

        if (!project) {
            throw new ObjectNotFoundException("Project $idProject was not found!")
        }

        def users = paramsService.getParamsSecUserList(usersParam,project)
        def terms = paramsService.getParamsTermList(termsParam,project)
        def images = paramsService.getParamsImageInstanceList(imagesParam,project)

        def termsName = [:]
        Term.findAllByIdInList(terms).each {
            termsName.put(it.id,it.name)
        }

        def exporterIdentifier = format;
        if (exporterIdentifier == "xls") {
            exporterIdentifier = "excel"
        }
        response.contentType = grailsApplication.config.grails.mime.types[format]
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String datePrefix = simpleFormat.format(new Date())
        response.setHeader("Content-disposition", "attachment; filename=${datePrefix}_annotations_project_${project.id}.${format}")

        AnnotationListing al = new UserAnnotationListing()
        if(type=="ALGOANNOTATION") {
            al = new AlgoAnnotationListing()
        } else if(type=="REVIEWEDANNOTATION") {
            al = new ReviewedAnnotationListing()
        }

        al.columnToPrint = ['basic','meta','wkt','gis','term','image','user']

        al.project = project.id
        al.images = images
        if(type=="REVIEWEDANNOTATION") {
            al.reviewUsers = users
        }
        else {
            al.users = users
        }
        al.terms = terms
        if(afterThan) {
            al.afterThan = new Date(afterThan)
        }
        if(beforeThan) {
            al.beforeThan = new Date(beforeThan)
        }

        def exportResult = []
        def termNameUsed = []
        def userNameUsed = []

        def annotations = annotationListingService.listGeneric(al)

        Set<Long> annotationsAlreadyFetched = new HashSet<>();
        annotations.each { annotation ->
            def data = [:]
            data.id = annotation.id
            data.perimeterUnit = annotation.perimeterUnit
            data.areaUnit = annotation.areaUnit
            data.area = annotation.area
            data.perimeter = annotation.perimeter
            data.XCentroid = String.format("%.2f", (float) annotation.centroid.x)
            data.YCentroid = String.format("%.2f", (float) annotation.centroid.y)
            data.image = annotation.image
            data.filename = annotation.instanceFilename
            data.user = annotation.creator
            data.term = annotation.term.collect{termsName.get(it)}.join(", ")
            data.cropURL = UrlApi.getCompleteAnnotationCropDrawedWithAnnotationId(annotation.id)
            data.cropGOTO = UrlApi.getAnnotationURL(annotation.project, annotation.image, annotation.id)
            exportResult.add(data)
            annotationsAlreadyFetched.add(annotation.id)
            annotation.term.each{termNameUsed << termsName.get(it)}
            userNameUsed << annotation.creator
        }

        if(multipleTerms) {
            al.multipleTerm = true
            annotations = annotationListingService.listGeneric(al)

            annotations.each { annotation ->
                if (!annotationsAlreadyFetched.contains(annotation.id)) {
                    def data = [:]
                    data.id = annotation.id
                    data.perimeterUnit = annotation.perimeterUnit
                    data.areaUnit = annotation.areaUnit
                    data.area = annotation.area
                    data.perimeter = annotation.perimeter
                    data.XCentroid = String.format("%.2f", (float) annotation.centroid.x)
                    data.YCentroid = String.format("%.2f", (float) annotation.centroid.y)
                    data.image = annotation.image
                    data.filename = annotation.instanceFilename
                    data.user = annotation.creator
                    data.term = annotation.term.collect{termsName.get(it)}.join(", ")
                    data.cropURL = UrlApi.getCompleteAnnotationCropDrawedWithAnnotationId(annotation.id)
                    data.cropGOTO = UrlApi.getAnnotationURL(annotation.project, annotation.image, annotation.id)
                    exportResult.add(data)
                    annotationsAlreadyFetched.add(annotation.id)
                    annotation.term.each{termNameUsed << termsName.get(it)}
                    userNameUsed << annotation.creator
                }
            }
        }



        if(noTerm) {
            al.noTerm = true
            al.multipleTerm = false
            al.terms = null
            al.usersForTerm = null
            annotations = annotationListingService.listGeneric(al)

            annotations.each { annotation ->
                if (!annotationsAlreadyFetched.contains(annotation.id)) {
                    def data = [:]
                    data.id = annotation.id
                    data.perimeterUnit = annotation.perimeterUnit
                    data.areaUnit = annotation.areaUnit
                    data.area = annotation.area
                    data.perimeter = annotation.perimeter
                    data.XCentroid = String.format("%.2f", (float) annotation.centroid.x)
                    data.YCentroid = String.format("%.2f", (float) annotation.centroid.y)
                    data.image = annotation.image
                    data.filename = annotation.instanceFilename
                    data.user = annotation.creator
                    data.term = annotation.term.collect { termsName.get(it) }.join(", ")
                    data.cropURL = UrlApi.getCompleteAnnotationCropDrawedWithAnnotationId(annotation.id)
                    data.cropGOTO = UrlApi.getAnnotationURL(annotation.project, annotation.image, annotation.id)
                    exportResult.add(data)
                    annotationsAlreadyFetched.add(annotation.id)
                    annotation.term.each { termNameUsed << termsName.get(it) }
                    userNameUsed << annotation.creator
                }
            }

            termNameUsed << "no term"
        }

        termNameUsed.unique()
        userNameUsed.unique()
        List fields = ["id", "area", "perimeter", "XCentroid", "YCentroid", "image", "filename", "user", "term", "cropURL", "cropGOTO"]
        Map labels = [
                "id": "ID",
                "area": "Area (microns²)",
                "perimeter": "Perimeter (mm)",
                "XCentroid": "Center X",
                "YCentroid": "Center Y",
                "image": "Image ID",
                "filename": "Image filename",
                "user": "User",
                "term": "Term",
                "cropURL": "Annotation thumb",
                "cropGOTO": "Annotation in Cytomine"
        ]
        String title = "Annotations in " + project.getName() + " created by " + userNameUsed.join(" or ") + " and associated with " + termNameUsed.join(" or ") + " @ " + (new Date()).toLocaleString()
        exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null,
                ["column.widths": [0.05, 0.07, 0.07, 0.06, 0.06, 0.05, 0.12, 0.10, 0.12, 0.15, 0.15],
                 "title": title,
                 "csv.encoding": "UTF-8",
                 "separator": ";"]
        )


    }

    // contain only username and user pretty name
    def createUserListingLightDocuments(Long idProject, def format,def response) {

        Project project = projectService.read(idProject)

        if (!project) {
            throw new ObjectNotFoundException("Project $idProject was not found!")
        }

        def exporterIdentifier = format;
        if (exporterIdentifier == "xls") {
            exporterIdentifier = "excel"
        }
        response.contentType = grailsApplication.config.grails.mime.types[format]
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String datePrefix = simpleFormat.format(new Date())
        response.setHeader("Content-disposition", "attachment; filename=${datePrefix}_annotations_project${project.id}.${format}")

        def exportResult = []

        def users = secUserService.listUsers(project);
        users.each { user ->
            def data = [:]
            data.username = user.username
            data.firstname = user.firstname
            data.lastname = user.lastname
            exportResult.add(data)
        }
        List fields = ["username", "firstname", "lastname"]
        Map labels = ["username": "User Name", "firstname": "First Name", "lastname": "Last Name"]
        String title = "Users in " + project.getName() + " @ " + (new Date()).toLocaleString()
        exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null, ["column.widths": [0.33, 0.33, 0.33], "title": title, "csv.encoding": "UTF-8", "separator": ";"])


    }

    // contain only username and user pretty name
    def createUserFullListingLightDocuments(def format,def response) {

        def exporterIdentifier = format;
        if (exporterIdentifier == "xls") {
            exporterIdentifier = "excel"
        }
        response.contentType = grailsApplication.config.grails.mime.types[format]
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String datePrefix = simpleFormat.format(new Date())
        response.setHeader("Content-disposition", "attachment; filename=${datePrefix}_users.${format}")

        def exportResult = []

        def users = secUserService.list([withRoles:true]);
        users.each { user ->
            def data = [:]
            data.username = user.username
            data.fullname = user.lastname + " " + user.firstname
            data.email = user.email
            data.status = user.accountLocked ? "locked" : "active"
            data.role = user.role
            data.lastConnection = user.lastConnection
            exportResult.add(data)
        }

        List fields = ["username", "fullname", "email", "status", "role", "lastConnection"]
        Map labels = ["username": "User Name", "fullname": "Full Name", "email": "Email", "status": "Status", "role" : "Role", "lastConnection": "Last connection"]
        String title = "Users @ " + (new Date()).toLocaleString()
        exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null, ["title": title, "csv.encoding": "UTF-8", "separator": ";"])
    }
}
