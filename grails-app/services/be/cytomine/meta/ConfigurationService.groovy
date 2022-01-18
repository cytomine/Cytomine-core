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

import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.meta.Configuration
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.transaction.Transactional

@Transactional
class ConfigurationService extends ModelService {

    static transactional = true
    def cytomineService
    def transactionService
    def dataSource
    def securityACLService
    def currentRoleServiceProxy
    def secRoleService

    def currentDomain() {
        return Configuration;
    }

    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        if(currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)) {
            return Configuration.list()
        } else if(currentRoleServiceProxy.isUser(cytomineService.currentUser)) {
            return Configuration.findAllByReadingRoleOrReadingRole(Configuration.Role.ALL, Configuration.Role.USER)
        } else {
            return Configuration.findAllByReadingRole(Configuration.Role.ALL)
        }
    }

    def readByKey(String key) {
        securityACLService.checkGuest(cytomineService.currentUser)
        Configuration config = Configuration.findByKey(key)
        if(!config) throw new ObjectNotFoundException("Configuration with key $key not found")
        checkPermission(config)
        return config
    }

    def add(def json) {
        securityACLService.checkAdmin(cytomineService.currentUser)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command command = new AddCommand(user: currentUser)
        return executeCommand(command,null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Configuration ap, def jsonNewData) {
        securityACLService.checkAdmin(cytomineService.currentUser)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command command = new EditCommand(user: currentUser)
        return executeCommand(command,ap,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Configuration domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.checkAdmin(cytomineService.currentUser)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    private void checkPermission(Configuration config){
        if(config.readingRole.equals(Configuration.Role.ALL)) return;
        if(currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)) return;
        if(currentRoleServiceProxy.isUser(cytomineService.currentUser)
                && config.readingRole.equals(Configuration.Role.USER)) return;
        else throw new ForbiddenException("You don't have the right to read this resource!")
    }

    def getStringParamsI18n(def domain) {
        return [domain.key]
    }

}
