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

class JobUrlMappings {

    static mappings = {
        /* Job */
        "/api/job.$format"(controller:"restJob"){
            action = [GET: "list",POST:"add"]
        }
        "/api/job/$id.$format"(controller:"restJob"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
        "/api/job/$id/alldata.$format"(controller:"restJob") {
            action = [DELETE: "deleteAllJobData", GET: "listAllJobData"]
        }
        "/api/job/$id/execute.$format" (controller : "restJob") {
            action = [POST : "execute", GET: "execute"]
        }
        "/api/job/$id/copy.$format" (controller : "restJob") {
            action = [POST : "copy"]
        }
        "/api/job/$job_id/processing_server/$processing_server_id/execute.$format" (controller: "restJob") {
            action = [POST: "executeWithProcessingServer", GET: "executeWithProcessingServer"]
        }
        "/api/job/$id/kill.$format"(controller: "restJob") {
            action = [POST: "kill", GET: "kill"]
        }

        "/api/job/$id/log.$format" (controller: "restJob") {
            action = [GET: "getLog"]
        }

        "/api/job/$id/log.$format" (controller: "restJob") {
            action = [GET: "getLog"]
        }

        "/api/job/$id/favorite.$format" (controller: "restJob") {
            action = [POST: "setFavorite"]
        }

        "/api/project/$id/job/purge.$format"(controller : "restJob") {
            action = [POST : "purgeJobNotReviewed", GET : "purgeJobNotReviewed"]
        }

        "/api/project/$projectId/bounds/job.$format"(controller : "restJob") {
            action = [GET : "bounds"]
        }


        /* Job template */
        "/api/jobtemplate.$format"(controller:"restJobTemplate"){
            action = [POST:"add"]
        }
        "/api/jobtemplate/$id.$format"(controller:"restJobTemplate"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
        "/api/project/$project/jobtemplate.$format"(controller:"restJobTemplate"){
            action = [GET: "list"]
        }

        /* Job template annotation */
        "/api/jobtemplateannotation.$format"(controller:"restJobTemplateAnnotation"){
            action = [POST:"add",GET: "list"]
        }
        "/api/jobtemplateannotation/$id.$format"(controller:"restJobTemplateAnnotation"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
    }
}
