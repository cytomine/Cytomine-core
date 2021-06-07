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
import org.apache.commons.lang.builder.HashCodeBuilder
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * User - workplace link
 * A user may have many workplace
 */
@RestApiObject(name = "Sec user sec role", description="User - role link. A user may have many role (USER, ADMIN, GUEST)")
class UserWorkplace extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The user id")
    User user

    @RestApiObjectField(description = "The workplace id")
    Workplace workplace

    static mapping = {
        id generator: "assigned"
        sort "id"
        workplace lazy: false
    }


    static UserWorkplace get(long userId, long workplaceId) {
        UserWorkplace.findByWorkplaceAndUser(Workplace.get(workplaceId),User.get(userId))
    }

    static UserWorkplace create(User user, Workplace workplace, boolean flush = true) {
        if(!get(user.id,workplace.id)) {
            new UserWorkplace(user: user, workplace: workplace).save(flush: flush, insert: true)
        }
    }

    static boolean remove(User user, Workplace workplace, boolean flush = false) {
        UserWorkplace instance = UserWorkplace.findByWorkplaceAndUser(workplace, user)
        instance ? instance.delete(flush: flush) : false
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static UserWorkplace insertDataIntoDomain(def json, def domain = new UserWorkplace()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new User(), true)
        domain.workplace = JSONUtils.getJSONAttrDomain(json, "workplace", new Workplace(), true)
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['user'] = domain?.user?.id
        returnArray['workplace'] = domain?.workplace?.id
        returnArray
    }


    boolean equals(other) {
        if (!(other instanceof UserWorkplace)) {
            return false
        }
        other.user?.id == user?.id && other.workplace?.id == workplace?.id
    }

    int hashCode() {
        def builder = new HashCodeBuilder()
        if (user) builder.append(user.id)
        if (workplace) builder.append(workplace.id)
        builder.toHashCode()
    }

    void checkAlreadyExist() {
        UserWorkplace.withNewSession {
            UserWorkplace workplaceAlready = UserWorkplace.findByWorkplaceAndUser(workplace, user)
            if(workplaceAlready && (workplaceAlready.id!=id))  throw new AlreadyExistException("Workplace ${workplace} already exist set for user ${user}!")
        }
    }
}
