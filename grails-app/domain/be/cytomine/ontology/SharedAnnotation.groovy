package be.cytomine.ontology

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

import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * A shared annotation is a comment on a specific annotation
 * (e.g. is it the good term?, ...)
 * Receiver user can see the comment and add answer
 */
@RestApiObject(name="Shared annotation", description = "A shared annotation is a comment on a specific annotation.")
class SharedAnnotation extends CytomineDomain {

    @RestApiObjectField(description = "User that write the comment")
    User sender

    @RestApiObjectField(description = "Comment that will be share with other user")
    String comment

    /**
     * Only user annotation for now (not reviewed/algo annotation)
     */
    @RestApiObjectField(description = "Id of the commented annotation ")
    Long annotationIdent

    @RestApiObjectField(description = "Class name of the commented annotation ")
    String annotationClassName

    static hasMany = [receivers : User]

    static constraints = {
        comment(type: 'text', nullable: true)
        receivers(nullable: false)
    }

    String toString() {
        "Annotation " + annotationIdent + " shared by " + sender
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['comment'] = domain?.comment
        returnArray['sender'] = domain?.sender?.id
        returnArray['senderName'] = domain?.sender.toString()
        returnArray['annotationIdent'] = domain?.annotationIdent
        returnArray['annotationClassName'] = domain?.annotationClassName
        returnArray['receivers'] = domain?.receivers?.collect { it.id }
        returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static SharedAnnotation insertDataIntoDomain(def json, def domain = new SharedAnnotation()) {

        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)

        domain.annotationClassName = JSONUtils.getJSONAttrStr(json, 'annotationClassName',true)
        domain.annotationIdent = JSONUtils.getJSONAttrLong(json,'annotationIdent',null)

        json.receivers?.each { userID ->
            User user = User.read(userID)
            if (user) domain.addToReceivers(user)
        }

        domain.comment = JSONUtils.getJSONAttrStr(json, 'comment',false)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.deleted = JSONUtils.getJSONAttrDate(json, 'deleted')
        domain.sender = JSONUtils.getJSONAttrDomain(json, "sender", new User(), true)
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')

        return domain
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return AnnotationDomain.getAnnotationDomain(annotationIdent).container();
    }
}

