package be.cytomine.meta

import be.cytomine.AnnotationDomain

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
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

@RestApiObject(name = "Tag domain association", description = "A flag that can be associated to a Cytomine domain.")
class TagDomainAssociation extends CytomineDomain implements Serializable{
    @RestApiObjectField(description = "The tag id")
    Tag tag

    @RestApiObjectField(description = "The domain class")
    String domainClassName

    @RestApiObjectField(description = "The domain identifier (id)")
    Long domainIdent

    @RestApiObjectFields(params = [
            @RestApiObjectField(apiFieldName = "tagName", description = "The tag name", allowedType = "string", useForCreation = false),
    ])
    static transients = []

    static constraints = {
        domainClassName(nullable: false, blank:  false)
    }

    /**
     * Helper method to set domain
     * @param domain to add
     */
    public void setDomain(CytomineDomain domain) {
        domainClassName = domain.class.getName()
        domainIdent = domain.id
    }

    /**
     * Get domain thanks to domainClassName and domainIdent
     * @return domain
     */
    public CytomineDomain retrieveCytomineDomain() {
        //Class.forName(domainClassName, false, Thread.currentThread().contextClassLoader).read(domainIdent)

        def domainClass = domainClassName
        CytomineDomain domain

        if(domainClass.contains("AnnotationDomain")) {
            domain = AnnotationDomain.getAnnotationDomain(domainIdent)
        } else {
            domain = Class.forName(domainClass, false, Thread.currentThread().contextClassLoader).read(domainIdent)
        }

        domain
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist(){
        TagDomainAssociation.withNewSession {
            TagDomainAssociation association = TagDomainAssociation.findByTagAndDomainClassNameAndDomainIdent(tag, domainClassName, domainIdent)
            if (association != null && (association.id!=id)) throw new AlreadyExistException("TagDomainAssociation " + tag.name + " on "+domainClassName+" "+domainIdent+" already exist!")
        }
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['domainIdent'] = domain?.domainIdent
        returnArray['domainClassName'] = domain?.domainClassName
        returnArray['tag'] = domain?.tag?.id
        returnArray['tagName'] = domain?.tag?.name
        return returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static TagDomainAssociation insertDataIntoDomain(def json, TagDomainAssociation domain = new TagDomainAssociation()){
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        Long id = JSONUtils.getJSONAttrLong(json, 'domainIdent', -1)
        domain.domainIdent = id
        domain.domainClassName = JSONUtils.getJSONAttrStr(json, 'domainClassName')
        domain.tag = JSONUtils.getJSONAttrDomain(json,'tag', new Tag(), true)

        return domain
    }

    public CytomineDomain container() {
        return retrieveCytomineDomain()?.container();
    }
}
