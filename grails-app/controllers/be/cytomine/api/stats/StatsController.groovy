package be.cytomine.api.stats

import be.cytomine.Exception.CytomineMethodNotYetImplementedException
import be.cytomine.Exception.WrongArgumentException

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
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.Ontology
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.Software
import be.cytomine.project.Project
import be.cytomine.security.User
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import static org.springframework.security.acls.domain.BasePermission.READ

class StatsController extends RestController {

    def cytomineService
    def securityACLService
    def termService
    def jobService
    def secUserService
    def projectConnectionService
    def statsService

    def allGlobalStats = {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())

        def result = [:];
        result["users"] = statsService.total(User).total
        result["projects"] = statsService.total(Project).total
        result["images"] = statsService.total(ImageInstance).total
        result["userAnnotations"] = statsService.total(UserAnnotation).total
        result["jobAnnotations"] = statsService.total(AlgoAnnotation).total
        result["terms"] = statsService.total(Term).total
        result["ontologies"] = statsService.total(Ontology).total
        result["softwares"] = statsService.total(Software).total
        result["jobs"] = statsService.total(Job).total
        responseSuccess(result)

    }

    /**
     * Compute for each user, the number of annotation of each term
     */
    def statUserAnnotations = {

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)
        responseSuccess(statsService.statUserAnnotations(project))
    }

    @RestApiMethod(description="Get the number of annotation for each user")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations before this date will not be counted [Optional]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations after this date will not be counted [Optional]")
    ])
    def statUser() {

        //Get project
        Project project = Project.read(params.id)
        if (!project) {
            responseNotFound("Project", params.id)
            return
        }

        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null

        securityACLService.check(project,READ)
        responseSuccess(statsService.statUser(project, startDate, endDate))
    }

    @RestApiMethod(description="Get the number of annotation for each term")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations before this date will not be counted [Optional]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations after this date will not be counted [Optional]"),
            @RestApiParam(name="leafsOnly", type="boolean", paramType = RestApiParamType.QUERY, description = "Include only the leafs terms in result [Optional - default=true]")
    ])
    def statTerm() {

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean leafsOnly = params.leafsOnly != null ? params.getBoolean("leafsOnly") : true

        securityACLService.check(project,READ)
        responseSuccess(statsService.statTerm(project, startDate, endDate, leafsOnly))
    }

    @RestApiMethod(description="Compute for each term the number of samples having at least one annotation associated with this term")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations before this date will not be counted [Optional]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations after this date will not be counted [Optional]"),
    ])
    def statTermSlide() {

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }
        securityACLService.check(project,READ)

        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        responseSuccess(statsService.statTermSlide(project, startDate, endDate))
    }

    @RestApiMethod(description="Compute for each user the number of samples in which (s)he created annotation")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations before this date will not be counted [Optional]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Annotations after this date will not be counted [Optional]"),
    ])
    def statUserSlide() {
        Project project = Project.read(params.id)
        if (!project) {
            responseNotFound("Project", params.id)
            return
        }
        securityACLService.check(project,READ)

        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        responseSuccess(statsService.statUserSlide(project, startDate, endDate))
    }

    /**
     * Compute user annotation number evolution over the time for a project
     */
    @RestApiMethod(description="Get the number of user annotations in a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider [Optional - default=project creation]"),
            @RestApiParam(name="term", type="long", paramType = RestApiParamType.QUERY, description = "If specified, only annotations associated with this term will be considered [Optional]"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider [Optional]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange) [Optional]"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of annotations will be accumulated (period n+1 count = period n count + nb of connections during period n+1) [Optional - default=true]"),
            @RestApiParam(name="reverseOrder", type="boolean", paramType = RestApiParamType.QUERY, description = "If true the periods will be returned in reverse order (first item of array = last period) [Optional - default=true]")
    ])
    def statAnnotationEvolution() {
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        boolean accumulate = params.accumulate != null ? params.getBoolean('accumulate') : true
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean reverseOrder = params.reverseOrder != null ? params.getBoolean("reverseOrder") : true
        Term term = Term.read(params.getLong("term"))
        if (params.term && !term) {
            responseNotFound("Term", params.term)
            return
        }

        responseSuccess(statsService.statAnnotationEvolution(project, term, daysRange, startDate, endDate, reverseOrder, accumulate))
    }

    @RestApiMethod(description="Get the number of algo annotations in a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider [Optional - default=1]"),
            @RestApiParam(name="term", type="long", paramType = RestApiParamType.QUERY, description = "If specified, only annotations associated with this term will be considered [Optional]"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider [Optional - default=project creation]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange) [Optional]"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of annotations will be accumulated (period n+1 count = period n count + nb of connections during period n+1) [Optional - default=true]"),
            @RestApiParam(name="reverseOrder", type="boolean", paramType = RestApiParamType.QUERY, description = "If true the periods will be returned in reverse order (first item of array = last period) [Optional - default=true]")
    ])
    def statAlgoAnnotationEvolution() {
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        boolean accumulate = params.accumulate != null ? params.getBoolean('accumulate') : true
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean reverseOrder = params.reverseOrder != null ? params.getBoolean("reverseOrder") : true
        Term term = Term.read(params.getLong("term"))
        if (params.term && !term) {
            responseNotFound("Term", params.term)
            return
        }

        responseSuccess(statsService.statAlgoAnnotationEvolution(project, term, daysRange, startDate, endDate, reverseOrder, accumulate))
    }

    @RestApiMethod(description="Get the number of reviewed annotations in a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider [Optional - default=1]"),
            @RestApiParam(name="term", type="long", paramType = RestApiParamType.QUERY, description = "If specified, only annotations associated with this term will be considered [Optional]"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider [Optional - default=project creation]"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange) [Optional]"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of annotations will be accumulated (period n+1 count = period n count + nb of connections during period n+1) [Optional - default=true]"),
            @RestApiParam(name="reverseOrder", type="boolean", paramType = RestApiParamType.QUERY, description = "If true the periods will be returned in reverse order (first item of array = last period) [Optional - default=true]")
    ])
    def statReviewedAnnotationEvolution() {
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        boolean accumulate = params.accumulate != null ? params.getBoolean('accumulate') : true
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean reverseOrder = params.reverseOrder != null ? params.getBoolean("reverseOrder") : true
        Term term = Term.read(params.getLong("term"))
        if (params.term && !term) {
            responseNotFound("Term", params.term)
            return
        }
        log.info(params.term)
        log.info(term)

        responseSuccess(statsService.statReviewedAnnotationEvolution(project, term, daysRange, startDate, endDate, reverseOrder, accumulate))
    }

    @RestApiMethod(description="Get the total of annotations with a term by project.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The term id")
    ])
    def statAnnotationTermedByProject() {
        Term term = Term.read(params.id)
        if (!term) {
            responseNotFound("Term", params.id)
            return
        }
        securityACLService.check(term.container(),READ)
        responseSuccess(statsService.statAnnotationTermedByProject(term))
    }

    @RestApiMethod(description="Get the total of user connection into a project by project.")
    def totalNumberOfConnectionsByProject(){
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        responseSuccess(projectConnectionService.totalNumberOfConnectionsByProject());
    }

    @RestApiMethod(description="Get the total of the domains made on this instance.")
    @RestApiParams(params=[
            @RestApiParam(name="domain", type="string", paramType = RestApiParamType.PATH, description = "The domain name")
    ])
    def totalDomains() {

        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        def clazz = grailsApplication.domainClasses.find { it.clazz.simpleName.toLowerCase() == params.domain.toLowerCase() }
        if(!clazz){
            throw new WrongArgumentException("This domain doesn't exist!")
        }
        responseSuccess(statsService.total(clazz.clazz));
    }

    @RestApiMethod(description="Get information about the current activity of Cytomine.")
    def statsOfCurrentActions() {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())

        def result = [:];
        result["users"] = statsService.numberOfCurrentUsers().total
        result["projects"] = statsService.numberOfActiveProjects().total
        result["mostActiveProject"] = statsService.mostActiveProjects()
        responseSuccess(result)
    }

    def statUsedStorage(){
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        responseSuccess(statsService.statUsedStorage())
    }

    @RestApiMethod(description="Get the number of connections to a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange)"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of connections will be accumulated (period n+1 count = period n count + nb of connections during period n+1)"),
    ])
    def statConnectionsEvolution() {
        Project project = Project.read(params.project)
        securityACLService.check(project, READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean accumulate = params.getBoolean("accumulate")

        responseSuccess(statsService.statConnectionsEvolution(project, daysRange, startDate, endDate, accumulate))
    }

    @RestApiMethod(description="Get the number of image consultations in a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange)"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of image consultations will be accumulated (period n+1 count = period n count + nb of consultations during period n+1)"),
    ])
    def statImageConsultationsEvolution() {
        Project project = Project.read(params.project)
        securityACLService.check(project, READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean accumulate = params.getBoolean("accumulate")

        responseSuccess(statsService.statImageConsultationsEvolution(project, daysRange, startDate, endDate, accumulate))
    }

    @RestApiMethod(description="Get the number of annotation actions in a project over time")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="daysRange", type="int", paramType = RestApiParamType.QUERY, description = "The duration of the intervals to consider"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "The start date of the first interval to consider"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "The end date to consider (therefore, the last interval may be shorter than specified in daysRange)"),
            @RestApiParam(name="accumulate", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, the number of actions will be accumulated (period n+1 count = period n count + nb of actions during period n+1)"),
            @RestApiParam(name="type", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) If specified, only annotation action of this type will be taken into account"),
    ])
    def statAnnotationActionsEvolution() {
        Project project = Project.read(params.project)
        securityACLService.check(project, READ)
        securityACLService.checkAdmin(cytomineService.currentUser)

        int daysRange = params.daysRange != null ? params.getInt('daysRange') : 1
        Date startDate = params.startDate ? new Date(params.long("startDate")) : null
        Date endDate = params.endDate ? new Date(params.long("endDate")) : null
        boolean accumulate = params.getBoolean("accumulate")

        responseSuccess(statsService.statAnnotationActionsEvolution(project, daysRange, startDate, endDate, accumulate, params.type))
    }

}
