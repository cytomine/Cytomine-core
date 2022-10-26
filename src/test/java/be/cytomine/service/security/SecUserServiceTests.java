package be.cytomine.service.security;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.UserJob;
import be.cytomine.domain.social.*;
import be.cytomine.dto.AuthInformation;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.dto.AreaDTO;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.service.social.ImageConsultationService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.service.social.UserPositionService;
import be.cytomine.service.social.UserPositionServiceTests;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.request.RequestContextHolder;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.BasicInstanceBuilder.ROLE_ADMIN;
import static be.cytomine.BasicInstanceBuilder.ROLE_GUEST;
import static be.cytomine.repository.security.SecRoleRepository.ROLE_USER;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@Transactional
public class SecUserServiceTests {

    @Autowired
    SecUserService secUserService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    ImageConsultationService imageConsultationService;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    PersistentConnectionRepository persistentConnectionRepository;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    SequenceService sequenceService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserPositionService userPositionService;

    @BeforeEach
    public void init() {
        persistentConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
        persistentImageConsultationRepository.deleteAll();
        persistentProjectConnectionRepository.deleteAll();
        projectConnectionRepository.deleteAll();
        lastUserPositionRepository.deleteAll();
        persistentUserPositionRepository.deleteAll();
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }

    PersistentImageConsultation given_a_persistent_image_consultation(SecUser user, ImageInstance imageInstance, Date created) {
        return imageConsultationService.add(user, imageInstance.getId(), "xxx", "mode", created);
    }

    PersistentConnection given_a_last_connection(SecUser user, Long idProject, Date date) {
            LastConnection connection = new LastConnection();
            connection.setId(sequenceService.generateID());
            connection.setUser(user.getId());
            connection.setDate(date);
            connection.setCreated(date);
            connection.setProject(idProject);
            lastConnectionRepository.insert(connection); //don't use save (stateless collection)

            PersistentConnection connectionPersist = new PersistentConnection();
            connectionPersist.setId(sequenceService.generateID());
            connectionPersist.setUser(user.getId());
            connectionPersist.setCreated(date);
            connectionPersist.setProject(idProject);
            connectionPersist.setSession(RequestContextHolder.currentRequestAttributes().getSessionId());
            persistentConnectionRepository.insert(connectionPersist); //don't use save (stateless collection)
        return connectionPersist;
    }


    @Test
    void find_secuser_with_success() {
        User user = builder.given_a_user();
        assertThat(secUserService.find(user.getId())).isPresent().contains(user);
    }

    @Test
    void find_unexisting_user_return_empty() {
        assertThat(secUserService.find(0L)).isEmpty();
    }

    @Test
    void find_user_with_success() {
        User user = builder.given_a_user();
        assertThat(secUserService.findUser(user.getId())).isPresent().contains(user);
    }

    @Test
    void find_user_by_username() {
        User user = builder.given_a_user();
        assertThat(secUserService.findByUsername(user.getUsername())).isPresent().contains(user);
        assertThat(secUserService.findByUsername(user.getUsername().toUpperCase(Locale.ROOT))).isPresent().contains(user);
        assertThat(secUserService.findByUsername(user.getUsername().toLowerCase(Locale.ROOT))).isPresent().contains(user);
    }

    @Test
    void find_user_by_email() {
        User user = builder.given_a_user();
        assertThat(secUserService.findByEmail(user.getEmail())).isPresent().contains(user);
        assertThat(secUserService.findByEmail(user.getEmail().toUpperCase(Locale.ROOT))).isPresent().contains(user);
        assertThat(secUserService.findByEmail(user.getEmail().toLowerCase(Locale.ROOT))).isPresent().contains(user);
    }

    @Test
    void find_user_by_public_key() {
        User user = builder.given_a_user();
        assertThat(secUserService.findByPublicKey(user.getPublicKey())).isPresent().contains(user);
    }

    @Test
    void get_auth_roles_for_user() {
        User user = builder.given_a_user();
        AuthInformation authInformation = secUserService.getAuthenticationRoles(user);
        assertThat(authInformation.getAdmin()).isFalse();
        assertThat(authInformation.getUser()).isTrue();
        assertThat(authInformation.getGuest()).isFalse();

        assertThat(authInformation.getAdminByNow()).isFalse();
        assertThat(authInformation.getUserByNow()).isTrue();
        assertThat(authInformation.getGuestByNow()).isFalse();
    }


    @Test
    void get_auth_roles_for_guest() {
        User user = builder.given_a_guest();
        AuthInformation authInformation = secUserService.getAuthenticationRoles(user);
        assertThat(authInformation.getAdmin()).isFalse();
        assertThat(authInformation.getUser()).isFalse();
        assertThat(authInformation.getGuest()).isTrue();

        assertThat(authInformation.getAdminByNow()).isFalse();
        assertThat(authInformation.getUserByNow()).isFalse();
        assertThat(authInformation.getGuestByNow()).isTrue();
    }

    @Test
    void get_auth_roles_for_superamdin() {
        User user = builder.given_superadmin();
        AuthInformation authInformation = secUserService.getAuthenticationRoles(user);
        assertThat(authInformation.getAdmin()).isTrue();
        assertThat(authInformation.getUser()).isFalse();
        assertThat(authInformation.getGuest()).isFalse();

        assertThat(authInformation.getAdminByNow()).isTrue();
        assertThat(authInformation.getUserByNow()).isFalse();
        assertThat(authInformation.getGuestByNow()).isFalse();
    }

    @Test
    void get_auth_roles_for_admin() {
        User user = builder.given_a_admin();
        AuthInformation authInformation = secUserService.getAuthenticationRoles(user);
        assertThat(authInformation.getAdmin()).isTrue();
        assertThat(authInformation.getUser()).isFalse();
        assertThat(authInformation.getGuest()).isFalse();

        assertThat(authInformation.getAdminByNow()).isFalse();
        assertThat(authInformation.getUserByNow()).isFalse();
        assertThat(authInformation.getGuestByNow()).isFalse();
    }


    @Test
    void list_users_with_no_filters_no_extension() {

        UserSearchExtension userSearchExtension = new UserSearchExtension();

        Page<Map<String, Object>> list = secUserService.list(userSearchExtension, new ArrayList<>(), "created", "desc", 0L, 0L);

        assertThat(list.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(list.getContent().stream()
                .map(x -> x.get("id"))).contains(builder.given_superadmin().getId());


    }

    @Test
    void list_users_with_with_multisearch_filters() {

        UserSearchExtension userSearchExtension = new UserSearchExtension();

        Page<Map<String, Object>> list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "superad"))), "created", "desc", 0L, 0L);

        assertThat(list.getContent().stream()
                .map(x -> x.get("id"))).contains(builder.given_superadmin().getId());

        list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, builder.given_superadmin().getEmail()))), "created", "desc", 0L, 0L);

        assertThat(list.getContent().stream()
                .map(x -> x.get("id"))).contains(builder.given_superadmin().getId());

        list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "johndoe@example.com"))), "created", "desc", 0L, 0L);

        assertThat(list.getContent().stream()
                .map(x -> x.get("id"))).doesNotContain(builder.given_superadmin().getId());
    }

    @Test
    void list_users_with_roles() {

        UserSearchExtension userSearchExtension = new UserSearchExtension();

        userSearchExtension.setWithRoles(true);
        Page<Map<String, Object>> list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "superadmin"))), "role", "asc", 0L, 0L);
        assertThat(list.getContent()).hasSize(1);
        assertThat(list.getContent().get(0).get("id")).isEqualTo(builder.given_superadmin().getId());
        assertThat(list.getContent().get(0).get("role")).isEqualTo("ROLE_SUPER_ADMIN");
        assertThat(list.getContent().get(0).get("algo")).isEqualTo(false);
        // FAIL because we get superadminjob too. It should not be return as we don't return result with job_id
    }

    @Test
    void list_users_with_roles_search_filter() {

        User guest = builder.given_a_guest();
        User user = builder.given_a_user();
        User admin = builder.given_a_admin();
        User publicU = builder.given_a_public();

        UserSearchExtension userSearchExtension = new UserSearchExtension();

        userSearchExtension.setWithRoles(true);
        Page<Map<String, Object>> list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of(new SearchParameterEntry("role", SearchOperation.in, "ROLE_GUEST,ROLE_ADMIN"))), "role", "asc", 0L, 0L);
        assertThat(list.getContent().size()).isGreaterThanOrEqualTo(2);

        assertThat(list.getContent().stream().map(x -> x.get("id"))).contains(guest.getId(), admin.getId()).doesNotContain(user.getId(),publicU.getId());

        list = secUserService.list(userSearchExtension,
                new ArrayList<>(List.of()), "role", "asc", 0L, 0L);
        assertThat(list.getContent().size()).isGreaterThanOrEqualTo(2);

        assertThat(list.getContent().stream().map(x -> x.get("id"))).contains(guest.getId(), admin.getId(),user.getId(),publicU.getId());
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(guest.getId())).findFirst().get().get("role")).isEqualTo(ROLE_GUEST);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(guest.getId())).findFirst().get().get("public")).isEqualTo(false);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(admin.getId())).findFirst().get().get("role")).isEqualTo(ROLE_ADMIN);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(admin.getId())).findFirst().get().get("public")).isEqualTo(false);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(user.getId())).findFirst().get().get("role")).isEqualTo(ROLE_USER);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(user.getId())).findFirst().get().get("public")).isEqualTo(false);
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(publicU.getId())).findFirst().get().get("role")).isEqualTo(ROLE_USER); // public user is a user
        assertThat(list.getContent().stream().filter(x -> x.get("id").equals(publicU.getId())).findFirst().get().get("public")).isEqualTo(true);
    }

    @Test
    void list_users_with_sort_username() {

        User user1 = builder.given_a_user("list_users_with_sort_username1");
        User user2 = builder.given_a_user("list_users_with_sort_username2");

        Page<Map<String, Object>> list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_sort_username"))), "username", "asc", 0L, 0L);
        assertThat(list.getContent()).hasSize(2);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user1.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user2.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_sort_username"))), "username", "desc", 0L, 0L);
        assertThat(list.getContent()).hasSize(2);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user2.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user1.getUsername());
    }

    @Test
    void list_users_with_page() {

        User user1 = builder.given_a_user("list_users_with_page1");
        User user2 = builder.given_a_user("list_users_with_page2");
        User user3 = builder.given_a_user("list_users_with_page3");
        User user4 = builder.given_a_user("list_users_with_page4");
        User user5 = builder.given_a_user("list_users_with_page5");


        Page<Map<String, Object>> list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_page"))), "username", "asc", 0L, 0L);
        assertThat(list.getContent()).hasSize(5);
        assertThat(list.getTotalElements()).isEqualTo(5);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user1.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user2.getUsername());
        assertThat(list.getContent().get(2).get("username")).isEqualTo(user3.getUsername());
        assertThat(list.getContent().get(3).get("username")).isEqualTo(user4.getUsername());
        assertThat(list.getContent().get(4).get("username")).isEqualTo(user5.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_page"))), "username", "asc", 3L, 0L);
        assertThat(list.getContent()).hasSize(3);
        assertThat(list.getTotalElements()).isEqualTo(5);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user1.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user2.getUsername());
        assertThat(list.getContent().get(2).get("username")).isEqualTo(user3.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_page"))), "username", "asc", 4L, 2L);
        assertThat(list.getContent()).hasSize(3);
        assertThat(list.getTotalElements()).isEqualTo(5);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user3.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user4.getUsername());
        assertThat(list.getContent().get(2).get("username")).isEqualTo(user5.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_page"))), "username", "asc", 4L, 4L);
        assertThat(list.getContent()).hasSize(1);
        assertThat(list.getTotalElements()).isEqualTo(5);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user5.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(List.of(new SearchParameterEntry("fullName", SearchOperation.like, "list_users_with_page"))), "username", "asc", 5L, 6L);
        assertThat(list.getContent()).hasSize(0);
        assertThat(list.getTotalElements()).isEqualTo(5);
    }

    @Test
    void list_users_with_last_connection() {

        User user_with_old_connection = builder.given_a_user("user_with_old_connection");
        User user_with_online_connection = builder.given_a_user("user_with_online_connection");
        User user_with_online_fresh_connection = builder.given_a_user("user_with_online_fresh_connection");
        User user_with_no_connection = builder.given_a_user("user_with_no_connection");

        given_a_last_connection(user_with_old_connection, DateUtils.addSeconds(new Date(), -1000));
        given_a_last_connection(user_with_online_connection, DateUtils.addSeconds(new Date(), -10));
        given_a_last_connection(user_with_online_fresh_connection, DateUtils.addSeconds(new Date(), -1));

        Page<Map<String, Object>> list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), null,"lastConnection", "desc", 0L, 0L);
        assertThat(list.getContent().size()).isGreaterThanOrEqualTo(3);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user_with_online_fresh_connection.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user_with_online_connection.getUsername());
        assertThat(list.getContent().get(2).get("username")).isEqualTo(user_with_old_connection.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), true,"lastConnection", "desc", 0L, 0L);
        assertThat(list.getContent()).hasSize(2);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user_with_online_fresh_connection.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user_with_online_connection.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), true,"lastConnection", "asc", 0L, 0L);
        assertThat(list.getContent()).hasSize(2);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user_with_online_connection.getUsername());
        assertThat(list.getContent().get(1).get("username")).isEqualTo(user_with_online_fresh_connection.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), true,"lastConnection", "desc", 1L, 0L);
        assertThat(list.getContent()).hasSize(1);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user_with_online_fresh_connection.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), true,"lastConnection", "desc", 2L, 1L);
        assertThat(list.getContent()).hasSize(1);
        assertThat(list.getContent().get(0).get("username")).isEqualTo(user_with_online_connection.getUsername());

        list = secUserService.list(new UserSearchExtension(),
                new ArrayList<>(), false,"lastConnection", "desc", 200000L, 0L);
        assertThat(list.getContent().stream().map(x -> x.get("username")))
                .contains(user_with_old_connection.getUsername(), user_with_no_connection.getUsername())
                .doesNotContain(user_with_online_fresh_connection.getUsername(),user_with_online_connection.getUsername());
    }

    @Test
    void list_user_by_project_with_success() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        Project projectWhereUserIsContributor = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();
        Project projectWithTwoUsers = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsContributor, "superadmin", WRITE);
        builder.addUserToProject(projectWithTwoUsers, "superadmin", WRITE);

        User anotherUser = builder.given_a_user();
        builder.addUserToProject(projectWhereUserIsMissing, anotherUser.getUsername(), WRITE);
        builder.addUserToProject(projectWithTwoUsers, anotherUser.getUsername(), WRITE);

        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = secUserService.listUsersByProject(projectWhereUserIsManager, new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("manager");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(anotherUser.getId());

        page = secUserService.listUsersByProject(projectWhereUserIsContributor, new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("contributor");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(anotherUser.getId());

        page = secUserService.listUsersByProject(projectWhereUserIsMissing, new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(anotherUser.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("contributor");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(user.getId());

        page = secUserService.listUsersByProject(projectWithTwoUsers, new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(anotherUser.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
    }

    @Test
    void list_user_extended_with_empty_extension() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        Project projectWhereUserIsContributor = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();
        Project projectWithTwoUsers = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsContributor, "superadmin", WRITE);
        builder.addUserToProject(projectWithTwoUsers, "superadmin", WRITE);

        User anotherUser = builder.given_a_user();
        builder.addUserToProject(projectWhereUserIsMissing, anotherUser.getUsername(), WRITE);
        builder.addUserToProject(projectWithTwoUsers, anotherUser.getUsername(), WRITE);

        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<JsonObject> page = secUserService.listUsersExtendedByProject(projectWhereUserIsManager, new UserSearchExtension(), new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("manager");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(anotherUser.getId());

        page = secUserService.listUsersExtendedByProject(projectWhereUserIsContributor, new UserSearchExtension(), new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("contributor");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(anotherUser.getId());

        page = secUserService.listUsersExtendedByProject(projectWhereUserIsMissing, new UserSearchExtension(), new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(anotherUser.getId());
        assertThat(page.getContent().get(0).get("role")).isEqualTo("contributor");
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(user.getId());

        page = secUserService.listUsersExtendedByProject(projectWithTwoUsers, new UserSearchExtension(), new ArrayList<>(), "id", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(anotherUser.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(user.getId());
    }


    @Test
    void list_user_extended_with_last_image_name() {
        User userWhoHasOpenImage = builder.given_a_user();
        User userWhoHasOpenImageAfter = builder.given_a_user();
        User userNeverOpenImage = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, userWhoHasOpenImage.getUsername(), READ);
        builder.addUserToProject(project, userWhoHasOpenImageAfter.getUsername(), READ);
        builder.addUserToProject(project, userNeverOpenImage.getUsername(), WRITE);

        ImageInstance imageInstance = builder.given_an_image_instance(project);
        imageInstance.setInstanceFilename(UUID.randomUUID().toString());

        given_a_persistent_image_consultation(userNeverOpenImage, imageInstance, DateUtils.addDays(new Date(), -2));
        given_a_persistent_image_consultation(userWhoHasOpenImageAfter, imageInstance, DateUtils.addDays(new Date(), -1));

        UserSearchExtension userSearchExtension = new UserSearchExtension();
        userSearchExtension.setWithLastImage(true);
        Page<JsonObject> page = secUserService.listUsersExtendedByProject(
                project, userSearchExtension, new ArrayList<>(), "lastImageName", "desc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().stream().map(x -> x.getJSONAttrLong("id"))).contains(userWhoHasOpenImage.getId(), userWhoHasOpenImageAfter.getId());
        assertThat(page.getContent().stream().map(x -> x.getJSONAttrLong("lastImage"))).contains(imageInstance.getId());
    }


    @Test
    void list_user_extended_with_last_connection() {
        User userWhoHasOpenProject = builder.given_a_user();
        User userWhoHasOpenProjectAfter = builder.given_a_user();
        User userNeverOpenProject = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, userWhoHasOpenProject.getUsername(), READ);
        builder.addUserToProject(project, userWhoHasOpenProjectAfter.getUsername(), READ);
        builder.addUserToProject(project, userNeverOpenProject.getUsername(), WRITE);

        PersistentProjectConnection userWhoHasOpenProjectConnection = given_a_persistent_connection_in_project(userWhoHasOpenProject, project, DateUtils.addDays(new Date(), -2));
        PersistentProjectConnection userWhoHasOpenProjectAfterConnection = given_a_persistent_connection_in_project(userWhoHasOpenProjectAfter, project, DateUtils.addDays(new Date(), -1));

        UserSearchExtension userSearchExtension = new UserSearchExtension();
        userSearchExtension.setWithLastConnection(true);
        Page<JsonObject> page = secUserService.listUsersExtendedByProject(
                project, userSearchExtension, new ArrayList<>(), "lastConnection", "desc", 0L, 0L);
        System.out.println(page);
        System.out.println(page.getContent());
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProjectAfter.getId());
        assertThat(page.getContent().get(0).getJSONAttrDate("lastConnection")).isEqualTo(userWhoHasOpenProjectAfterConnection.getCreated());
        assertThat(page.getContent().get(1).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProject.getId());
        assertThat(page.getContent().get(1).getJSONAttrDate("lastConnection")).isEqualTo(userWhoHasOpenProjectConnection.getCreated());
        assertThat(page.getContent().get(2).getJSONAttrLong("id")).isEqualTo(userNeverOpenProject.getId());
        assertThat(page.getContent().get(2).getJSONAttrStr("lastImage")).isNull();

        page = secUserService.listUsersExtendedByProject(
                project, userSearchExtension, new ArrayList<>(), "lastConnection", "asc", 0L, 0L);
        System.out.println(page);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getJSONAttrLong("id")).isEqualTo(userNeverOpenProject.getId());
        assertThat(page.getContent().get(0).getJSONAttrDate("lastConnection")).isNull();
        assertThat(page.getContent().get(1).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProject.getId());
        assertThat(page.getContent().get(1).getJSONAttrDate("lastConnection")).isEqualTo(userWhoHasOpenProjectConnection.getCreated());
        assertThat(page.getContent().get(2).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProjectAfter.getId());
        assertThat(page.getContent().get(2).getJSONAttrDate("lastConnection")).isEqualTo(userWhoHasOpenProjectAfterConnection.getCreated());
    }

    @Test
    void list_user_extended_with_connection_frequency() {
        User userWhoHasOpenOnce = builder.given_a_user();
        User userWhoHasOpenProject11x = builder.given_a_user();
        User userNeverOpenProject = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, userWhoHasOpenOnce.getUsername(), READ);
        builder.addUserToProject(project, userWhoHasOpenProject11x.getUsername(), READ);
        builder.addUserToProject(project, userNeverOpenProject.getUsername(), WRITE);

        PersistentProjectConnection userWhoHasOpenProjectConnection = given_a_persistent_connection_in_project(userWhoHasOpenOnce, project, DateUtils.addDays(new Date(), -2));
        for (int i = 0 ;i<11;i++) {
            given_a_persistent_connection_in_project(userWhoHasOpenProject11x, project, DateUtils.addDays(new Date(), -1));
        }

        UserSearchExtension userSearchExtension = new UserSearchExtension();
        userSearchExtension.setWithNumberConnections(true);
        Page<JsonObject> page = secUserService.listUsersExtendedByProject(
                project, userSearchExtension, new ArrayList<>(), "frequency", "desc", 0L, 0L);
        System.out.println(page);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProject11x.getId());
        assertThat(page.getContent().get(0).getJSONAttrInteger("numberConnections")).isEqualTo(11);
        assertThat(page.getContent().get(1).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenOnce.getId());
        assertThat(page.getContent().get(1).getJSONAttrInteger("numberConnections")).isEqualTo(1);
        assertThat(page.getContent().get(2).getJSONAttrLong("id")).isEqualTo(userNeverOpenProject.getId());
        assertThat(page.getContent().get(2).getJSONAttrInteger("numberConnections")).isEqualTo(0);

        page = secUserService.listUsersExtendedByProject(
                project, userSearchExtension, new ArrayList<>(), "frequency", "asc", 0L, 0L);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getJSONAttrLong("id")).isEqualTo(userNeverOpenProject.getId());
        assertThat(page.getContent().get(0).getJSONAttrInteger("numberConnections")).isEqualTo(0);
        assertThat(page.getContent().get(1).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenOnce.getId());
        assertThat(page.getContent().get(1).getJSONAttrInteger("numberConnections")).isEqualTo(1);
        assertThat(page.getContent().get(2).getJSONAttrLong("id")).isEqualTo(userWhoHasOpenProject11x.getId());
        assertThat(page.getContent().get(2).getJSONAttrInteger("numberConnections")).isEqualTo(11);
    }

    @Test
    void list_project_admins() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        Project projectWhereUserIsContributor = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();
        Project projectWithTwoUsers = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsContributor, "superadmin", WRITE);
        builder.addUserToProject(projectWithTwoUsers, "superadmin", WRITE);

        User anotherUser = builder.given_a_user();
        builder.addUserToProject(projectWhereUserIsMissing, anotherUser.getUsername(), WRITE);
        builder.addUserToProject(projectWithTwoUsers, anotherUser.getUsername(), WRITE);

        assertThat(secUserService.listAdmins(projectWhereUserIsManager))
                .contains(user).doesNotContain(anotherUser);
        assertThat(secUserService.listAdmins(projectWhereUserIsContributor))
                .doesNotContain(user);
    }

    @Test
    void list_project_users() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        Project projectWhereUserIsContributor = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();
        Project projectWithTwoUsers = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsContributor, "superadmin", WRITE);
        builder.addUserToProject(projectWithTwoUsers, "superadmin", WRITE);

        User anotherUser = builder.given_a_user();
        builder.addUserToProject(projectWhereUserIsMissing, anotherUser.getUsername(), WRITE);
        builder.addUserToProject(projectWithTwoUsers, anotherUser.getUsername(), WRITE);

        assertThat(secUserService.listUsers(projectWhereUserIsManager)).contains(user).doesNotContain(anotherUser);
        assertThat(secUserService.listUsers(projectWhereUserIsContributor)).contains(user).doesNotContain(anotherUser);
        assertThat(secUserService.listUsers(projectWithTwoUsers)).contains(user, anotherUser);
    }

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void change_password() {
        User user = builder.given_a_user();
        user.setPassword(passwordEncoder.encode("oldPassword"));
        builder.persistAndReturn(user);

        secUserService.changeUserPassword(user, "newPassword");

        assertThat(passwordEncoder.matches("newPassword", user.getPassword())).isTrue();
    }

    @Test
    void check_password() {
        User user = builder.given_a_user();
        user.setPassword(passwordEncoder.encode("newPassword"));
        builder.persistAndReturn(user);

        assertThat(secUserService.isUserPassword(user, "newPassword")).isTrue();
        assertThat(secUserService.isUserPassword(user, "badPassword")).isFalse();
    }

    @Test
    void find_project_creator() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);

        assertThat(secUserService.findCreator(projectWhereUserIsManager)).contains(user);
    }

    @Disabled("software package")
    @Test
    void list_project_users_show_userjob() {

    }

    @Test
    void list_ontology_users() {
        User user = builder.given_superadmin();

        Project projectWhereUserIsManager = builder.given_a_project();
        Project projectWhereUserIsContributor = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsManager, "superadmin", ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsContributor, "superadmin", WRITE);

        assertThat(secUserService.listUsers(projectWhereUserIsManager.getOntology()))
                .contains(user);
        assertThat(secUserService.listUsers(projectWhereUserIsContributor.getOntology()))
                .contains(user);

    }

    @Test
    void list_storage_users() {
        Storage storage = builder.given_a_storage(builder.given_superadmin());

        assertThat(secUserService.listUsers(storage))
                .contains(builder.given_superadmin());

    }

    @Test
    void list_all_project_users() {
        User user = builder.given_superadmin();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, "superadmin", WRITE);

        assertThat(secUserService.listAll(project))
                .contains(user);
    }

    @Disabled("software package")
    @Test
    void list_all_project_users_with_user_job() {
    }

    @Test
    void lock_user() {
       User user = builder.given_a_user();

       assertThat(user.getEnabled()).isTrue();
       secUserService.lock(user);
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    void lock_user_already_locked() {
        User user = builder.given_a_user();
        user.setEnabled(false);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            secUserService.lock(user);
        });
    }

    @Test
    void unlock_user() {
        User user = builder.given_a_user();
        user.setEnabled(false);

        assertThat(user.getEnabled()).isFalse();
        secUserService.unlock(user);
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    void unlock_user_already_unlocked() {
        User user = builder.given_a_user();
        user.setEnabled(true);

        Assertions.assertThrows(WrongArgumentException.class, () -> {
            secUserService.unlock(user);
        });
    }


    @Test
    void list_layers() {
        User user = builder.given_a_user();
        User anotherUserInProject = builder.given_a_user();
        User anotherUserNotInProject = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, user.getUsername(), WRITE);
        builder.addUserToProject(project, anotherUserInProject.getUsername(), WRITE);

        assertThat(secUserService.listLayers(project, builder.given_an_image_instance(project)).stream().map(x -> x.getJSONAttrLong("id")))
                .contains(user.getId(), anotherUserInProject.getId())
                .doesNotContain(anotherUserNotInProject.getId());
    }

    @WithMockUser("user")
    @Test
    void list_layers_with_project_with_private_admin_layer() {
        User user = builder.given_default_user();
        User adminInProject = builder.given_a_user();

        Project project = builder.given_a_project();
        project.setHideAdminsLayers(true);

        builder.addUserToProject(project, user.getUsername(), WRITE);
        builder.addUserToProject(project, adminInProject.getUsername(), ADMINISTRATION);

        assertThat(secUserService.listLayers(project, builder.given_an_image_instance(project)).stream().map(x -> x.getJSONAttrLong("id")))
                .hasSize(1)
                .contains(user.getId())
                .doesNotContain(adminInProject.getId());
    }

    @WithMockUser("user")
    @Test
    void list_layers_with_project_with_private_user_layer() {
        User user = builder.given_default_user();
        User userInProject = builder.given_a_user();

        Project project = builder.given_a_project();
        project.setHideUsersLayers(true);

        builder.addUserToProject(project, user.getUsername(), WRITE);
        builder.addUserToProject(project, userInProject.getUsername(), WRITE);

        assertThat(secUserService.listLayers(project, builder.given_an_image_instance(project)).stream().map(x -> x.getJSONAttrLong("id")))
                .hasSize(1)
                .contains(user.getId())
                .doesNotContain(userInProject.getId());
    }

    @WithMockUser("user")
    @Test
    void list_layers_with_project_with_private_user_layer_with_project_admin_role() {
        User user = builder.given_default_user();
        User userInProject = builder.given_a_user();

        Project project = builder.given_a_project();
        project.setHideUsersLayers(true);

        builder.addUserToProject(project, user.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, userInProject.getUsername(), WRITE);

        assertThat(secUserService.listLayers(project, builder.given_an_image_instance(project)).stream().map(x -> x.getJSONAttrLong("id")))
                .hasSize(2)
                .contains(user.getId(), userInProject.getId());
    }

    @Test
    void list_online_user() {
        User userOnline = builder.given_default_user();
        User userOffline = builder.given_a_user();

        assertThat(secUserService.getAllOnlineUsers()).isEmpty();
        given_a_last_connection(userOnline, null, new Date());

        assertThat(secUserService.getAllOnlineUsers()).contains(userOnline)
                .doesNotContain(userOffline);
    }

    @Test
    void list_online_user_for_project() {
        User userOnline = builder.given_default_user();
        User userOnlineButOnDifferentProject = builder.given_a_user();
        User userOffline = builder.given_a_user();

        Project project = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        given_a_last_connection(userOffline, project.getId(), DateUtils.addDays(new Date(), -15));
        given_a_last_connection(userOnline, project.getId(), DateUtils.addSeconds(new Date(), -15));
        given_a_last_connection(userOnlineButOnDifferentProject, anotherProject.getId(), DateUtils.addSeconds(new Date(), -10));


        assertThat(secUserService.getAllOnlineUserIds(project)).contains(userOnline.getId())
                .doesNotContain(userOnlineButOnDifferentProject.getId(), userOffline.getId());
        assertThat(secUserService.getAllOnlineUsers(project)).contains(userOnline)
                .doesNotContain(userOnlineButOnDifferentProject, userOffline);
    }


    @Test
    void list_friend_users() {
        User user = builder.given_default_user();
        User userFriend = builder.given_a_user();
        User userNotFriend = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, userFriend.getUsername(), READ);

        assertThat(secUserService.getAllFriendsUsers(user)).contains(userFriend)
                .doesNotContain(userNotFriend);
    }

    @Test
    void list_friend_users_offline() {
        User user = builder.given_default_user();
        User userFriendOnline = builder.given_a_user();
        User userFriendOffline = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, userFriendOnline.getUsername(), READ);
        builder.addUserToProject(project, userFriendOffline.getUsername(), READ);

        given_a_last_connection(userFriendOffline, project.getId(), DateUtils.addDays(new Date(), -15));
        given_a_last_connection(userFriendOnline, project.getId(), DateUtils.addSeconds(new Date(), -15));

        assertThat(secUserService.getAllFriendsUsersOnline(user)).contains(userFriendOnline)
                .doesNotContain(userFriendOffline);
    }

    @Test
    void list_friend_users_offline_on_a_project() {
        User user = builder.given_default_user();
        User userFriendOnline = builder.given_a_user();
        User userFriendOnlineButOnAnotherProject = builder.given_a_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, userFriendOnline.getUsername(), READ);
        builder.addUserToProject(project, userFriendOnlineButOnAnotherProject.getUsername(), READ);

        given_a_last_connection(userFriendOnlineButOnAnotherProject, builder.given_a_project().getId(), DateUtils.addSeconds(new Date(), -15));
        given_a_last_connection(userFriendOnline, project.getId(), DateUtils.addSeconds(new Date(), -15));

        assertThat(secUserService.getAllFriendsUsersOnline(user, project)).contains(userFriendOnline)
                .doesNotContain(userFriendOnlineButOnAnotherProject);
    }

    @Test
    void list_all_users_id_of_a_project() {
        Project project = builder.given_a_project();
        User user1 = builder.given_a_user("Paul");
        User user2 = builder.given_a_user("Bob");

        builder.addUserToProject(project, user1.getUsername());
        builder.addUserToProject(project, user2.getUsername());

        String userIds = secUserService.getUsersIdsFromProject(project.getId());
        String expectedUserIds = user1.getId() + "," + user2.getId() + ",";

        assertThat(expectedUserIds).isEqualTo(userIds);
    }

    @Test
    void list_online_user_for_project_wit_their_activities() {
        User userOnline = builder.given_default_user();

        Project project = builder.given_a_project();

        builder.addUserToProject(project, userOnline.getUsername());

        PersistentProjectConnection lastConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addSeconds(new Date(), -15));

        PersistentImageConsultation consultation = given_a_persistent_image_consultation(userOnline, builder.given_an_image_instance(project), new Date());

        List<JsonObject> allOnlineUserWithTheirPositions = secUserService.getUsersWithLastActivities(project);
        assertThat(allOnlineUserWithTheirPositions).hasSize(1);
        assertThat(allOnlineUserWithTheirPositions.get(0).get("id")).isEqualTo(userOnline.getId());
        assertThat(allOnlineUserWithTheirPositions.get(0).get("lastImageId")).isEqualTo(consultation.getImage());
        assertThat(allOnlineUserWithTheirPositions.get(0).get("lastImageName")).isNotNull();
        assertThat(allOnlineUserWithTheirPositions.get(0).get("lastConnection")).isNotNull();
        assertThat(allOnlineUserWithTheirPositions.get(0).get("frequency")).isEqualTo(1);
    }


    @Test
    void list_online_user_for_project_wit_their_position() {
        User userOnline = builder.given_default_user();
        User userOnlineButOnDifferentProject = builder.given_a_user();
        User userOffline = builder.given_a_user();

        Project project = builder.given_a_project();
        Project anotherProject = builder.given_a_project();

        given_a_last_connection(userOffline, project.getId(), DateUtils.addDays(new Date(), -15));
        given_a_last_connection(userOnline, project.getId(), DateUtils.addSeconds(new Date(), -15));
        given_a_last_connection(userOnlineButOnDifferentProject, anotherProject.getId(), DateUtils.addSeconds(new Date(), -10));

        given_a_persistent_user_position(DateUtils.addSeconds(new Date(), -15), userOnline,
                builder.given_a_not_persisted_slice_instance(builder.given_an_image_instance(project), builder.given_an_abstract_slice()), UserPositionServiceTests.USER_VIEW);

        List<JsonObject> allOnlineUserWithTheirPositions = secUserService.getAllOnlineUserWithTheirPositions(project);
        assertThat(allOnlineUserWithTheirPositions.stream().filter(x -> x.getId().equals(userOnline.getId())).findFirst()).isPresent();
        assertThat(allOnlineUserWithTheirPositions.stream().filter(x -> x.getId().equals(userOnline.getId())).findFirst().get().get("position")).isNotNull();
        assertThat(allOnlineUserWithTheirPositions.stream().filter(x -> x.getId().equals(userOnlineButOnDifferentProject.getId())).findFirst()).isEmpty();
        assertThat(allOnlineUserWithTheirPositions.stream().filter(x -> x.getId().equals(userOffline.getId())).findFirst()).isEmpty();
    }

    PersistentUserPosition given_a_persistent_user_position(Date creation, User user, SliceInstance sliceInstance, AreaDTO areaDTO) {
        PersistentUserPosition connection =
                userPositionService.add(creation, user, sliceInstance, sliceInstance.getImage(), areaDTO,
                        1,
                        5.0,
                        false
                );
        return connection;
    }

    @Test
    void list_user_resume_activities() {
        User userOnline = builder.given_default_user();
        Project project = builder.given_a_project();
        builder.addUserToProject(project, userOnline.getUsername());

        PersistentProjectConnection firstConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addDays(new Date(), -15));
        PersistentProjectConnection lastConnection = given_a_persistent_connection_in_project(userOnline, project, DateUtils.addSeconds(new Date(), -15));

        given_a_persistent_image_consultation(userOnline, builder.given_an_image_instance(project), new Date());

        JsonObject data = secUserService.getResumeActivities(project, userOnline);

        assertThat(data.getJSONAttrDate("firstConnection")).isEqualTo(firstConnection.getCreated());
        assertThat(data.getJSONAttrDate("lastConnection")).isEqualTo(lastConnection.getCreated());
        assertThat(data.getJSONAttrInteger("totalAnnotations")).isEqualTo(0);
        assertThat(data.getJSONAttrInteger("totalConnections")).isEqualTo(2);
        assertThat(data.getJSONAttrInteger("totalConsultations")).isEqualTo(1);
        assertThat(data.getJSONAttrInteger("totalAnnotationSelections")).isEqualTo(0);

    }

    @Test
    void fill_not_empty_users_ids_from_project_works(){
        User user = builder.given_a_user();
        Project project = builder.given_a_project_with_user(user);
        String users = secUserService.fillEmptyUserIds(user.getId().toString(), project.getId());
        assertThat(users).isEqualTo(user.getId().toString());
    }

    @Test
    void fill_empty_users_ids_from_project_works(){
        User user1 = builder.given_a_user();
        User user2 = builder.given_a_user();
        Project project = builder.given_a_project_with_user(user1);
        builder.addUserToProject(project, user2.getUsername());
        String users = secUserService.fillEmptyUserIds("", project.getId());
        assertThat(users).isEqualTo(user1.getId().toString() + "," + user2.getId().toString() + ",");
    }

    @Test
    void add_valid_user() {
        User user = builder.given_a_not_persisted_user();


        CommandResponse commandResponse = secUserService.add(user.toJsonObject().withChange("password", "kikoulol"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(secUserService.findByUsername(user.getUsername())).isPresent();
    }

    @Test
    void add_user_with_already_existing_username() {
        User sameUsername = builder.given_a_user();
        User user = builder.given_a_not_persisted_user();
        user.setUsername(sameUsername.getUsername());

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secUserService.add(user.toJsonObject());
        });
    }

    @Test
    void add_user_with_already_existing_username_different_case() {
        User sameUsername = builder.given_a_user();
        User user = builder.given_a_not_persisted_user();
        user.setUsername(sameUsername.getUsername().toUpperCase(Locale.ROOT));

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secUserService.add(user.toJsonObject());
        });

        user.setUsername(sameUsername.getUsername().toLowerCase(Locale.ROOT));

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secUserService.add(user.toJsonObject());
        });
    }

    @Test
    @Disabled("should we allow this (grails version accept same email)?")
    void add_user_with_already_existing_email_different_case() {
        User sameEmail = builder.given_a_user();
        User user = builder.given_a_not_persisted_user();
        user.setEmail(sameEmail.getEmail().toUpperCase(Locale.ROOT));

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secUserService.add(user.toJsonObject().withChange("password", "password"));
        });

        user.setEmail(sameEmail.getEmail().toLowerCase(Locale.ROOT));

        Assertions.assertThrows(AlreadyExistException.class, () -> {
            secUserService.add(user.toJsonObject().withChange("password", "password"));
        });
    }


    @Test
    void edit_valid_user_with_success() {
        User user = builder.given_a_user();
        String originalPasswordHash = user.getPassword();

        CommandResponse commandResponse = secUserService.update(user, user.toJsonObject().withChange("lastname", "NEW LASTNAME"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        Optional<SecUser> edited = secUserService.findByUsername(user.getUsername());
        assertThat(((User)edited.get()).getLastname()).isEqualTo("NEW LASTNAME");
        assertThat(((User)edited.get()).getPassword()).isEqualTo(originalPasswordHash);
    }


    @Test
    void edit_valid_user_with_password() {
        User user = builder.given_a_user();

        CommandResponse commandResponse = secUserService.update(user, user.toJsonObject().withChange("password", "NEWPASSWORD"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        Optional<SecUser> edited = secUserService.findByUsername(user.getUsername());

        assertThat(passwordEncoder.matches("NEWPASSWORD", edited.get().getPassword())).isTrue();
    }


    @Test
    void delete_valid_user_with_success() {
        User user = builder.given_a_user();

        CommandResponse commandResponse = secUserService.delete(user, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(secUserService.find(user.getId()).isEmpty());
    }

    @Autowired
    StorageService storageService;

    @Test
    void delete_user_with_dependency() {
        User user = builder.given_a_user();

        UserAnnotation userAnnotation = builder.given_a_user_annotation();
        userAnnotation.setUser(user);

        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        annotationTerm.setUser(user);

        ImageInstance imageInstance = builder.given_an_image_instance();
        imageInstance.setUser(user);

        Ontology ontology = builder.given_an_ontology();
        ontology.setUser(user);

        ReviewedAnnotation reviewedAnnotation = builder.given_a_reviewed_annotation();
        reviewedAnnotation.setUser(user);

        UploadedFile uploadedFile = builder.given_a_uploaded_file();
        uploadedFile.setUser(user);

        Storage storage = builder.given_a_storage(user);
        //storageService.initUserStorage(user);

        ProjectDefaultLayer projectDefaultLayer =
                builder.given_a_project_default_layer(builder.given_a_project(), user);

        ProjectRepresentativeUser projectRepresentativeUser =
                builder.given_a_project_representative_user(builder.given_a_project(), user);

        // add another representative so that we can delete the first one
        builder.given_a_project_representative_user(projectRepresentativeUser.getProject(), builder.given_superadmin());

        CommandResponse commandResponse = secUserService.delete(user, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        AssertionsForClassTypes.assertThat(secUserService.find(user.getId()).isEmpty());

        assertThat(entityManager.find(UserAnnotation.class, userAnnotation.getId())).isNull();
        assertThat(entityManager.find(AnnotationTerm.class, annotationTerm.getId())).isNull();
        assertThat(entityManager.find(ImageInstance.class, imageInstance.getId())).isNull();
        assertThat(entityManager.find(Ontology.class, ontology.getId())).isNull();
        assertThat(entityManager.find(ReviewedAnnotation.class, reviewedAnnotation.getId())).isNull();
        assertThat(entityManager.find(UploadedFile.class, uploadedFile.getId())).isNull();
        //assertThat(entityManager.find(Storage.class, storage.getId())).isNull();
        assertThat(entityManager.find(ProjectDefaultLayer.class, projectDefaultLayer.getId())).isNull();
        assertThat(entityManager.find(ProjectRepresentativeUser.class, projectRepresentativeUser.getId())).isNull();
    }

    @Test
    void delete_user_create_with_service_with_dependency() {
        User user = builder.given_a_not_persisted_user();

        CommandResponse commandResponse = secUserService.add(user.toJsonObject().withChange("password", "kikoulol"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);

        assertThat(secUserService.findByUsername(user.getUsername())).isPresent();
        user = (User)secUserService.findByUsername(user.getUsername()).get();
        commandResponse = secUserService.delete(user, null, null, true);
        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }



    @Disabled("software package")
    @Test
    void delete_user_job_with_dependency() {

    }

    @Test
    void add_user_to_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isFalse();

        secUserService.addUserToProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();

        secUserService.addUserToProject(user, project, true);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
    }

    @Test
    void remove_user_from_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();

        secUserService.addUserToProject(user, project, true);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isTrue();

        secUserService.deleteUserFromProject(user, project, true);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isTrue();

        secUserService.deleteUserFromProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isFalse();

    }

    @Test
    void remove_ontology_right_when_removing_user_from_project() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();

        secUserService.addUserToProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isTrue();

        secUserService.deleteUserFromProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isFalse();

    }

    @Test
    void remove_ontology_right_when_removing_user_from_project_keep_right_if_user_has_another_project_with_ontology() {
        User user = builder.given_a_user();
        Project project = builder.given_a_project();

        secUserService.addUserToProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isTrue();

        Project projectWithSameOntology = builder.given_a_project();
        projectWithSameOntology.setOntology(project.getOntology());
        secUserService.addUserToProject(user, projectWithSameOntology, false);

        secUserService.deleteUserFromProject(user, project, false);

        assertThat(permissionService.hasACLPermission(project, user.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project.getOntology(), user.getUsername(), READ)).isTrue();

    }

    @Test
    void add_and_delete_user_to_storage() {
        User user = builder.given_a_user();
        Storage storage = builder.given_a_storage();

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();

        secUserService.addUserToStorage(user, storage);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isTrue();

        secUserService.deleteUserFromStorage(user, storage);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();
    }

    @Test
    void add_and_delete_user_to_storage_with_permission() {
        User user = builder.given_a_user();
        Storage storage = builder.given_a_storage();

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();

        secUserService.addUserToStorage(user, storage, READ);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isTrue();

        secUserService.deleteUserFromStorage(user, storage);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();
    }

    @Test
    void change_storage_storage_permissions() {
        User user = builder.given_a_user();
        Storage storage = builder.given_a_storage();

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isFalse();

        secUserService.changeUserPermission(user, storage, ADMINISTRATION);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isTrue();

        secUserService.changeUserPermission(user, storage, READ);

        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(storage, user.getUsername(), READ)).isTrue();
    }




    @Disabled
    @Test
    void delete_valid_user_job_with_success() {

    }




    PersistentConnection given_a_last_connection(SecUser user, Date date) {
        LastConnection connection = new LastConnection();
        connection.setId(sequenceService.generateID());
        connection.setUser(user.getId());
        connection.setDate(date);
        connection.setCreated(date);
        lastConnectionRepository.insert(connection); //don't use save (stateless collection)

        PersistentConnection connectionPersist = new PersistentConnection();
        connectionPersist.setId(sequenceService.generateID());
        connectionPersist.setUser(user.getId());
        connectionPersist.setCreated(date);
        connectionPersist.setSession(RequestContextHolder.currentRequestAttributes().getSessionId());
        persistentConnectionRepository.insert(connectionPersist); //don't use save (stateless collection)
        return connectionPersist;
    }
}
