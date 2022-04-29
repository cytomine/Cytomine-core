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
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.transaction.annotation.Transactional

class DeleteOldProjectsJob {

    def projectService

    //def sessionRequired = true

    def transactionService
    def notificationService
    def secUserService
    def currentRoleServiceProxy

    static triggers = {
        simple name: 'deleteOldProjectsJob', startDelay: 60*1000, repeatInterval: 24*3600*1000
    }

    @Transactional
    def execute() {
        Date today = new Date()
        Date todayPlus5days = new Date(today.time + (5 * 24 * 3600 * 1000))
        log.info("todayPlus5days = " + todayPlus5days)
        Project.findAllByToDeleteAtIsNotNull().each { project ->
            if (project.toDeleteAt) {
                log.info("Project ${project.name} has toDeleteAt " + project.toDeleteAt)

                if (project.toDeleteAt.before(today)) {
                    // delete project
                    log.info("Delete project ${project.id} ${project.name}")
                    SpringSecurityUtils.doWithAuth("superadmin", {
                        projectService.delete(project, transactionService.start(), null, false, false)
                    })
                } else if (project.toDeleteAt.before(todayPlus5days)) {
                    // send email
                    log.info("Send an email because we will delete project")
                    def secUsers = secUserService.listAdmins(project, false)
                    def receiversEmail = secUsers.collect{it.email}.unique()
                    notificationService.notifyProjectDelete(receiversEmail, project)
                }
            }
        }
    }
}
