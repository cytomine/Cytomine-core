package be.cytomine.ontology

import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import static org.springframework.security.acls.domain.BasePermission.READ

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

class TrackService extends ModelService {

    static transactional = true

    def securityACLService
    def imageInstanceService

    def currentDomain() {
        Track
    }

    def read(def id) {
        Track track = Track.read(id)
        if (track) {
            securityACLService.check(track, READ)
            checkDeleted(track)
        }
        track
    }

    def list(ImageInstance image) {
        securityACLService.check(image, READ)
        Track.findAllByImageAndDeletedIsNull(image)
    }

    def list(Project project) {
        securityACLService.check(project, READ)
        Track.findAllByProjectAndDeletedIsNull(project)
    }

    def count(ImageInstance image) {
        securityACLService.check(image, READ)
        return Track.countByImageAndDeletedIsNull(image)
    }

    def countByProject(Project project, Date startDate, Date endDate) {
        securityACLService.check(project, READ)
        String request = "SELECT COUNT(*) FROM Track WHERE project = $project.id " +
                (startDate ? "AND created > '$startDate' " : "") +
                (endDate ? "AND created < '$endDate' " : "")
        def result = Track.executeQuery(request)
        return result[0]
    }

    def add(def json) {
        ImageInstance image = imageInstanceService.read(json.image)
        Project project = image?.project

        if (!project) {
            throw new WrongArgumentException("Track does not have a valid project.")
        }

        json.image = image.id
        json.project = project.id

        securityACLService.check(project, READ)
        securityACLService.checkFullOrRestrictedForOwner(image, image.user)

        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(Track track, def json) {
        securityACLService.check(track, READ)
        securityACLService.checkFullOrRestrictedForOwner(track.image, track.image.user)
        SecUser currentUser = cytomineService.getCurrentUser()

        ImageInstance image = imageInstanceService.read(json.image)
        Project project = image?.project
        json.project = project.id

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, track, json)
    }

    def delete(Track track, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.check(track, READ)
        securityACLService.checkFullOrRestrictedForOwner(track.image, track.image.user)

        SecUser currentUser = cytomineService.getCurrentUser()
        def jsonNewData = JSON.parse(track.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        Command c = new EditCommand(user: currentUser)
        c.delete = true
        return executeCommand(c,track,jsonNewData)
    }

    def annotationTrackService
    def deleteDependentAnnotationTrack(Track track, Transaction transaction, Task task = null) {
        AnnotationTrack.findAllByTrack(track).each {
            annotationTrackService.delete(it, transaction, task)
        }
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }
}
