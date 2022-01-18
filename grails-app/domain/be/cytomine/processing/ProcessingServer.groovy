package be.cytomine.processing

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
import be.cytomine.middleware.AmqpQueue
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Processing server", description = "A processing server is a server that can be used to run algorithms (containers)")
class ProcessingServer extends CytomineDomain {

    @RestApiObjectField(description = "The name of the processing server")
    String name

    @RestApiObjectField(description = "The host of the processing server")
    String host

    @RestApiObjectField(description = "The user of the processing server")
    String username

    @RestApiObjectField(description = "The port of the processing server")
    Integer port

    @RestApiObjectField(description = "The type of the processing server (cpu, gpu, ...)")
    String type

    @RestApiObjectField(description = "The processing method name of the processing server")
    String processingMethodName

    @RestApiObjectField(description = "The amqp queue associated to a given processing server")
    AmqpQueue amqpQueue

    @RestApiObjectField(description = "The absolute directory path for persistent data, on the processing server")
    String persistentDirectory

    @RestApiObjectField(description = "The absolute directory path for temporary data, on the processing server")
    String workingDirectory = ""

    @RestApiObjectField(description = "The index in the default processing server table. Lowest is the default processing server")
    Integer index

    // Most of parameters must be nullable to accept rename of old ProcessingServer to ImagingServer
    static constraints = {
        name(nullable: false, blank: false, unique: true)
        host(blank: false)
        username(blank: false, nullable: false)
        port(nullable: false)
        type(nullable: true)
        processingMethodName(blank: false, nullable: true)
        amqpQueue(nullable: true)
        persistentDirectory(blank: true, nullable: true)
        workingDirectory(blank: true, nullable: true)
    }

    static mapping = {
        id(generator: "assigned")
        sort("index")
        host(defaultValue: "'localhost'")
        port(defaultValue: "22")
//        index(defaultValue: "10")
    }

    @Override
    void checkAlreadyExist() {
        ProcessingServer.withNewSession {
            if (name) {
                ProcessingServer processingServer = ProcessingServer.findByName(name)
                if (processingServer != null && processingServer.id != id) {
                    throw new AlreadyExistException("Processing server ${processingServer.name} + already exists !")
                }
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ProcessingServer insertDataIntoDomain(def json, def domain = new ProcessingServer()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name', true)
        domain.host = JSONUtils.getJSONAttrStr(json, 'host', true)
        domain.username = JSONUtils.getJSONAttrStr(json, 'username', true)
        domain.port = JSONUtils.getJSONAttrInteger(json, 'port', null)
        domain.type = JSONUtils.getJSONAttrStr(json, 'type', false)
        domain.processingMethodName = JSONUtils.getJSONAttrStr(json, 'processingMethodName', false)
        domain.amqpQueue = JSONUtils.getJSONAttrDomain(json, 'amqpQueue', new AmqpQueue(), false)
        domain.persistentDirectory = JSONUtils.getJSONAttrStr(json, 'persistentDirectory', false)
        domain.workingDirectory = JSONUtils.getJSONAttrStr(json, 'workingDirectory', false)
        domain.index = JSONUtils.getJSONAttrInteger(json, 'index', 10)
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['host'] = domain?.host
        returnArray['username'] = domain?.username
        returnArray['port'] = domain?.port
        returnArray['type'] = domain?.type
        returnArray['processingMethodName'] = domain?.processingMethodName
        returnArray['amqpQueue'] = domain?.amqpQueue
        returnArray['persistentDirectory'] = domain?.persistentDirectory
        returnArray['workingDirectory'] = domain?.workingDirectory
        returnArray['index'] = domain?.index
        return returnArray
    }

}
