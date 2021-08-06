package be.cytomine.image

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

import be.cytomine.api.UrlApi
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.server.Storage
import be.cytomine.Exception.ForbiddenException
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.utils.ModelService
import be.cytomine.utils.SQLUtils
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class UploadedFileService extends ModelService {

    static transactional = true

    def cytomineService
    def abstractImageService
    def securityACLService
    def dataSource

    def currentDomain() {
        return UploadedFile
    }

    def list(String sortedProperty = null, String sortDirection = null, Long max  = 0, Long offset = 0) {
        securityACLService.checkAdmin(cytomineService.currentUser)

        return criteriaRequestWithPagination(UploadedFile, max, offset, {
            isNull("deleted")
        }, [], sortedProperty, sortDirection)

    }

    def list(User user, Long parentId = null, Boolean onlyRoot = null, String sortedProperty = null, String sortDirection = null, Long max  = 0, Long offset = 0) {

        securityACLService.checkIsSameUser(user, cytomineService.currentUser)
        List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser, false)
        return criteriaRequestWithPagination(UploadedFile, max, offset, {
            eq("user.id", user.id)
            if(onlyRoot) {
                isNull("parent.id")
            } else if(parentId != null){
                eq("parent.id", parentId)
            }
            isNull("deleted")
            'in'("storage.id", storages.collect{ it.id })
        }, [], sortedProperty, sortDirection)

    }

    def listWithDetails(User user, def searchParameters = [], def sortedProperty = "created", def sortDirection = "desc") {
        securityACLService.checkIsSameUser(user, cytomineService.currentUser)

        String search = ""
        searchParameters.each {
            if (it.field == 'storage') {
                search += "AND uf.storage_id in (${it.value}) "
            } else {
                search += "AND uf.${SQLUtils.toSnakeCase(it.field)} ${it.sqlOperator} '${it.value}' "
            }
        }

        String sort = ""
        if (["content_type", "id", "created", "filename", "originalFilename", "size", "status"].contains(sortedProperty)) {
            sort += "uf.${SQLUtils.toSnakeCase(sortedProperty)}"
        }
        else if(sortedProperty == "globalSize") {
            sort += "COALESCE(SUM(DISTINCT tree.size),0)+uf.size"
        }

        if (!sort.isEmpty()) {
            sort = "ORDER BY $sort $sortDirection"
        }

        String request = "SELECT uf.id, " +
                "uf.content_type, " +
                "uf.created, " +
                "uf.filename, " +
                "uf.original_filename, " +
                "uf.size, " +
                "uf.status, " +
                "CASE WHEN (nlevel(uf.l_tree) > 0) THEN ltree2text(subltree(uf.l_tree, 0, 1)) ELSE NULL END AS root, " +
                "COUNT(DISTINCT tree.id) AS nb_children, " +
                "COALESCE(SUM(DISTINCT tree.size),0)+uf.size AS global_size, " +
                "CASE WHEN (uf.status = ${UploadedFile.Status.CONVERTED.code} OR uf.status = ${UploadedFile.Status.DEPLOYED.code}) " +
                "THEN ai.id ELSE NULL END AS image " +
                "FROM uploaded_file uf " +
                "LEFT JOIN (SELECT *  FROM uploaded_file t " +
                "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                "WHERE aoi.object_id_identity = t.storage_id AND asi.sid = :username) AND t.deleted IS NULL) " +
                "AS tree ON (uf.l_tree @> tree.l_tree AND tree.id != uf.id) " +
                "LEFT JOIN abstract_image AS ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN uploaded_file AS parent ON parent.id = uf.parent_id " +
                "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi " +
                    "LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                    "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                    "WHERE aoi.object_id_identity = uf.storage_id AND asi.sid = :username) " +
                "AND (uf.parent_id IS NULL OR parent.content_type similar to '%zip%') " +
                "AND uf.content_type NOT similar to '%zip%' " +
                "AND uf.deleted IS NULL " +
                search +
                "GROUP BY uf.id, ai.id " +
                sort

        def data = []
        def sql = new Sql(dataSource)
        println request
        sql.eachRow(request, [username: user.username]) { resultSet ->
            def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
            row.thumbURL = (row.image) ? UrlApi.getAbstractImageThumbUrl(row.image as Long) : null
            data << row
        }
        sql.close()

        return data
    }

    def listHierarchicalTree(User user, Long rootId) {
        UploadedFile root = read(rootId)
        if(!root) {
            throw new ForbiddenException("UploadedFile not found")
        }
        securityACLService.checkAtLeastOne(root, READ)
        String request = "SELECT uf.id, uf.created, uf.original_filename, uf.content_type, " +
                "uf.l_tree, uf.parent_id as parent, " +
                "uf.size, uf.status, " +
                "array_agg(ai.id) as image, array_agg(asl.id) as slices, array_agg(cf.id) as companion_file " +
                "FROM uploaded_file uf " +
                "LEFT JOIN abstract_image ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN abstract_slice asl ON asl.uploaded_file_id = uf.id " +
                "LEFT JOIN companion_file cf ON cf.uploaded_file_id = uf.id " +
                "LEFT JOIN acl_object_identity as aoi ON aoi.object_id_identity = uf.storage_id " +
                "LEFT JOIN acl_entry as ae ON ae.acl_object_identity = aoi.id " +
                "LEFT JOIN acl_sid as asi ON asi.id = ae.sid " +
                "WHERE uf.l_tree <@ '" + root.lTree + "'::text::ltree " +
                "AND asi.sid = :username " +
                "AND uf.deleted IS NULL " +
                "GROUP BY uf.id " +
                "ORDER BY uf.l_tree ASC "

        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request, [username: user.username]) { resultSet ->
            def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
            row.lTree = row.lTree.value
            row.image = row.image.array.find { it != null }
            row.slices = row.slices.array.findAll { it != null } // A same UF can be linked to several slices (virtual stacks)
            row.companionFile = row.companionFile.array.find { it != null }
            row.thumbURL =  null
            if(row.image) {
                row.thumbURL = UrlApi.getAbstractImageThumbUrl(row.image as Long)
                row.macroURL = UrlApi.getAssociatedImage(row.image as Long, "macro", row.contentType as String, 256)
            } else if (row.slices.size() > 0) {
                row.thumbURL = UrlApi.getAbstractSliceThumbUrl(row.slices[0] as Long)
            }
            data << row
        }
        sql.close()

        return data
    }

    UploadedFile read(def id) {
        UploadedFile uploadedFile = UploadedFile.read(id)
        if (uploadedFile) {
            securityACLService.checkAtLeastOne(uploadedFile, READ)
        }
        uploadedFile
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        if(currentUser instanceof UserJob) currentUser = ((UserJob)currentUser).user
        securityACLService.checkUser(currentUser)
        if (json.storage) {
            securityACLService.check(json.storage, Storage, WRITE)
        }
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(UploadedFile uploadedFile, def jsonNewData, Transaction transaction = null) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        securityACLService.checkAtLeastOne(uploadedFile, WRITE)

        if (jsonNewData.storage && jsonNewData.storage != uploadedFile.storage.id) {
            securityACLService.check(jsonNewData.storage, Storage, WRITE)
        }

        return executeCommand(new EditCommand(user: currentUser, transaction : transaction), uploadedFile,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(UploadedFile domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        securityACLService.checkAtLeastOne(domain, WRITE)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.filename]
    }

    def deleteDependentAbstractImage(UploadedFile uploadedFile, Transaction transaction,Task task=null) {
        AbstractImage.findAllByUploadedFile(uploadedFile).each {
            abstractImageService.delete(it, transaction, task, false)
        }
    }

    def abstractSliceService
    def deleteDependentAbstractSlice(UploadedFile uploadedFile, Transaction transaction, Task task = null) {
        AbstractSlice.findAllByUploadedFile(uploadedFile).each {
            abstractSliceService.delete(it, transaction, task, false)
        }
    }

    def companionFileService
    def deleteDependentCompanionFile(UploadedFile uploadedFile, Transaction transaction, Task task = null) {
        CompanionFile.findAllByUploadedFile(uploadedFile).each {
            companionFileService.delete(it, transaction, task, false)
        }
    }

    def deleteDependentUploadedFile(UploadedFile uploadedFile, Transaction transaction,Task task=null) {
        taskService.updateTask(task,task? "Delete ${UploadedFile.countByParent(uploadedFile)} uploadedFile parents":"")

        // Update all children so that their parent is the grandfather
        UploadedFile.findAllByParent(uploadedFile).each { child ->
            child.parent = uploadedFile.parent
            this.update(child, JSON.parse(child.encodeAsJSON()), transaction)
        }

        String currentTree = uploadedFile?.lTree ?: ""
        String request = "UPDATE uploaded_file SET l_tree = '' WHERE id= "+uploadedFile.id+";\n"

        String parentTree = (uploadedFile?.parent?.lTree)?:""
        if (!parentTree.isEmpty()) {
            request += "UPDATE uploaded_file " +
                    "SET l_tree = '" +parentTree +"' || subpath(l_tree, nlevel('" +currentTree +"')) " +
                    "WHERE l_tree <@ '" +currentTree +"';"
        }

        def sql = new Sql(dataSource)
        sql.execute(request)
        sql.close()
    }
}
