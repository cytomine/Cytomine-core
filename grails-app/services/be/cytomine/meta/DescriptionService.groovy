package be.cytomine.meta

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
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.AnnotationDomain
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ

class DescriptionService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService

    def currentDomain() {
        return Description
    }

    /**
     * List all description, Only for admin
     */
    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        return Description.list()
    }

    def get(def domain) {
        securityACLService.check(domain.container(),READ)
        Description.findByDomainIdentAndDomainClassName(domain.id,domain.class.name)
    }

    /**
     * Get a description thanks to its domain info (id and class)
     */
    def get(def domainIdent, def domainClassName) {
        if(domainClassName.contains("AnnotationDomain")){
            AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(domainIdent)
            domainClassName = annotation.getClass().name
        }
        securityACLService.check(domainIdent,domainClassName,READ)
        Description.findByDomainIdentAndDomainClassName(domainIdent,domainClassName)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        if(json.domainClassName.equals("be.cytomine.project.Project")){
            securityACLService.check(json.domainIdent,json.domainClassName,READ)
            securityACLService.checkisNotReadOnly(json.domainIdent,json.domainClassName)
        } else if(json.domainClassName.contains("AnnotationDomain")){
            AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(json.domainIdent)
            json.domainClassName = annotation.getClass().name
            securityACLService.check(json.domainIdent,annotation.getClass().name,READ)
            securityACLService.checkFullOrRestrictedForOwner(json.domainIdent,annotation.getClass().name, "user")
        } else if (!json.domainClassName.contains("AbstractImage")){
            securityACLService.check(json.domainIdent,json.domainClassName,READ)
            securityACLService.checkFullOrRestrictedForOwner(json.domainIdent,json.domainClassName, "user")
        }
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Description description, def jsonNewData) {
        securityACLService.check(description.container(),READ)
        if(description.domainClassName.equals("be.cytomine.project.Project")){
            securityACLService.checkisNotReadOnly(description)
        } else {
            securityACLService.checkFullOrRestrictedForOwner(description.domainIdent,description.domainClassName, "user")
        }
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), description,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Description domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),READ)
        if (domain.hasProperty('user') && domain.user) {
            securityACLService.checkFullOrRestrictedForOwner(domain,domain.user)
        } else {
            securityACLService.checkisNotReadOnly(domain)
        }
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.domainIdent, domain.domainClassName]
    }


    def retrieve(Map json) {
        try {
            def domain = Class.forName(json.domainClassName, false, Thread.currentThread().contextClassLoader).read(json.domainIdent)
            def description
            if (domain) {
                description = get(domain)
            }
            if(description) {
                return description
            } else {
                throw new ObjectNotFoundException("Description not found for domain ${json.domainClassName} ${json.domainIdent}")
            }
        }catch(Exception e) {
            throw new ObjectNotFoundException("Description not found for domain ${json.domainClassName} ${json.domainIdent}")
        }
    }
}
