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
import groovy.sql.Sql
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.model.Permission
import static org.springframework.security.acls.domain.BasePermission.*

class PermissionService {

    static transactional = true

    static LinkedHashMap<String, Integer> permissionsMasks = new LinkedHashMap<>([READ: 1, WRITE: 2, CREATE: 4, DELETE: 8, ADMINISTRATION: 16])

    def aclService
    def aclUtilService
    def aclPermissionFactory
    def objectIdentityRetrievalStrategy
    def dataSource
    def cytomineService


//    synchronized void deletePermission(CytomineDomain domain, String username, Permission permission) {
//        def acl = aclUtilService.readAcl(domain)
//        log.info "Delete Permission " +  permission.mask + " for " + username + " from " + domain.class + " " + domain.id
//        // Remove all permissions associated with this particular recipient
//        acl.entries.eachWithIndex { entry, i ->
//            if (entry.sid.getPrincipal().equals(username) && entry.permission.equals(permission)) {
//                acl.deleteAce(i)
//            }
//        }
//        aclService.updateAcl(acl)
//    }

    Map<String, Integer> listUsersAndPermissions(def domain) {
        Map<String, Integer> results = new HashMap<>()
        def sql = new Sql(dataSource)
        sql.eachRow("SELECT acs.sid, max(mask)\n" +
                    "FROM acl_entry ae, acl_sid acs, acl_object_identity aoi \n" +
                    "WHERE ae.sid = acs.id AND ae.acl_object_identity = aoi.id AND aoi.object_id_identity = ?\n" +
                    "GROUP BY acs.sid",[domain.id]) {
            results.put(it[0], it[1])
        }
        try {
            sql.close()
        }catch (Exception e) {}
        return results
    }

    void deletePermission(def domain, String username, Permission permission) {
        log.info "Delete permission for $username, ${permission.mask}, ${domain.id}"
        def aoi = executeAclRequest("SELECT id FROM acl_object_identity WHERE object_id_identity = ?",[domain.id])
        int mask = permission.mask
        def sid = executeAclRequest("SELECT id FROM acl_sid WHERE sid = ?",[username])

        if(!aoi || !sid) throw ObjectNotFoundException("User ${username} or Object ${domain.id} are not in ACL")

        executeAclCUD("DELETE FROM acl_entry WHERE acl_object_identity = ? AND mask = ? AND sid = ?",[aoi,mask,sid])
    }

    void deletePermission(def domain, String username) {
        log.info "Delete permission for $username, ${domain.id}"
        def aoi = executeAclRequest("SELECT id FROM acl_object_identity WHERE object_id_identity = ?",[domain.id])
        def sid = executeAclRequest("SELECT id FROM acl_sid WHERE sid = ?",[username])
        if(!aoi || !sid) throw ObjectNotFoundException("User ${username} or Object ${domain.id} are not in ACL")
        executeAclCUD("DELETE FROM acl_entry WHERE acl_object_identity = ? AND sid = ?",[aoi,sid])
    }

    /**
     * Add Permission right
     * @param domain
     * @param username
     * @param permission
     */
    void addPermission(def domain, String username, int permission) {
        addPermission(domain, username, aclPermissionFactory.buildFromMask(permission))
    }

    void addPermission(def domain, String username, Permission permission) {

        addPermission(domain,username,permission,cytomineService.currentUser)
    }

    void addPermission(def domain, String username, Permission permission,SecUser user) {

        //get domain class id
        def ac = getAclClass(domain)

        //get acl sid for current user (run request)
        def sidCurrentUser = getAclSid(user.username)

        //get acl object id
        def aoi = getAclObjectIdentity(domain, ac, sidCurrentUser)

        //get acl sid for the user
        def sid = getAclSid(username)

        //get acl entry
        def ace = getAclEntry(aoi,sid,permission.mask)
    }

    public void getAclEntry(def aoi, def sid, def mask) {
        def ace = executeAclRequest("SELECT id FROM acl_entry WHERE acl_object_identity = ? AND mask = ? AND sid=?",[aoi,mask,sid])
        if (!ace) {
            def max = executeAclRequest("SELECT max(ace_order) FROM acl_entry WHERE acl_object_identity = ?",[aoi])
            if(max==null) {
                max=0
            } else {
                max = max +1
            }
            executeAclCUD("" +
                    "INSERT INTO acl_entry(id,ace_order,acl_object_identity,audit_failure,audit_success,granting,mask,sid) " +
                    "VALUES(nextval('hibernate_sequence'),?,?,false,false,true,?,?)",[max,aoi,mask,sid])

            ace = executeAclRequest("SELECT id FROM acl_entry WHERE acl_object_identity = ? AND mask = ? AND sid=?",[aoi,mask,sid])
        }
        ace
    }

    public def getAclObjectIdentity(domain, ac, sidCurrentUser) {
        def aoi = executeAclRequest("SELECT id FROM acl_object_identity WHERE object_id_identity = ?",[domain.id])
        if (!aoi) {
            //id=nextVal()
            executeAclCUD("" +
                    "INSERT INTO acl_object_identity(id,object_id_class,entries_inheriting,object_id_identity,owner_sid,parent_object) " +
                    "VALUES (nextval('hibernate_sequence'),?,true,?,?,null)",[ac, domain.id,sidCurrentUser])
            aoi = executeAclRequest("SELECT id FROM acl_object_identity WHERE object_id_identity = ?",[domain.id])
        }
        aoi
    }

    public def getAclSid(String username) {
        def sidCurrentUser = executeAclRequest("SELECT id FROM acl_sid WHERE sid = ?",[username])
        if (!sidCurrentUser) {
            executeAclCUD("INSERT INTO acl_sid(id,principal,sid) VALUES(nextval('hibernate_sequence'),true,?)",[username])
            sidCurrentUser = executeAclRequest("SELECT id FROM acl_sid WHERE sid = ?",[username])
        }
        sidCurrentUser
    }

    public def getAclClass(domain) {
        def ac = executeAclRequest("SELECT id FROM acl_class WHERE class = ?",[domain.class.name])
        if (!ac) {
            executeAclCUD("INSERT INTO acl_class(id,class) VALUES(nextval('hibernate_sequence'),?)",[domain.class.name])
            ac = executeAclRequest("SELECT id FROM acl_class WHERE class = ?",[domain.class.name])
        }
        ac
    }

    def executeAclRequest(String request,def params = []) {
        def id = null
        def sql = new Sql(dataSource)
        sql.eachRow(request,params) {
            id = it[0]
        }
        try {
            sql.close()
        }catch (Exception e) {}
        return id
    }

    def executeAclCUD(String request,def params = []) {
        def sql = new Sql(dataSource)
         sql.execute(request,params)
        try {
            sql.close()
        }catch (Exception e) {}
    }

    static Permission retrievePermissionFromString(String permissionName) {
        if (permissionName.equals("READ")) {
            return READ
        }
        if (permissionName.equals("WRITE")) {
            return WRITE
        }
        if (permissionName.equals("CREATE")) {
            return CREATE
        }
        if (permissionName.equals("DELETE")) {
            return DELETE
        }
        if (permissionName.equals("ADMINISTRATION")) {
            return ADMINISTRATION
        }
        throw new Exception("Permission $permissionName not found")
    }

    static String retrievePermissionFromInt(int permissionMask) {
        for (Map.Entry entry : permissionsMasks.entrySet()) {
            if (entry.value == permissionMask) {
                return entry.key
            }
        }
        throw new Exception("PermissionMask $permissionMask not found")
    }
}



