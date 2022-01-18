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

class ProjectConnectionUrlMappings {

    static mappings = {
        "/api/project/$project/userconnection.$format"(controller: "restProjectConnection") {
            action = [POST: "add"]
        }
        "/api/project/$project/userconnection/$user.$format"(controller: "restProjectConnection") {
            action = [GET: "getConnectionByUserAndProject"]
        }
        "/api/project/$project/lastConnection.$format"(controller: "restProjectConnection") {
            action = [GET: "lastConnectionInProject"]
        }
        "/api/project/$project/lastConnection/$user.$format"(controller: "restProjectConnection") {
            action = [GET: "lastConnectionInProjectByUser"]
        }
        "/api/project/$project/connectionFrequency.$format"(controller: "restProjectConnection") {
            action = [GET: "numberOfConnectionsByProject"]
        }
        "/api/project/$project/userconnection/count.$format"(controller: "restProjectConnection") {
            action = [GET: "countByProject"]
        }
        "/api/project/$project/connectionFrequency/$user.$format"(controller: "restProjectConnection") {
            action = [GET: "numberOfConnectionsByProjectAndUser"]
        }
        "/api/project/$project/connectionHistory/$user.$format"(controller: "restProjectConnection") {
            action = [GET: "userProjectConnectionHistory"]
        }
        "/api/projectConnection/$id.$format"(controller: "restProjectConnection") {
            action = [GET: "getUserActivityDetails"]
        }
        "/api/connectionFrequency.$format"(controller: "restProjectConnection") {
            action = [GET: "numberOfProjectConnections"]
        }
        "/api/averageConnections.$format"(controller: "restProjectConnection") {
            action = [GET: "averageOfProjectConnections"]
        }
    }
}