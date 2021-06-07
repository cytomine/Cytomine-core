package be.cytomine.security

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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Workplace", description="A Workplace is related to users. A user may be in many workplaces")
class Workplace extends CytomineDomain {

    @RestApiObjectField(description="The workplace name")
    String name

    @RestApiObjectField(description = "The id for external connection (LDAP, etc.)")
    String address


    static mapping = {
        sort "id"
    }

    static constraints = {
        name(blank: false, unique: true)
        address nullable: true
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Workplace insertDataIntoDomain(def json, def domain=new Workplace()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json,'name',true)
        domain.address = JSONUtils.getJSONAttrStr(json,'address',false)
        return domain;
    }

    String toString() {
        name
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['address'] = domain?.address
        returnArray
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        Workplace.withNewSession {
            Workplace workplaceAlreadyExist = Workplace.findByName(name)
            if(workplaceAlreadyExist && (workplaceAlreadyExist.id!=id))  throw new AlreadyExistException("Workplace $name already exist!")
        }
    }

}
