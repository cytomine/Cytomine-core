package be.cytomine.utils.database.mongodb

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

import com.mongodb.CommandFailureException

class NoSQLCollectionService {

    def sessionFactory
    def grailsApplication
    static transactional = true
    def mongo

    static String databaseName = "cytomine"

    public String getDatabaseName() {
        return grailsApplication.config.grails.mongo.databaseName
    }

    public String getDatabaseFullDetails() {
        return grailsApplication.config.grails.mongo
    }

    public def cleanActivityDB() {
        log.info "Clean data from "+ getDatabaseName()
        def db = mongo.getDB(getDatabaseName())
        db.annotationAction.drop()
        db.lastUserPosition.drop()
        db.lastConnection.drop()
        db.persistentConnection.drop()
        db.persistentImageConsultation.drop()
        db.persistentProjectConnection.drop()
        db.persistentUserPosition.drop()
    }

    public def dropIndex(String collection, String name) {
        log.info "Drop index $name from $collection collection"
        def db = mongo.getDB(getDatabaseName())
        try {
            db."${collection}".dropIndex(name)
        } catch(CommandFailureException e){
            log.error (collection+" : "+e.message)
        }
    }

}
