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
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission

/**
 * Software is an application that can read/add/update/delete data from cytomine
 * Each time a software is launch, we create a job instance
 */
@RestApiObject(name = "Software", description = "Software is an application that can read/add/update/delete data from cytomine. Each time a software is launch, we create a job instance")
class Software extends CytomineDomain {

    /**
     * Application name
     */
    @RestApiObjectField(description = "The software name")
    String name

    @RestApiObjectField(description = "The software's software user repository")
    SoftwareUserRepository softwareUserRepository

    @RestApiObjectField(description = "The software's default processing server")
    ProcessingServer defaultProcessingServer

    /**
     * Type of result page
     * For UI client, we load a specific page for each software to print data (charts, listing,...)
     */
    @RestApiObjectField(description = "For UI client: Type of result page. We load a specific page for each software to print data (charts, listing,...)", mandatory = false)
    String resultName

    /**
     * Command to execute software
     */
    @RestApiObjectField(description = "The command used to execute the piece of software")
    String executeCommand

    @RestApiObjectField(description = "The command used to retrieve the image")
    String pullingCommand

    @RestApiObjectField(description = "Flag used to identify the validity of a piece of software")
    Boolean deprecated

    @RestApiObjectField(description = "The version")
    String softwareVersion

    @RestApiObjectField(description = "The path of the source files of the software")
    String sourcePath

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "fullName", description = "Full name, including version.", allowedType = "string", useForCreation = false),
        @RestApiObjectField(apiFieldName = "executable", description = "True if it can be executed by Cytomine", allowedType = "boolean", useForCreation = false),
        @RestApiObjectField(apiFieldName = "parameters", description = "List of 'software parameter' for this software (sort by index asc)",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfJob", description = "The number of job for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfKilled", description = "The number of job killed for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfNotLaunch", description = "The number of job not launch for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfInQueue", description = "The number of job in queue for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfRunning", description = "The number of job currently running for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfSuccess", description = "The number of job finished with success for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfFailed", description = "The number of job failed for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfIndeterminate", description = "The number of job in indeterminate status for this software",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfWait", description = "The number of job waiting for this software",allowedType = "long",useForCreation = false),
    ])
    static transients = []

    static belongsTo = [SoftwareUserRepository]

    static constraints = {
        name(nullable: false, unique: false)
        softwareVersion(nullable: true, unique: false)
        resultName(nullable:true)
        executeCommand(nullable: true, maxSize: 5000)
        defaultProcessingServer(nullable: true)
        deprecated(nullable: true)
        pullingCommand(nullable: true)
        softwareUserRepository(nullable: true)
        sourcePath(nullable: true)
    }

    static mapping = {
        id generator: "assigned"
        sort "id"
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    @Override
    void checkAlreadyExist() {
        Software.withNewSession {
            if (name && softwareVersion) {
                Software softwareSameNameAndVersion = Software.findByNameAndSoftwareVersion(name, softwareVersion)
                if (softwareSameNameAndVersion && softwareSameNameAndVersion.id != id) {
                    throw new AlreadyExistException("Software " + softwareSameNameAndVersion.name + " " + softwareSameNameAndVersion.softwareVersion + " already exist !")
                }
            }
            else if(name) {
                Software softwareSameName = Software.findByName(name)
                if(softwareSameName && (softwareSameName.id!=id))  {
                    throw new AlreadyExistException("Software "+softwareSameName.name + " already exist!")
                }
            }
        }
    }

    String toString() {
        fullName()
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Software insertDataIntoDomain(def json, def domain = new Software()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name')
        domain.softwareUserRepository = JSONUtils.getJSONAttrDomain(json, "softwareUserRepository", new
                SoftwareUserRepository(), false)
        domain.defaultProcessingServer = JSONUtils.getJSONAttrDomain(json, "defaultProcessingServer", new ProcessingServer(), false)
        domain.resultName = JSONUtils.getJSONAttrStr(json, 'resultName')
        domain.executeCommand = JSONUtils.getJSONAttrStr(json, 'executeCommand')
        domain.pullingCommand = JSONUtils.getJSONAttrStr(json, 'pullingCommand')
        domain.deprecated = JSONUtils.getJSONAttrBoolean(json, 'deprecated', false)
        domain.softwareVersion = JSONUtils.getJSONAttrStr(json, 'softwareVersion')
        domain.sourcePath= JSONUtils.getJSONAttrStr(json, 'sourcePath')
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
        returnArray['softwareUserRepository'] = domain?.softwareUserRepository?.id
        returnArray['defaultProcessingServer'] = domain?.defaultProcessingServer?.id
        returnArray['resultName'] = domain?.resultName
        returnArray['executeCommand'] = domain?.executeCommand
        returnArray['pullingCommand'] = domain?.pullingCommand
        returnArray['deprecated'] = domain?.deprecated
        returnArray['softwareVersion'] = domain?.softwareVersion
        returnArray['sourcePath'] = domain?.sourcePath
        returnArray['fullName'] = domain?.fullName()
        returnArray['executable'] = domain?.executable()
        try {
            returnArray['parameters'] = SoftwareParameter.findAllBySoftwareAndSetByServer(domain, false, [sort : "index", order : "asc"])
            returnArray['numberOfJob'] = Job.countBySoftware(domain)
            returnArray['numberOfNotLaunch'] = Job.countBySoftwareAndStatus(domain,Job.NOTLAUNCH)
            returnArray['numberOfInQueue'] = Job.countBySoftwareAndStatus(domain,Job.INQUEUE)
            returnArray['numberOfRunning'] = Job.countBySoftwareAndStatus(domain,Job.RUNNING)
            returnArray['numberOfSuccess'] = Job.countBySoftwareAndStatus(domain,Job.SUCCESS)
            returnArray['numberOfFailed'] = Job.countBySoftwareAndStatus(domain,Job.FAILED)
            returnArray['numberOfIndeterminate'] = Job.countBySoftwareAndStatus(domain,Job.INDETERMINATE)
            returnArray['numberOfWait'] = Job.countBySoftwareAndStatus(domain,Job.WAIT)
            returnArray['numberOfKilled'] = Job.countBySoftwareAndStatus(domain,Job.KILLED)
        } catch(Exception e) { }
        return returnArray
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return this;
    }

    def executable() {
        return this.executeCommand?.trim() && this.pullingCommand?.trim()
    }

    def fullName() {
        if (this.softwareVersion?.trim())
            return "${this.name} (${this.softwareVersion})"

        return this.name;
    }

    @Override
    boolean checkPermission(Permission permission, boolean isAdmin) {
        if (permission == BasePermission.READ) {
            return true
        }
        return super.checkPermission(permission, isAdmin)
    }
}
