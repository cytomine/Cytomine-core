package be.cytomine.utils.database

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

import groovy.sql.Sql
import org.postgresql.util.PSQLException

/**
 * Service used to create index at the application bootstrap
 */
class IndexService {

    static transactional = true

    def sessionFactory
    def grailsApplication
    def dataSource

    /**
     * Create domain index
     * -------------------
     *
     * When to create an index ?
     * --------------------------
     * There is no simple formula to determine whether an index should be created.
     * You must consider the trade-off of the benefits of indexed retrieval versus
     * the maintenance overhead of that index.
     *
     * - Frequency of search :
     * If a particular column is searched frequently, you can achieve performance benefits
     * by creating an index on that column.
     * Creating an index on a column that is rarely searched may not be worthwhile.
     *
     * - Size of table :
     * Indexes on relatively large tables with many rows provide greater benefits than
     * indexes on relatively small tables. For example, a table with only 20 rows is
     * unlikely to benefit from an index, since a sequential scan would not take any
     * longer than an index lookup.
     *
     * - Number of updates :
     * An index is updated every time a row is inserted or deleted from the table and
     * every time an indexed column is updated. An index on a column slows the performance
     * of inserts, updates and deletes. A database that is frequently updated should have
     * fewer indexes than one that is read-only.
     *
     * Ref: http://dcx.sap.com/1200/fr/dbusage/when-using-perform.html
     *
     * Other considerations:
     * - A primary key is automatically created by Grails on the ID
     * - Fields declared as unique in their domain have a unique key automatically managed by Grails.
     * - A foreign key IS NOT an index.
     */
    def initIndex() {
        try {
            // Command
            createIndex("command", "user_id")
            createIndex("command", "project_id")
            createIndex("command", "created")
            createIndex("command_history", "project_id")
            createIndex("command_history", "user_id")
            createIndex("command_history", "created")
            createIndex("command_history", "command_id")
            createIndex("undo_stack_item", "command_id")
            createIndex("redo_stack_item", "command_id")

            // ACL
            createIndex("acl_object_identity", "object_id_identity")
            createIndex("acl_entry", "acl_object_identity")
            createIndex("acl_entry", "sid")
            createIndex("acl_sid", "sid")

            // Storage
            createIndex("storage", "user_id")

            // Uploaded file
            createIndex("uploaded_file", "user_id")
            createIndex("uploaded_file", "storage_id")
            createIndex("uploaded_file", "parent_id")
            createIndex("uploaded_file","l_tree","GIST")

            // Sample
            createIndex("sample", "name")
            createIndex("sample", "created")

            // Abstract image
            createIndex("abstract_image", "created")
            createIndex("abstract_image", "sample_id")
            createIndex("abstract_image", "uploaded_file_id")
            createIndex("abstract_image", "user_id")

            // Abstract slice
            createIndex("abstract_slice", "uploaded_file_id")
            createIndex("abstract_slice", "image_id")

            // Image instance
            createIndex("image_instance", "created")
            createIndex("image_instance", "base_image_id")
            createIndex("image_instance", "project_id")
            createIndex("image_instance", "user_id")

            // Slice instance
            createIndex("slice_instance", "base_slice_id")

            // Property
            createIndex("property", "domain_ident")
            createIndex("property", "key")

            // Attached file
            createIndex("attached_file", "domain_ident")

            // Description
            createIndex("description", "domain_ident")

            // Annotation index
            createIndex("annotation_index", "slice_id")
            createIndex("annotation_index", "user_id")

            // Annotation
            createIndex("user_annotation", "image_id");
            createIndex("user_annotation", "slice_id");
            createIndex("user_annotation", "user_id");
            createIndex("user_annotation", "created");
            createIndex("user_annotation", "project_id");
            createIndex("user_annotation", "location", "GIST");

            createIndex("algo_annotation", "image_id");
            createIndex("algo_annotation", "slice_id");
            createIndex("algo_annotation", "user_id");
            createIndex("algo_annotation", "created");
            createIndex("algo_annotation", "project_id");
            createIndex("algo_annotation", "location", "GIST");

            createIndex("reviewed_annotation", "project_id");
            createIndex("reviewed_annotation", "user_id");
            createIndex("reviewed_annotation", "image_id");
            createIndex("reviewed_annotation", "slice_id");
            createIndex("reviewed_annotation", "location", "GIST");

            // Annotation term
            createIndex("annotation_term", "user_annotation_id");
            createIndex("annotation_term", "term_id");
            createIndex("annotation_term", "user_id");

            // Algo_annotation_term
            createIndex("algo_annotation_term","annotation_ident")
            createIndex("algo_annotation_term","project_id")
            createIndex("algo_annotation_term","rate")
            createIndex("algo_annotation_term","term_id")
            createIndex("algo_annotation_term","user_job_id")

            // Relation term
            createIndex("relation_term", "relation_id");
            createIndex("relation_term", "term1_id");
            createIndex("relation_term", "term2_id");

            // Term
            createIndex("term", "ontology_id");

            // Track
            createIndex("track", "image_id")
            createIndex("track", "project_id")

            // Annotation track
            createIndex("annotation_track", "track_id")
            createIndex("annotation_track", "slice_id")
            createIndex("annotation_track", "annotation_ident")

            // User Job
            createIndex("sec_user", "job_id")
            createIndex("sec_user", "user_id")

            // Job
            createIndex("job", "project_id")
            createIndex("job", "software_id")
            createIndex("job", "processing_server_id")

            // JobParameter
            createIndex("job_parameter", "job_id")
            createIndex("job_parameter", "software_parameter_id")

            // Software Parameter
            createIndex("software_parameter", "software_id")

            // Software project
            createIndex("software_project", "software_id")
            createIndex("software_project", "project_id")

            // User group
            createIndex("user_group", "user_id")
            createIndex("user_group", "group_id")

            // Auth with token
            createIndex("auth_with_token", "user_id")
            createIndex("auth_with_token", "token_key","hash")

        } catch (PSQLException e) {
            log.error e
        }
    }

    /**
     * Create Btree index
     * @param statement Database statement
     * @param table Table for index
     * @param col Column for index
     */
    def createIndex(String table, String col) {
        createIndex(table,col,"btree",null);
    }

    /**
     * Create an index (various type: BTREE, HASH, GIST,...)
     * @param statement Database statement
     * @param table Table for index
     * @param col Column for index
     * @param type Index structure type (BTREE, HASH, GIST,...)
     */
    def createIndex(String table, String col, String type, String overidedname = null) {
        String name = overidedname ?: table + "_" + col + "_index"

        boolean alreadyExist = false
        def sql = new Sql(dataSource)
        sql.eachRow("select indexname from pg_indexes where indexname like ?",[name]) {
            alreadyExist = true
        }
        sql.close()

        if(alreadyExist) {
            log.debug "$name already exist, don't create it"
            return
        }

        try {
            String request = "CREATE INDEX " + name + " ON " + table + " USING $type (" + col + ");"
            log.debug request

            sql = new Sql(dataSource)
            sql.execute(request)
            sql.close()
        }
        catch(Exception e) {
            log.error e
        }
    }
}
