package be.cytomine.authorization.security;

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
import be.cytomine.authorization.AbstractAuthorizationTest;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.ConfigurationService;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.service.security.SecUserSecRoleService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class UserAuthorizationTest extends AbstractAuthorizationTest {


    @Autowired
    SecUserService secUserService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    SecUserSecRoleService secUserSecRoleService;

    @BeforeEach
    public void before() throws Exception {
        ;
    }


    @Test
    @WithMockUser(username = GUEST)
    public void every_body_can_read_user() {
        User userNoAcl = userRepository.findByUsernameLikeIgnoreCase(USER_NO_ACL).get();
        assertThat(secUserService.findUser(userNoAcl.getId())).isPresent();
        assertThat(secUserService.find(userNoAcl.getId())).isPresent();
        assertThat(secUserService.get(userNoAcl.getId())).isNotNull();
        assertThat(secUserService.findByUsername(userNoAcl.getUsername())).isPresent();
        assertThat(secUserService.findByEmail(((User)userNoAcl).getEmail())).isPresent();
        assertThat(secUserService.findByPublicKey(((User)userNoAcl).getPublicKey())).isPresent();
        assertThat(secUserService.getAuthenticationRoles(userNoAcl)).isNotNull();
    }

    @Test
    @WithMockUser(username = GUEST)
    public void every_body_list_user() {
        secUserService.list(new UserSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L);
    }

    @Test
    @WithMockUser(username = GUEST)
    public void every_body_cannot_list_user_from_project() {
        expectForbidden(() -> secUserService.listUsersExtendedByProject(builder.given_a_project(), new UserSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L));
        expectForbidden(() -> secUserService.listUsersByProject(builder.given_a_project(), new ArrayList<>(), "created", "desc", 0L, 0L));
        expectForbidden(() -> secUserService.listAdmins(builder.given_a_project()));
        expectForbidden(() -> secUserService.listUsers(builder.given_a_project()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_project_can_list_user_from_project() {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, USER_ACL_READ);
        expectOK(() ->secUserService.listUsersExtendedByProject(project, new UserSearchExtension(), new ArrayList<>(), "created", "desc", 0L, 0L));
        expectOK(() -> secUserService.listAdmins(project));
        expectOK(() -> secUserService.listUsers(project));
    }


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_lock_user() {
        User user = builder.given_a_user();
        expectOK(() -> secUserService.lock(user));
        expectOK(() -> secUserService.unlock(user));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_lock_user() {
        User user = builder.given_a_user();
        expectForbidden(() -> secUserService.lock(user));
        user.setEnabled(false);
        expectForbidden(() -> secUserService.unlock(user));
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_can_add_user() {
        User user = builder.given_a_not_persisted_user();
        expectOK(() -> secUserService.add(user.toJsonObject().withChange("password", UUID.randomUUID().toString())));
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_can_modify_himself() {
        User user = userRepository.findByUsernameLikeIgnoreCase(USER_NO_ACL).get();
        expectOK(() -> secUserService.update(user, user.toJsonObject().withChange("lastname", "user_can_modify_himself")));
        assertThat(user.getLastname()).isEqualTo("user_can_modify_himself");
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_modify_a_user() {
        User user = userRepository.findByUsernameLikeIgnoreCase(USER_NO_ACL).get();
        expectOK(() -> secUserService.update(user, user.toJsonObject().withChange("lastname", "admin_can_modify_a_user")));
        assertThat(user.getLastname()).isEqualTo("admin_can_modify_a_user");
    }


    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_modify_another_user() {
        User user =userRepository.findByUsernameLikeIgnoreCase(GUEST).get();
        expectForbidden(() -> secUserService.update(user, user.toJsonObject().withChange("lastname", "user_can_modify_himself")));
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_delete_another_user() {
        User user = builder.given_a_user();
        expectForbidden(() -> secUserService.delete(user, null, null, false));
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_cannot_delete_himself() {
        User user = userRepository.findByUsernameLikeIgnoreCase(USER_NO_ACL).get();
        expectForbidden(() -> secUserService.delete(user, null, null, false));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_another_user() {
        User user = builder.given_a_user();
        expectOK(() -> secUserService.delete(user, null, null, false));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_a_user_to_a_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();
        expectOK(() -> secUserService.addUserToProject(user, project, false));
        expectOK(() -> secUserService.deleteUserFromProject(user, project, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_right_can_add_a_user_to_a_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, USER_ACL_ADMIN, ADMINISTRATION);
        expectOK(() -> secUserService.addUserToProject(user, project, false));
        expectOK(() -> secUserService.deleteUserFromProject(user, project, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_right_can_add_a_user_to_a_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, USER_ACL_READ, READ);
        expectForbidden(() -> secUserService.addUserToProject(user, project, false));
        expectForbidden(() -> secUserService.deleteUserFromProject(user, project, false));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_give_admin_right_to_a_user() {
        User user = builder.given_a_user();
        expectForbidden(() ->
                secUserSecRoleService.add(builder.given_a_not_persisted_user_role(user, "ROLE_ADMIN").toJsonObject()));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void user_can_give_admin_right_to_a_user() {
        User user = builder.given_a_user();
        expectOK(() ->
                secUserSecRoleService.add(builder.given_a_not_persisted_user_role(user, "ROLE_ADMIN").toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_remove_right_to_from_a_user() {
        User user = builder.given_a_user();
        expectForbidden(() ->
                secUserSecRoleService.delete(builder.given_a_user_role(user), null, null, false));
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_remove_right_from_a_user() {
        User user = builder.given_a_user();
        expectOK(() ->
                secUserSecRoleService.delete(builder.given_a_user_role(user), null, null, false));
    }
}
