package be.cytomine.meta

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

import be.cytomine.CytomineDomain
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "configuration", description = "A key-value entry that save the configurations through the application")
class Configuration extends CytomineDomain implements Serializable {

    // Can evolve with more than these simple roles (project manager, etc.)
    static enum Role {
        ADMIN, USER, ALL
    }

    @RestApiObjectField(description = "The property key")
    String key

    @RestApiObjectField(description = "The property value")
    String value

    @RestApiObjectField(description = "The minimum role needed to access to the configuration value", mandatory = true)
    Role readingRole

    static constraints = {
        key (blank: false, unique: true, validator: {
            val, obj ->
                return !val.contains(".")
        })
        value(blank: false)
        readingRole(nullable: false)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        value type: 'text'
        sort "id"
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['key'] = domain?.key
        returnArray['value'] = domain?.value
        returnArray['readingRole'] = domain?.readingRole.toString()

        return returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Configuration insertDataIntoDomain(def json, def domain = new Configuration()){
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.key = JSONUtils.getJSONAttrStr(json,'key')
        domain.value = JSONUtils.getJSONAttrStr(json,'value')
        domain.readingRole = Role.valueOf(JSONUtils.getJSONAttrStr(json, "readingRole", true).toUpperCase())

        return domain
    }

    //TODO fix this
    public CytomineDomain container() {
        return this;
    }

}
