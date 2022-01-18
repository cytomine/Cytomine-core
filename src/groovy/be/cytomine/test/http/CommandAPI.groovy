package be.cytomine.test.http

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

import be.cytomine.test.Infos

class CommandAPI extends DomainAPI {

    static def listHistory(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/commandhistory.json"
        return doGET(URL, username, password)
    }

    static def listAllDeleted(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/deletecommand.json"
        return doGET(URL, username, password)
    }

    static def listDeletedDomain(String username, String password, String domain) {
        String URL = Infos.CYTOMINEURL + "api/deletecommand.json?domain=$domain"
        return doGET(URL, username, password)
    }
}
