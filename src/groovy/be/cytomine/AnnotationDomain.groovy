package be.cytomine

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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.RoiAnnotation
import be.cytomine.project.Project
import be.cytomine.utils.GeometryUtils
import be.cytomine.utils.GisUtils
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import groovy.sql.Sql
import groovy.util.logging.Log
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * Annotation generic domain
 * Annotation can be:
 * -UserAnnotation => created by human user
 * -AlgoAnnotation => created by job
 * -ReviewedAnnotation => User or AlgoAnnotation validate by user
 */
@Log
@RestApiObject(name = "generic annotation")
abstract class AnnotationDomain extends CytomineDomain implements Serializable {

    def dataSource

    @RestApiObjectField(description = "The slice on which the annotation is drawn")
    SliceInstance slice

    @RestApiObjectField(description = "The image on which the annotation is drawn")
    ImageInstance image // Redundant with slice, used for speed up in security checks

    @RestApiObjectField(description = "The project in which the annotation is drawn")
    Project project // Redundant with slice, used for speed up in security checks

    @RestApiObjectField(description = "The annotation geometry (shape) in WKT", allowedType = "string")
    Geometry location
    String wktLocation // Redundant, better to use this than getting WKT from location properties

    @RestApiObjectField(description = "The geometry compression rate used to simplify the annotation (during creation)", mandatory = false)
    Double geometryCompression

    @RestApiObjectField(description = "The annotation area", useForCreation = false)
    Double area

    @RestApiObjectField(description = "The unit used for area (pixels²=1,micron²=3)", useForCreation = false)
    Integer areaUnit

    @RestApiObjectField(description = "The annotation perimeter", useForCreation = false)
    Double perimeter

    @RestApiObjectField(description = "The unit used for perimeter (pixels=0,mm=2,)", useForCreation = false)
    Integer perimeterUnit

    @RestApiObjectField(description = "The number of comments added by a user on this annotation", apiFieldName = "nbComments", useForCreation = false)
    long countComments = 0L


    static belongsTo = [ImageInstance, Project, SliceInstance]

    @RestApiObjectFields(params=[
            @RestApiObjectField(apiFieldName = "centroid", description = "X,Y coord of the annotation centroid",allowedType = "map(x,y)",useForCreation = false),
            @RestApiObjectField(apiFieldName = "term", description = "List of term id mapped with this annotation",allowedType = "list",useForCreation = true, mandatory=false),
    ])

    static constraints = {
        location(nullable: false)
        geometryCompression(nullable: true)
        wktLocation(nullable:false, empty:false)
        area(nullable:true)
        perimeter(nullable:true)
        areaUnit(nullable:true)
        perimeterUnit(nullable:true)
        project(nullable:true)
        slice(nullable: true)
    }

    static mapping = {
        wktLocation(type: 'text')
        tablePerHierarchy false
        id generator: "assigned"
        columns {
            location type: org.hibernatespatial.GeometryUserType
        }
    }

    public beforeInsert() {
        super.beforeInsert()
        if(!project) {
            project = image.project
        }

        if (!slice) {
            slice = image.referenceSlice
        }

        this.makeValid()
        wktLocation = location.toText()
    }

    def beforeUpdate() {
        super.beforeUpdate()
        if(!project) {
            project = image.project
        }

        if (!slice) {
            slice = image.referenceSlice
        }

        this.makeValid()
        this.computeGIS()
        wktLocation = location.toText()
    }

    def beforeValidate() {
        super.beforeValidate()
        this.computeGIS()
        if(!wktLocation)
            wktLocation = location.toText()
    }

    /**
     * Get all terms map with the annotation
     * @return Terms list
     */
    abstract def terms()

    /**
     * Get all annotation terms id
     * @return Terms id list
     */
    abstract def termsId()

    /**
     * Check if its an algo annotation
     */
    abstract boolean isAlgoAnnotation()

    /**
     * Check if its a review annotation
     */
    abstract boolean isReviewedAnnotation()

    /**
     * Get all terms for automatic review
     * If review is done "for all" (without manual user control), we add these term to the new review annotation
     * @return
     */
    abstract List<Term> termsForReview()

    String toString() {return "Annotation " + id}

    def getFilename() {
        return this.image?.baseImage?.getFilename()
    }

    def retrieveAreaUnit() {
        GisUtils.retrieveUnit(areaUnit)
    }

    def retrievePerimeterUnit() {
        GisUtils.retrieveUnit(perimeterUnit)
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project;
    }

    def computeGIS() {
        if (this.image.physicalSizeX == null) {
            area = Math.round(this.location.getArea())
            areaUnit = GisUtils.PIXELS2v

            perimeter = Math.round(this.location.getLength())
            perimeterUnit = GisUtils.PIXELv
        }
        else {
            area = this.location.getArea() * image.physicalSizeX * image.physicalSizeX
            areaUnit = GisUtils.MICRON2v

            perimeter = this.location.getLength() * image.physicalSizeX / 1000
            perimeterUnit = GisUtils.MMv
        }
    }

    def getCentroid() {
        def centroid = location.getCentroid()
        return [x: centroid.x, y: centroid.y]
    }
/*
    def toCropURL(params=[:]) {
        def boundaries = retrieveCropParams(params)
        return UrlApi.getCropURL(image.baseImage.id, boundaries, boundaries.format)
    }

    def toCropParams(params=[:]) {
        def boundaries = retrieveCropParams(params)
        def parameters = boundaries
        parameters.id = image.baseImage.id
        parameters.format = params.format
        return parameters
    }

    def urlImageServerCrop(def abstractImageService) {
        def params = toCropParams()
        URL url = new URL(toCropURL())
        String urlCrop = abstractImageService.crop(params, url.query)
        return urlCrop
    }

    public LinkedHashMap<String, Integer> retrieveCropParams(params) {
// In the window service, boundaries are already set and do not correspond to geometry/location boundaries
        def geometry = location

        def boundaries = params.boundaries
        if (!boundaries && geometry) {
            boundaries = GeometryUtils.getGeometryBoundaries(geometry)
        }

        boundaries.imageWidth = image.baseImage.getWidth()
        boundaries.imageHeight = image.baseImage.getHeight()


        if (params.format) boundaries.format = params.format
        else boundaries.format = "png"

        if (params.format) boundaries.format = params.format
        else boundaries.format = "png"

        if (params.zoom) boundaries.zoom = params.zoom
        if (params.maxSize) boundaries.maxSize = params.maxSize
        if (params.draw) {
            boundaries.draw = true
            boundaries.location = location.toText()
            if(params.color) boundaries.color = params.color
            if(params.thickness) boundaries.thickness = params.thickness
        }
        if (params.get('increaseArea')) {
            boundaries.increaseArea = params.get('increaseArea')
        }
        if(params.square) {
            boundaries.square = params.square
        }

        if (params.mask) {
            boundaries.mask = true
            boundaries.location = location.toText()
        }
        if (params.alphaMask) {
            boundaries.alphaMask = true
            boundaries.location = location.toText()
            boundaries.format = "png"
        }

        if(location instanceof com.vividsolutions.jts.geom.Point && !params.point.equals("false")) {
            boundaries.point = true
        }

        boolean complete = Boolean.parseBoolean(params.complete)
        if (complete && geometry)
            boundaries.location = simplifyGeometryService.reduceGeometryPrecision(location)
        else if (geometry)
            boundaries.location = simplifyGeometryService.simplifyPolygonForCrop(location)

        boundaries.location = boundaries.location.toText()

        if (params.colormap) boundaries.colormap = params.colormap
        if (params.inverse) boundaries.inverse = params.inverse
        if (params.bits) boundaries.bits = params.bits
        if (params.contrast) boundaries.contrast = params.contrast
        if (params.gamma) boundaries.gamma = params.gamma

        boundaries
    }*/

    def getCallBack() {
        return [annotationID: this.id, imageID: this.image.id]
    }

    /**
     * Get user/algo/reviewed annotation with id
     * Check the correct type and return it
     * @param id Annotation id
     * @return Annotation
     */
    public static AnnotationDomain getAnnotationDomain(String id, String className = null) {
        try {
            getAnnotationDomain(Long.parseLong(id), className)
        } catch(NumberFormatException e) {
            throw new ObjectNotFoundException("Annotation ${id} not found")
        }
    }

    /**
     * Get user/algo/reviewed annotation with id
     * Check the correct type and return it
     * @param id Annotation id
     * @return Annotation
     */
    public static AnnotationDomain getAnnotationDomain(long id, String className = null) {
        def domain = null
        switch (className) {
            case "be.cytomine.ontology.UserAnnotation":
                domain = UserAnnotation
                break
            case "be.cytomine.ontology.AlgoAnnotation":
                domain = AlgoAnnotation
                break
            case "be.cytomine.ontology.ReviewedAnnotation":
                domain = ReviewedAnnotation
                break
            case "be.cytomine.processing.RoiAnnotation":
                domain = RoiAnnotation
                break
        }

        AnnotationDomain annotation
        if (domain) {
            annotation = domain.read(id)
        }
        else {
            annotation = UserAnnotation.read(id)
            if (!annotation) annotation = AlgoAnnotation.read(id)
            if (!annotation) annotation = ReviewedAnnotation.read(id)
            if (!annotation) annotation = RoiAnnotation.read(id)
        }

        if (annotation) {
            return annotation
        }
        else {
            throw new ObjectNotFoundException("Annotation ${id} not found")
        }
    }

    public void makeValid() {
        String backupLocation = this.location.toText()
        Geometry geom = new WKTReader().read(this.location.toText())
        Geometry validGeom
        String type = geom.getGeometryType().toUpperCase()

        if (!geom.isValid()) {
            log.info "Geometry is not valid"
            //selfintersect,...
            validGeom = geom.buffer(0)
            this.location = validGeom
            this.wktLocation = validGeom.toText()
            geom = new WKTReader().read(this.location.toText())
            type = geom.getGeometryType().toUpperCase()

            if(!geom.isValid() || geom.isEmpty()) {
                //if not valid after buffer(0) or empty after buffer 0
                //user_image already filter nested image
                def sql = new Sql(dataSource)
                log.info "Geometry is not valid, even after a buffer(0)!"
                log.info "SELECT ST_AsText(ST_MakeValid(ST_AsText('"+backupLocation+"')))"
                sql.eachRow("SELECT ST_AsText(ST_MakeValid(ST_AsText('"+backupLocation+"')))") {
                    String text = it[0]
                    geom = new WKTReader().read(text)
                    type = geom.getGeometryType().toUpperCase()

                    if(type.equals("GEOMETRYCOLLECTION")) {
                        geom = geom.getGeometryN(0)
                        type = geom.getGeometryType().toUpperCase()
                    }

                    this.location = geom
                    this.wktLocation = geom.toText()
                }
                sql.close()
            }
        }

        if (geom.isEmpty()) {
            log.info "Geometry is empty"
            //empty polygon,...
            throw new WrongArgumentException("${geom.toText()} is an empty geometry!")
        }

        //for geometrycollection, we may take first collection element
        if (type.equals("MULTILINESTRING") || type.equals("GEOMETRYCOLLECTION")) {
            //geometry collection, take first elem
            throw new WrongArgumentException("${geom.getGeometryType()} is not a valid geometry type!")
        }
    }

    static def getDataFromDomain(AnnotationDomain domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)

        returnArray['slice'] = domain?.slice?.id
        returnArray['image'] = domain?.image?.id
        returnArray['project'] = domain?.project?.id
        returnArray['user'] = domain?.user?.id

        returnArray['location'] = domain?.location?.toString()
        returnArray['geometryCompression'] = domain?.geometryCompression
        returnArray['centroid'] = domain?.getCentroid()

        returnArray['area'] = domain?.area
        returnArray['areaUnit'] = domain?.retrieveAreaUnit()
        returnArray['perimeter'] = domain?.perimeter
        returnArray['perimeterUnit'] = domain?.retrievePerimeterUnit()

        returnArray['term'] = domain?.termsId()
        returnArray['nbComments'] = domain?.countComments

        return returnArray
    }
}
