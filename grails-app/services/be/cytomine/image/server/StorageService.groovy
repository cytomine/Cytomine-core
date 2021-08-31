package be.cytomine.image.server

import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi

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

import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.PaginationUtils
import be.cytomine.utils.Task
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.security.acls.domain.BasePermission.*

class StorageService extends ModelService {

    def cytomineService
    def transactionService
    def permissionService
    def securityACLService
    def springSecurityService
    def currentRoleServiceProxy
    def secUserService
    def dataSource

    static transactional = true

    def currentDomain() {
        return Storage
    }

    def list() {
        return securityACLService.getStorageList(cytomineService.currentUser, true)
    }

    def list(SecUser user, Boolean adminByPass = false) {
        return securityACLService.getStorageList(user, adminByPass)
    }

    def list(SecUser user, String searchString, Boolean adminByPass = false) {
        return securityACLService.getStorageList(user, adminByPass, searchString)
    }

    def usersStats(Storage storage, String sortColumn, String sortDirection, Long max = 0, Long offset = 0) {
        Map<Long, Object> results = [:]
        secUserService.listUsers(storage).each {
            def usersData = [:]
            usersData['id'] = it.id
            usersData['username'] = it.username
            usersData['firstname'] = it.firstname
            usersData['lastname'] = it.lastname
            usersData['fullName'] = it.firstname + ' ' + it.lastname
            usersData['numberOfFiles'] = 0
            usersData['totalSize'] = 0
            usersData['created'] = it.created
            results.put(it.username, usersData)
        }

        permissionService.listUsersAndPermissions(storage).each {
            if(results.containsKey(it.key)) {
                results.get(it.key)['role'] = permissionService.retrievePermissionFromInt(it.value)
            }
        }

        def sql = new Sql(dataSource)
        sql.eachRow("" +
                "SELECT su.username, su.id, count(uf.id) as files, sum(uf.size) as size\n" +
                "FROM uploaded_file uf, sec_user su \n" +
                "WHERE su.id = uf.user_id AND uf.storage_id = ${storage.id}\n" +
                "GROUP BY su.username, su.id") {

            if(results.containsKey(it[0])) {
                results.get(it[0]).numberOfFiles = it[2]
                results.get(it[0]).totalSize = it[3]
            }
        }
        sql.close()
        return PaginationUtils.convertListToPage(results.values(), sortColumn, sortDirection, max, offset)
    }

    def userAccess(SecUser user) {
        return securityACLService.getStoragesIdsWithMaxPermission(user)
    }

    def read(def id) {
        def storage =  Storage.read((Long) id)
        if(storage) {
            securityACLService.check(storage,READ)
        }
        storage
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data)
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        json.user = (currentRoleServiceProxy.isAdminByNow(currentUser)) ? json.user : currentUser.id
        Command c = new AddCommand(user: currentUser)
        executeCommand(c,null,json)
    }

    def afterAdd(Storage domain, def response) {
        log.info("Add permission on $domain to ${domain.user.username}")
        if(!domain.hasACLPermission(READ)) {
            permissionService.addPermission(domain, domain.user.username, READ)
        }
        if(!domain.hasACLPermission(WRITE)) {
            permissionService.addPermission(domain, domain.user.username, WRITE)
        }
        if(!domain.hasACLPermission(ADMINISTRATION)) {
            permissionService.addPermission(domain, domain.user.username, ADMINISTRATION)
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Storage storage,def jsonNewData) {
        securityACLService.check(storage, ADMINISTRATION)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        executeCommand(c,storage,jsonNewData)
    }

    /**
     * Delete domain in argument
     * @param json JSON that was passed in request parameter
     * @param security Security service object (user for right check)
     * @return Response structure (created domain data,..)
     */
    def delete(Storage storage, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.check(storage.container(), ADMINISTRATION)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,storage,null)
    }

    def deleteDependentUploadedFile(Storage storage, Transaction transaction, Task task = null) {
        // TODO: do we want to allow this ?
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    def initUserStorage(SecUser user) {
        log.info ("create storage for $user.username")
        SpringSecurityUtils.doWithAuth(user.username, {
            Command c = new AddCommand(user: user)
            executeCommand(c,null, new JSONObject([name: "$user.username storage", user: user.id]))
        })
    }
}
