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
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

@RestApiObject(name = "Tag", description = "A flag that can be associated to a Cytomine domain.")
class Tag extends CytomineDomain implements Serializable{
    @RestApiObjectField(description = "The tag name")
    String name

    @RestApiObjectField(description = "user that created the tag")
    User user

    @RestApiObjectFields(params = [
            @RestApiObjectField(apiFieldName = "creatorName", description = "The username of the creator", allowedType = "string", useForCreation = false),
    ])

    static constraints = {
        name(blank: false)
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist(){
        Tag.withNewSession {
            Tag tag = Tag.findByNameIlike(name)
            if (tag != null && (tag.id!=id)) throw new AlreadyExistException("Tag " + tag.name + " already exist!")
        }
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['user'] = domain?.user?.id
        returnArray['creatorName'] = domain?.user?.username
        return returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Tag insertDataIntoDomain(def json, Tag domain = new Tag()){
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name')
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new User(), true)
        return domain
    }

    @Override
    public SecUser userDomainCreator() {
        return user
    }

}
