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
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.READ

class ImageScoreService extends ModelService {

    static transactional = true

    def secUserService
    def cytomineService
    def transactionService
    def modelService
    def jobParameterService
    def securityACLService
    def imageScoreConstraintService
    def dataSource

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

    def listByProjectAndUser(Project project, User user) {
        def imageScores = ImageScore.executeQuery(
                "SELECT s FROM ImageInstance i, ImageScore s WHERE i.project = ${project.id} AND i = s.imageInstance AND s.user = ${user.id}"
        )
        return imageScores
    }

    def statsGroupByImageInstances(Project project, String sortColumn, String sortDirection, def searchFilter) {
        List<ImageInstance> imageInstanceList = ImageInstance.findAllByProject(project)
        if (searchFilter && !searchFilter.isEmpty()) {
            imageInstanceList = imageInstanceList.findAll { image -> return image?.baseImage?.filename?.toLowerCase()?.contains(searchFilter) }
        }

        List<Score> scoreList = ScoreProject.findAllByProject(project).collect {it.score}

        def stats = new HashMap<Long, HashMap<String, Integer>>()
        imageInstanceList.each { imageInstance ->
            def initFields = ImageInstance.getDataFromDomain(imageInstance)
            scoreList.each { score ->
                initFields[String.valueOf(score.id)] = 0
            }
            stats.putIfAbsent(imageInstance.id, initFields)
        }



        def request = "  select " +
                "           image_instance.id as ImageInstanceId, " +
                "           score_value.score_id as ScoreId " +
                "        from image_score, image_instance, score_value\n" +
                "        where image_score.image_instance_id = image_instance.id\n" +
                "        and image_score.score_value_id  = score_value.id\n" +
                "        and image_instance.project_id = ${project.id};"
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            Long imageInstanceId = it['ImageInstanceId']
            String scoreId = String.valueOf(it['ScoreId'])

            if (stats.containsKey(imageInstanceId)) {
                Integer counter = stats.get(imageInstanceId).get(scoreId)
                stats.get(imageInstanceId).put(scoreId, counter + 1)
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

    def statsGroupBySecUser(Project project, String sortColumn, String sortDirection, def searchFilter) {
        List<User> userList = secUserService.listUsers(project, false, false)
        if (searchFilter && !searchFilter.isEmpty()) {
            userList = userList.findAll { user -> return (user?.username + user?.firstname + user?.lastname).toLowerCase()?.contains(searchFilter)
            }
        }

        def stats = new HashMap<Long, HashMap<String, Integer>>()
        List<Score> scoreList = ScoreProject.findAllByProject(project).collect {it.score}


        userList.each { user ->
            def initFields = User.getDataFromDomain(user)
            scoreList.each { score ->
                initFields[String.valueOf(score.id)] = 0
            }
            stats.putIfAbsent(user.id, initFields)
        }


        def request = "select sec_user.id as userId, score_value.score_id as ScoreId\n" +
                "from sec_user, image_score, image_instance, score_value\n" +
                "where sec_user.id = image_score.user_id\n" +
                "and image_score.image_instance_id  = image_instance.id \n" +
                "and score_value.id  = image_score.score_value_id \n" +
                "and image_instance.project_id  = ${project.id};"

        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            Long userId = it['userId']
            String scoreId = String.valueOf(it['ScoreId'])

            if (stats.containsKey(userId)) {
                Integer counter = stats.get(userId).get(scoreId)
                stats.get(userId).put(scoreId, counter + 1)
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
