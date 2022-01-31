package be.cytomine.job

import be.cytomine.command.CommandHistory
import be.cytomine.meta.Configuration
import be.cytomine.project.Project
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

import be.cytomine.security.SecUser
import org.springframework.transaction.annotation.Transactional

class DeleteOldProjectsJob {

    def projectService

    def transactionService
    def notificationService
    def secUserService

    static triggers = {
        simple name: 'deleteOldProjectsJob', startDelay: 60*1000, repeatInterval: 60*1000 //24*3600*1000
    }

    @Transactional
    def execute() {
        Configuration configuration = Configuration.findByKey(Configuration.DELETE_PROJECT_AFTER_DELAY_IN_DAYS)
        log.info("DeleteOldProjectsJob with configuration " + (configuration ? configuration.value : "null"))
        if (!configuration || configuration.value.trim().equals("0")) {
            return
        }

        Date today = new Date()
        Date todayPlus3days = new Date(today.time + (3 * 24 * 3600 * 1000))
        log.info("todayPlus3days = " + todayPlus3days)
        Project.findAllByToDeleteAtIsNotNull().each { project ->
            if (project.toDeleteAt) {
                log.info("Project ${project.name} has toDeleteAt " + project.toDeleteAt)

                if (project.toDeleteAt.before(today)) {
                    // delete project
                    log.info("Delete project ${project.id} ${project.name}")
                    projectService.delete(project, transactionService.start(), null, false)
                } else if (project.toDeleteAt.before(todayPlus3days)) {
                    // send email
                    log.info("Send an email because we will delete project")
                    def secUsers = secUserService.listAdmins(project, false)
                    notificationService.notifyProjectDelete(secUsers.collect{it.email}, project)
                }
            }
        }

        Long delay = Long.parseLong(configuration.value)
        Date maxBeforeDeleting = new Date(new Date().getTime()-(delay*1000*60*60))
        log.info("deleteOldActivitiesJob: delete all command before " + maxBeforeDeleting)
//        println CommandHistory.exe
        //CommandHistory.executeUpdate('delete CommandHistory ch where ch.created < ?', [maxBeforeDeleting])
        def content = CommandHistory.executeQuery("SELECT ch FROM CommandHistory ch WHERE ch.created < '$maxBeforeDeleting'")
        println content.size()
        CommandHistory.executeUpdate("delete CommandHistory ch where ch.created < '$maxBeforeDeleting'")
    }
}
