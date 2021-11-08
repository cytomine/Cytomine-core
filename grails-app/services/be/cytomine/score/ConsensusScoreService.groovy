package be.cytomine.score

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.InvalidRequestException

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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import groovy.sql.Sql
import be.cytomine.score.ConsensusScore

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION
import static org.springframework.security.acls.domain.BasePermission.READ

class ConsensusScoreService extends ModelService {

    static transactional = true

    def secUserService
    def cytomineService
    def transactionService
    def modelService
    def jobParameterService
    def securityACLService
    def dataSource

    def currentDomain() {
        return ConsensusScore
    }

    def read(def id) {
        def consensusScore = ConsensusScore.read(id)
        consensusScore
    }

    def read(ImageInstance imageInstance, Score score) {
        def consensusScore = ConsensusScore.findAllByImageInstance(imageInstance).find{it.scoreValue.score == score}
        consensusScore
    }

    def list() {
        securityACLService.checkUser(cytomineService.currentUser)
        ConsensusScore.list()
    }

    def listByImageInstance(ImageInstance imageInstance) {
        def consensusScores = ConsensusScore.findAllByImageInstance(imageInstance)
        consensusScores
    }


    def statsGroupByImageInstances(Project project, String sortColumn, String sortDirection, def searchFilter) {
        List<ImageInstance> imageInstanceList = ImageInstance.findAllByProjectAndDeletedIsNull(project)
        if (searchFilter && !searchFilter.isEmpty()) {
            imageInstanceList = imageInstanceList.findAll { image -> return image?.baseImage?.filename?.toLowerCase()?.contains(searchFilter) }
        }

        List<Score> scoreList = ScoreProject.findAllByProject(project).collect {it.score}

        def stats = new HashMap<Long, HashMap<String, Long>>()
        imageInstanceList.each { imageInstance ->
            def initFields = ImageInstance.getDataFromDomain(imageInstance)
            scoreList.each { score ->
                initFields[String.valueOf(score.id)] = null
            }
            stats.putIfAbsent(imageInstance.id, initFields)
        }



        def request = "  select " +
                "           image_instance.id as ImageInstanceId, " +
                "           consensus_score.score_id as ScoreId," +
                "           consensus_score.score_value_id as ScoreValueId     " +
                "        from consensus_score, image_instance, score_value\n" +
                "        where consensus_score.image_instance_id = image_instance.id\n" +
                "        and consensus_score.score_value_id  = score_value.id\n" +
                "        and image_instance.project_id = ${project.id}" +
                "        and image_instance.deleted is null;"
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            log.info(it)
            Long imageInstanceId = it['ImageInstanceId']
            String scoreId = String.valueOf(it['ScoreId'])

            if (stats.containsKey(imageInstanceId)) {
                stats.get(imageInstanceId).put(scoreId, (Long)it['ScoreValueId']);
            }
        }
        try {
            sql.close()
        }catch (Exception e) {}

        def rows = new ArrayList(stats.values())
        // score sort parameter is provided as String (name) while results column title is its id
        Score scoreSortedColumn = scoreList.find {it.name.equals(sortColumn)}
        if (scoreSortedColumn) {
            sortColumn = scoreSortedColumn.id
        }

        rows.sort { it[sortColumn]}
        if(sortDirection.equals("desc")) {
            rows = rows.reverse()
        }
        return rows;
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
        securityACLService.check(imageInstance.container(), ADMINISTRATION)
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsSameUser(currentUser, SecUser.get(json.user))
        ScoreValue scoreValue = ScoreValue.read(json.scoreValue)
        if (!scoreValue) {
            throw new ObjectNotFoundException("Score value ${json.scoreValue} not found")
        }
        ConsensusScore scoreAlreadyExist = ConsensusScore.findAllByImageInstance(imageInstance).find{it.scoreValue.score == scoreValue.score}
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
    def delete(ConsensusScore domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(), ADMINISTRATION)
        securityACLService.checkIsSameUser(currentUser, domain.user)
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }

    def getStringParamsI18n(def domain) {
        return [domain?.scoreValue?.score?.name, domain?.scoreValue?.value, domain?.imageInstance?.instanceFilename]
    }
}
