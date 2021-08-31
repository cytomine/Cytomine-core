package be.cytomine.ontology

import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
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

class AnnotationTrackService extends ModelService {

    static transactional = true

    def securityACLService
    def imageInstanceService

    def currentDomain() {
        AnnotationTrack
    }

    def read(def id) {
        AnnotationTrack annotationTrack = AnnotationTrack.read(id)
        if (annotationTrack) {
            securityACLService.check(annotationTrack, READ)
        }
        annotationTrack
    }

    def read(AnnotationDomain annotation, Track track) {
        AnnotationTrack annotationTrack = AnnotationTrack.findByAnnotationIdentAndTrack(annotation.id, track)
        if (annotationTrack) {
            securityACLService.check(annotationTrack, READ)
        }
        annotationTrack
    }

    def list(Track track) {
        securityACLService.check(track, READ)
        AnnotationTrack.findAllByTrack(track)
    }

    def list(AnnotationDomain annotation) {
        securityACLService.check(annotation, READ)
        AnnotationTrack.findAllByAnnotationIdent(annotation.id)
    }

    def add(def json) {
        AnnotationDomain annotation
        try {
            annotation= AnnotationDomain.getAnnotationDomain(json.annotationIdent, json.annotationClassName)
        } catch(ObjectNotFoundException e) {
            throw new WrongArgumentException("Annotation does not have a valid project.")
        }
        securityACLService.check(annotation .project, READ)
        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user)
        json.slice = annotation.slice.id
        json.annotationIdent = annotation.id
        json.annotationClassName = annotation.getClass().getName()

        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def addAnnotationTrack(def annotationClassName, def annotationIdent, def idTrack, def idSlice, SecUser currentUser, Transaction transaction) {
        def json = JSON.parse("{annotationClassName: $annotationClassName, annotationIdent: $annotationIdent, track: $idTrack, slice: $idSlice}")
        return executeCommand(new AddCommand(user: currentUser, transaction: transaction), null,json)
    }

    def delete(AnnotationTrack domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(domain.annotationIdent, domain.annotationClassName)
        securityACLService.check(annotation .project, READ)
        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, domain, null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.annotationIdent, domain.track]
    }

    def retrieve(Map json) {
        CytomineDomain domain = null
        if(json.id && !json.id.toString().equals("null")) {
            domain = currentDomain().get(json.id)
        }
        else if (json.annotationIdent && json.track) {
            def track = Track.get(json.track)
            domain = AnnotationTrack.findByAnnotationIdentAndTrack(json.annotationIdent, track)
        }

        if (!domain) {
            throw new ObjectNotFoundException("${currentDomain().class} " + json.id + " not found")
        }
        def container = domain.container()
        if (container) {
            //we only check security if container is defined
            securityACLService.check(container,READ)
        }
        return domain
    }
}
