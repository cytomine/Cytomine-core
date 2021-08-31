package be.cytomine.ontology

import be.cytomine.AnnotationDomain
import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.image.SliceInstance
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

@RestApiObject(name = "AnnotationTrack", description = "Link between an annotation and a track")
class AnnotationTrack extends CytomineDomain {

    @RestApiObjectField(description = "Annotation identifier")
    Long annotationIdent

    @RestApiObjectField(description = "Annotation class name")
    String annotationClassName

    @RestApiObjectField(description = "The track the association is linked to")
    Track track

    @RestApiObjectField(description = "The slice where the annotation is")
    SliceInstance slice

    static constraints = {
    }

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort 'id'
    }

    void checkAlreadyExist() {
        AnnotationTrack.withNewSession {
            AnnotationTrack annotationTrack = AnnotationTrack.findByAnnotationIdentAndTrack(annotationIdent, track)
            if (annotationTrack != null && annotationTrack?.id != id) {
                throw new AlreadyExistException("AnnotationTrack linking ${annotationTrack?.annotationIdent} with ${annotationTrack.track} already exist!")
            }

            annotationTrack = AnnotationTrack.findBySliceAndTrack(slice, track)
            if (annotationTrack != null) {
                throw new AlreadyExistException("An annotation on slice ${slice} is already linked to this track ${track} !")
            }
        }
    }

    static AnnotationTrack insertDataIntoDomain(def json, def domain = new AnnotationTrack()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, 'deleted')

        domain.track = JSONUtils.getJSONAttrDomain(json, "track", new Track(), true)
        domain.annotationClassName = JSONUtils.getJSONAttrStr(json, 'annotationClassName',true)
        domain.annotationIdent = JSONUtils.getJSONAttrLong(json,'annotationIdent',null)
        domain.slice = JSONUtils.getJSONAttrDomain(json, "slice", new SliceInstance(), true)
        return domain
    }

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['annotationIdent'] = domain?.annotationIdent
        returnArray['annotationClassName'] = domain?.annotationClassName
        returnArray['track'] = domain?.track?.id
        returnArray['slice'] = domain?.slice?.id
        return returnArray
    }

    CytomineDomain container() {
        return track.container()
    }

    AnnotationDomain annotation() {
        return AnnotationDomain.getAnnotationDomain(annotationIdent, annotationClassName)
    }
}
