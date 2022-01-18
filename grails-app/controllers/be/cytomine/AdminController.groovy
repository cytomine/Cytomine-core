package be.cytomine

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

import be.cytomine.api.RestController
import be.cytomine.security.User
import be.cytomine.security.UserJob
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN','ROLE_SUPER_ADMIN'])
class AdminController extends RestController {


    def grailsApplication
    def modelService
    def springSecurityService
    def archiveCommandService
    def cytomineService
    def cytomineMailService
    def securityACLService

    @Secured(['ROLE_ADMIN','ROLE_SUPER_ADMIN'])
    def index() {
      //don't remove this, it calls admin/index.gsp layout !
    }

    @Secured(['ROLE_ADMIN','ROLE_SUPER_ADMIN'])
    def archive() {
        archiveCommandService.archiveOldCommand()
        responseSuccess([])

    }

    @Secured(['ROLE_ADMIN','ROLE_SUPER_ADMIN'])
    def mailTesting() {

        def user = cytomineService.currentUser
        securityACLService.checkAdmin(user)


        while(user instanceof UserJob) {
            user = ((UserJob) user).user
        }
        user = (User) user

        try {
            cytomineMailService.send(
                    cytomineMailService.NO_REPLY_EMAIL,
                    (String[]) [user.email],
                    null,
                    null,
                    "test mail : ok",
                    "This is a test of the mail sending feature of the Cytomine instance ${grailsApplication.config.grails.serverURL}")
            response([message : "success"], 200)
        } catch (Exception e){
            e.printStackTrace()
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            e.printStackTrace(pw)
            String sStackTrace = sw.toString(); // stack trace as a string

            response([stackTrace:sStackTrace, message : e.getMessage()], 500)
        }
    }

}
