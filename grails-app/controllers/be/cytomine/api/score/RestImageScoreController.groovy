package be.cytomine.api.score

import be.cytomine.api.RestController
import be.cytomine.api.UrlApi
import be.cytomine.image.ImageInstance

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, score
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.project.Project
import be.cytomine.score.ImageScore
import be.cytomine.score.Score
import be.cytomine.score.ScoreProject
import be.cytomine.security.SecUser
import be.cytomine.security.User
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import java.text.SimpleDateFormat

/**
 * Controller for score project link
 * A score may be used by some project
 */
@RestApi(name = "Score image services", description = "Methods for managing score for an image")
class RestImageScoreController extends RestController{

    def imageScoreService
    def projectService
    def cytomineService
    def imageInstanceService
    def scoreService
    def exportService


    /**
     * Get a score project link
     */
    @RestApiMethod(description="Get an image score")
    @RestApiParams(params=[
        @RestApiParam(name="imageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image id"),
        @RestApiParam(name="score", type="long", paramType = RestApiParamType.PATH, description = "The score id")
    ])
    def show() {
        SecUser user = cytomineService.currentUser
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        Score score = scoreService.read(params.getLong("score"))
        println "Look for score ${score?.id} image ${imageInstance?.id} user ${user?.id}"
        ImageScore imageScore = imageScoreService.read(imageInstance, score, (User)user)
        if (imageScore) responseSuccess(imageScore)
        else responseNotFound("ImageScore", null)
    }

    /**
     * List scores values for an image
     */
    @RestApiMethod(description="List scores values for an image")
    @RestApiParams(params=[
            @RestApiParam(name="imageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image id"),
    ])
    def listByImageInstance() {
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        SecUser user = cytomineService.currentUser
        responseSuccess(imageScoreService.listByImageInstanceAndUser(imageInstance, (User)user))
    }

    /**
     * List scores values for a project
     */
    @RestApiMethod(description="List scores values for a project")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
    ])
    def listByProject() {
        Project project = projectService.read(params.getLong("project"))
        SecUser user = cytomineService.currentUser
        responseSuccess(imageScoreService.listByProjectAndUser(project, (User)user))
    }

    def statsGroupByImageInstances() {
        String sortColumn = params.sort ? params.sort : "created"
        String sortDirection = params.order ? params.order : "desc"
        Project project = projectService.read(params.getLong("project"))
        def searchString = params['name[ilike]']
        def stats = imageScoreService.statsGroupByImageInstances(project, sortColumn, sortDirection, searchString)
        responseSuccess(stats)
    }

    def statsGroupByUsers() {
        String sortColumn = params.sort ? params.sort : "created"
        String sortDirection = params.order ? params.order : "desc"
        Project project = projectService.read(params.getLong("project"))
        def searchString = params['name[ilike]']
        def stats = imageScoreService.statsGroupBySecUser(project, sortColumn, sortDirection, searchString)
        responseSuccess(stats)
    }

    def statsReport() {
        Project project = projectService.read(params.getLong("project"))
        def stats = imageScoreService.statsReport(project)
        if (params.format!='json') {
            downloadImageReport(stats, project)
        } else {
            responseSuccess(stats)
        }
    }


    private downloadImageReport(def rows, Project project) {
        if (params?.format && params.format != "html") {
            def exporterIdentifier = params.format;
            if (exporterIdentifier == "xls") exporterIdentifier = "excel"
            response.contentType = grailsApplication.config.grails.mime.types[params.format]
            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
            String datePrefix = simpleFormat.format(new Date())
            response.setHeader("Content-disposition", "attachment; filename=${datePrefix}_score_report.${params.format}")

            def scores = ScoreProject.findAllByProject(project).collect { it.score }
            scores.sort { it.name }
            List fields = ["imageId", "instanceFilename", "userId", "username"]
            scores.each {
                fields << String.valueOf(it.id)
                fields << "consensus" + String.valueOf(it.id)
            }
            Map labels = ["imageId": "Id", "instanceFilename": "Filename", "userId": "UserId", "username": "Username"]
            scores.each {
                labels[String.valueOf(it.id)] = it.name
                labels["consensus"+String.valueOf(it.id)] = "consensus " + it.name
            }

            String title = "Scores report"

            String GDPRWarning = "NB: Regnearket inneholder personinformasjon, som ikke må distribueres uten avklaring med brukere. Eventuelt  kan du slette bruker id kolonne med brukerID"
            if (exporterIdentifier == "pdf") {
                title = GDPRWarning
            } else {
                def data = [:]
                data.imageId = GDPRWarning
                rows.add(0,data)
            }

            exportService.export(exporterIdentifier, response.outputStream, rows, fields, labels, null, ["title": title, "csv.encoding": "UTF-8", "separator": ";"])
        }
    }

    /**
     * Add an existing score to a project
     */
    @RestApiMethod(description="Add an existing score to a project")
    def add () {
        def json = JSON.parse("{imageInstance: ${params.getLong("imageInstance")}, scoreValue: ${params.getLong("value")}, user: ${cytomineService.currentUser.id}}")
        add(imageScoreService, json)
    }

    /**
     * Delete the score for the project
     */
    @RestApiMethod(description="Remove the score from the project")
    def delete() {
        SecUser user = cytomineService.currentUser
        ImageInstance imageInstance = imageInstanceService.read(params.getLong("imageInstance"))
        Score score = scoreService.read(params.getLong("score"))
        ImageScore imageScore = imageScoreService.read(imageInstance, score, (User)user)
        delete(imageScoreService, JSON.parse("{id : $imageScore.id}"),null)
    }
}
