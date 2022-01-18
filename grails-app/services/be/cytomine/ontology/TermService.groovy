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

import be.cytomine.Exception.ConstraintException
import be.cytomine.command.*
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import grails.transaction.Transactional
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.*

@Transactional
class TermService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def annotationTermService
    def algoAnnotationTermService
    def relationTermService
    def modelService
    def securityACLService

    def dataSource

    protected def secUserService

    def initialize() { this.secUserService = grailsApplication.mainContext.secUserService }

    def currentDomain() {
        return Term
    }

    /**
     * List all term, Only for admin
     */
    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        return Term.findAllByDeletedIsNull()
    }

    Term read(def id) {
        def term = Term.read(id)
        if (term) {
            securityACLService.check(term.container(),READ)
            checkDeleted(term)
        }
        term
    }

    Term get(def id) {
        def term = Term.get(id)
        if (term) {
            securityACLService.check(term.container(),READ)
        }
        term
    }

    def list(Ontology ontology) {
        securityACLService.check(ontology.container(),READ)
        return ontology?.leafTerms()
    }

    def list(Project project) {
        securityACLService.check(project,READ)
        return project?.ontology?.terms()
    }

    def list(UserAnnotation annotation, User user) {
        securityACLService.check(annotation.container(),READ)
        return AnnotationTerm.findAllByUserAndUserAnnotationAndDeletedIsNull(user, annotation).collect {it.term.id}
    }

    /**
     * Get all term id for a project
     */
    public List<Long> getAllTermId(Project project) {
        securityACLService.check(project.container(),READ)

        if(!project.ontology) {
            return []
        }

        //better perf with sql request
        String request = "SELECT t.id FROM term t WHERE t.ontology_id="+project.ontology.id
        def data = []
        def sql = new Sql(dataSource)
         sql.eachRow(request) {
            data << it[0]
        }
        try {
            sql.close()
        }catch (Exception e) {}
        return data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        securityACLService.check(json.ontology, Ontology,WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Term term, def jsonNewData) {
        securityACLService.check(term.container(),WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), term,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Term domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time

        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),DELETE)
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    def deleteDependentAlgoAnnotationTerm(Term term, Transaction transaction, Task task = null) {
        def nbreAlgoAnnotation = AlgoAnnotationTerm.createCriteria().count {
            isNull("deleted")
            or {
                eq("term", term)
                eq("expectedTerm", term)
            }
        }

        if (nbreAlgoAnnotation>0) {
            throw new ConstraintException("Term is still linked with ${nbreAlgoAnnotation} annotations created by job. Cannot delete term!")
        }
    }

    def deleteDependentAnnotationTerm(Term term, Transaction transaction, Task task = null) {
        def nbreUserAnnotation = AnnotationTerm.countByTermAndDeletedIsNull(term)

        if (nbreUserAnnotation>0) {
            throw new ConstraintException("Term is still linked with ${nbreUserAnnotation} annotations created by user. Cannot delete term!")
        }
    }

    def deleteDependentRelationTerm(Term term, Transaction transaction, Task task = null) {
        RelationTerm.findAllByTerm1(term).each {
            relationTermService.delete(it,transaction,null,false)
        }
        RelationTerm.findAllByTerm2(term).each {
            relationTermService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentHasManyReviewedAnnotation(Term term, Transaction transaction, Task task = null) {
        def criteria = ReviewedAnnotation.createCriteria()
        def results = criteria.list {
            terms {
             inList("id", term.id)
            }
        }

        if(!results.isEmpty()) {
            throw new ConstraintException("Term is linked with ${results.size()} validate annotations. Cannot delete term!")
        }
     }

    def deleteDependentHasManyAnnotationFilter(Term term, Transaction transaction, Task task = null) {
        def criteria = AnnotationFilter.createCriteria()
        def results = criteria.list {
          users {
             inList("id", term.id)
          }
        }
        results.each {
            it.removeFromTerms(term)
            it.save()
        }
     }

}
