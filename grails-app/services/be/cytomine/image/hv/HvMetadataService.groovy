package be.cytomine.image.hv

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by a     pplicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.Exception.CytomineException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.ParameterConstraint
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

class HvMetadataService extends ModelService {

    static transactional = true

    def transactionService
    def securityACLService

    @Override
    def currentDomain() {
        return HVMetadata
    }


    def listByType(HVMetadata.Type type) {
        println "listByType 1"
        securityACLService.checkGuest(cytomineService.getCurrentUser())
        println "listByType 2"
        return HVMetadata.findAllByType(type)
    }

    def delete(HVMetadata domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }


    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }


/*
    ParameterConstraint get(def id) {
        securityACLService.checkUser(cytomineService.getCurrentUser())
        return ParameterConstraint.get(id)
    }

    ParameterConstraint read(def id) {
        securityACLService.checkUser(cytomineService.getCurrentUser())
        return ParameterConstraint.read(id)
    }

    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    /*def update(ParameterConstraint domain, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new EditCommand(user: currentUser), domain, jsonNewData)
    }

    def delete(ParameterConstraint domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new DeleteCommand(user: currentUser, transaction: transaction), domain, null)
    }*/

    @Override
    def getStringParamsI18n(def domain) {
        return ["", ""]
    }

}
