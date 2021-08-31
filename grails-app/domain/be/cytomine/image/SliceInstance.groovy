package be.cytomine.image

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.project.Project
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField


/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

@RestApiObject(name = "Slice Instance", description = "A slice instance of a N-dimensional image")
class SliceInstance extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The abstract slice")
    AbstractSlice baseSlice

    @RestApiObjectField(description = "The image instance")
    ImageInstance image

    @RestApiObjectField(description = "The project")
    Project project


    static belongsTo = [ImageInstance, Project, AbstractSlice]

    static mapping = {
        id(generator: 'assigned', unique: true)
//        sort([time: 'asc', zStack: 'asc', channel: 'asc'])
        baseSlice fetch: 'join'
    }

    static constraints = {
    }

    void checkAlreadyExist() {
        withNewSession {
            SliceInstance slice = SliceInstance.findByImageAndBaseSlice(image, baseSlice)
            if (slice!=null && (slice?.id != id))
                throw new AlreadyExistException("SliceInstance (C:${baseSlice?.channel}, Z:${baseSlice?.zStack}, T:${baseSlice?.time}) already exists for ImageInstance ${image?.id}")
        }
    }

    static SliceInstance insertDataIntoDomain(def json, def domain = new SliceInstance()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new ImageInstance(), true)
        domain.baseSlice = JSONUtils.getJSONAttrDomain(json,"baseSlice",new AbstractSlice(),true)

        domain
    }

    static def getDataFromDomain(SliceInstance domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['uploadedFile'] = domain?.baseSlice?.uploadedFile?.id
        returnArray['imageServerUrl'] = domain?.imageServerUrl
        returnArray['project'] = domain?.project?.id
        returnArray['baseSlice'] = domain?.baseSlice?.id
        returnArray['path'] = domain?.path
        returnArray['image'] = domain?.image?.id
        returnArray['mime'] = domain?.baseSlice?.mime?.mimeType
        returnArray['channel'] = domain?.baseSlice?.channel
        returnArray['zStack'] = domain?.baseSlice?.zStack
        returnArray['time'] = domain?.baseSlice?.time
        returnArray['rank'] = domain?.baseSlice?.rank

        returnArray
    }

    def getPath() {
        return baseSlice?.uploadedFile?.path
    }

    def getImageServerUrl() {
        return baseSlice?.uploadedFile?.imageServer?.url
    }

    def getMimeType(){
        return baseSlice?.mime?.mimeType
    }

    CytomineDomain container() {
        return project.container()
    }
}
