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
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import grails.converters.JSON
import grails.transaction.Transactional

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class AnnotationTermService extends ModelService {

    static transactional = true
    def cytomineService
    def transactionService
    def commandService
    def securityACLService

    def currentDomain() {
        return AnnotationTerm
    }

    def list(UserAnnotation userAnnotation) {
        securityACLService.check(userAnnotation.container(),READ)
        AnnotationTerm.findAllByUserAnnotationAndDeletedIsNull(userAnnotation)
    }

    def list(Project project) {
        return AnnotationTerm.withCriteria{
            createAlias('userAnnotation', 'ua')
            eq('ua.project', project)
            isNull("deleted")
        }
    }

    def listNotUser(UserAnnotation userAnnotation, User user) {
        securityACLService.check(userAnnotation.container(),READ)
        AnnotationTerm.findAllByUserAnnotationAndUserNotEqualAndDeletedIsNull(userAnnotation, user)
    }

    def read(AnnotationDomain annotation, Term term, SecUser user) {
        securityACLService.check(annotation.container(),READ)
        AnnotationTerm result
        if (user) {
            result = AnnotationTerm.findWhere('userAnnotation.id': annotation.id, 'term': term, 'user': user)
        } else {
            result = AnnotationTerm.findWhere('userAnnotation.id': annotation.id, 'term': term)
        }
        if(result) checkDeleted(result)
        result
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        SecUser creator = SecUser.read(json.user)
        if (!creator) {
            json.user = currentUser.id
            creator = currentUser
        }

        Term term = Term.read(json.term)
        UserAnnotation ua = UserAnnotation.read(json.userannotation)
        AnnotationTerm alreadyExist = AnnotationTerm.findByTermAndUserAnnotationAndUser(term, ua, creator)
        if (alreadyExist && alreadyExist.checkDeleted()) {
            //was previously deleted, restore it
            def jsonNewData = JSON.parse(alreadyExist.encodeAsJSON())
            jsonNewData.deleted = null
            Command c = new EditCommand(user: currentUser)
            return executeCommand(c, alreadyExist, jsonNewData)
        }
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AnnotationTerm domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.userAnnotation.id, UserAnnotation, "container", READ)
        securityACLService.checkFullOrRestrictedForOwner(domain, domain.userAnnotation.user)
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }


    def addAnnotationTerm(def idUserAnnotation, def idTerm, def idExpectedTerm, def idUser, SecUser currentUser, Transaction transaction) {
        Term term = Term.read(idTerm)
        UserAnnotation ua = UserAnnotation.read(idUserAnnotation)
        SecUser creator = SecUser.read(idUser)
        AnnotationTerm alreadyExist = AnnotationTerm.findByTermAndUserAnnotationAndUser(term, ua, creator)
        if (alreadyExist && alreadyExist.checkDeleted()) {
            //was previously deleted, restore it
            def jsonNewData = JSON.parse(alreadyExist.encodeAsJSON())
            jsonNewData.deleted = null
            Command c = new EditCommand(user: currentUser, transaction: transaction)
            return executeCommand(c, alreadyExist, jsonNewData)
        }
        else {
            def json = JSON.parse("{userannotation: $idUserAnnotation, term: $idTerm, expectedTerm: $idExpectedTerm, user: $idUser}")
            return executeCommand(new AddCommand(user: currentUser, transaction: transaction), null,json)
        }
    }

    /**
     * Add annotation-term for an annotation and delete all annotation-term that where already map with this annotation by this user
     */
    def addWithDeletingOldTerm(def idAnnotation, def idterm, Boolean fromAllUser = false) {
        SecUser currentUser = cytomineService.getCurrentUser()
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(idAnnotation)
        if (!annotation) throw new ObjectNotFoundException("Annotation $idAnnotation not found")
        //Start transaction
        if(annotation instanceof UserAnnotation) {
            Transaction transaction = transactionService.start()

            //Delete all annotation term
            def annotationTerm
            if(!fromAllUser) {

                annotationTerm = AnnotationTerm.findAllByUserAnnotationAndUser(annotation, currentUser)
            } else {
                annotationTerm = AnnotationTerm.findAllByUserAnnotation(annotation)
            }
            log.info "Delete old annotationTerm= " + annotationTerm.size()
            annotationTerm.each { annotterm ->
                log.info "unlink annotterm:" + annotterm.id
                this.delete(annotterm,transaction,null,true)
            }
            //Add annotation term
            return addAnnotationTerm(idAnnotation, idterm, null, currentUser.id, currentUser, transaction)
        } else if(annotation instanceof ReviewedAnnotation) {
            Transaction transaction = transactionService.start()
            ReviewedAnnotation reviewed = (ReviewedAnnotation)annotation
            reviewed.terms.clear()
            reviewed.terms.add(Term.read(idterm))
            reviewed.save(flush:true)
            return [status:200]

        }

    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.userAnnotation.id, domain.term.name, domain.user?.username]
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    def retrieve(Map json) {
        UserAnnotation annotation = UserAnnotation.get(json.userannotation)
        Term term = Term.get(json.term)
        User user = User.get(json.user)
        AnnotationTerm relation = AnnotationTerm.findWhere(userAnnotation: annotation, 'term': term, 'user': user)
        if (!relation) {
            throw new ObjectNotFoundException("Annotation term not found ($annotation,$term,$user)")
        }
        checkDeleted(relation)
        return relation
    }
}
