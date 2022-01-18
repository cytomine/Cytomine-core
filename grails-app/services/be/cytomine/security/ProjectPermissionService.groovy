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

import be.cytomine.Exception.ConstraintException
import be.cytomine.project.Project

import static org.springframework.security.acls.domain.BasePermission.READ

class ProjectPermissionService {

    static transactional = true

    def cytomineService
    def securityACLService

    boolean isUserInProject(User user, Project project) {
        securityACLService.check(project,READ)
        List<SecUser> users = SecUser.executeQuery("select count(secUser) from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, SecUser as secUser "+
                "where aclObjectId.objectId = "+project.id+" and aclEntry.aclObjectIdentity = aclObjectId.id and aclEntry.sid = aclSid.id and aclSid.sid = secUser.username " +
                "and secUser.class = 'be.cytomine.security.User' and secUser.id = "+user.id);
        return (users.get(0) > 0)
    }

    void checkIsUserInProject(User user, Project project) {
        boolean result = isUserInProject(user, project)
        if(!result) throw new ConstraintException("Error: the user "+user.id+" is not into the project "+project.id)

    }
}
