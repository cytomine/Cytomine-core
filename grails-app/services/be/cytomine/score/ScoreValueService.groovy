package be.cytomine.score

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.processing.JobParameter
import be.cytomine.processing.Software
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import static org.springframework.security.acls.domain.BasePermission.*

class ScoreValueService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def modelService
    def jobParameterService
    def securityACLService
    def scoreValueConstraintService

    def currentDomain() {
        return ScoreValue
    }

    def read(def id) {
        def softValue = ScoreValue.read(id)
        softValue
    }

    def list() {
        securityACLService.checkUser(cytomineService.currentUser)
        ScoreValue.list()
    }

    def list(Score score) {
        return ScoreValue.findAllByScore(score)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        if(!json.score) throw new InvalidRequestException("score not set")
        securityACLService.checkAdmin(cytomineService.currentUser)

        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    def update(ScoreValue scoreValue, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new EditCommand(user: currentUser), scoreValue, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ScoreValue domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.value, domain.score?.name]
    }

    def void reorder(Score score, List<Long> longs) {
        log.info "reorder $score => $longs"
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        int index = 0;
        Transaction transaction = transactionService.start()
        longs.each { id ->
            ScoreValue scoreValue = ScoreValue.read(id)
            if (!scoreValue) {
                throw new ObjectNotFoundException("scoreValue $scoreValue not found")
            }
            if (scoreValue && scoreValue.score!=score) {
                throw new ObjectNotFoundException("scoreValue $scoreValue not found in score $score")
            }
            def json = JSON.parse(scoreValue.encodeAsJSON())
            json['index'] = index
            update(scoreValue, json)
            index++
        }
    }
}
