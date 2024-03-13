package be.cytomine.authorization;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class PermissionTests {

    @Autowired
    PermissionService permissionService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    AclRepository aclRepository;

    @Test
    public void create_permission() {
        Ontology ontology = builder.given_an_ontology();

        permissionService.addPermission(ontology, "superadmin", BasePermission.ADMINISTRATION);

        Long aclClassId = permissionService.getAclClassId(ontology);
        assertThat(aclClassId).isPositive();
        Long aclSidId = permissionService.getAclSid("superadmin");
        assertThat(aclSidId).isPositive();
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);
        assertThat(aclObjectIdentity).isPositive();

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isPositive();
    }

    @Test
    public void delete_permission() {
        Ontology ontology = builder.given_an_ontology();

        permissionService.addPermission(ontology, "superadmin", BasePermission.ADMINISTRATION);

        Long aclClassId = permissionService.getAclClassId(ontology);
        Long aclSidId = permissionService.getAclSid("superadmin");
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isPositive();

        permissionService.deletePermission(ontology, "superadmin",  BasePermission.ADMINISTRATION);

        acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isNull();
    }

    @Test
    public void delete_permission_that_does_not_exist() {
        Ontology ontology = builder.given_an_ontology();

        permissionService.addPermission(ontology, "superadmin", BasePermission.WRITE);

        Long aclClassId = permissionService.getAclClassId(ontology);
        Long aclSidId = permissionService.getAclSid("superadmin");
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);

        permissionService.deletePermission(ontology, "superadmin",  BasePermission.ADMINISTRATION);

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isNull();
        acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 2);
        assertThat(acEntry).isNotNull();
    }

//    public void deletePermission(CytomineDomain domain, String username, Permission permission) {
//        log.info("Delete permission for {}, {}, {}", username, permission.getMask(), domain.getId());
//        Long aclObjectIdentity = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
//        int mask = permission.getMask();
//        Long sid = aclRepository.getAclSid(username);
//
//        if(aclObjectIdentity==null || sid==null) {
//            throw new ObjectNotFoundException("User " + username + " or Object " + domain.getId() + " are not in ACL");
//        }
//        aclRepository.deleteAclEntry(aclObjectIdentity, mask, sid);
//    }
//
//    /**
//     * Add Permission right
//     * @param domain
//     * @param username
//     * @param permission
//     */
//    public void addPermission(CytomineDomain domain, String username, int permission) {
//        addPermission(domain, username, readFromMask(permission));
//    }
//
//    public void addPermission(CytomineDomain domain, String username, Permission permission) {
//        addPermission(domain,username,permission,currentUserService.getCurrentUser());
//    }
//
//    public void addPermission(CytomineDomain domain, String username, Permission permission, SecUser user) {
//
//        //get domain class id
//        Long aclClassId = getAclClassId(domain);
//
//        //get acl sid for current user (run request)
//        Long sidCurrentUser = getAclSid(user.getUsername());
//
//        //get acl object id
//        Long aclObjectIdentity = getAclObjectIdentity(domain, aclClassId, sidCurrentUser);
//
//        //get acl sid for the user
//        Long sid = getAclSid(username);
//
//        //get acl entry
//        createAclEntry(aclObjectIdentity, sid, permission.getMask());
//    }
//
//
//
//
//    public Long createAclEntry(Long aoi, Long sid, Integer mask) {
//        Long aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
//        if (aclEntryId == null) {
//            Integer max = aclRepository.getMaxAceOrder(aoi);
//            if(max==null) {
//                max=0;
//            } else {
//                max = max+1;
//            }
//            aclRepository.insertAclEntry(mask, aoi, mask, sid);
//            aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
//        }
//        return aclEntryId;
//    }
//
//    public Long getAclObjectIdentity(CytomineDomain domain, Long aclClassId, Long aclSidId) {
//        Long aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
//        if (aclObjectIdentityId == null) {
//            aclRepository.insertAclObjectIdentity(aclClassId, domain.getId(), aclSidId);
//            aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
//        }
//        return aclObjectIdentityId;
//    }
//
//    public Long getAclSid(String username) {
//        Long id = aclRepository.getAclSidFromUsername(username);
//        if (id == null) {
//            aclRepository.insertAclSid(username);
//            id = aclRepository.getAclSidFromUsername(username);
//        }
//        return id;
//    }
//
//    public Long getAclClassId(CytomineDomain domain) {
//        Long id = aclRepository.getAclClassId(domain.getClass().getName());
//        if (id == null) {
//            aclRepository.insertAclClassId(domain.getClass().getName());
//            id = aclRepository.getAclClassId(domain.getClass().getName());
//        }
//        return id;
//    }
//
//    Permission readFromMask(int mask) {
//        switch (mask) {
//            case 1:
//                return BasePermission.READ;
//            case 2:
//                return BasePermission.WRITE;
//            case 4:
//                return BasePermission.CREATE;
//            case 8:
//                return BasePermission.DELETE;
//            case 16:
//                return BasePermission.ADMINISTRATION;
//        }
//        throw new RuntimeException("Mask " + mask + " not supported");
//    }

}
