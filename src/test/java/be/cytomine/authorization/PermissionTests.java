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
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;

import javax.transaction.Transactional;

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

        permissionService.addPermission(ontology, "superadmin", ADMINISTRATION);

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

        permissionService.addPermission(ontology, "superadmin", ADMINISTRATION);

        Long aclClassId = permissionService.getAclClassId(ontology);
        Long aclSidId = permissionService.getAclSid("superadmin");
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isPositive();

        permissionService.deletePermission(ontology, "superadmin",  ADMINISTRATION);

        acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isNull();
    }

    @Test
    public void delete_permissions() {
        Ontology ontology = builder.given_an_ontology();

        permissionService.addPermission(ontology, "superadmin", READ);
        permissionService.addPermission(ontology, "superadmin", ADMINISTRATION);

        Long aclClassId = permissionService.getAclClassId(ontology);
        Long aclSidId = permissionService.getAclSid("superadmin");
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isPositive();
        acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 1);
        assertThat(acEntry).isPositive();

        permissionService.deletePermission(ontology, "superadmin");

        assertThat(aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16)).isNull();
        assertThat(aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 1)).isNull();
    }


    @Test
    public void delete_permission_that_does_not_exist() {
        Ontology ontology = builder.given_an_ontology();

        permissionService.addPermission(ontology, "superadmin", BasePermission.WRITE);

        Long aclClassId = permissionService.getAclClassId(ontology);
        Long aclSidId = permissionService.getAclSid("superadmin");
        Long aclObjectIdentity = permissionService.getAclObjectIdentity(ontology, aclClassId, aclSidId);

        permissionService.deletePermission(ontology, "superadmin",  ADMINISTRATION);

        Long acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 16);
        assertThat(acEntry).isNull();
        acEntry = aclRepository.getAclEntryId(aclObjectIdentity, aclSidId, 2);
        assertThat(acEntry).isNotNull();
    }

    @Test
    public void retrieve_permission_from_string() {
        assertThat(PermissionService.readFromString("READ")).isEqualTo(READ);
        assertThat(PermissionService.readFromString("WRITE")).isEqualTo(WRITE);
        assertThat(PermissionService.readFromString("ADMINISTRATION")).isEqualTo(ADMINISTRATION);
    }

}
