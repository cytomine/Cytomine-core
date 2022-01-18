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
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.middleware.ImageServer
import be.cytomine.image.server.Storage
import be.cytomine.postgresql.LTreeType
import be.cytomine.security.SecUser
import be.cytomine.security.UserJob
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

import java.nio.file.Paths

/**
 * An UploadedFile is a file uploaded through the API.
 * Uploaded are temporaly instances, files related to them are placed
 * in a buffer space before being converted into the right format and copied to the storages
 */
@RestApiObject(name = "Uploaded file", description = "A file uploaded on the server")
class UploadedFile extends CytomineDomain implements Serializable {

    enum Status {
        /**
         * Even codes lower than 100 => information
         * Even codes greater or equal to 100 => success
         * Odd codes => error
         */
        UPLOADED (0),

        DETECTING_FORMAT (10),
        ERROR_FORMAT (11), // 3

        EXTRACTING_DATA (20),
        ERROR_EXTRACTION (21),

        CONVERTING (30),
        ERROR_CONVERSION (31), // 4

        DEPLOYING (40),
        ERROR_DEPLOYMENT (41), // 8

        DEPLOYED (100),
        EXTRACTED (102),
        CONVERTED (104)

        private final int code

        Status(int code) {
            this.code = code
        }

        int getCode() {
            return code
        }

        static Status findByCode(int code) {
            return values().find{ it.code == code }
        }
    }

    @RestApiObjectField(description = "The uploader")
    SecUser user

    @RestApiObjectField(description = "The virtual storage where the file is uploaded")
    Storage storage

    @RestApiObjectField(description = "List of projects (id) that will have the image, if it can be deployed")
    Long[] projects

    @RestApiObjectField(description = "The internal filename path, including extension")
    String filename

    @RestApiObjectField(description = "The original filename, including extension")
    String originalFilename

    @RestApiObjectField(description = "Extension name")
    String ext

    @RestApiObjectField(description = "The image server managing the file")
    ImageServer imageServer

    @RestApiObjectField(description = "File content type")
    String contentType

    @RestApiObjectField(description = "The parent uploaded file in the hierarchy")
    UploadedFile parent

    @RestApiObjectField(description = "File size", mandatory = false)
    Long size

    @RestApiObjectField(description = "File status", mandatory = false)
    int status = 0

    @RestApiObjectField(description = "Hierarchical tree of uploaded files", mandatory = false, presentInResponse = false)
    String lTree

    @RestApiObjectFields(params=[
            @RestApiObjectField(apiFieldName = "path", description = "The internal path of the file", allowedType = "string", useForCreation = false),
            @RestApiObjectField(apiFieldName = "statusText", description = "Textual file status", allowedType = "string", useForCreation = false),
    ])

    static belongsTo = [ImageServer, Storage]

    static mapping = {
        id(generator: 'assigned', unique: true)
        lTree(type: LTreeType, sqlType: 'ltree')
        cache(true)
    }

    static constraints = {
        projects nullable: true
        parent(nullable : true)
        lTree nullable : true //Due to DB schema update issue
        imageServer nullable: true //Due to DB schema update issue
        storage nullable: true //Due to DB schema update issue
    }

    static def getDataFromDomain(def uploaded) {
        def returnArray = CytomineDomain.getDataFromDomain(uploaded)
        returnArray['user'] = uploaded?.user?.id
        returnArray['parent'] = uploaded?.parent?.id
        returnArray['imageServer'] = uploaded?.imageServer?.id
        returnArray['storage'] = uploaded?.storage?.id

        returnArray['originalFilename'] = uploaded?.originalFilename
        returnArray['filename'] = uploaded?.filename
        returnArray['ext'] = uploaded?.ext
        returnArray['contentType'] = uploaded?.contentType
        returnArray['size'] = uploaded?.size
        returnArray['path'] = uploaded?.path

        returnArray['status'] = uploaded?.status
        returnArray['statusText'] = uploaded?.statusText

        returnArray['projects'] = uploaded?.projects
        returnArray
    }

    static UploadedFile insertDataIntoDomain(def json, def domain = new UploadedFile()) throws CytomineException {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        def user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        domain.user = (user instanceof UserJob) ? ((UserJob) user).user : user

        domain.parent = JSONUtils.getJSONAttrDomain(json, "parent", new UploadedFile(), false)
        domain.imageServer = JSONUtils.getJSONAttrDomain(json, "imageServer", new ImageServer(), true)
        domain.storage = JSONUtils.getJSONAttrDomain(json, "storage", new Storage(), true)

        domain.filename = JSONUtils.getJSONAttrStr(json,'filename')
        domain.originalFilename = JSONUtils.getJSONAttrStr(json,'originalFilename')
        domain.ext = JSONUtils.getJSONAttrStr(json,'ext')
        domain.contentType = JSONUtils.getJSONAttrStr(json,'contentType')
        domain.size = JSONUtils.getJSONAttrLong(json,'size',0)

        domain.status = JSONUtils.getJSONAttrInteger(json,'status',0)
        domain.projects = JSONUtils.getJSONAttrListLong(json,'projects')

        domain
    }

    def getStatusText() {
        return Status.findByCode(status)?.name()
    }

    def getPath() {
        if (contentType == "virtual/stack")
            return null;
        return Paths.get(imageServer?.basePath, storage.id as String, filename).toString()
    }

    def beforeInsert() {
        super.beforeInsert()
        lTree = parent ? parent.lTree+"." : ""
        lTree += id
    }

    def beforeUpdate() {
        super.beforeUpdate()
        lTree = parent ? parent.lTree+"." : ""
        lTree += id
    }

    CytomineDomain container() {
        return storage
    }
}
