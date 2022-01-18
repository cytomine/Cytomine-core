package be.cytomine.social

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
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Info on user connection for a project
 * ex : User x connect to project y the 2013/01/01 at time y
 */
@RestApiObject(name = "Persistent project connection", description = "Each PersistentProjectConnection represents an user connection to a project.")
class PersistentProjectConnection extends CytomineDomain implements Cloneable {

    static mapWith = "mongo"

    static transients = ['id','updated','deleted','class','extraProperties']

    static belongsTo = [user : SecUser, project: Project]

    @RestApiObjectField(description = "The user id")
    Long user
    @RestApiObjectField(description = "The consultated project id")
    Long project
    @RestApiObjectField(description = "The duration of the user connection into the project", useForCreation = false)
    Long time
    @RestApiObjectField(description = "The sessionID active during the connection")
    String session
    @RestApiObjectField(description = "The OS of the user")
    String os
    @RestApiObjectField(description = "The browser of the user")
    String browser
    @RestApiObjectField(description = "The browser version of the user")
    String browserVersion
    @RestApiObjectField(description = "The count of viewed image during the project connection", useForCreation = false)
    Integer countViewedImages
    @RestApiObjectField(description = "The count of created annotation during the project connection", useForCreation = false)
    Integer countCreatedAnnotations
    def extraProperties = [:]

    static constraints = {
        user (nullable:false)
        project (nullable: false)
        session nullable: true
        time(nullable: true)
        browser(nullable: true)
        browserVersion(nullable: true)
        os(nullable: true)
        countViewedImages(nullable: true)
        countCreatedAnnotations(nullable: true)
    }

    static mapping = {
        version false
        stateless true //don't store data in memory after read&co. These data don't need to be update.
        project index:true
        compoundIndex project:1, created:-1
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(PersistentProjectConnection domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray.user = domain?.user
        returnArray.project = domain?.project
        returnArray.time = domain?.time
        returnArray.os = domain?.os;
        returnArray.browser = domain?.browser;
        returnArray.browserVersion = domain?.browserVersion;
        returnArray.countViewedImages = domain?.countViewedImages
        returnArray.countCreatedAnnotations = domain?.countCreatedAnnotations
        domain?.extraProperties.each { key, value ->
            returnArray.put(key, value)
        }
        returnArray
    }

    @Override
    public Object clone() {
        PersistentProjectConnection result = new PersistentProjectConnection()
        result.user = user;
        result.project = project;
        result.time = time;
        result.os = os;
        result.browser = browser;
        result.browserVersion = browserVersion;
        result.countViewedImages = countViewedImages;
        result.countCreatedAnnotations = countCreatedAnnotations;
        result.id = id;
        result.created = created;

        return result
    }

    def propertyMissing(String name, def value) {
        extraProperties.put(name, value)
    }
}
