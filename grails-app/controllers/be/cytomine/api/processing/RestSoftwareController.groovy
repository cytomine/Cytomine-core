package be.cytomine.api.processing

import be.cytomine.Exception.CytomineException

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

import be.cytomine.api.RestController
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.middleware.AmqpQueue
import be.cytomine.processing.Job
import be.cytomine.processing.Software
import be.cytomine.processing.SoftwareUserRepository
import be.cytomine.project.Project
import grails.converters.JSON
import groovy.json.JsonBuilder
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import static org.springframework.security.acls.domain.BasePermission.WRITE

/**
 * Controller for software: application that can be launch (job)
 */
@RestApi(name = "Processing | software services", description = "Methods for managing software, application that can be launch (job)")
class RestSoftwareController extends RestController {

    def softwareService
    def securityACLService
    def amqpQueueService

    /**
     * List all software available in cytomine
     */
    @RestApiMethod(description="Get all software available in cytomine", listing = true)
    def list() {
        boolean executableOnly = params.boolean('executableOnly', false)
        String sort = params.sort ?: 'id'
        if (!['id', 'name', 'fullName', 'softwareVersion', 'created'].contains(sort)) sort = 'id'
        String order = params.order ?: 'desc'
        if (!['asc', 'desc'].contains(order)) order = 'desc'
        responseSuccess(softwareService.list(executableOnly, sort, order))
    }

    /**
     * List all software by project
     */
    @RestApiMethod(description="Get all software available in a project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = Project.read(params.long('id'))
        if(project) responseSuccess(softwareService.list(project))
        else responseNotFound("Project", params.id)
    }

    /**
     * List all software by software user repository
     */
    @RestApiMethod(description = "Get all the software for a software use repository", listing = true)
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software user repository id")
    ])
    def listBySoftwareUserRepository() {
        SoftwareUserRepository softwareUserRepository = SoftwareUserRepository.read(params.long('id'))
        if (softwareUserRepository) {
            responseSuccess(softwareService.list(softwareUserRepository))
        } else {
            responseNotFound("SoftwareUserRepository", params.id)
        }
    }

    /**
     * Get a specific software
     */
    @RestApiMethod(description="Get a specific software")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
    ])
    def show() {
        Software software = softwareService.read(params.long('id'))
        if (software) {
            responseSuccess(software)
        } else {
            responseNotFound("Software", params.id)
        }
    }

    /**
     * Add a new software to cytomine
     * We must add in other request: parameters, software-project link,...
     */
    @RestApiMethod(description="Add a new software to cytomine. We must add in other request: software parameters, software project link,...")
    def add() {
        add(softwareService, request.JSON)
    }

    /**
     * Update a software info
     */
    @RestApiMethod(description="Update a software.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
    ])
    def update() {
        update(softwareService, request.JSON)
    }

    /**
     * Delete software
     */
    @RestApiMethod(description="Delete a software.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
    ])
    def delete() {
        delete(softwareService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="Upload the sources of a software")
    def upload() {
        log.info "Upload software source"

        params.remove("controller")
        params.remove("files[]")
        params.remove("action")

        try{
            def result = softwareService.add(new JSONObject(params))

            Software software = result.object

            // check if software has not softwareUserRepository && software has not already been uploaded
            if(software.softwareUserRepository != null || software.sourcePath != null) {
                responseError(new ForbiddenException("This software has already source files. You cannot upload new sources"))
            }
            else {
                if(request instanceof AbstractMultipartHttpServletRequest) {
                    MultipartFile f = ((AbstractMultipartHttpServletRequest) request).getFile('files[]')

                    String filename = ((AbstractMultipartHttpServletRequest) request).getParameter('filename')
                    if(!filename) filename = f.originalFilename

                    String sourcePath = software.id+"/"+filename
                    File source = new File((grailsApplication.config.cytomine.software.path.softwareSources as String)+"/"+sourcePath)
                    if(!source.parentFile.exists()) source.parentFile.mkdir()
                    f.transferTo(source)

                    log.info "Upload $filename for domain software $software.name at ${source.path}"
                    log.info "File size = ${f.size}"

                    def json = software.encodeAsJSON()
                    json = new JSONObject(json)
                    json.sourcePath = sourcePath

                    softwareService.update(software, json)

                    // Sends a message on the communication queue to warn the software router a new queue has been created
                    def message = [requestType: "addSoftware",
                                   //exchange: amqpQueue.exchange,
                                   SoftwareId: software.id]

                    JsonBuilder jsonBuilder = new JsonBuilder()
                    jsonBuilder(message)
                    amqpQueueService.publishMessage(AmqpQueue.findByName("queueCommunication"), jsonBuilder.toString())

                    result.data.software.sourcePath = sourcePath

                    responseSuccess(result)
                } else {
                    responseError(new WrongArgumentException("No File attached"))
                }
            }
        } catch (CytomineException e) {
            log.error("add error:" + e.msg)
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    @RestApiMethod(description="Download a software source files.", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
    ])
    def download() {
        log.info "Download source files for software = " + params.getLong('id')
        Software software = softwareService.read(params.getLong('id'))
        if(software.sourcePath == null) {
            responseError(new ObjectNotFoundException("Source files not found"))
        } else {

            final URI u = new URI(software.sourcePath);

            if(u.isAbsolute()) {
                redirect (url : u)
            } else {
                String globalFilePath = grailsApplication.config.cytomine.software.path.softwareSources
                String softwaresPath = globalFilePath+"/"+software.sourcePath

                File f = new File(softwaresPath)

                if(!f.exists()) {
                    responseError(new ObjectNotFoundException("Source files not found"))
                } else {
                    responseFile(software.name, f)
                }
            }
        }
    }

    /**
     * List software
     * TODO:: could be improved with a single SQL request
     *
     */
    @RestApiMethod(description="For a software and a project, get the stats (number of job, succes,...)", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="idSoftware", type="long", paramType = RestApiParamType.PATH, description = "The software id"),
    ])
    @RestApiResponseObject(objectIdentifier = "[numberOfJob:x,numberOfNotLaunch:x,numberOfInQueue:x,numberOfRunning:x,numberOfSuccess:x,numberOfFailed:x,numberOfIndeterminate:x,numberOfWait:x]")
    def softwareInfoForProject() {
        Project project = Project.read(params.long('idProject'))
        Software software = Software.read(params.long('idSoftware'))
        if(!project) {
            responseNotFound("Project", params.idProject)
        } else if(!software) {
            responseNotFound("Software", params.idSoftware)
        } else {
            def result = [:]
            List<Job> jobs = Job.findAllByProjectAndSoftware(project,software)
            
            //Number of job for this software and this project
            result['numberOfJob'] = jobs.size()
            
            //Number of job by state
            result['numberOfNotLaunch'] = 0
            result['numberOfInQueue'] = 0
            result['numberOfRunning'] = 0
            result['numberOfSuccess'] = 0
            result['numberOfFailed'] = 0
            result['numberOfIndeterminate'] = 0
            result['numberOfWait'] = 0
            
            jobs.each { job ->
                if(job.status==Job.NOTLAUNCH) result['numberOfNotLaunch']++
                if(job.status==Job.INQUEUE) result['numberOfInQueue']++
                if(job.status==Job.RUNNING) result['numberOfRunning']++
                if(job.status==Job.SUCCESS) result['numberOfSuccess']++
                if(job.status==Job.FAILED) result['numberOfFailed']++
                if(job.status==Job.INDETERMINATE) result['numberOfIndeterminate']++
                if(job.status==Job.WAIT) result['numberOfWait']++
            }

            responseSuccess(result)
        }
    }
}
