package be.cytomine.security

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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class GroupService extends ModelService {

    static transactional = true
    def cytomineService
    def commandService
    def userGroupService
    def transactionService
    def securityACLService
    def dataSource

    def currentDomain() {
        Group
    }

    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        return Group.list(sort: "name", order: "asc")
    }

    def listWithUser() {
        securityACLService.checkGuest(cytomineService.currentUser)
        log.info "listWithUser"

        String request = "SELECT g.id as group_id, g.name as group_name, u.id as user_id, u.username as user_name, u.lastname as user_lastname, u.firstname  as user_firstname " +
                "FROM user_group ug, \"group\" g, sec_user u " +
                "WHERE ug.user_id=u.id AND g.id = ug.group_id " +
                "ORDER BY g.name, u.lastname;";

        log.info request
        def sql = new Sql(dataSource)

        def groups = [];
        def group;
        def idGroup = -1;
        def users = new JSONArray();
        sql.eachRow(request) {
            log.info it
            if(idGroup != it.group_id) {
                if(group != null) {
                    group.put("users", users)
                    groups << group
                }

                group = new JSONObject();
                group.put("id", it.group_id)
                group.put("name", it.group_name)
                idGroup = it.group_id
                users = new JSONArray();
            }
            def user = new JSONObject();
            user.put("id", it.user_id)
            user.put("username", it.user_name)
            user.put("lastname", it.user_lastname)
            user.put("firstname", it.user_firstname)

            users.add(user)
        }

        if(group != null) {
            group.put("users", users)
            groups << group
        }


        return groups
    }

    def list(User user) {
        securityACLService.checkGuest(cytomineService.currentUser)
        UserGroup.findByUser(user).collect{it.group}
    }

    def read(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        return Group.read(id)
    }

    def get(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        return Group.get(id)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkGuest(currentUser)
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Group group, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIfUserIsMemberGroup(currentUser,group)
        return executeCommand(new EditCommand(user: currentUser),group, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Group domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }


    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    def deleteDependentUserGroup(Group group, Transaction transaction, Task task = null) {
        UserGroup.findAllByGroup(group).each {
            userGroupService.delete(it, transaction,null,false)
        }
    }

}
