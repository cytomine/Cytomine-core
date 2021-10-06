package be.cytomine.score


import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ

class ScoringDescriptionService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService

    def currentDomain() {
        return ScoringDescription
    }

    /**
     * List all description, Only for admin
     */
    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        return ScoringDescription.list()
    }

    def get(def domain, def user) {
        securityACLService.check(domain.container(),READ)
        ScoringDescription.findByDomainIdentAndDomainClassNameAndUser(domain.id,domain.class.name,user)
    }

    /**
     * Get a description thanks to its domain info (id and class)
     */
    def get(def domainIdent, def domainClassName, def user) {
        securityACLService.check(domainIdent,domainClassName,READ)
        ScoringDescription.findByDomainIdentAndDomainClassNameAndUser(domainIdent,domainClassName,user)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        if(json.domainClassName.contains("ImageInstance")){
            securityACLService.check(json.domainIdent,json.domainClassName,READ)
            securityACLService.checkisNotReadOnly(json.domainIdent,json.domainClassName)
        }  else {
            throw new WrongArgumentException("ScoringDescription is only supported for ImageInstance")
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
    def update(ScoringDescription description, def jsonNewData) {
        if (!description) {
            throw new ObjectNotFoundException("Description not found")
        }
        securityACLService.check(description.container(),READ)
        securityACLService.checkisNotReadOnly(description)
        securityACLService.checkFullOrRestrictedForOwner(description.domainIdent,description.domainClassName, "user")
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
    def delete(ScoringDescription domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        if (!domain) {
            throw new ObjectNotFoundException("Domain not found")
        }
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
        println json
        try {
            def domain = Class.forName(json.domainClassName, false, Thread.currentThread().contextClassLoader).read(json.domainIdent)
            def user = User.read(json.user)
            def description
            if (domain && user) {
                description = get(domain, user)
            }
            if(description) {
                return description
            } else {
                throw new ObjectNotFoundException("ScoringDescription not found for domain ${json.domainClassName} ${json.domainIdent} for user ${json.user}")
            }
        }catch(Exception e) {
            println e
        }
    }
}
