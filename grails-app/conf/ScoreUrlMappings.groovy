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

class ScoreUrlMappings {

    static mappings = {
        /* Score */
        "/api/score.$format"(controller:"restScore"){
            action = [GET: "list",POST:"add"]
        }
        "/api/score/$id.$format"(controller:"restScore"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
        "/api/project/$id/score.$format"(controller:"restScore"){
            action = [GET: "listByProject"]
        }

        "/api/imageinstance/$imageInstance/score/$score.$format"(controller:"restImageScore") {
            action = [GET: "show", DELETE: "delete"]
        }
        "/api/imageinstance/$imageInstance/score/$score/value/$value.$format"(controller:"restImageScore") {
            action = [POST: "add"]
        }

        "/api/imageinstance/$imageInstance/image-score.$format"(controller:"restImageScore") {
            action = [GET: "listByImageInstance"]
        }

        "/api/project/$project/image-score.$format"(controller:"restImageScore") {
            action = [GET: "listByProject"]
        }
        "/api/project/$project/image-score/stats-group-by-image.$format"(controller:"restImageScore") {
            action = [GET: "statsGroupByImageInstances"]
        }
        "/api/project/$project/image-score/stats-group-by-user.$format"(controller:"restImageScore") {
            action = [GET: "statsGroupByUsers"]
        }



    }
}
