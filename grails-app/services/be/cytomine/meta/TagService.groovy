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
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

class TagService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService
    def tagDomainAssociationService

    def currentDomain() {
        return Tag
    }

    /**
     * List all tags
     */
    def list() {
        return Tag.list()
    }

    Tag read(def id) {
        Tag.read(id)
    }

    Tag getByName(String name) {
        Tag.findByName(name)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()

        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Tag tag, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()

        //if not admin then check if there is no association
        if(TagDomainAssociation.countByTag(tag) > 0) securityACLService.checkAdmin(currentUser)


        return executeCommand(new EditCommand(user: currentUser),tag, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Tag domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }


    def deleteDependentTagDomainAssociation(Tag tag, Transaction transaction, Task task = null) {
        log.info "deleteDependentTagDomainAssociation ${TagDomainAssociation.findAllByTag(tag).size()}"
        TagDomainAssociation.findAllByTag(tag).each {
            tagDomainAssociationService.delete(it,transaction,null, false)
        }
    }
}
