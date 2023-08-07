package be.cytomine.authorization.image.server;

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
import be.cytomine.authorization.CRUDAuthorizationTest;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.service.PermissionService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class StorageAuthorizationTest extends CRUDAuthorizationTest {

    // We need more flexibility:






    private Storage storage = null;

    @Autowired
    StorageService storageService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (storage == null) {
            storage = builder.given_a_storage();
            ;
            initACL(storage);
        }
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list_storages() {
        assertThat(storageService.list()).contains(storage);
        Storage anotherStorage = builder.given_a_storage();
        assertThat(storageService.list()).contains(anotherStorage);
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list_all_storages(){
        expectOK (() -> storageService.list());
        Storage anotherStorage = builder.given_a_storage();
        assertThat(storageService.list()).doesNotContain(anotherStorage);
    }




    @Override
    public void when_i_get_domain() {
        storageService.get(storage.getId());
    }

    @Override
    public void guest_with_permission_get_domain() {
        expectForbidden (() -> when_i_get_domain());
    }

    @Override
    protected void when_i_add_domain() {
        storageService.add(builder.given_a_not_persisted_storage().toJsonObject());
    }

    @Override
    public void when_i_edit_domain() {
        storageService.update(storage, storage.toJsonObject());
    }

    @Override
    protected void when_i_delete_domain() {
        Storage storageToDelete = storage;
        storageService.delete(storageToDelete, null, null, true);
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.empty();
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(ADMINISTRATION);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(ADMINISTRATION);
    }


    @Override
    protected Optional<String> minimalRoleForCreate() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForDelete() {
        return Optional.of("ROLE_USER");
    }

    @Override
    protected Optional<String> minimalRoleForEdit() {
        return Optional.of("ROLE_USER");
    }
}
