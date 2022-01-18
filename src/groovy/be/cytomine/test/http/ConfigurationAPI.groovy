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

/**
 * Created by hoyoux on 06.05.15.
 */
class ConfigurationAPI extends DomainAPI {

    //SHOW
    static def show(String key, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/configuration/key/${key}.json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/configuration.json"
        return doGET(URL, username, password)
    }

    //ADD
    static def create(String json, String username, String password) {

        String URL = Infos.CYTOMINEURL + "api/configuration.json"
        def result = doPOST(URL,json,username,password)
        return result
    }

    //UPDATE
    static def update(String key, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/configuration/key/${key}.json"
        return doPUT(URL,json,username,password)
    }

    //DELETE
    static def delete(String key, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/configuration/key/${key}.json"
        return doDELETE(URL,username,password)
    }
}
