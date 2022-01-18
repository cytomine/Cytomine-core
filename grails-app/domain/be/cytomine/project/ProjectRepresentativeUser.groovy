package be.cytomine.project

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
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Project representative user", description = "A Project representative user is an user considered as the reference for the associate project")
class ProjectRepresentativeUser extends CytomineDomain {

    @RestApiObjectField(description = "The user considered as the reference")
    User user
    @RestApiObjectField(description = "The project")
    Project project

    static belongsTo = [project: Project, user:User]

    static constraints = {
        user(nullable: false)
        project(nullable: false)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        withNewSession {
            ProjectRepresentativeUser ref = ProjectRepresentativeUser.findByProjectAndUser(project, user)
            if(ref!=null && (ref.id!=id))  {
                throw new AlreadyExistException("ProjectRepresentativeUser already exist!")
            }
        }
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {

        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['project'] = domain?.project?.id
        returnArray['user'] = domain?.user?.id

        return returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ProjectRepresentativeUser insertDataIntoDomain(def json,def domain = new ProjectRepresentativeUser()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        return domain;
    }


    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }

}