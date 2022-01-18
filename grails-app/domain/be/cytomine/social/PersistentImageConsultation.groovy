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
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Info on user consultation of an image in a project
 * ex : User x consulted image y the 2013/01/01 at xxhyymin
 */
@RestApiObject(name = "Persistent image consultation", description = "Each PersistentImageConsultation represents an user consultation to an imageInstance.")
class PersistentImageConsultation extends CytomineDomain {

    static mapWith = "mongo"

    static transients = ['id','updated','deleted','class','extraProperties']

    @RestApiObjectField(description = "The user")
    Long user
    @RestApiObjectField(description = "The consulted image")
    Long image
    @RestApiObjectField(description = "The project which contains the image")
    Long project
    @RestApiObjectField(description = "The project connection active during the consultation")
    Long projectConnection
    @RestApiObjectField(description = "The sessionID active during the consultation")
    String session
    @RestApiObjectField(description = "The image name")
    String imageName
    @RestApiObjectField(description = "The image thumb")
    String imageThumb
    @RestApiObjectField(description = "The consultation mode (Explore, review)")
    String mode
    @RestApiObjectField(description = "The duration of the user consultation into the image", useForCreation = false)
    Long time
    @RestApiObjectField(description = "The count of created annotation during the project connection", useForCreation = false)
    Integer countCreatedAnnotations
    def extraProperties = [:]

    static constraints = {
        projectConnection nullable: true
        session nullable: true
        project nullable: true
        time nullable: true
        imageName nullable: true
        imageThumb nullable: true
        countCreatedAnnotations nullable: true
    }

    static mapping = {
        version false
        stateless true //don't store data in memory after read&co. These data don't need to be update.
        image index:true
        compoundIndex user:1, image:1, created:-1
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray.created = domain?.created
        returnArray.user = domain?.user
        returnArray.image = domain?.image
        returnArray.imageName = domain?.imageName
        returnArray.imageThumb = domain?.imageThumb
        returnArray.mode = domain?.mode
        returnArray.project = domain?.project
        returnArray.projectConnection = domain?.projectConnection
        returnArray.time = domain?.time
        returnArray.countCreatedAnnotations = domain?.countCreatedAnnotations

        domain?.extraProperties.each { key, value ->
            returnArray.put(key, value)
        }
        returnArray
    }

    @Override
    public Object clone() {
        PersistentImageConsultation result = new PersistentImageConsultation()
        result.user = user;
        result.project = project
        result.projectConnection = projectConnection;
        result.time = time;
        result.image = image;
        result.imageName = imageName;
        result.imageThumb = imageThumb;
        result.mode = mode;
        result.countCreatedAnnotations = countCreatedAnnotations;
        result.id = id;
        result.created = created;

        return result
    }

    def propertyMissing(String name, def value) {
        extraProperties.put(name, value)
    }
}