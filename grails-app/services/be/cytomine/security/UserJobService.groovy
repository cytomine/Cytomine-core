package be.cytomine.security

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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
import be.cytomine.command.EditCommand
import be.cytomine.utils.ModelService

class UserJobService extends ModelService {

    static transactional = true

    def springSecurityService
    def transactionService
    def cytomineService
    def commandService
    def modelService
    def dataSource
    def algoAnnotationService
    def algoAnnotationTermService
    def annotationTermService
    def imageInstanceService
    def reviewedAnnotationService
    def userAnnotationService
    def currentRoleServiceProxy
    def securityACLService
    def storageService

    def currentDomain() {
        User
    }

    def get(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        UserJob.get(id)
    }
    
    def read(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        UserJob.read(id)
    }
    

//    def list() {
//        securityACLService.checkGuest(cytomineService.currentUser)
//        User.list(sort: "username", order: "asc")
//    }
//    
//
//    def list(Project project, List ids) {
//        securityACLService.check(project,READ)
//        UserJob.findAllByIdInList(ids)
//    }
//
//    def listAll(Project project) {
//        def data = []
//        data.addAll(listUsers(project))
//        //TODO: could be optim!!!
//        data.addAll(UserJob.findAllByJobInList(Job.findAllByProject(project)))
//        data
//    }
//
//    def listUsers(Project project, boolean showUserJob = false) {
//        securityACLService.check(project,READ)
//        List<UserJob> users = UserJob.executeQuery("select distinct UserJob from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, UserJob as UserJob "+
//                "where aclObjectId.objectId = "+project.id+" and aclEntry.aclObjectIdentity = aclObjectId.id and aclEntry.sid = aclSid.id and aclSid.sid = UserJob.username and UserJob.class = 'be.cytomine.security.User'")
//        if(showUserJob) {
//            //TODO:: should be optim (see method head comment)
//            List<Job> allJobs = Job.findAllByProject(project, [sort: 'created', order: 'desc'])
//
//            allJobs.each { job ->
//                def userJob = UserJob.findByJob(job);
//                if (userJob) {
//                    userJob.username = job.software.name + " " + job.created
//                }
//                users << userJob
//            }
//        }
//        return users
//    }


//    private def getUserJobImage(ImageInstance image) {
//        //better perf with sql request
//        String request = "SELECT u.id as id, u.username as username, s.name as softwareName, j.created as created \n" +
//                "FROM annotation_index ai, sec_user u, job j, software s\n" +
//                "WHERE ai.image_id = ${image.id}\n" +
//                "AND ai.user_id = u.id\n" +
//                "AND u.job_id = j.id\n" +
//                "AND j.software_id = s.id\n" +
//                "ORDER BY j.created"
//        def data = []
//        def sql = new Sql(dataSource)
//        sql.eachRow(request) {
//            def item = [:]
//            item.id = it.id
//            item.username = it.username
//            item.softwareName = it.softwareName
//            item.created = it.created
//            item.algo = true
//            data << item
//        }
//        try {
//            sql.close()
//        }catch (Exception e) {}
//        data
//    }


    

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
//    def add(def json) {
//        SecUser currentUser = cytomineService.getCurrentUser()
//        securityACLService.checkUser(currentUser)
//        return executeCommand(new AddCommand(user: currentUser),null,json)
//    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(UserJob user, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsCreator(user,currentUser)
        return executeCommand(new EditCommand(user: currentUser),user, jsonNewData)
    }
    
    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
//    def delete(UserJob domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
//        SecUser currentUser = cytomineService.getCurrentUser()
//        if(domain.algo()) {
//            Job job = ((UserJob)domain).job
//            securityACLService.check(job?.container(),READ)
//            securityACLService.checkFullOrRestrictedForOwner(job, ((UserJob)domain).user)
//        } else {
//            securityACLService.checkAdmin(currentUser)
//            securityACLService.checkIsNotSameUser(domain,currentUser)
//        }
//        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
//        return executeCommand(c,domain,null)
//    }

   
    /**
     * Retrieve domain thanks to a JSON object
     * WE MUST OVERRIDE THIS METHOD TO READ USER AND USERJOB (ALL UserJob)
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    def retrieve(Map json) {
        UserJob user = UserJob.get(json.id)
        if (!user) throw new ObjectNotFoundException("User " + json.id + " not found")
        return user
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.username]
    }
}
