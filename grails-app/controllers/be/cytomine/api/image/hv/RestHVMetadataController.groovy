package be.cytomine.api.image.hv

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

import be.cytomine.api.RestController
import be.cytomine.image.hv.HVMetadata
import be.cytomine.processing.ParameterConstraint
import com.mongodb.util.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

class RestHVMetadataController extends RestController {

    def hvMetadataService

    /*def show() {
        ParameterConstraint parameterConstraint = parameterConstraintService.read(params.long('id'))
        if (parameterConstraint) {
            responseSuccess(parameterConstraint)
        } else {
            responseNotFound("ParameterConstraint", params.id)
        }
    }*/

    def addStaining() {
        def json = request.JSON
        json.type = HVMetadata.Type.STAINING.toString()
        add(hvMetadataService, json)
    }
    def addLaboratory() {
        def json = request.JSON
        json.type = HVMetadata.Type.LABORATORY.toString()
        add(hvMetadataService, json)
    }
    def addAntibody() {
        def json = request.JSON
        json.type = HVMetadata.Type.ANTIBODY.toString()
        add(hvMetadataService, json)
    }
    def addDetection() {
        def json = request.JSON
        json.type = HVMetadata.Type.DETECTION.toString()
        add(hvMetadataService, json)
    }
    def addDilution() {
        def json = request.JSON
        json.type = HVMetadata.Type.DILUTION.toString()
        add(hvMetadataService, json)
    }
    def addInstrument() {
        def json = request.JSON
        json.type = HVMetadata.Type.INSTRUMENT.toString()
        add(hvMetadataService, json)
    }

    def delete() {
        delete(hvMetadataService, grails.converters.JSON.parse("{id : $params.id}"),null)

    }

    def listLaboratory() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.LABORATORY))
    }

    def listStaining() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.STAINING))
    }

    def listAntibody() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.ANTIBODY))
    }

    def listDetection() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.DETECTION))
    }

    def listDilution() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.DILUTION))
    }

    def listInstrument() {
        responseSuccess(hvMetadataService.listByType(HVMetadata.Type.INSTRUMENT))
    }




/*    @RestApiMethod(description = "Update a parameter constraint available on Cytomine")
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The container id")
    ])
    def update() {
        update(parameterConstraintService, request.JSON)
    }

    @RestApiMethod(description = "Delete a parameter constraint", listing = true)
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The container id")
    ])
    def delete() {
        delete(parameterConstraintService, JSON.parse("{id : $params.id}"), null)
    }*/

}
