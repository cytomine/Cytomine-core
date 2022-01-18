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
import be.cytomine.image.SliceInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Info on action done with annotations
 */
@RestApiObject(name = "Annotation action", description = "Each AnnotationAction represent an action (Select, add, update, delete) on an annotation.")
class AnnotationAction extends CytomineDomain {

    static mapWith = "mongo"

    static transients = ['id','updated','deleted','class']

    static belongsTo = [user : SecUser, image : ImageInstance, project: Project]

    @RestApiObjectField(description = "The user that did the action")
    SecUser user

    @RestApiObjectField(description = "The image which contains the annotation")
    ImageInstance image

    @RestApiObjectField(description = "The slice which contains the annotation")
    SliceInstance slice

    @RestApiObjectField(description = "The project which contains the annotation")
    Project project

    @RestApiObjectField(description = "The annotation class type (roi,user,algo,...)", useForCreation = false)
    String annotationClassName

    @RestApiObjectField(description = "The annotation id")
    Long annotationIdent

    @RestApiObjectField(description = "The user that created the annotation")
    SecUser annotationCreator

    @RestApiObjectField(description = "The action on the annotation (Select, add, delete, update, ...)")
    String action

    static constraints = {
        annotationIdent (nullable:false)
        action (nullable: false, blank: false)
        slice nullable: true
    }

    static mapping = {
        version false
        stateless true //don't store data in memory after read&co. These data don't need to be update.
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
        returnArray.user = domain?.user?.id
        returnArray.image = domain?.image?.id
        returnArray.slice = domain?.slice?.id
        returnArray.project = domain?.project?.id
        returnArray.action = domain?.action
        returnArray.annotationIdent = domain?.annotationIdent
        returnArray.annotationClassName = domain?.annotationClassName
        returnArray.annotationCreator = domain?.annotationCreator?.id
        returnArray
    }
}
