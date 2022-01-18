package be.cytomine.image

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
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields


@RestApiObject(name = "Image instance", description = "A link between 'abstract image' and 'project'. An 'abstract image' may be in multiple projects.")
class ImageInstance extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The image linked to the project")
    AbstractImage baseImage

    @RestApiObjectField(description = "The project that keeps the image")
    Project project

    @RestApiObjectField(description = "The user that add the image to the project")
    SecUser user

    @RestApiObjectField(description = "Instance image filename", useForCreation = false)
    String instanceFilename

    // ----- Annotation counts
    @RestApiObjectField(description = "The number of user annotation in the image", useForCreation = false, apiFieldName = "numberOfAnnotations")
    Long countImageAnnotations = 0L

    @RestApiObjectField(description = "The number of job annotation in the image", useForCreation = false, apiFieldName = "numberOfJobAnnotations")
    Long countImageJobAnnotations = 0L

    @RestApiObjectField(description = "The number of reviewed annotation in the image", useForCreation = false, apiFieldName = "numberOfReviewedAnnotations")
    Long countImageReviewedAnnotations = 0L


    // ----- Image review
    @RestApiObjectField(description = "The start review date", useForCreation = false)
    Date reviewStart

    @RestApiObjectField(description = "The stop review date", useForCreation = false)
    Date reviewStop

    @RestApiObjectField(description = "The user who reviewed (or still reviewing) this image", useForCreation = false)
    SecUser reviewUser

    @RestApiObjectField(description = "The image max zoom")
    Integer magnification

    @RestApiObjectField(description = "Physical size of a pixel along X axis", mandatory = false)
    Double physicalSizeX

    @RestApiObjectField(description = "Physical size of a pixel along Y axis", mandatory = false)
    Double physicalSizeY

    @RestApiObjectField(description = "Physical size of a pixel along Z axis", mandatory = false)
    Double physicalSizeZ

    @RestApiObjectField(description = "The number of frames per second", mandatory = false)
    Double fps

    @RestApiObjectFields(params = [
            @RestApiObjectField(apiFieldName = "filename", description = "Similar to originalFilename", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "originalFilename", description = "Original filename", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "sample", description = "Abstract image sample (see Abstract Image)", allowedType = "long", useForCreation = false),
            @RestApiObjectField(apiFieldName = "width", description = "Abstract image width (see Abstract Image)", allowedType = "int", useForCreation = false),
            @RestApiObjectField(apiFieldName = "height", description = "Abstract image height (see Abstract Image)", allowedType = "int", useForCreation = false),
            @RestApiObjectField(apiFieldName = "reviewed", description = "Image has been reviewed", allowedType = "boolean", useForCreation = false),
            @RestApiObjectField(apiFieldName = "inReview", description = "Image currently reviewed", allowedType = "boolean", useForCreation = false),
            @RestApiObjectField(apiFieldName = "thumb", description = "URL to get abstract image short view (thumb)",allowedType = "string",useForCreation = false),
            @RestApiObjectField(apiFieldName = "path", description = "The internal path of the file", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "contentType", description = "The image content type", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "zoom", description = "The number of zooms available in the image", allowedType = "int", useForCreation = false),
            @RestApiObjectField(apiFieldName = "macroURL", description = "URL to get image macros", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "sample", description = "The source of the image (human, animal,...)", allowedType = "long", mandatory = false),
            @RestApiObjectField(apiFieldName = "width", description = "The N-dimensional image width, in pixels (X)", allowedType = "int", useForCreation = false, mandatory = false, defaultValue = "-1"),
            @RestApiObjectField(apiFieldName = "height", description = "The N-dimensional image height, in pixels (Y)", allowedType = "int", useForCreation = false, mandatory = false, defaultValue = "-1"),
            @RestApiObjectField(apiFieldName = "depth", description = "The N-dimensional image depth, in z-slices (Z)", allowedType = "int", useForCreation = false, mandatory = false, defaultValue = "1"),
            @RestApiObjectField(apiFieldName = "duration", description = "The N-dimensional image duration, in frames (T)", allowedType = "int", useForCreation = false, mandatory = false, defaultValue = "1"),
            @RestApiObjectField(apiFieldName = "channels", description = "The N-dimensional image channels (C)", allowedType = "int", useForCreation = false, mandatory = false, defaultValue = "1"),
            @RestApiObjectField(apiFieldName = "bitPerSample", description = "The number of bits per sample (color)", allowedType = "int", useForCreation = false, mandatory = false),
            @RestApiObjectField(apiFieldName = "samplePerPixel", description = "The number of samples (colors) per pixel", allowedType = "int", useForCreation = false, mandatory = false),
            @RestApiObjectField(apiFieldName = "colorspace", description = "The image colorspace", allowedType = "string", useForCreation = false, mandatory = false),
            @RestApiObjectField(apiFieldName = "preview", description = "URL to get image preview", allowedType = "string", useForCreation = false),
    ])
    static transients = []

    static belongsTo = [AbstractImage, Project, User]

    static constraints = {
        baseImage(unique: ['project'])
        countImageAnnotations nullable: true
        reviewStart nullable: true
        reviewStop nullable: true
        reviewUser nullable: true
        instanceFilename nullable: true
        magnification nullable: true
        physicalSizeX nullable: true
        physicalSizeY nullable: true
        physicalSizeZ nullable: true
        fps nullable: true
    }

    static mapping = {
        id generator: "assigned"
        baseImage fetch: 'join'
        sort "id"
        tablePerHierarchy true
        cache true
        physicalSizeX column: "physical_size_x"
        physicalSizeY column: "physical_size_y"
        physicalSizeZ column: "physical_size_z"
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        withNewSession {
            ImageInstance imageAlreadyExist = ImageInstance.findByBaseImageAndProject(baseImage, project)
            if (imageAlreadyExist != null && (imageAlreadyExist.id != id)) {
                throw new AlreadyExistException("Image " + baseImage?.filename + " already map with project " + project.name)
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ImageInstance insertDataIntoDomain(def json, def domain = new ImageInstance()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.created = JSONUtils.getJSONAttrDate(json, "created")
        domain.updated = JSONUtils.getJSONAttrDate(json, "updated")
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), false)
        domain.baseImage = JSONUtils.getJSONAttrDomain(json, "baseImage", new AbstractImage(), false)
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), false)
        domain.instanceFilename = JSONUtils.getJSONAttrStr(json, "instanceFilename", false)

        domain.reviewStart = JSONUtils.getJSONAttrDate(json, "reviewStart")
        domain.reviewStop = JSONUtils.getJSONAttrDate(json, "reviewStop")
        domain.reviewUser = JSONUtils.getJSONAttrDomain(json, "reviewUser", new User(), false)

        domain.magnification = JSONUtils.getJSONAttrInteger(json,'magnification',null)
        domain.physicalSizeX = JSONUtils.getJSONAttrDouble(json, "physicalSizeX", null)
        domain.physicalSizeY = JSONUtils.getJSONAttrDouble(json, "physicalSizeY", null)
        domain.physicalSizeZ = JSONUtils.getJSONAttrDouble(json, "physicalSizeZ", null)
        domain.fps = JSONUtils.getJSONAttrDouble(json, "fps", null)

        //Check review constraint
        if ((domain.reviewUser == null && domain.reviewStart != null)
                || (domain.reviewUser != null && domain.reviewStart == null)
                || (domain.reviewStart == null && domain.reviewStop != null)) {
            throw new WrongArgumentException("Review data are not valid: user=${domain.reviewUser} " +
                    "start=${domain.reviewStart} stop=${domain.reviewStop}")
        }

        domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def image) {

        def returnArray = CytomineDomain.getDataFromDomain(image)
        returnArray['baseImage'] = image?.baseImage?.id
        returnArray['project'] = image?.project?.id
        returnArray['user'] = image?.user?.id
        returnArray['instanceFilename'] = image?.blindInstanceFilename

        returnArray['originalFilename'] = image?.blindOriginalFilename
        returnArray['filename'] = image?.baseImage?.filename
        returnArray['blindedName'] = image?.getBlindedName()
        returnArray['path'] = image?.baseImage?.path
        returnArray['contentType'] = image?.baseImage?.uploadedFile?.contentType
        returnArray['sample'] = image?.baseImage?.sample?.id

        returnArray['width'] = image?.baseImage?.width
        returnArray['height'] = image?.baseImage?.height
        returnArray['depth'] = image?.baseImage?.depth // /!!\ Breaking API : image?.baseImage?.getZoomLevels()?.max
        returnArray['duration'] = image?.baseImage?.duration
        returnArray['channels'] = image?.baseImage?.channels

        returnArray['physicalSizeX'] = image?.physicalSizeX
        returnArray['physicalSizeY'] = image?.physicalSizeY
        returnArray['physicalSizeZ'] = image?.physicalSizeZ
        returnArray['fps'] = image?.fps

        returnArray['zoom'] = image?.baseImage?.getZoomLevels()
        returnArray['magnification'] = image?.magnification
        returnArray['bitDepth'] = image?.baseImage?.bitDepth
        returnArray['colorspace'] = image?.baseImage?.colorspace

        returnArray['reviewStart'] =  image?.reviewStart?.time?.toString()
        returnArray['reviewStop'] = image?.reviewStop?.time?.toString()
        returnArray['reviewUser'] = image?.reviewUser?.id
        returnArray['reviewed'] = image?.isReviewed()
        returnArray['inReview'] = image?.isInReviewMode()

        returnArray['numberOfAnnotations'] = image?.countImageAnnotations
        returnArray['numberOfJobAnnotations'] = image?.countImageJobAnnotations
        returnArray['numberOfReviewedAnnotations'] = image?.countImageReviewedAnnotations

        returnArray['thumb'] = UrlApi.getImageInstanceThumbUrlWithMaxSize(image?.id, 512)
        returnArray['preview'] = UrlApi.getImageInstanceThumbUrlWithMaxSize(image?.id, 1024)
        returnArray['macroURL'] = UrlApi.getAssociatedImageInstance(image?.id, "macro", image?.baseImage?.uploadedFile?.contentType, 512)
        return returnArray
    }

    def getSliceCoordinates() {
        return this.baseImage?.getSliceCoordinates()
    }

    def getReferenceSliceCoordinate() {
        return this.baseImage?.getReferenceSliceCoordinate()
    }

    def getReferenceSlice() {
        def base = this.baseImage?.getReferenceSlice()
        return SliceInstance.findByBaseSliceAndImage(base, this)
    }

    /**
     * Flag to control if image is beeing review, and not yet validated
     * @return True if image is review but not validate, otherwise false
     */
    public boolean isInReviewMode() {
        return (reviewStart != null && reviewUser != null && reviewStop == null)
    }

    /**
     * Flag to control if image is validated
     * @return True if review user has validate this image
     */
    public boolean isReviewed() {
        return (reviewStop != null)
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }

    /**
     * Return domain user (annotation user, image user...)
     * By default, a domain has no user.
     * You need to override userDomainCreator() in domain class
     * @return Domain user
     */
    @Override
    public SecUser userDomainCreator() {
        return user
    }

    String getBlindOriginalFilename() {
        if (project?.blindMode)
            return "${baseImage?.id}"
        return baseImage?.originalFilename
    }

    String getBlindInstanceFilename() {
        if (project?.blindMode)
            return "${baseImage?.id}"
        else if (instanceFilename && instanceFilename?.trim() != '')
            return instanceFilename
        else
            return baseImage?.originalFilename
    }

    private String getBlindedName(){
        if(project?.blindMode) return baseImage.id
        return null
    }

    Double getPhysicalSizeX() {
        if (physicalSizeX != null && physicalSizeX != 0) {
            return physicalSizeX
        }
        return baseImage.physicalSizeX
    }

    Double getPhysicalSizeY() {
        if (physicalSizeY != null && physicalSizeY != 0) {
            return physicalSizeY
        }
        return baseImage.physicalSizeY
    }

    Double getPhysicalSizeZ() {
        if (physicalSizeZ != null && physicalSizeZ != 0) {
            return physicalSizeZ
        }
        return baseImage.physicalSizeZ
    }

    public Double getFps() {
        if (fps != null && fps != 0) {
            return fps
        }
        return baseImage.fps
    }

    public Integer getMagnification() {
        if (magnification != null && magnification != 0) {
            return magnification
        }
        return baseImage.magnification
    }
}