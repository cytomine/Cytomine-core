package be.cytomine.api.processing

import be.cytomine.api.RestController
import be.cytomine.processing.SoftwareProject
import be.cytomine.project.Project
import grails.converters.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApi

import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for software project link
 * A software may be used by some project
 */
@RestApi(name = "software project services", description = "Methods for managing software, application that can be launch (job)")
class RestSoftwareProjectController extends RestController{

    def softwareProjectService
    def projectService

    /**
     * List all software project links
     */
    @RestApiMethod(description="List all software project links", listing = true)
    def list() {
        responseSuccess(softwareProjectService.list())
    }

    /**
     * List all software by project
     */
    @RestApiMethod(description="List all software project links by project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = projectService.read(params.long('id'))
        if (project) {
            responseSuccess(softwareProjectService.list(project))
        } else {
            responseNotFound("Project", params.id)
        }
    }

    /**
     * Get a software project link
     */
    @RestApiMethod(description="Get a software project link")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software project id")
    ])
    def show() {
        SoftwareProject parameter = softwareProjectService.read(params.long('id'))
        if (parameter) responseSuccess(parameter)
        else responseNotFound("SoftwareProject", params.id)
    }

    /**
     * Add an existing software to a project
     */
    @RestApiMethod(description="Add an existing software to a project")
    def add () {
        add(softwareProjectService, request.JSON)
    }

    /**
     * Delete the software for the project
     */
    @RestApiMethod(description="Remove the software from the project")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software project id")
    ])
    def delete() {
        delete(softwareProjectService, JSON.parse("{id : $params.id}"),null)
    }
}
