package be.cytomine.score

import be.cytomine.Exception.CytomineException

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, score
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.command.*
import be.cytomine.processing.*
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import org.springframework.security.acls.domain.BasePermission

import static org.springframework.security.acls.domain.BasePermission.*

class ScoreService extends ModelService {

    static transactional = true

    boolean saveOnUndoRedoStack = false

    def cytomineService
    def transactionService
    def aclUtilService
    def scoreProjectService
    def securityACLService
    def scoreValueService

    def currentDomain() {
        Score
    }

    Score read(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        Score.read(id)
    }

    def readMany(def ids) {
        securityACLService.checkGuest(cytomineService.currentUser)
        Score.findAllByIdInList(ids)
    }

    def list(def sort = 'id', def order_ = 'desc') {
        securityACLService.checkGuest(cytomineService.currentUser)
        return Score.list([sort: sort, order: order_])
    }

    def list(Project project) {
        securityACLService.check(project.container(),READ)
        ScoreProject.findAllByProject(project).collect {it.score}
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Score score, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new EditCommand(user: currentUser),score, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Score domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        log.info "delete score"
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    def deleteDependentScoreValue(Score score, Transaction transaction, Task task = null) {
        log.info "deleteDependentScoreValue ${ScoreValue.findAllByScore(score).size()}"
        ScoreValue.findAllByScore(score).each {
            score.values.remove(it)
            scoreValueService.delete(it,transaction,null, false)
        }
    }

    def deleteDependentScoreProject(Score score, Transaction transaction, Task task = null) {
        ScoreProject.findAllByScore(score).each {
            scoreProjectService.delete(it,transaction,null, false)
        }
    }
}
