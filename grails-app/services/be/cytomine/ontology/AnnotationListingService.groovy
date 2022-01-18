package be.cytomine.ontology

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

import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.sql.AlgoAnnotationListing
import be.cytomine.sql.AnnotationListing
import be.cytomine.sql.UserAnnotationListing
import be.cytomine.utils.GisUtils
import be.cytomine.utils.ModelService
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.READ

class AnnotationListingService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def dataSource
    def kmeansGeometryService
    def securityACLService


    def listGeneric(AnnotationListing al) {
        securityACLService.check(al.container(),READ)
        if(al.kmeans && !al.kmeansValue) {
            if(!al.bbox) {
                throw new WrongArgumentException("If you want to use kmeans, you must provide image bbox (=${al.bbox})")
            }

            if (!al.slice) {
                throw new WrongArgumentException("If you want to use kmeans, you must provide slice Id")
            }

            def rule = kmeansGeometryService.mustBeReduce(al.slice,al.user,al.bbox)
            al.kmeansValue = rule
        } else {
            //no kmeans
            al.kmeansValue = kmeansGeometryService.FULL
        }
        executeRequest(al)
    }

    def executeRequest(AnnotationListing al) {

        if(al.kmeansValue==kmeansGeometryService.FULL) {
            selectGenericAnnotation(al)
        } else if(al.kmeansValue==kmeansGeometryService.KMEANSFULL) {
            kmeansGeometryService.doKeamsFullRequest(al.getAnnotationsRequest())
        } else {
            kmeansGeometryService.doKeamsSoftRequest(al.getAnnotationsRequest())
        }

    }

    /**
     * Execute request and format result into a list of map
     */
    def selectGenericAnnotation(AnnotationListing al) {

        def data = []
        long lastAnnotationId = -1
        long lastTermId = -1
        long lastTrackId = -1

        boolean first = true;

        def realColumn = []
        def request = al.getAnnotationsRequest()

        boolean termAsked = false
        boolean trackAsked = false

        def excludedColumns = ['annotationTerms', 'annotationTracks', 'userTerm', 'x', 'y']

        def sql = new Sql(dataSource)
        log.info request
        sql.eachRow(request) {


            /**
             * If an annotation has n multiple term, it will be on "n" lines.
             * For the first line for this annotation (it.id!=lastAnnotationId), add the annotation data,
             * For the other lines, we add term data to the last annotation
             */
            if (it.id != lastAnnotationId) {
                termAsked = false
                trackAsked = false

                if(first) {
                    al.getAllPropertiesName().each { columnName ->
                        if(columnExist(it,columnName) && !excludedColumns.contains(columnName)) {
                            realColumn << columnName
                        }
                    }
                    first = false
                }

                def item = [:]
                item['class'] = al.getDomainClass()

                realColumn.each { columnName ->
                    item[columnName] = it[columnName]
                }

                if(al.columnToPrint.contains('term')) {
                    termAsked = true
                    item['term'] = (it.term ? [it.term] : [])
                    item['userByTerm'] = (it.term ? [[id: it.annotationTerms, term: it.term, user: [it.userTerm]]] : [])
                }

                if (al.columnToPrint.contains('track') && (al instanceof UserAnnotationListing || al instanceof AlgoAnnotationListing)) {
                    trackAsked = true
                    item['track'] = (it?.track ? [it.track] : [])
                    item['annotationTrack'] = (it?.track ? [[id: it.annotationTracks, track: it.track]] : [])
                }

                if(al.columnToPrint.contains('gis')) {
                    item['perimeterUnit'] = (it.perimeterUnit != null? GisUtils.retrieveUnit(it.perimeterUnit) : null)
                    item['areaUnit'] = (it.areaUnit ? GisUtils.retrieveUnit(it.areaUnit) : null)
                    item['centroid'] = [x: it.x, y: it.y]
                }

                if(al.columnToPrint.contains('meta')) {
                    if(al.getClass().name.contains("UserAnnotation")) {
                        item['cropURL'] = UrlApi.getUserAnnotationCropWithAnnotationId(it.id)
                        item['smallCropURL'] = UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize(it.id, 256)
                        item['url'] = UrlApi.getUserAnnotationCropWithAnnotationId(it.id)
                        item['imageURL'] = UrlApi.getAnnotationURL(it.project, it.image, it.id)
                    } else if(al.getClass().name.contains("AlgoAnnotation")) {
                        item['cropURL'] = UrlApi.getAlgoAnnotationCropWithAnnotationId(it.id)
                        item['smallCropURL'] = UrlApi.getAlgoAnnotationCropWithAnnotationIdWithMaxSize(it.id, 256)
                        item['url'] = UrlApi.getAlgoAnnotationCropWithAnnotationId(it.id)
                        item['imageURL'] = UrlApi.getAnnotationURL(it.project, it.image, it.id)
                    }else if(al.getClass().name.contains("ReviewedAnnotation")) {
                        item['cropURL'] = UrlApi.getReviewedAnnotationCropWithAnnotationId(it.id)
                        item['smallCropURL'] = UrlApi.getReviewedAnnotationCropWithAnnotationIdWithMaxSize(it.id, 256)
                        item['url'] = UrlApi.getReviewedAnnotationCropWithAnnotationId(it.id)
                        item['imageURL'] = UrlApi.getAnnotationURL(it.project, it.image, it.id)
                    }
                }
                data << item
            } else {
                if (termAsked && it.term) {
                    if (it.term == lastTermId) {
                        data.last().userByTerm.last().user.add(it.userTerm)
                        data.last().userByTerm.last().user.unique()
                    } else if (!data.last().term.contains(it.term)) {
                        data.last().term.add(it.term)
                        data.last().userByTerm.add([id: it.annotationTerms, term: it.term, user: [it.userTerm]])
                    }
                }

                if (trackAsked && it.track && it.track != lastTrackId && !data.last().track.contains(it.track)) {
                    data.last().track.add(it.track)
                    data.last().annotationTrack.add([id: it.annotationTracks, track: it.track])
                }
            }

            if (termAsked) {
                lastTermId = it.term
            }

            if (trackAsked) {
                lastTrackId = it.track
            }

            lastAnnotationId = it.id
        }
        sql.close()
        data
    }



}
