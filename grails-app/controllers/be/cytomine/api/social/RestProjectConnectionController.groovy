package be.cytomine.api.social


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

import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.social.PersistentProjectConnection
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import java.text.SimpleDateFormat

import static org.springframework.security.acls.domain.BasePermission.READ

@RestApi(name="Social | project connection services", description="Methods to manage a user connection record to a project")
class RestProjectConnectionController extends RestController {

    def cytomineService
    def secUserService
    def projectService
    def projectConnectionService
    def exportService
    def securityACLService

    @RestApiMethod(description = "Add a new user connection record to a project")
    def add() {
        try {
            responseSuccess(projectConnectionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    @RestApiMethod(description = "List the last user connections in a project", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def lastConnectionInProject() {
        Project project = projectService.read(params.project)
        responseSuccess(projectConnectionService.lastConnectionInProject(project, null));
    }

    @RestApiMethod(description = "Get the last user connections in a project for a given user")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
    ])
    def lastConnectionInProjectByUser() {
        Project project = projectService.read(params.project)
        Long userId = params.user ? Long.parseLong(params.user): null
        responseSuccess(projectConnectionService.lastConnectionInProject(project, userId));
    }

    @RestApiMethod(description = "Get user connections in a project", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
    ])
    def getConnectionByUserAndProject() {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer limit = params.limit != null ? params.getInt('limit') : -1
        def results = projectConnectionService.getConnectionByUserAndProject(user, project, limit, offset)
        // hack to avoid list to be cut. offset was already used in db request
        params.remove("offset")
        responseSuccess(results)
    }

    @RestApiMethod(description = "Get the number of connection by project")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="afterThan", type="date", paramType = RestApiParamType.QUERY, description = "The date when counting starts"),
            @RestApiParam(name="period", type="string", paramType = RestApiParamType.QUERY, description = "The period of counting"),
    ])
    def numberOfConnectionsByProject() {
        Project project = projectService.read(params.project)
        Long afterThan = params.long("afterThan");
        String period = params.period

        if(params.boolean('heatmap')) {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectOrderedByHourAndDays(project, afterThan, null))
        }else if(period) {
            responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, null, project))
        } else {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectAndUser(project, null))
        }
    }

    @RestApiMethod(description = "Get the number of connection by project and user")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id"),
            @RestApiParam(name="afterThan", type="date", paramType = RestApiParamType.QUERY, description = "The date when counting starts"),
            @RestApiParam(name="period", type="string", paramType = RestApiParamType.QUERY, description = "The period of connections (hour : by hours, day : by days, week : by weeks)"),
    ])
    def numberOfConnectionsByProjectAndUser() {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        Long afterThan = params.long("afterThan")
        Long beforeThan = params.long("beforeThan")
        String period = params.period

        if(params.boolean('heatmap')) {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectOrderedByHourAndDays(project, afterThan, user))
        }else if(period) {
            responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, beforeThan, project, user))
        } else {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectAndUser(project, user))
        }
    }

    @RestApiMethod(description = "Get the number of connections to all projects")
    @RestApiParams(params=[
            @RestApiParam(name="afterThan", type="date", paramType = RestApiParamType.QUERY, description = "The date when counting starts"),
            @RestApiParam(name="period", type="string", paramType = RestApiParamType.QUERY, description = "The period of connections (hour : by hours, day : by days, week : by weeks)"),
    ])

    def numberOfProjectConnections() {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        Long beforeThan = params.long("beforeThan")
        Long afterThan = params.long("afterThan")
        String period = params.get("period").toString()
        if(period){
            responseSuccess(projectConnectionService.numberOfProjectConnections(period, afterThan, beforeThan))
        } else {
            response([success: false, message: "Mandatory parameter 'period' not found. Parameters are : "+params], 400)
        }
    }

    @RestApiMethod(description="Get the number of connections in the specified project")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Only connections after this date will be counted (optional)"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Only connections before this date will be counted (optional)"),
    ])
    def countByProject() {
        Project project = projectService.read(params.project)
        securityACLService.check(project, READ)

        Long startDate = params.long("startDate")
        Long endDate = params.long("endDate")

        responseSuccess(projectConnectionService.countByProject(project, startDate, endDate))
    }

    @RestApiMethod(description="Get the average project connections on Cytomine.")
    @RestApiParams(params=[
            @RestApiParam(name="afterThan", type="long", paramType = RestApiParamType.QUERY, description = "Average on the project connection where created > the afterThan parameter. Optional, the beforeThan Date -1 year will be considered if none is given."),
            @RestApiParam(name="beforeThan", type="long", paramType = RestApiParamType.QUERY, description = "Average on the project connection where created < the beforeThan parameter. Optional, the current Date will be considered if none is given."),
            @RestApiParam(name="period", type="string", paramType = RestApiParamType.QUERY, description = "The period of connections (hour : by hours, day : by days, week : by weeks) (Mandatory)"),
    ])
    def averageOfProjectConnections() {
        Long afterThan = params.long("afterThan");
        Long beforeThan = params.long("beforeThan");
        String period = params.get("period").toString()
        Project project = params.project ? projectService.read(params.project) : null;
        SecUser user = params.user ? secUserService.read(params.user) : null;
        if(params.project){
            securityACLService.check(project,READ)
            if(params.user){
                securityACLService.checkIsSameUserOrAdminContainer(project, user, cytomineService.getCurrentUser())
            }
        } else{
            securityACLService.checkAdmin(cytomineService.getCurrentUser())
            if(params.user){
                securityACLService.checkIsSameUser(user, cytomineService.getCurrentUser())
            }
        }

        if(period){
            responseSuccess(projectConnectionService.averageOfProjectConnections(afterThan,beforeThan,period, project, user))
        } else {
            response([success: false, message: "Mandatory parameter 'period' not found. Parameters are : "+params], 400)
        }
    }

    @RestApiMethod(description="Get the project connections of one user into a project.")
    @RestApiParams(params=[
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id. Mandatory"),
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id. Mandatory"),
            @RestApiParam(name="offset", type="integer", paramType = RestApiParamType.QUERY, description = "An offset. Default value = 0"),
            @RestApiParam(name="limit", type="integer", paramType = RestApiParamType.QUERY, description = "Limit the project connections. Optionnal"),
    ])
    def userProjectConnectionHistory() {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)

        params.put("offset", params.start)
        params.put("max", params['length'])

        // for datatables, all is done after the data collect.
        Integer offset = 0
        Integer limit = -1

        def result = projectConnectionService.getConnectionByUserAndProject(user, project, limit, offset)

        if(params.export.equals("csv")) {
            response.contentType = grailsApplication.config.grails.mime.types[params.format]
            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
            String now = simpleFormat.format(new Date())
            response.setHeader("Content-disposition", "attachment; filename=user_${user.id}_connections_project_${project.id}_${now}.${params.export}")

            def exporterIdentifier = params.export;
            def exportResult = []
            List fields = ["date", "time", "numberOfViewedImages", "numberOfCreatedAnnotations", "os", "browser", "browserVersion"]
            Map labels = ["date": "Date", "time": "Duration (ms)", "numberOfViewedImages": "Number of viewed images", "numberOfCreatedAnnotations": "Number of created annotations", "os": "Operating System", "browser": "Browser", "browserVersion": "Browser Version"]
            result.each {
                def data = [:]
                data.date = it.created;
                data.time = it.time ?: 0;
                data.numberOfViewedImages = it.countViewedImages
                data.numberOfCreatedAnnotations = it.countCreatedAnnotations
                data.os = it.os
                data.browser = it.browser
                data.browserVersion = it.browserVersion
                exportResult << data
            }

            String title = "Connections of user ${user.id} into project ${project.id}"
            exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null, ["column.widths": [0.14, 0.14, 0.14, 0.14, 0.14, 0.14, 0.14], "title": title, "csv.encoding": "UTF-8", "separator": ";"])
        } else {
            responseSuccess(result)
        }
    }

    @RestApiMethod(description="Get the details of a project connection including the actions done during a project connection of one user into a project.")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The project id. Mandatory"),
            @RestApiParam(name="activity", type="long", paramType = RestApiParamType.PATH, description = "The project connection id. Mandatory"),
            //@RestApiParam(name="offset", type="integer", paramType = RestApiParamType.QUERY, description = "An offset. Default value = 0"),
            //@RestApiParam(name="limit", type="integer", paramType = RestApiParamType.QUERY, description = "Limit the project connections. Optionnal"),
    ])
    def getUserActivityDetails() {
        Long activity = Long.parseLong(params.id)
        params.put("offset", params.start)
        params.put("max", params['length'])

        def result = projectConnectionService.getUserActivityDetails(activity/*,limit, offset*/)

        if(params.export.equals("csv")) {
            PersistentProjectConnection connection = PersistentProjectConnection.read(activity);
            response.contentType = grailsApplication.config.grails.mime.types[params.format]
            response.setHeader("Content-disposition", "attachment; filename=image_consultations_of_user_${connection.user}_project_${connection.project}_${activity}.${params.export}")

            def exporterIdentifier = params.export;
            def exportResult = []
            List fields = ["date", "time", "imageId", "imageName", "imageThumb", "mode", "numberOfCreatedAnnotations"]
            Map labels = ["date": "Date", "time": "Duration (ms)", "imageId": "Id of image", "imageName": "Name", "imageThumb": "Thumb", "mode": "Consultation mode", "numberOfCreatedAnnotations": "Number of created annotations"]
            result.each {
                def data = [:]
                data.date = it.created;
                data.time = it.time ?: 0;
                data.imageId = it.image
                data.imageName = it.imageName
                data.imageThumb = it.imageThumb
                data.mode = it.mode
                data.numberOfCreatedAnnotations = it.countCreatedAnnotations
                exportResult << data
            }

            String title = "Consultations of images into project ${connection.project} by user ${connection.user}"
            exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null, ["column.widths": [0.14, 0.14, 0.14, 0.14, 0.14, 0.14, 0.14], "title": title, "csv.encoding": "UTF-8", "separator": ";"])
        } else {
            responseSuccess(result)
        }
    }

}
