package be.cytomine.image.hv

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
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.ontology.Ontology
import be.cytomine.project.Discipline
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

class HVMetadata extends CytomineDomain implements Serializable {

    static enum Type {
        LABORATORY, STAINING, ANTIBODY, DETECTION, DILUTION, INSTRUMENT
    }

    String value

    Type type// = Type.CLASSIC;

    static constraints = {
        value(maxSize: 150, /*unique: true, */blank: false)
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        HVMetadata.withNewSession {
            HVMetadata metadataAlreadyExist = HVMetadata.findByTypeAndValue(type, value)
            if(metadataAlreadyExist && (metadataAlreadyExist.id!=id))  throw new AlreadyExistException("Metadata "+metadataAlreadyExist?.value + " already exist!")
        }
    }

    static mapping = {
        id generator: "assigned"
        sort "id"
        cache true
    }

    String toString() {
        type+" "+name
    }

    static HVMetadata insertDataIntoDomain(def json, def domain = new HVMetadata()) {

        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.value = JSONUtils.getJSONAttrStr(json, 'value',true)

        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.type = Type.valueOf(JSONUtils.getJSONAttrStr(json, 'type',true))
        //if(JSONUtils.getJSONAttrBoolean(json, 'isRestricted', false)) domain.mode = EditingMode.RESTRICTED;
        //if(JSONUtils.getJSONAttrBoolean(json, 'isReadOnly', false)) domain.mode = EditingMode.READ_ONLY;

        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['value'] = domain?.value
        returnArray['type'] = domain?.type.toString()
        //returnArray['retrievalDisable'] = domain?.retrievalDisable
        //returnArray['retrievalAllOntology'] = domain?.retrievalAllOntology

        /*if(domain?.mode.equals(EditingMode.READ_ONLY)){
            returnArray['isReadOnly'] = true
        } else if(domain?.mode.equals(EditingMode.RESTRICTED)){
            returnArray['isRestricted'] = true
        }*/

        return returnArray
    }


    public boolean equals(Object o) {
        if (!o) {
            return false
        } else {
            try {
                return ((HVMetadata) o).getId() == this.getId()
            } catch (Exception e) {
                return false
            }
        }
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return this;
    }

}
