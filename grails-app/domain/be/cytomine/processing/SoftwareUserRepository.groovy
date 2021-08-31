package be.cytomine.processing

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Software user repository", description = "Representation of a repository manager and its docker hub")
class SoftwareUserRepository extends CytomineDomain {

    @RestApiObjectField(description = "The provider name the user repository")
    String provider

    @RestApiObjectField(description = "The username of the user repository")
    String username

    @RestApiObjectField(description = "The docker username associated to the software repository")
    String dockerUsername

    @RestApiObjectField(description = "The prefix used to identify a software repository")
    String prefix

    static constraints = {
        provider(nullable: false, blank: false)
        username(nullable: false, blank: false)
        dockerUsername(nullable: false, blank: false)
        prefix(nullable: true)
    }

    static mapping = {
        id(generator: "assigned")
        sort("id")
    }

    @Override
    void checkAlreadyExist() {
        SoftwareUserRepository.withNewSession {
            SoftwareUserRepository softwareUserRepository = SoftwareUserRepository.findByProviderAndUsernameAndDockerUsernameAndPrefix(provider, username, dockerUsername, prefix)
            if (softwareUserRepository != null && softwareUserRepository.id != id) {
                throw new AlreadyExistException("The software user repository ${softwareUserRepository.username} already exists !")
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static SoftwareUserRepository insertDataIntoDomain(def json, def domain = new SoftwareUserRepository()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.provider = JSONUtils.getJSONAttrStr(json, 'provider')
        domain.username = JSONUtils.getJSONAttrStr(json, 'username')
        domain.dockerUsername = JSONUtils.getJSONAttrStr(json, 'dockerUsername')
        domain.prefix = JSONUtils.getJSONAttrStr(json, 'prefix')
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['provider'] = domain?.provider
        returnArray['username'] = domain?.username
        returnArray['dockerUsername'] = domain?.dockerUsername
        returnArray['prefix'] = domain?.prefix
        return returnArray
    }

}
