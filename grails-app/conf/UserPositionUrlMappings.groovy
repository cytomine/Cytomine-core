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

/**
 * Cytomine
 * User: stevben
 * Date: 10/10/11
 * Time: 13:49
 */
class UserPositionUrlMappings {

    static mappings = {
        "/api/imageinstance/$image/position.$format" (controller : "restUserPosition") {
            action = [POST:"add"]
        }
        "/api/imageinstance/$id/position/$user.$format" (controller : "restUserPosition") {
            action = [GET:"lastPositionByUser"]
        }

        "/api/sliceinstance/$slice/position.$format" (controller : "restUserPosition") {
            action = [POST:"add"]
        }

        "/api/imageinstance/$image/positions.$format" (controller : "restUserPosition") {
            action = [GET:"list"]
        }
        "/api/imageinstance/$id/online.$format"(controller: "restUserPosition"){
            action = [GET:"listOnlineUsersByImage"]
        }
    }
}
