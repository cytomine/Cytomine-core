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
import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.Exception.ForbiddenException
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class TagDomainAssociationService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService

    def currentDomain() {
        return TagDomainAssociation
    }

    def read(Long id) {
        def association = TagDomainAssociation.read(id)
        if (association && !association.domainClassName.contains("AbstractImage")) {
            securityACLService.check(association.container(),READ)
        }
        association
    }

    // cannot paginate because I don't know the total of the list before checking permissions !
    def list(def searchParameters = [], Long max = 0, Long offset = 0) {
        def validSearchParameters = getDomainAssociatedSearchParameters(TagDomainAssociation, searchParameters)

        def result = criteriaRequestWithPagination(TagDomainAssociation, 0, 0, {}, validSearchParameters, "domainClassName", "desc")
        List<TagDomainAssociation> associations = result.data
        result = [data:[], total : 0]

        def cache = []
        for(TagDomainAssociation association : associations) {

            try {
                def cached = cache.find{it.id == association.domainIdent && it.clazz == association.domainClassName}
                if(cached) {
                    if(cached.granted) result.data << association
                } else {
                    def current = [id : association.domainIdent, clazz : association.domainClassName, granted : false]
                    cache << current
                    if(!association.domainClassName.contains("AbstractImage")) {
                        securityACLService.check(association.container(),READ)
                    }
                    current.granted = true
                    result.data << association
                }

            } catch (ForbiddenException e){}
        }
        result.total = result.data.size()

        // as there is no efficient backend pagination, I do it at the end.
        if(max > 0) {
            if (offset >= result.data.size()) {
                result.data = []
            } else {
                def maxForCollection = Math.min(result.data.size() - offset, max)
                result.data = result.data.subList((int)offset,(int)offset + (int)maxForCollection)
            }
        }

        max = (max > 0) ? max : Integer.MAX_VALUE
        result.offset = offset
        result.perPage = Math.min(max, result.total)
        result.totalPages = Math.ceil(result.total / max)

        return result




    }

    /**
     * List all tags
     */
    def listByTag(Tag tag, Long max = 0, Long offset = 0) {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        return list([[operator : "in", field : "tag", values:tag.id]], max, offset)
    }

    /**
     * List all tags
     */
    def listByDomain(CytomineDomain domain, Long max = 0, Long offset = 0) {
        return list([[operator : "equals", field : "domainClassName", values:domain.getClass().name], [operator : "equals", field : "domainIdent", values:domain.id]], max, offset)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        def domainClass = json.domainClassName
        CytomineDomain domain

        if(domainClass.contains("AnnotationDomain")) {
            domain = AnnotationDomain.getAnnotationDomain(json.domainIdent)
        } else {
            domain = Class.forName(domainClass, false, Thread.currentThread().contextClassLoader).read(JSONUtils.getJSONAttrLong(json,'domainIdent',0))
        }

        if (domain != null && !domain.class.name.contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ)
            if (domain.hasProperty('user') && domain.user) {
                securityACLService.checkFullOrRestrictedForOwner(domain, domain.user)
            } else if (domainClass.contains("Project")){
                securityACLService.check(domain, WRITE)
            } else {
                securityACLService.checkFullOrRestrictedForOwner(domain)
            }
        }

        SecUser currentUser = cytomineService.getCurrentUser()
        Command command = new AddCommand(user: currentUser)
        return executeCommand(command,null,json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(TagDomainAssociation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()

        if(!domain.domainClassName.contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ)
            if (domain.retrieveCytomineDomain().hasProperty('user') && domain.retrieveCytomineDomain().user) {
                securityACLService.checkFullOrRestrictedForOwner(domain, domain.retrieveCytomineDomain().user)
            } else {
                securityACLService.checkFullOrRestrictedForOwner(domain)
            }
        }

        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(TagDomainAssociation domain) {
        return [domain.tag.name, domain.domainIdent, domain.domainClassName]
    }
}
