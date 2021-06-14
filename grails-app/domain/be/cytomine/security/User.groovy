package be.cytomine.security

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

import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * A cytomine human user
 */
@RestApiObject(name = "User", description="A cytomine human user")
class User extends SecUser {

    transient springSecurityService

    @RestApiObjectField(description = "The firstname of the user")
    String firstname

    @RestApiObjectField(description = "The lastname of the user")
    String lastname

    @RestApiObjectField(description = "The email of the user")
    String email

    String color //deprecated

    @RestApiObjectField(description = "The language of the user")
    Language language

    @RestApiObjectField(description = "User that created this user. Its parent.")
    User creator

    String pidPseudonym;
    String phoneNumber;

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "admin", description = "(ONLY VISIBLE WHEN DOING GET /api/user/id.format service) True if the user is ADMIN ",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "user", description = "(ONLY VISIBLE WHEN DOING GET /api/user/id.format service) True if the user is NOT ADMIN and is USER ",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "ghest", description = "(ONLY VISIBLE WHEN DOING GET /api/user/id.format service) True if the user is NOT ADMIN, NOT USER but a simple GHEST ",allowedType = "boolean",useForCreation = false)
    ])
    static constraints = {
        firstname blank: false
        lastname blank: false
        language(nullable: true)
        email(blank: false, email: true)
        color(blank: false, nullable: true)
        pidPseudonym(nullable: true)
        phoneNumber(nullable: true)
    }

    static mapping = {
        id(generator: 'assigned', unique: true)
        creator column: 'user_id'
        sort "id"
        cache true
    }

    def beforeInsert() {
        super.beforeInsert()
        if (!language) {
            language = Language.ENGLISH
        }
    }

    def beforeUpdate() {
        super.beforeUpdate()
    }

    /**
     * Username of the human user back to this user
     * If User => humanUsername is username
     * If Algo => humanUsername is user that launch algo username
     */
    String humanUsername() {
        return username
    }

    String toString() {
        firstname + " " + lastname
    }

    /**
     * Check if user is a job
     */
    boolean algo() {
        return false
    }

    Set<Workplace> getWorkplaces() {
        (UserWorkplace.findAllByUser(this) as List<UserWorkplace>)*.workplace as Set<Workplace>
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */         
    static User insertDataIntoDomain(def json, def domain = new User()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.username = JSONUtils.getJSONAttrStr(json,'username')
        domain.firstname = JSONUtils.getJSONAttrStr(json,'firstname')
        domain.lastname = JSONUtils.getJSONAttrStr(json,'lastname')
        domain.email = JSONUtils.getJSONAttrStr(json,'email')
        domain.color = JSONUtils.getJSONAttrStr(json,'color')
        domain.language = Language.findByCode(JSONUtils.getJSONAttrStr(json,'language') ?: "EN")
        if(!domain.language) domain.language = Language.valueOf(JSONUtils.getJSONAttrStr(json,'language') ?: "EN")
        domain.origin = JSONUtils.getJSONAttrStr(json,'origin')
        if (json.password && domain.password != null) {
            domain.newPassword = JSONUtils.getJSONAttrStr(json,'password') //user is updated
        } else if (json.password) {
            domain.password = JSONUtils.getJSONAttrStr(json,'password') //user is created
        }
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        domain.enabled = JSONUtils.getJSONAttrBoolean(json,'enabled', true)
        domain.pidPseudonym = JSONUtils.getJSONAttrStr(json,'pidPseudonym')
        domain.phoneNumber = JSONUtils.getJSONAttrStr(json,'phoneNumber')

        if (domain.getPublicKey() == null || domain.getPrivateKey() == null || json.publicKey == "" || json.privateKey == "") {
            domain.generateKeys()
        }
        return domain;
    }    

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = SecUser.getDataFromDomain(domain)
        returnArray['firstname'] = domain?.firstname
        returnArray['lastname'] = domain?.lastname
        returnArray['email'] = domain?.email
        returnArray['language'] = domain?.language.toString()
        if (!(domain?.springSecurityService?.principal instanceof String) && domain?.id == domain?.springSecurityService?.currentUser?.id) {
            returnArray['publicKey'] = domain?.publicKey
            returnArray['privateKey'] = domain?.privateKey
            returnArray['passwordExpired'] = domain?.passwordExpired
        }
        returnArray['enabled'] = domain?.enabled
        returnArray['color'] = domain?.color
        returnArray['user'] = domain?.creator
        returnArray['pidPseudonym'] = domain?.pidPseudonym
        if(domain instanceof User) returnArray['workplaces'] = domain?.getWorkplaces().collect{it.name}.join(",")
        else returnArray['workplaces'] = ((UserWorkplace.findAllByUser(User.findById(domain?.id)) as List<UserWorkplace>)*.workplace as Set<Workplace>).collect{it.name}.join(",")
        //TODO previous line is not performant at all. Include perf optimization into the user service ?
        returnArray['phoneNumber'] = domain?.phoneNumber
        returnArray
    }


    static enum Language {
        // see https://fr.wikipedia.org/wiki/Liste_des_codes_ISO_639-1
        ARABIC("AR"),
        GERMAN("DE"),
        GREEK("EL"),
        ENGLISH("EN"),
        FRENCH("FR"),
        DUTCH("NL"),
        SPANISH("ES")

        private final String code

        Language(String code) {this.code = code}

        private static final Map<String,Language> map;

        static {
            map = new HashMap<String,Language>();
            for (Language l : Language.values()) {
                map.put(l.code, l);
            }
        }
        public static Language findByCode(String c) {
            return map.get(c);
        }

        @Override
        String toString(){
            return this.code
        }
    }

}
