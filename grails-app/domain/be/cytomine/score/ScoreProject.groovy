package be.cytomine.score

import be.cytomine.CytomineDomain

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.processing.Software
import be.cytomine.project.Project
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * A link between a score and a project
 * We can add a score to many projects
 */
@RestApiObject(name = "score project", description = "A link between a project and a score.")
class ScoreProject extends CytomineDomain implements Serializable{

    @RestApiObjectField(description = "The score")
    Score score

    @RestApiObjectField(description = "The project")
    Project project

    static transients = []

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ScoreProject insertDataIntoDomain(def json, def domain=new ScoreProject()) {
        try {
            domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
            domain.score = JSONUtils.getJSONAttrDomain(json.score, "id", new Score(), true)
            domain.project = JSONUtils.getJSONAttrDomain(json.project, "id", new Project(), true)
        }
        catch (Exception e) {
            domain.score = JSONUtils.getJSONAttrDomain(json, "score", new Score(), true)
            domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        }
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['score'] = domain?.score?.id
        returnArray['name'] = domain?.score?.name
        returnArray['project'] = domain?.project?.id
        returnArray['values'] = domain?.score?.values?.sort {a,b -> a.index <=> b.index}?.collect{ScoreValue.getDataFromDomain(it)}
        return returnArray
    }


    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }
}
