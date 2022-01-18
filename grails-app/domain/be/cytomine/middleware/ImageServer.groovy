package be.cytomine.middleware

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
import be.cytomine.utils.JSONUtils
import grails.util.Holders
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Image server", description = "An image server (IMS) instance")
class ImageServer extends CytomineDomain {
    /* TODO: Allow DNS sharding by linking several urls to an image server */

    @RestApiObjectField(description = "A user friendly name for IMS instance.", mandatory = false)
    String name

    @RestApiObjectField(description = "The URL of the image server instance")
    String url

    @RestApiObjectField(description = "The base path used by the image server")
    String basePath

    @RestApiObjectField(description = "A flag for the server availability")
    Boolean available

    static constraints = {
        name blank: false
        url blank: false //unique ?
        basePath(nullable: true) // It shouldn't be nullable but required for already encoded data.
        available nullable: false
    }

    static mapping = {
        cache true
    }

    static ImageServer insertDataIntoDomain(def json, def domain = new ImageServer()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.name = JSONUtils.getJSONAttrStr(json, 'name', true)
        domain.url = JSONUtils.getJSONAttrStr(json, 'url', true)
        domain.basePath = JSONUtils.getJSONAttrStr(json, 'basePath', true)
        domain.available = JSONUtils.getJSONAttrBoolean(json, 'available', true)

        return domain
    }

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['url'] = domain?.url
        returnArray['basePath'] = domain?.basePath
        returnArray['available'] = domain?.available
        return returnArray
    }

    CytomineDomain container() {
        this
    }

    String getInternalUrl() {
        return (Holders.config.grails.useHTTPInternally) ? this.url.replace("https", "http") : this.url;
    }
}
