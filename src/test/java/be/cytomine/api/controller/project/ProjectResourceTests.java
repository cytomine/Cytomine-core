package be.cytomine.api.controller.project;

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
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.repository.security.ForgotPasswordTokenRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repositorynosql.social.PersistentConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.PermissionService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import liquibase.pro.packaged.A;
import org.apache.commons.lang3.time.DateUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
//@WithMockUser(username = "superadmin")
@WithMockUser("superadmin")
public class ProjectResourceTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private BasicInstanceBuilder builder;

    @Autowired
    private MockMvc restProjectControllerMockMvc;

    @Autowired
    private UserAnnotationService userAnnotationService;

    @Autowired
    private AclRepository aclRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ForgotPasswordTokenRepository forgotPasswordTokenRepository;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    ImageInstanceRepository imageInstanceRepository;

    @Autowired
    SecurityACLService securityACLService;

    @BeforeEach
    public void cleanActivities() {
        persistentProjectConnectionRepository.deleteAll();
    }

    @Test
    @Transactional
    public void list_all_projects() throws Exception {
        Project project = builder.given_a_project();
        restProjectControllerMockMvc.perform(get("/api/project.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+project.getName()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "1000")
                        .param("offset", "0")
                        .param("withLastActivity", "true")
                        .param("withMembersCount", "true")
                        .param("withCurrentUserRoles", "true")
                        .param("numberOfImages[lte]", "10")
                        .param("membersCount[lte]", "10")
                        .param("numberOfAnnotations[lte]", "100")
                        .param("nnumberOfJobAnnotations[lte]", "100")
                        .param("numberOfReviewedAnnotations[lte]", "100")
                        .param("sort", "lastActivity")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.name=='"+project.getName()+"')]").exists());
    }

    @Test
    @Transactional
    public void list_all_projects_with_extension() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project);
        userAnnotationService.add(userAnnotation.toJsonObject());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "10")
                        .param("offset", "0")
                        .param("withLastActivity", "true")
                        .param("withMembersCount", "true")
                        .param("withCurrentUserRoles", "true")
                        .param("sort", "id")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')].lastActivity").hasJsonPath())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')].currentUserRoles").hasJsonPath())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')].currentUserRoles.admin").value(true))
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')].currentUserRoles.representative").value(false))
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')].membersCount").value(1))
        ;
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_members() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithCriteria, builder.given_a_user().getUsername());

        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("withMembersCount", "true")
                        .param("membersCount[gte]", "2")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_number_of_annotations() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        projectWithCriteria.setCountAnnotations(500);
        builder.persistAndReturn(projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("numberOfAnnotations[gte]", "100")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }


    @Test
    @Transactional
    public void list_all_projects_with_filters_number_of_job_annotations() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        projectWithCriteria.setCountJobAnnotations(500);
        builder.persistAndReturn(projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("numberOfJobAnnotations[gte]", "100")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_number_of_images() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        projectWithCriteria.setCountImages(500);
        builder.persistAndReturn(projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("numberOfImages[gte]", "100")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_name() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        builder.persistAndReturn(projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("name[ilike]", projectWithCriteria.getName().substring(5))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_current_role() throws Exception {
        Project projectWhereUserIsAdmin = builder.given_a_project();
        builder.persistAndReturn(projectWhereUserIsAdmin);
        Project projectWhereUserIsSimpleUser = builder.given_a_project();

        builder.addUserToProject(projectWhereUserIsAdmin, builder.given_superadmin().getUsername(), ADMINISTRATION);
        builder.addUserToProject(projectWhereUserIsSimpleUser, builder.given_superadmin().getUsername(), READ);

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("currentUserRole[in]", "contributor,manager")
                        .param("withCurrentUserRoles", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsAdmin.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsSimpleUser.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("currentUserRole[in]", "contributor")
                        .param("withCurrentUserRoles", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsAdmin.getId()+"')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsSimpleUser.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("currentUserRole[in]", "manager")
                        .param("withCurrentUserRoles", "true")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsAdmin.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWhereUserIsSimpleUser.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_tags() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        builder.persistAndReturn(projectWithCriteria);
        TagDomainAssociation tagDomainAssociation1 = builder.given_a_tag_association(builder.given_a_tag(), projectWithCriteria);
        TagDomainAssociation tagDomainAssociation2 = builder.given_a_tag_association(builder.given_a_tag(), projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());
        builder.addUserToProject(projectWithoutCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("tag[in]", tagDomainAssociation1.getTag().getId()+"," + tagDomainAssociation2.getTag().getId())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist());
    }

    @Test
    @Transactional
    public void list_all_projects_with_filters_ontologies() throws Exception {
        Project projectWithCriteria = builder.given_a_project();
        builder.persistAndReturn(projectWithCriteria);
        Project projectWithoutCriteria = builder.given_a_project();
        Project projectWithNoOntology = builder.given_a_project_with_ontology(null);

        builder.addUserToProject(projectWithCriteria, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("ontology[in]", projectWithCriteria.getOntology().getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithNoOntology.getId()+"')]").doesNotExist());

        // all ontology or no ontology
        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("ontology[in]", "null," + projectWithCriteria.getOntology().getId().toString())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").exists())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithNoOntology.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("ontology[in]", "null")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithCriteria.getId()+"')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithoutCriteria.getId()+"')]").doesNotExist())
                .andExpect(jsonPath("$.collection[?(@.id=='"+projectWithNoOntology.getId()+"')]").exists());
    }


    @Test
    @Transactional
    public void list_all_projects_with_pagination() throws Exception {
        Project project1 = builder.given_a_project();
        Project project2 = builder.given_a_project();
        Project project3 = builder.given_a_project();
        builder.addUserToProject(project1, builder.given_superadmin().getUsername());
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());
        builder.addUserToProject(project3, builder.given_superadmin().getUsername());

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "10")
                        .param("offset", "0")
                        .param("sort", "id")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(project3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(project2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(project1.getId()))
        ;

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "2")
                        .param("offset", "0")
                        .param("sort", "id")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[0].id").value(project3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(project2.getId()))
        ;

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "7")
                        .param("offset", "0")
                        .param("sort", "id")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(project3.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(project2.getId()))
                .andExpect(jsonPath("$.collection[2].id").value(project1.getId()))
        ;

        restProjectControllerMockMvc.perform(get("/api/project.json")
                        .param("max", "7")
                        .param("offset", "1")
                        .param("sort", "id")
                        .param("order", "desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.collection[0].id").value(project2.getId()))
                .andExpect(jsonPath("$.collection[1].id").value(project1.getId()))
        ;
    }


    @Test
    @Transactional
    public void get_a_project() throws Exception {
        Project project = builder.given_a_project();

        restProjectControllerMockMvc.perform(get("/api/project/{id}.json", project.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().intValue()))
                .andExpect(jsonPath("$.class").value("be.cytomine.domain.project.Project"))
                .andExpect(jsonPath("$.created").exists())
                .andExpect(jsonPath("$.updated").exists())
                .andExpect(jsonPath("$.name").value(project.getName()))
                .andExpect(jsonPath("$.ontology").value(project.getOntology().getId().intValue()))
                .andExpect(jsonPath("$.ontologyName").value(project.getOntology().getName()))
                .andExpect(jsonPath("$.blindMode").value(false))
                .andExpect(jsonPath("$.areImagesDownloadable").value(false))
                .andExpect(jsonPath("$.numberOfImages").value(0))
                .andExpect(jsonPath("$.numberOfAnnotations").value(0))
                .andExpect(jsonPath("$.numberOfJobAnnotations").value(0))
                .andExpect(jsonPath("$.numberOfReviewedAnnotations").value(0))
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.blindMode").value(false))
                .andExpect(jsonPath("$.isReadOnly").value(false))
                .andExpect(jsonPath("$.isRestricted").value(false))
                .andExpect(jsonPath("$.hideUsersLayers").value(false))
                .andExpect(jsonPath("$.hideAdminsLayers").value(false))
        ;
    }


    @Test
    @Transactional
    public void get_a_project_that_does_not_exist() throws Exception {
        restProjectControllerMockMvc.perform(get("/api/project/{id}.json", 0))
                .andDo(print())
                .andExpect(status().isNotFound())
        ;
    }


    @Test
    @Transactional
    public void add_valid_project() throws Exception {
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();
        project.setOntology(builder.given_an_ontology());
        project.setName("add_valid_project");
        restProjectControllerMockMvc.perform(post("/api/project.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists())
                .andExpect(jsonPath("$.project.name").value(project.getName()))
                .andExpect(jsonPath("$.project.ontology").value(project.getOntology().getId()));

        project = projectRepository.findByName("add_valid_project").get();
        assertThat(aclRepository.listMaskForUsers(project.getId(), builder.given_superadmin().getUsername()))
                .contains(ADMINISTRATION.getMask());

    }

    @Test
    @Transactional
    public void add_valid_project_without_ontology() throws Exception {
        Project project = BasicInstanceBuilder.given_a_not_persisted_project();
        project.setOntology(null);
        restProjectControllerMockMvc.perform(post("/api/project.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists())
                .andExpect(jsonPath("$.project.name").value(project.getName()))
                .andExpect(jsonPath("$.ontology").doesNotExist());
    }


    @Test
    @Transactional
    public void add_valid_project_with_users_admins() throws Exception {
        User user = builder.given_a_user();
        User admin = builder.given_a_user();

        Project project = BasicInstanceBuilder.given_a_not_persisted_project();
        project.setOntology(builder.given_an_ontology());
        project.setName("add_valid_project_with_users_admins");
        restProjectControllerMockMvc.perform(post("/api/project.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJsonObject()
                                .withChange("users",List.of(user.getId()))
                                .withChange("admins", List.of(admin.getId()))
                                .toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.AddProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists())
                .andExpect(jsonPath("$.project.name").value(project.getName()));

        Project projectCreated = projectRepository.findByName("add_valid_project_with_users_admins")
                .orElseThrow(() -> new ObjectNotFoundException("Project", "xxx"));

        assertThat(permissionService.hasACLPermission(projectCreated, builder.given_superadmin().getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, user.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(projectCreated, user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, admin.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated, admin.getUsername(), READ)).isTrue();

        // check ontology access
        assertThat(permissionService.hasACLPermission(projectCreated.getOntology(), user.getUsername(), READ)).isTrue();
        assertThat(permissionService.hasACLPermission(projectCreated.getOntology(), admin.getUsername(), READ)).isTrue();

    }

    @Test
    @Transactional
    public void add_project_with_already_existing_name() throws Exception {
        // expected: 409 / {"success":false,"errors":"Project LROLLUS-TEST already exist!"}
        Project project = builder.given_a_project();

        restProjectControllerMockMvc.perform(post("/api/project.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJsonObject().withChange("id", null).toJsonString()))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").value(containsString("already exist")));
    }


    @Test
    @Transactional
    public void edit_valid_project() throws Exception {
        Project project = builder.given_a_project();
        restProjectControllerMockMvc.perform(put("/api/project/{id}.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJsonObject().withChange("name", "new_name").toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists())
                .andExpect(jsonPath("$.project.name").value("new_name"));

    }


    @Test
    @Transactional
    public void fail_when_editing_project_does_not_exists() throws Exception {
        Project project = builder.given_a_project();
        em.remove(project);
        restProjectControllerMockMvc.perform(put("/api/project/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJSON()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());

    }

    @Test
    @Transactional
    public void edit_valid_project_with_users() throws Exception {
        Project project = builder.given_a_project();

        User previousUser = builder.given_a_user();
        User newUser = builder.given_a_user();
        builder.addUserToProject(project, previousUser.getUsername(), READ);

        restProjectControllerMockMvc.perform(put("/api/project/{id}.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJsonObject().withChange("users", List.of(newUser.getId())).toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists());

        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), READ)).isFalse();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), READ)).isTrue();

    }


    @Test
    @Transactional
    public void edit_valid_project_with_admins() throws Exception {
        Project project = builder.given_a_project();

        User previousUser = builder.given_a_user();
        User newUser = builder.given_a_user();
        builder.addUserToProject(project, previousUser.getUsername(), READ);

        restProjectControllerMockMvc.perform(put("/api/project/{id}.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJsonObject().withChange("admins", List.of(newUser.getId())).toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.EditProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists());

        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, previousUser.getUsername(), ADMINISTRATION)).isFalse();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), ADMINISTRATION)).isTrue();
        assertThat(permissionService.hasACLPermission(project, newUser.getUsername(), READ)).isTrue();

    }

    @Test
    @Transactional
    public void fail_when_editing_project_with_annotation_terms_and_ontology_change() throws Exception {
        AnnotationTerm annotationTerm = builder.given_an_annotation_term();
        Project project = annotationTerm.getUserAnnotation().getProject();

        Ontology newOntology = builder.given_an_ontology();
        restProjectControllerMockMvc.perform(put("/api/project/{id}.json", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(project.toJsonObject().withChange("ontology", List.of(newOntology.getId())).toJsonString()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errorValues").exists())
                .andExpect(jsonPath("$.errorValues.userAssociatedTermsCount").value(1));
    }

    @Test
    @Transactional
    public void delete_project() throws Exception {
        Project project = builder.given_a_project();
        restProjectControllerMockMvc.perform(delete("/api/project/{id}.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toJSON()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.printMessage").value(true))
                .andExpect(jsonPath("$.callback").exists())
                .andExpect(jsonPath("$.callback.projectID").exists())
                .andExpect(jsonPath("$.callback.method").value("be.cytomine.DeleteProjectCommand"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.command").exists())
                .andExpect(jsonPath("$.project.id").exists())
                .andExpect(jsonPath("$.project.name").value(project.getName()));
    }

    @Test
    @Transactional
    public void fail_when_delete_project_not_exists() throws Exception {
        restProjectControllerMockMvc.perform(delete("/api/project/{id}.json", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }


    @Test
    @Transactional
    public void list_last_action() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project);
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());

        restProjectControllerMockMvc.perform(get("/api/project/{id}/last/{max}.json", project.getId(), 10))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(1))));
    }


    @Test
    @Transactional
    public void list_last_opened_with_empty_dataset() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        Project projectNotOpened = builder.given_a_project();
        builder.addUserToProject(projectNotOpened, builder.given_superadmin().getUsername());


        restProjectControllerMockMvc.perform(get("/api/project/method/lastopened.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))));
        // last opened list unopened project (hum...) if user has not open any project
    }


    @Test
    @Transactional
    public void list_last_opened() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        Project projectNotOpened = builder.given_a_project();
        builder.addUserToProject(projectNotOpened, builder.given_superadmin().getUsername());

        assertThat(persistentProjectConnectionRepository.count()).isEqualTo(0);
        given_a_persistent_connection_in_project(builder.given_superadmin(), project, new Date());
        assertThat(persistentProjectConnectionRepository.count()).isEqualTo(1);
//        restProjectControllerMockMvc.perform(post("/server/ping.json")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"project\": \"" + project.getId() + "\"}"))
//                .andDo(print())
//                .andExpect(status().isOk());

        restProjectControllerMockMvc.perform(get("/api/project/method/lastopened.json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.collection[0].date").exists())
                .andExpect(jsonPath("$.collection[0].id").value(project.getId()))
                .andExpect(jsonPath("$.collection[0].opened").value("true"))
                .andExpect(jsonPath("$.collection[?(@.id=="+projectNotOpened.getId()+")].date").exists())
                .andExpect(jsonPath("$.collection[?(@.id=="+projectNotOpened.getId()+")].id").value(projectNotOpened.getId().intValue()))
                .andExpect(jsonPath("$.collection[?(@.id=="+projectNotOpened.getId()+")].opened").value(false));
    }

    @Test
    @Transactional
    public void list_by_ontology() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        
        
        restProjectControllerMockMvc.perform(get("/api/ontology/{id}/project.json", project.getOntology().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(project.getId()));
        
    }

    @Test
    @Transactional
    public void list_by_ontology_that_does_not_exist() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        restProjectControllerMockMvc.perform(get("/api/ontology/{id}/project.json", 0L))
                .andDo(print())
                .andExpect(status().isNotFound());

    }


    @Disabled("implement when software package is done")
    @Test
    @Transactional
    public void list_by_software() throws Exception {
//        Project project = builder.given_a_project();
//        builder.addUserToProject(project, builder.given_superadmin().getUsername());
//
//
//        restProjectControllerMockMvc.perform(get("/api/software/{id}/project.json", project.getsoftware().getId()))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.collection[0].id").value(project.getId()));

    }

    @Disabled("implement when software package is done")
    @Test
    @Transactional
    public void list_by_software_that_does_not_exist() throws Exception {
//        Project project = builder.given_a_project();
//        builder.addUserToProject(project, builder.given_superadmin().getUsername());
//        restProjectControllerMockMvc.perform(get("/api/software/{id}/project.json", 0L))
//                .andDo(print())
//                .andExpect(status().isNotFound());

    }


    @Test
    @Transactional
    public void list_by_user() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        restProjectControllerMockMvc.perform(get("/api/user/{id}/project.json", builder.given_superadmin().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].id").value(project.getId()));

    }

    @Test
    @Transactional
    public void list_by_user_that_does_not_exist() throws Exception {
        restProjectControllerMockMvc.perform(get("/api/user/{id}/project.json", 0L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Transactional
    public void list_by_user_light() throws Exception {
        Project project = builder.given_a_project();
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", builder.given_superadmin().getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

    }

    @Test
    @Transactional
    public void list_by_creator_light() throws Exception {
        Project project = builder.given_a_project();
        User creator = builder.given_superadmin();
        User user = builder.given_a_user();
        User admin = builder.given_a_user();
        builder.addUserToProject(project, creator.getUsername());
        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, admin.getUsername(), ADMINISTRATION);

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", builder.given_superadmin().getId())
                        .param("creator", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", user.getId())
                        .param("creator", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").doesNotExist());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", admin.getId())
                        .param("creator", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").doesNotExist());

    }

    @Test
    @Transactional
    public void list_by_admin_light() throws Exception {
        Project project = builder.given_a_project();
        User creator = builder.given_superadmin();
        User user = builder.given_a_user();
        User admin = builder.given_a_user();
        builder.addUserToProject(project, creator.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, admin.getUsername(), ADMINISTRATION);

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", builder.given_superadmin().getId())
                        .param("admin", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", user.getId())
                        .param("admin", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").doesNotExist());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", admin.getId())
                        .param("admin", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

    }

    @Test
    @Transactional
    public void list_by_simple_user_light() throws Exception {
        Project project = builder.given_a_project();
        User creator = builder.given_superadmin();
        User user = builder.given_a_user();
        User admin = builder.given_a_user();
        builder.addUserToProject(project, creator.getUsername(), ADMINISTRATION);
        builder.addUserToProject(project, user.getUsername(), READ);
        builder.addUserToProject(project, admin.getUsername(), ADMINISTRATION);

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", builder.given_superadmin().getId())
                        .param("user", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", user.getId())
                        .param("user", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", admin.getId())
                        .param("user", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[?(@.id=='"+project.getId()+"')]").exists());

    }

    @Test
    @Transactional
    public void list_by_user_light_with_unexisting_user() throws Exception {
        restProjectControllerMockMvc.perform(get("/api/user/{id}/project/light.json", 0L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @javax.transaction.Transactional
    public void get_project_bounds() throws Exception {
        Date start = DateUtils.addSeconds(new Date(), -5);
        Project project = builder.given_a_project();
        project.setName("0001");
        project.setCountImages(10);
        project.setCountAnnotations(20);
        project.setCountJobAnnotations(30);
        project.setCountReviewedAnnotations(40);
        builder.addUserToProject(project, builder.given_a_user().getUsername());
        builder.addUserToProject(project, builder.given_superadmin().getUsername());
        builder.persistAndReturn(project);
        Project project2 = builder.given_a_project();
        project2.setName("zzzzz");
        builder.persistAndReturn(project2);
        builder.addUserToProject(project2, builder.given_superadmin().getUsername());

        Date stop = DateUtils.addSeconds(new Date(), 5);

        restProjectControllerMockMvc.perform(get("/api/bounds/project.json")
                        .param("withMembersCount", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.min").exists())
                .andExpect(jsonPath("$.created.max").value(lessThanOrEqualTo(stop.getTime())))
                .andExpect(jsonPath("$.updated.min").exists())
                .andExpect(jsonPath("$.updated.max").value(lessThanOrEqualTo(stop.getTime())))
                .andExpect(jsonPath("$.mode.min").hasJsonPath())
                .andExpect(jsonPath("$.mode.max").hasJsonPath())
                .andExpect(jsonPath("$.name.min").value("0001"))
                .andExpect(jsonPath("$.name.max").value("zzzzz"))
                .andExpect(jsonPath("$.numberOfAnnotations.min").value(0))
                .andExpect(jsonPath("$.numberOfAnnotations.max").value(20))
                .andExpect(jsonPath("$.numberOfJobAnnotations.min").value(0))
                .andExpect(jsonPath("$.numberOfJobAnnotations.max").value(30))
                .andExpect(jsonPath("$.numberOfReviewedAnnotations.min").value(0))
                .andExpect(jsonPath("$.numberOfReviewedAnnotations.max").value(40))
                .andExpect(jsonPath("$.numberOfImages.min").value(0))
                .andExpect(jsonPath("$.numberOfImages.max").value(10))
                .andExpect(jsonPath("$.members.min").value(lessThanOrEqualTo(1)))
                .andExpect(jsonPath("$.members.max").value(2));

    }



    @Test
    @Transactional
    public void list_command_history() throws Exception {
        Project project = builder.given_a_project();
        User creator = builder.given_superadmin();
        builder.addUserToProject(project, creator.getUsername());

        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project);
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());

        restProjectControllerMockMvc.perform(get("/api/commandhistory.json")
                        .param("fullData", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.project=="+project.getId()+")]").exists());

        restProjectControllerMockMvc.perform(get("/api/project/{id}/commandhistory.json", project.getId())
                        .param("user", builder.given_superadmin().getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.project=="+project.getId()+")]").exists());


    }


    @Test
    @Transactional
    public void list_command_history_with_dates() throws Exception {
        Project project = builder.given_a_project();
        User creator = builder.given_superadmin();
        builder.addUserToProject(project, creator.getUsername());

        Date start = DateUtils.addSeconds(new Date(), -5);
        UserAnnotation userAnnotation = builder.given_a_not_persisted_user_annotation(project);
        CommandResponse commandResponse = userAnnotationService.add(userAnnotation.toJsonObject());
        Date stop = DateUtils.addSeconds(new Date(), 5);


        restProjectControllerMockMvc.perform(get("/api/project/{id}/commandhistory.json", project.getId())
                        .param("startDate", start.getTime()+"")
                        .param("endDate", stop.getTime()+""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.project=="+project.getId()+")]").exists());

        restProjectControllerMockMvc.perform(get("/api/project/{id}/commandhistory.json", project.getId())
                        .param("startDate", DateUtils.addSeconds(start, -5).getTime()+"")
                        .param("endDate", DateUtils.addSeconds(start, -3).getTime()+""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.project=="+project.getId()+")]").doesNotExist());

        restProjectControllerMockMvc.perform(get("/api/project/{id}/commandhistory.json", project.getId())
                        .param("startDate", DateUtils.addSeconds(start, 3).getTime()+"")
                        .param("endDate", DateUtils.addSeconds(start, 5).getTime()+""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.project=="+project.getId()+")]").doesNotExist());

    }


    @Test
    public void invite_user_in_project() throws Exception {

        Project project = builder.given_a_project();
        forgotPasswordTokenRepository.deleteAll();
        String username = "invitedUser"+System.currentTimeMillis();
        // invite user
        restProjectControllerMockMvc.perform(post("/api/project/{id}/invitation.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of(
                                "name", username,
                                "firstname", "firstname",
                                "lastname", "lastname",
                                "mail", username+"@example.com").toJsonString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));


        assertThat(forgotPasswordTokenRepository.findAll()).hasSize(1);

        assertThat(secUserRepository.findByUsernameLikeIgnoreCase(username)).isPresent();

        assertThat(secUserRepository.findByUsernameLikeIgnoreCase(username).get().getPasswordExpired()).isTrue();

        assertThat(permissionService.hasACLPermission(project, username, READ)).isTrue();


    }



    @Test
    @Transactional
    public void clone_project_with_all_data() throws Exception {

        Project project = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        restProjectControllerMockMvc.perform(post("/api/project/{id}/clone.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of(
                                "name", newName,
                                "cloneSetup", true,
                                "cloneMembers", true,
                                "cloneImages", true,
                                "cloneAnnotations", true).toJsonString()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(projectRepository.findByName(newName)).isPresent();

        Project cloned = projectRepository.findByName(newName).get();

        assertThat(cloned.getBlindMode()).isTrue();
        assertThat(imageInstanceRepository.countAllByProject(cloned)).isEqualTo(1);
        assertThat(userAnnotationService.countByProject(cloned)).isEqualTo(1);
        assertThat(securityACLService.getProjectUsers(cloned)).hasSize(3);
    }


    @Test
    @Transactional
    public void clone_project_without_anything() throws Exception {

        Project project = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        restProjectControllerMockMvc.perform(post("/api/project/{id}/clone.json", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonObject.of(
                                "name", newName,
                                "cloneSetup", false,
                                "cloneMembers", false,
                                "cloneImages", false,
                                "cloneAnnotations", false).toJsonString()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(projectRepository.findByName(newName)).isPresent();

        Project cloned = projectRepository.findByName(newName).get();

        assertThat(cloned.getBlindMode()).isFalse();
        assertThat(imageInstanceRepository.countAllByProject(cloned)).isEqualTo(0);
        assertThat(userAnnotationService.countByProject(cloned)).isEqualTo(0);
        assertThat(securityACLService.getProjectUsers(cloned)).hasSize(1);
    }



    private Project given_a_full_project() {
        Project project = builder.given_a_project();
        project.setMode(EditingMode.READ_ONLY);
        project.setBlindMode(true);
        project = builder.persistAndReturn(project);


        builder.addUserToProject(project, builder.given_a_user().getUsername(), READ);

        User user = builder.given_a_user();
        builder.addUserToProject(project, user.getUsername(), ADMINISTRATION);

        builder.given_a_project_representative_user(project, user);

        SliceInstance sliceInstance = builder.given_a_slice_instance(project);

        builder.given_a_description(sliceInstance.getImage());
        builder.given_a_property(sliceInstance.getImage(), "key", "value");
        builder.given_a_tag_association(builder.given_a_tag(), sliceInstance.getImage());
        builder.given_a_attached_file(sliceInstance.getImage());

        UserAnnotation userAnnotation = builder.given_a_user_annotation(sliceInstance);
        builder.given_an_annotation_term(userAnnotation);
        builder.given_a_description(userAnnotation);
        builder.given_a_property(userAnnotation, "key", "value");
        builder.given_a_tag_association(builder.given_a_tag(), userAnnotation);
        return project;
    }


//
//    void testImageNamesOfBlindProject() {
//        Project project = BasicInstanceBuilder.getProjectNotExist(true)
//        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
//
//        def result = ImageInstanceAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        def json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection[0].instanceFilename == image.getBlindInstanceFilename()
//
//        assert json.collection[0].blindedName instanceof JSONObject.Null
//
//        result = ImageInstanceAPI.show(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//
//        assert json.instanceFilename == image.getBlindInstanceFilename()
//        assert json.blindedName instanceof JSONObject.Null
//
//        project.blindMode = true
//        project.save(true)
//
//        result = ImageInstanceAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection[0].instanceFilename == image.getBlindInstanceFilename()
//        assert !(json.collection[0].blindedName instanceof JSONObject.Null)
//
//        result = ImageInstanceAPI.show(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//
//        assert json.instanceFilename == image.getBlindInstanceFilename()
//        assert !(json.blindedName instanceof JSONObject.Null)
//
//
//        User user = BasicInstanceBuilder.getUser()
//
//        assert (200 ==ProjectAPI.addUserProject(project.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code)
//
//        result = ImageInstanceAPI.listByProject(project.id, user.username, "password")
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//        assert json.collection instanceof JSONArray
//        assert json.collection[0].instanceFilename instanceof JSONObject.Null
//        assert !(json.collection[0].blindedName instanceof JSONObject.Null)
//
//        result = ImageInstanceAPI.show(image.id, user.username, "password")
//        assert 200 == result.code
//        json = JSON.parse(result.data)
//
//        assert json.instanceFilename instanceof JSONObject.Null
//        assert !(json.blindedName instanceof JSONObject.Null)
//    }

    @Autowired
    ProjectConnectionService projectConnectionService;

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project, Date created) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project, "xxx", "linux", "chrome", "123", created);
        return connection;
    }

}
