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

package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Created by laurent on 06.02.17.
 */
@RestApiObject(name = "Image Group HDF5", description = "A group of images from the same source with different dimension and HDF5 support")

class ImageGroupHDF5  extends CytomineDomain implements  Serializable {

    public static int NOTLAUNCH = 0
    public static int RUNNING = 2
    public static int SUCCESS = 3
    public static int FAILED = 4

    @RestApiObjectField(description = "The image group")
    ImageGroup group

    @RestApiObjectField(description = "The filename for the HDF5 file")
    String filename

    @RestApiObjectField(description = "The conversion progression (from 0 to 100)", mandatory = false)
    int progress = 0

    @RestApiObjectField(description = "The conversion status (NOTLAUNCH = 0, RUNNING = 1, SUCCESS = 2, FAILED = 3)", mandatory = false)
    int status = 0

    static mapping = {
        id generator: "assigned"
        sort "id"
    }

    static constraints = {
        filename nullable: false
        progress(min: 0, max: 100)
        status(range:0..4)
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        ImageGroupHDF5.withNewSession {
            ImageGroupHDF5 imageAlreadyExist = ImageGroupHDF5.findByGroup(group)
            if (imageAlreadyExist != null && (imageAlreadyExist.id != id)) {
                throw new AlreadyExistException("ImageGroupHDF5 with group $group already exists")
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ImageGroupHDF5 insertDataIntoDomain(def json, def domain = new ImageGroupHDF5()) {
        domain.group = JSONUtils.getJSONAttrDomain(json, "group", new ImageGroup(), true)
        domain.filename = JSONUtils.getJSONAttrStr(json, "filename")
        domain.progress = JSONUtils.getJSONAttrInteger(json, "progress", 0)
        domain.status = JSONUtils.getJSONAttrInteger(json, "status", 0)
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['group'] = domain?.group?.id
        returnArray['filename'] = domain?.filename
        returnArray['progress'] = domain?.progress
        returnArray['status'] = domain?.status
        return returnArray
    }

    public CytomineDomain container() {
        return group.container()
    }
}
