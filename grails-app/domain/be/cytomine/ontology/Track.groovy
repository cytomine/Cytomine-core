package be.cytomine.ontology

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.image.ImageInstance
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

@RestApiObject(name = "Track", description = "A track is a group of annotations in different slices of a same image.")
class Track extends CytomineDomain {

    @RestApiObjectField(description = "The track name")
    String name

    @RestApiObjectField(description = "The color associated to the track")
    String color

    @RestApiObjectField(description = "The image on which the track is drawn")
    ImageInstance image

    @RestApiObjectField(description = "The project in which the track is drawn")
    Project project // Redundant with image, used for speed up in security checks

    static belongsTo = [ImageInstance, Project]

    static constraints = {
    }

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort 'id'
    }

    void checkAlreadyExist() {
        Track.withNewSession {
            Track track = Track.findByNameAndImageAndDeletedIsNull(name, image)
            if (track != null && track?.id != id) {
                throw new AlreadyExistException("Track ${track?.name} already exist!")
            }
        }
    }

    static Track insertDataIntoDomain(def json, def domain = new Track()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, 'deleted')

        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new ImageInstance(), true)
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)

        domain.name = JSONUtils.getJSONAttrStr(json, "name", true)
        domain.color = JSONUtils.getJSONAttrStr(json, "color", true)
        return domain
    }

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['color'] = domain?.color
        returnArray['image'] = domain?.image?.id
        returnArray['project'] = domain?.project?.id
        return returnArray
    }

    CytomineDomain container() {
        return image.container()
    }
}
