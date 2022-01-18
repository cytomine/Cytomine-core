package be.cytomine.processing

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

import be.cytomine.Exception.CytomineMethodNotYetImplementedException
import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.*
import be.cytomine.meta.AttachedFile
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.SecUserSecRole
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.sql.AlgoAnnotationListing
import be.cytomine.sql.ReviewedAnnotationListing
import be.cytomine.meta.AttachedFile
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.util.ReflectionUtils

import java.text.SimpleDateFormat

import static org.springframework.security.acls.domain.BasePermission.READ

class JobService extends ModelService {

    static transactional = true
    def cytomineService
    def modelService
    def transactionService
    def jobParameterService
    def springSecurityService
    def jobDataService
    def secUserService
    def annotationListingService
    def dataSource
    def currentRoleServiceProxy
    def securityACLService

   // static final String[] IGNORES_JOB_PARAMETER = cytomine_host, cytomine_public_key, cytomine_private_key, cytomine_id_software, cytomine_id_project

    def currentDomain() {
        return Job
    }

    def read(def id) {
        def job = Job.read(id)
        if(job) {
            securityACLService.check(job.container(),READ)
        }
        job
    }

    /**
     * List max job for a project and a software
     * Light flag allow to get a light list with only main job properties
     */
    def list(def softwares, def projects, def extended = [:], String sortColumn = "created", String sortDirection = "desc", def searchParameters = [], Long max = 0, Long offset = 0, boolean light) {

        if (searchParameters.find {it.field.equals("username")} && !extended.withUser) throw new WrongArgumentException("Cannot search on username without argument withUser")

        String jobAlias = "j"
        String softwareAlias = "s"
        String userAlias = "u"

        if(!sortColumn)  sortColumn = "created"
        if(!sortDirection)  sortDirection = "desc"

        if(sortColumn.equals("softwareName")) sortColumn = "name"


        String sortedProperty = ReflectionUtils.findField(Job, sortColumn) ? "${jobAlias}.$sortColumn" : null
        if(!sortedProperty) sortedProperty = ReflectionUtils.findField(Software, sortColumn) ? softwareAlias + "." + sortColumn : null
        if(!sortedProperty && sortColumn == "username") sortedProperty = "${userAlias}.$sortColumn"
        if(!sortedProperty) throw new CytomineMethodNotYetImplementedException("Job list sorted by $sortColumn is not implemented")

        if (sortedProperty == "${userAlias}.$sortColumn" && !extended.withUser) throw new WrongArgumentException("Cannot sort on username without argument withUser")

        for (def parameter : searchParameters){
            if(parameter.field.equals("softwareName")) parameter.field = "name"
        }


        def validatedSearchParameters = getDomainAssociatedSearchParameters(Job, searchParameters).collect {[operator:it.operator, property:"$jobAlias."+it.property, value:it.value]}

        validatedSearchParameters.addAll(
                getDomainAssociatedSearchParameters(Software, searchParameters).collect {[operator:it.operator, property:"$softwareAlias."+it.property, value:it.value]})

        if(searchParameters.size() > 1){
            log.debug "The following search parameters have not been validated: "+searchParameters
        }

        def sqlSearchConditions = searchParametersToSQLConstraints(validatedSearchParameters)


        sqlSearchConditions = [
                job : sqlSearchConditions.data.findAll{it.property.startsWith("$jobAlias.")}.collect{it.sql}.join(" AND "),
                software : sqlSearchConditions.data.findAll{it.property.startsWith("$softwareAlias.")}.collect{it.sql}.join(" AND "),
                parameters: sqlSearchConditions.sqlParameters
        ]

        boolean joinSoftware = true//sqlSearchConditions.software || sortedProperty.contains(softwareAlias+".") as we return softwareName by default
        def usernameSearch = searchParameters.find {it.field.equals("username")}


        String select, from, where, search, sort
        String request

        select = "SELECT distinct $jobAlias.* "
        from = "FROM job $jobAlias "
        where = "WHERE true = true "

        if(joinSoftware) {
            select +=", ${softwareAlias}.name as software_name, ${softwareAlias}.software_version as software_version "
            from += "JOIN software $softwareAlias ON ${softwareAlias}.id = ${jobAlias}.software_id "
        }

        def usernameParams = [:]
        if(extended.withUser) {
            select +=", $userAlias.*, uj.id as user_job_id "
            from += "LEFT OUTER JOIN sec_user uj ON uj.job_id = ${jobAlias}.id "
            from += "LEFT OUTER JOIN sec_user $userAlias ON uj.user_id = ${userAlias}.id "

            if(usernameSearch) {
                if(!usernameSearch.values.class.isArray() && !(usernameSearch.values instanceof List)){
                    usernameSearch.values = [usernameSearch.values]
                }
                def placeholders = (1..usernameSearch.values.size()).collect { "u_username_$it" }
                usernameSearch.values.eachWithIndex { username, i ->
                    usernameParams[placeholders[i]] = username
                }
                where += "AND ${userAlias}.username IN ("+ placeholders.collect{ ":$it" }.join(",") +") "
            }

        }


        if(softwares && !softwares.isEmpty()){
            where += "AND ${jobAlias}.software_id IN ("+softwares.collect{it.id}.join(",")+") "
        }
        else where += "AND ${jobAlias}.id IS NULL "
        if(projects && !projects.isEmpty()) {
            where += "AND ${jobAlias}.project_id IN ("+projects.collect{it.id}.join(",")+") "
        }
        else where += "AND ${jobAlias}.id IS NULL "

        search = ""

        if(sqlSearchConditions.job){
            search +=" AND "
            search += sqlSearchConditions.job
        }
        if(sqlSearchConditions.software){
            search +=" AND "
            search += sqlSearchConditions.software
        }

        sort = " ORDER BY "+sortedProperty
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC "
        //sort += (sortDirection.equals("desc")) ? " NULLS LAST " : " NULLS FIRST "


        request = select + from + where + search + sort
        if(max > 0) request += " LIMIT $max"
        if(offset > 0) request += " OFFSET $offset"


        def sql = new Sql(dataSource)
        def data = []
        def mapParams = sqlSearchConditions.parameters
        if(usernameParams.size() > 0) mapParams += usernameParams

        if (mapParams instanceof Map) {
            if (mapParams.containsKey("j_favorite_1")) {
                mapParams["j_favorite_1"] = (mapParams["j_favorite_1"] == 'true');
            }
            if (mapParams.containsKey("j_favorite_2")) {
                mapParams["j_favorite_2"] = (mapParams["j_favorite_2"] == 'true');
            }
        }
        // https://stackoverflow.com/a/42080302
        if (mapParams.isEmpty() && usernameParams.isEmpty()) {
            mapParams = []
        }
        else if (mapParams.isEmpty()) {
            mapParams = usernameParams
        }
        else {
            mapParams += usernameParams
        }

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1; i<=((GroovyResultSet) it).getMetaData().getColumnCount(); i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll("(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() })


                map.putAt(objectKey, it[key])
            }

            // I mock methods and fields to pass through getDataFromDomain of Project
            map["class"] = Job.class
            map['project'] = [id : map['projectId']]
            map['software'] = [
                    id             : map['softwareId'],
                    name           : map['softwareName'],
                    softwareVersion: map['softwareVersion'],
                    fullName       : { _ ->
                        if (map['softwareVersion']?.trim())
                            return "${map['softwareName']} (${map['softwareVersion']})"

                        return map['softwareName'];
                    }
            ]
            map['processingServer'] = [id: map['processingServerId']]


            def line = Job.getDataFromDomain(map)
            if (extended.withUser) {
                line.putAt('username', map.username)
                line.putAt('userJob', map.userJobId)
            }
            data << line
        }

        if(extended.withJobParameters){
            def parameters = jobParameterService.list(data.collect{it.id})
            for(def line : data){
                line.putAt('jobParameters', parameters.findAll{it.job == line.id})
            }
        }

        def size
        request = "SELECT COUNT(DISTINCT ${jobAlias}.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        def result = [data:data, total:size]
        max = (max > 0) ? max : Integer.MAX_VALUE
        result.offset = offset
        result.perPage = Math.min(max, result.total)
        result.totalPages = Math.ceil(result.total / max)

        return result
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(JSONObject json) {
        if(!json.project) throw new InvalidRequestException("software not set")

        securityACLService.check(json.project,Project, READ)
        securityACLService.checkisNotReadOnly(json.project,Project)
        SecUser currentUser = cytomineService.getCurrentUser()

        //Start transaction
        Transaction transaction = transactionService.start()

        //Synchronized this part of code, prevent two job to be add at the same time
        synchronized (this.getClass()) {
            //Add Job
            log.debug this.toString()
            def result = executeCommand(new AddCommand(user: currentUser, transaction: transaction),null,json)
            def job = result?.data?.job?.id

            //add all job params
            def params = json.params;
            if (params) {
                params.each { param ->
                    log.info "add param = " + param
                    jobParameterService.addJobParameter(job,param.softwareParameter,param.value, currentUser, transaction)
                }
            }

            return result
        }
    }

    /**
     * Copy an already existing job to create a new one with same values
     * @param originalJob Job to copy
     * @return Response job copied
     */
    def copy(Job originalJob) {
        log.info "copy job ${originalJob?.id}"
        Job newJob = Job.insertDataIntoDomain(Job.getDataFromDomain(originalJob))
        newJob.id = null
        newJob.rate = null
        newJob.statusComment = null
        newJob.status = 0
        newJob.progress = 0
        newJob.dataDeleted = false
        newJob.created = null
        newJob.updated = null
        newJob.deleted = null
        def map = Job.getDataFromDomain(newJob)

        def params = jobParameterService.list(originalJob)
        params.each {
            if (!it.softwareParameter.setByServer) {
                map.get('params', []).add([softwareParameter: it.softwareParameter.id, value: it.value])
            }
        }
        return this.add(JSON.parse((map as JSON).toString()))
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Job job, def jsonNewData) {
        log.info "update"
        securityACLService.check(job.container(),READ)
        securityACLService.checkisNotReadOnly(job.container())
        log.info "securityACLService.check"
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser),job, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Job domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),READ)
        securityACLService.checkisNotReadOnly(domain.container())

        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.software.name]
    }

    def getLog(Job job) {
        def log = AttachedFile.findByDomainClassNameAndDomainIdentAndFilename("be.cytomine.processing.Job", job.id, "log.out")

        if (!log) {
            return null
        }
        def ret = AttachedFile.getDataFromDomain(log)
        ret['data'] = new String(log.data);
        return ret
    }

    def markAsFavorite(Job job, boolean favorite) {
        new Sql(dataSource).executeUpdate("UPDATE job SET favorite = ${favorite} WHERE id = ${job.id}");
        job.favorite = favorite
        return job
    }

    List<UserJob> getAllLastUserJob(Project project, Software software) {
        securityACLService.check(project,READ)
        //TODO: inlist bad performance
        List<Job> jobs = Job.findAllWhere('software':software,'status':Job.SUCCESS, 'project':project)
        List<UserJob>  userJob = UserJob.findAllByJobInList(jobs,[sort:'created', order:"desc"])
        return userJob
    }

    private UserJob getLastUserJob(Project project, Software software) {
        List<UserJob> userJobs = getAllLastUserJob(project,software)
        return userJobs.isEmpty()? null : userJobs.first()
    }


    /**
     * If params.project && params.software, get the last userJob from this software from this project
     * If params.job, get userjob with job
     * @param params
     * @return
     */
    public UserJob retrieveUserJobFromParams(def params) {
        log.info "retrieveUserJobFromParams:" + params
        SecUser userJob = null
        if (params.project != null && params.software != null) {
            Project project = Project.read(params.project)
            Software software = Software.read(params.software)
            if(project && software) userJob = getLastUserJob(project, software)
        } else if (params.job != null) {
            Job job = Job.read(params.long('job'))
            if(job) {
                userJob = UserJob.findByJob(job)
            }
        }
        return userJob
    }

    /**
     * Chek if job has reviewed annotation
     */
    public def getReviewedAnnotation(def annotations, def job) {
        List<Long> annotationsId = annotations.collect{ it.id }
        if (annotationsId.isEmpty()) {
            return  []
        }
        ReviewedAnnotationListing al = new ReviewedAnnotationListing(project:job.project.id, parents:annotationsId)


        return annotationListingService.listGeneric(al)
    }

    public boolean hasReviewedAnnotation(def job) {
        def user = UserJob.findByJob(job)
        if(!user) {
           return true
        }

        def annotations = annotationListingService.listGeneric(new AlgoAnnotationListing(project:job.project.id,user:user.id))
        log.info "Job ${job.id} has ${annotations.size()} annotations"
        if (annotations.isEmpty()) return false
        ReviewedAnnotationListing al = new ReviewedAnnotationListing(project:job.project.id, parents:annotations.collect{ it.id })
        def list = annotationListingService.listGeneric(al)
        log.info "Job ${job.id} has ${list.size()} annotations reviewed"
        return !list.isEmpty()
    }

    /**
     * Delete all annotation created by a user job from argument
     */
    public void deleteAllAlgoAnnotations(Job job) {
        securityACLService.check(job.container(),READ)
        List<Long> usersId = UserJob.findAllByJob(job).collect{ it.id }
        if (usersId.isEmpty()) return
        def request = "delete from algo_annotation where user_id in (" + usersId.join(',') +")"
        def sql = new Sql(dataSource)
         sql.execute(request,[])
        try {
            sql.close()
        }catch (Exception e) {}
    }

    /**
     * Delete all algo-annotation-term created by a user job from argument
     */
    public void deleteAllAlgoAnnotationsTerm(Job job) {
        securityACLService.check(job.container(),READ)
        List<Long> usersId = UserJob.findAllByJob(job).collect{ it.id }
        if (usersId.isEmpty()) return
        def request = "delete from algo_annotation_term where user_job_id in ("+ usersId.join(',')+")"
        def sql = new Sql(dataSource)
        sql.execute(request,[])
        try {
            sql.close()
        }catch (Exception e) {}

    }

    /**
     * Delete all data filescreated by a user job from argument
     */
    public void deleteAllJobData(Job job) {
        securityACLService.check(job.container(),READ)
        List<JobData> jobDatas = JobData.findAllByJob(job)
        List<Long> jobDatasId = jobDatas.collect{ it.id }
        if (jobDatasId.isEmpty()) return
        JobData.executeUpdate("delete from JobData a where a.id IN (:list)",[list:jobDatasId])
    }

    public UserJob createUserJob(User user, Job job) {
        securityACLService.check(job.container(),READ)
        UserJob userJob = new UserJob()
        userJob.job = job
        userJob.username = "JOB[" + user.username + " ], " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS").format(new Date())
        userJob.password = user.password
        userJob.generateKeys()
        userJob.enabled = user.enabled
        userJob.accountExpired = user.accountExpired
        userJob.accountLocked = user.accountLocked
        userJob.passwordExpired = user.passwordExpired
        userJob.user = user
        userJob.origin = "JOB"
        userJob = userJob.save(flush: true, failOnError: true)

        currentRoleServiceProxy.findCurrentRole(user).each { secRole ->
            SecUserSecRole.create(userJob, secRole)
        }

        return userJob
    }

    /**
     * Convert jobs list to a simple list with json object and main job properties
     */
    private def getJOBResponseList(List<Job> jobs) {
        def data = []
        jobs.each {
            def job = [:]
            job.id = it.id
            job.status = it.status
            job.number = it.number
            job.created = it.created?.time?.toString()
            job.dataDeleted = it.dataDeleted
            job.favorite = it.favorite
            data << job
        }
        return data
    }


    def deleteDependentJobParameter(Job job, Transaction transaction, Task task = null) {
        JobParameter.findAllByJob(job).each {
            jobParameterService.delete(it, transaction, null,false)
        }
    }

    def deleteDependentJobData(Job job, Transaction transaction, Task task = null) {
        JobData.findAllByJob(job).each {
            jobDataService.delete(it, transaction, null,false)
        }
    }

    def deleteDependentUserJob(Job job, Transaction transaction, Task task = null) {
        UserJob.findAllByJob(job).each {
            secUserService.delete(it, transaction,null, false)
        }
    }

}
