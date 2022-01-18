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

import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.meta.AttachedFile
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

class AttachedFileService extends ModelService {

    static transactional = true
    def transactionService
    def commandService
    def cytomineService

    def currentDomain() {
        return AttachedFile
    }

    /**
     * List all description, Only for admin
     */
    def list() {
        return AttachedFile.list()
    }

    def list(Long domainIdent,String domainClassName) {
        return AttachedFile.findAllByDomainIdentAndDomainClassName(domainIdent,domainClassName)
    }


    def read(def id) {
        AttachedFile.read(id)
    }

    def add(String filename,byte[] data, String key, Long domainIdent,String domainClassName) {
        AttachedFile file = new AttachedFile()
        file.domainIdent =  domainIdent
        file.domainClassName = domainClassName
        file.filename = filename
        file.data = data
        file.key = key
        saveDomain(file)
        file
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AttachedFile domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.domainClassName]
    }

}
