package be.cytomine.api.image.multidim

import be.cytomine.Exception.ForbiddenException

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.image.multidim.ImageSequence
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApi(name = "Image | multidim | HDF5 services", description = "Methods for managing HDF5 image group")
class RestImageGroupHDF5Controller extends RestController {

    def imageGroupService
    def imageGroupHDF5Service
    def projectService


    @RestApiMethod(description="[REMOVED] Get a HDF5 image group")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The HDF5 image group id")
    ])
    def show() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.read(params.id)
        if(groupHDF5)
            responseSuccess(groupHDF5)
        else
            responseNotFound("ImageGroupHDF5", params.id)*/
    }


    @RestApiMethod(description="[REMOVED] Get a HDF5 image group for a specified image group")
    @RestApiParams(params=[
            @RestApiParam(name="group", type="long", paramType = RestApiParamType.PATH,description = "The image group ID that is linked to the HDF5 image group")
    ])
    def showFromImageGroup() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroup group = imageGroupService.read(params.long('group'))
        if (group) {
            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.getByGroup(group)
            if(groupHDF5)
                responseSuccess(groupHDF5)
            else
                responseNotFound("ImageGroupHDF5", params.id)
        } else {
            responseNotFound("ImageGroup", params.group)
        }*/
    }


    @RestApiMethod(description="[REMOVED] Add a new HDF5 image group. It extends an image group with HDF5 functionalities")
    def add() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        //add(imageGroupHDF5Service, request.JSON)
    }


    @RestApiMethod(description="[REMOVED] Update a HDF5 image group")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="int", paramType = RestApiParamType.PATH, description = "The HDF5 image group id")
    ])
    def update() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*log.info request.JSON
        update(imageGroupHDF5Service, request.JSON)*/
    }


    @RestApiMethod(description="[REMOVED] Delete a HDF5 image group")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The HDF5 image group id")
    ])
    def delete() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.read(params.long('id'))
        if(groupHDF5)
            delete(imageGroupHDF5Service, JSON.parse("{id : $groupHDF5.id}"), null)
        else
            responseNotFound("ImageGroupHDF5", params.id)*/
    }


    @RestApiMethod(description="[REMOVED] Delete a HDF5 image group for a specified image group")
    @RestApiParams(params=[
            @RestApiParam(name="group", type="long", paramType = RestApiParamType.PATH,description = "The image group ID that is linked to the HDF5 image group")
    ])
    def deleteFromImageGroup() {
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
        /*ImageGroup group = imageGroupService.read(params.long('group'))
        if (group) {
            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.getByGroup(group)
            if(groupHDF5)
                delete(imageGroupHDF5Service, JSON.parse("{id : $groupHDF5.id}"), null)
            else
                responseNotFound("ImageGroupHDF5", params.id)
        } else {
            responseNotFound("ImageGroup", params.group)
        }*/
    }


    @RestApiMethod(description="[REMOVED] Get the spectrum of a pixel using a HDF5 image group")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType= RestApiParamType.PATH, description="The HDF5 image group ID", required=true),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.PATH, description="The x coordinate (0 is left)", required=true),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.PATH, description="The y coordinate (0 is top)", required=true),
            @RestApiParam(name="minChannel", type="int", paramType=RestApiParamType.QUERY, description="The minimum channel", required=false),
            @RestApiParam(name="maxChannel", type="int", paramType=RestApiParamType.QUERY, description="The maximum channel", required=false),
    ])
    def pixelHDF5(){
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
/*//        ImageGroup group = imageGroupService.read(params.long('group'))
//        if (group) {
//            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.getByGroup(group)
            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.read(params.id)
            if(groupHDF5){
                def parameters = [:]
                parameters.x = params.int('x')
                parameters.y = params.int('y')
                parameters.fif = groupHDF5.filename
                if (params.minChannel) parameters.minChannel = params.minChannel
                if (params.maxChannel) parameters.maxChannel = params.maxChannel

                String imageServerURL =  grailsApplication.config.grails.imageServerURL[0]
                String url = "$imageServerURL/multidim/pixel.json?" + parameters.collect {k, v -> "$k=$v"}.join("&")
                log.info url
                responseSuccess(JSON.parse( new URL(url).text ))
            }
            else
                responseNotFound("ImageGroupHDF5", params.id)
//        } else {
//            responseNotFound("ImageGroup", params.id)
//        }*/
    }


    @RestApiMethod(description="[REMOVED] Get the spectrum of a rectangle using a HDF5 image group")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType= RestApiParamType.PATH, description="The HDF5 image group ID", required=true),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.PATH, description="The x coordinate (0 is left)", required=true),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.PATH, description="The y coordinate (0 is top)", required=true),
            @RestApiParam(name="w", type="int", paramType= RestApiParamType.QUERY, description="The width of the rectangle"),
            @RestApiParam(name="h", type="int", paramType= RestApiParamType.QUERY, description="The height of the rectangle"),
            @RestApiParam(name="minChannel", type="int", paramType=RestApiParamType.QUERY, description="The minimum channel", required=false),
            @RestApiParam(name="maxChannel", type="int", paramType=RestApiParamType.QUERY, description="The maximum channel", required=false),
    ])
    def rectangleHDF5(){
        throw new ForbiddenException("The ImageGroup features has been removed in this version")
/*//        ImageGroup group = imageGroupService.read(params.long('group'))
//        if (group) {
//            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.getByGroup(group)
            ImageGroupHDF5 groupHDF5 = imageGroupHDF5Service.read(params.id)
            if(groupHDF5){
                def parameters = [:]
                parameters.x = params.int('x')
                parameters.y = params.int('y')
                parameters.w = params.int('w')
                parameters.h = params.int('h')
                parameters.fif = groupHDF5.filename
                if (params.minChannel) parameters.minChannel = params.minChannel
                if (params.maxChannel) parameters.maxChannel = params.maxChannel

                String imageServerURL =  grailsApplication.config.grails.imageServerURL[0]
                String url = "$imageServerURL/multidim/rectangle.json?" + parameters.collect {k, v -> "$k=$v"}.join("&")
                log.info url
                responseSuccess(JSON.parse( new URL(url).text ))
            }
            else
                responseNotFound("ImageGroupHDF5", params.id)
//        } else {
//            responseNotFound("ImageGroup", params.id)
//        }*/
    }
}
