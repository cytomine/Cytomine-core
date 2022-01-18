package be.cytomine.api.meta

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
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.security.SecUser
import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.api.RestController
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ

@RestApi(name = "tag domain association services", description = "Methods for managing associations between a tag and a domain")
class RestTagDomainAssociationController extends RestController {

    def tagDomainAssociationService
    def cytomineService
    def securityACLService

    @RestApiMethod(description="Get a specific tag-domain association")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The association id")
    ])
    def show() {
        TagDomainAssociation association = tagDomainAssociationService.read(params.long('id'))
        if (association) {
            responseSuccess(association)
        } else {
            responseNotFound("TagDomainAssociation", params.id)
        }
    }

    @RestApiMethod(description="Get all tag-domain associations available in cytomine", listing = true)
    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        def result = tagDomainAssociationService.list(searchParameters, params.long('max'), params.long('offset'))
        responseSuccess([collection : result.data, size:result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages])
    }

    @RestApiMethod(description="Get all tag-domain association available in cytomine related to a specific domain", listing = true)
    def listByDomain() {
        CytomineDomain domain
        if(params.domainClassName.contains("AnnotationDomain")) {
            domain = AnnotationDomain.getAnnotationDomain(params.domainId)
        } else {
            domain = Class.forName(params.domainClassName, false, Thread.currentThread().contextClassLoader).read(params.domainId)
        }
        if (!params.domainClassName.contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ)
        }
        def result = tagDomainAssociationService.listByDomain(domain, params.long('max'), params.long('offset'))
        responseSuccess([collection : result.data, size:result.total, offset: result.offset, perPage: result.perPage, totalPages: result.totalPages])
    }

    @RestApiMethod(description="Add a new tag-domain association to cytomine.")
    def add() {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        add(tagDomainAssociationService, request.JSON)
    }

    @RestApiMethod(description="Update a tag-domain association.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The tag id")
    ])
    def update() {
        update(tagDomainAssociationService, request.JSON)
    }

    @RestApiMethod(description="Delete a tag-domain association.", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The tag id")
    ])
    def delete() {
        delete(tagDomainAssociationService, JSON.parse("{id : $params.id}"),null)
    }
}
