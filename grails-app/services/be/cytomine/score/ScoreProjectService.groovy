package be.cytomine.score

import be.cytomine.Exception.AlreadyExistException
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

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import be.cytomine.score.Score
import be.cytomine.score.ScoreProject

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.READ

class ScoreProjectService extends ModelService{

    static transactional = true

    def cytomineService
    def transactionService
    def modelService
    def securityACLService

    def currentDomain() {
        return ScoreProject
    }

    def read(def id) {
        def sp = ScoreProject.get(id)
        if(sp) {
            securityACLService.check(sp.container(),READ)
        }
        sp
    }

    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        ScoreProject.list()
    }

    def list(Project project) {
        securityACLService.check(project.container(),READ)
        ScoreProject.findAllByProject(project)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        securityACLService.check(json.project,Project, ADMINISTRATION)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        synchronized (this.getClass()) {
            Project project = Project.read(json.project)
            Score score = Score.read(json.score)
            if (ScoreProject.findByProjectAndScore(project, score)) {
                throw new AlreadyExistException("Score ${json.score} already linked to ${json.project}")
            }
            return executeCommand(new AddCommand(user: currentUser),null,json)
        }

    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ScoreProject domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),ADMINISTRATION)
        securityACLService.checkisNotReadOnly(domain)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }


    def getStringParamsI18n(def domain) {
        return [domain.score?.name, domain.project?.name]
    }
}
