package be.cytomine.job

import be.cytomine.command.CommandHistory
import be.cytomine.meta.Configuration

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

class DeleteOldActivitiesJob {

    static triggers = {
        simple name: 'deleteOldActivitiesJob', startDelay: 1000, repeatInterval: 1000*30
    }

    @Transactional
    def execute() {
        Configuration configuration = Configuration.findByKey("ACTIVITIES_RETENTION_DELAY_IN_HOURS")
        log.info("deleteOldActivitiesJob with configuration " + configuration)
        if (!configuration || configuration.value.trim().equals("0")) {
            return
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
