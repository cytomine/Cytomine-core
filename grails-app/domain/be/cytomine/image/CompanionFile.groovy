package be.cytomine.image

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

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

@RestApiObject(name = "Companion file", description = "A secondary file related to an abstract image")
class CompanionFile extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The underlying file")
    UploadedFile uploadedFile

    @RestApiObjectField(description = "The abstract image using this file")
    AbstractImage image

    @RestApiObjectField(description = "The original filename from the upload")
    String originalFilename

    @RestApiObjectField(description = "A user-friendly filename")
    String filename

    @RestApiObjectField(description = "The type of file")
    String type

    @RestApiObjectField(description = "The file conversion progress", mandatory = false)
    Integer progress = 0

    @RestApiObjectFields(params = [
            @RestApiObjectField(apiFieldName = "path", description = "The internal path of the file", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "status", description = "File status", allowedType = "int", useForCreation = false),
            @RestApiObjectField(apiFieldName = "statusText", description = "Textual file status", allowedType = "string", useForCreation = false)
    ])
    static transients = []

    static belongsTo = [AbstractImage, UploadedFile]

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort('id')
        cache(false)
        uploadedFile fetch: 'join', cache: false
        image fetch: 'join', cache: false
    }

    static constraints = {
        originalFilename(blank: false)
        filename(blank: false)
        progress(nullable: true)
    }

    def getPath() {
        return uploadedFile?.path
    }

    void checkAlreadyExist() {
        withNewSession {
            CompanionFile file = CompanionFile.findByImageAndUploadedFile(image, uploadedFile)
            if (file?.id != id) {
                throw new AlreadyExistException("Companion file ${originalFilename} already exists for AbstractImage ${image?.id}")
            }
        }
    }

    static CompanionFile insertDataIntoDomain(def json, def domain = new CompanionFile()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.uploadedFile = JSONUtils.getJSONAttrDomain(json, "uploadedFile", new UploadedFile(), true)
        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new AbstractImage(), true)

        domain.originalFilename = JSONUtils.getJSONAttrStr(json, "originalFilename", true)
        domain.filename = JSONUtils.getJSONAttrStr(json, "filename", true)
        domain.type = JSONUtils.getJSONAttrStr(json, "type", true)
        domain.progress = JSONUtils.getJSONAttrInteger(json, "progress", 0)

        domain
    }

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['uploadedFile'] = domain?.uploadedFile?.id
        returnArray['path'] = domain?.path
        returnArray['image'] = domain?.image?.id
        returnArray['originalFilename'] = domain?.originalFilename
        returnArray['filename'] = domain?.filename
        returnArray['type'] = domain?.type

        returnArray['progress'] = domain?.progress
        returnArray['status'] = domain?.uploadedFile?.status
        returnArray['statusText'] = domain?.uploadedFile?.statusText

        returnArray
    }

    CytomineDomain[] containers() {
        return image?.containers()
    }

    def getImageServerUrl() {
        return uploadedFile?.imageServer?.url
    }

    def getImageServerInternalUrl() {
        return uploadedFile?.imageServer?.internalUrl
    }
}
