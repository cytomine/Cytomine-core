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
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import static org.springframework.security.acls.domain.BasePermission.READ

class ImageScoreService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def modelService
    def jobParameterService
    def securityACLService
    def imageScoreConstraintService

    def currentDomain() {
        return ImageScore
    }

    def read(def id) {
        def imageScore = ImageScore.read(id)
        imageScore
    }

    def read(ImageInstance imageInstance, Score score, User user) {
        def imageScore = ImageScore.findAllByImageInstanceAndUser(imageInstance, user).find{it.scoreValue.score == score}
        imageScore
    }

    def list() {
        securityACLService.checkUser(cytomineService.currentUser)
        ImageScore.list()
    }

    def listByImageInstanceAndUser(ImageInstance imageInstance, User user) {
        def imageScores = ImageScore.findAllByImageInstanceAndUser(imageInstance, user)
        imageScores
    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        if(!json.imageInstance) throw new InvalidRequestException("imageInstance not set")
        ImageInstance imageInstance = ImageInstance.read(json.imageInstance)
        if (!imageInstance) {
            throw new ObjectNotFoundException("ImageInstance ${json.imageInstance} not found")
        }
        securityACLService.check(imageInstance.container(), READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsSameUser(currentUser, SecUser.get(json.user))
        ScoreValue scoreValue = ScoreValue.read(json.scoreValue)
        if (!scoreValue) {
            throw new ObjectNotFoundException("Score value ${json.scoreValue} not found")
        }
        ImageScore scoreAlreadyExist = ImageScore.findAllByImageInstanceAndUser(imageInstance, currentUser).find{it.scoreValue.score == scoreValue.score}
        Transaction transaction;
        if (scoreAlreadyExist) {
            transaction = transactionService.start()
            delete(scoreAlreadyExist, transaction)
        }
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser, transaction: transaction), null, json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ImageScore domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsSameUser(currentUser, domain.user)
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }

    def getStringParamsI18n(def domain) {
        return [domain?.scoreValue?.score?.name, domain?.scoreValue?.value, domain?.imageInstance?.instanceFilename]
    }
}
