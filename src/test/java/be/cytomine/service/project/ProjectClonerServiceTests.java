package be.cytomine.service.project;

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
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.dto.NamedCytomineDomain;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.meta.AttachedFileRepository;
import be.cytomine.repository.meta.DescriptionRepository;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.meta.TagDomainAssociationRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.ProjectBounds;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectClonerServiceTests {

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ProjectClonerService projectClonerService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    EntityManager entityManager;
    
    @Autowired
    ProjectRepresentativeUserService projectRepresentativeUserService;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    DescriptionRepository descriptionRepository;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    TagDomainAssociationRepository tagDomainAssociationRepository;

    @Autowired
    AttachedFileRepository attachedFileRepository;


    @Test
    void clone_project_with_setup_only() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, true, false, false, false);

        then_it_has_this_name(newProject, newName);
        then_it_has_the_same_setup(newProject, originalProject);
        then_it_has_only_current_user(newProject);
        then_it_has_no_images(newProject, originalProject);
        then_it_has_no_annotations(newProject, originalProject);
    }


    @Test
    void clone_project_with_members_only() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, false, true, false, false);

        then_it_has_this_name(newProject, newName);
        then_it_doesnt_have_the_same_setup(newProject, originalProject);
        then_it_has_same_members(newProject, originalProject);
        then_it_has_no_images(newProject, originalProject);
        then_it_has_no_annotations(newProject, originalProject);
    }

    @Test
    void clone_project_with_setup_and_members_only() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, true, true, false, false);

        then_it_has_this_name(newProject, newName);
        then_it_has_the_same_setup(newProject, originalProject);
        then_it_has_same_members(newProject, originalProject);
        then_it_has_no_images(newProject, originalProject);
        then_it_has_no_annotations(newProject, originalProject);
    }

    @Test
    void clone_project_with_images_only() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, false, false, true, false);

        then_it_has_this_name(newProject, newName);
        then_it_doesnt_have_the_same_setup(newProject, originalProject);
        then_it_has_only_current_user(newProject);
        then_it_has_same_images(newProject, originalProject);
        then_it_has_no_annotations(newProject, originalProject);
    }

    @Test
    void clone_project_with_setup_images() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, true, false, true, false);

        then_it_has_this_name(newProject, newName);
        then_it_has_the_same_setup(newProject, originalProject);
        then_it_has_only_current_user(newProject);
        then_it_has_same_images(newProject, originalProject);
        then_it_has_no_annotations(newProject, originalProject);
    }

    @Test
    void clone_project_with_setup_images_members_annotations() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Project newProject = when_i_clone_it(originalProject, newName, true, true, true, true);

        then_it_has_this_name(newProject, newName);
        then_it_has_the_same_setup(newProject, originalProject);
        then_it_has_same_members(newProject, originalProject);
        then_it_has_same_images(newProject, originalProject);
        then_it_has_same_annotations(newProject, originalProject);
    }

    @Test
    void clone_project_with_annotations_but_without_setup_must_be_refused() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Assertions.assertThrows(ServerException.class, () -> {
            when_i_clone_it(originalProject, newName, false, true, true, true);
        }) ;

    }

    @Test
    void clone_project_with_annotations_but_without_members_must_be_refused() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Assertions.assertThrows(ServerException.class, () -> {
            when_i_clone_it(originalProject, newName, true, false, true, true);
        }) ;
    }

    @Test
    void clone_project_with_annotations_but_without_images_must_be_refused() {
        Project originalProject = given_a_full_project();
        String newName = BasicInstanceBuilder.randomString();

        Assertions.assertThrows(ServerException.class, () -> {
            when_i_clone_it(originalProject, newName, true, true, false, true);
        }) ;
    }

    public Project when_i_clone_it(Project original, String newProjectName, boolean setupIncluded, boolean membersIncluded, boolean imagesIncluded, boolean annotationsIncluded) {
        return (Project) projectClonerService.cloneProject(original, newProjectName, setupIncluded, membersIncluded, imagesIncluded, annotationsIncluded).getObject();
    }

    public void then_it_has_this_name(Project project, String name) {
        assertThat(project.getName()).isEqualTo(name);
    }

    public void then_it_has_the_same_setup(Project newProject, Project originalProject) {
        assertThat(newProject.getBlindMode()).isEqualTo(originalProject.getBlindMode());
        assertThat(newProject.getOntology()).isEqualTo(originalProject.getOntology());
        assertThat(newProject.getMode()).isEqualTo(originalProject.getMode());
    }

    public void then_it_doesnt_have_the_same_setup(Project newProject, Project originalProject) {
        assertThat(newProject.getBlindMode()).isNotEqualTo(originalProject.getBlindMode());
        assertThat(newProject.getOntology()).isNotEqualTo(originalProject.getOntology());
    }

    public void then_it_has_same_members(Project newProject, Project originalProject) {
        List<Long> users = securityACLService.getProjectUsersIds(originalProject);
        users.add(builder.given_superadmin().getId()); // add the current user as it will be the project owner
        assertThat(securityACLService.getProjectUsersIds(newProject)).containsAll(users);
    }

    public void then_it_has_only_current_user(Project newProject) {
        assertThat(securityACLService.getProjectUsersIds(newProject)).containsOnly(builder.given_superadmin().getId());
    }

    public void then_it_has_same_images(Project newProject, Project originalProject) {
        assertThat(imageInstanceService.listByProject(newProject).stream().map(x -> x.getBaseImage()).toList()).containsExactlyElementsOf(imageInstanceService.listByProject(originalProject).stream().map(x -> x.getBaseImage()).toList());
        ImageInstance imageInstanceFromNewProject = imageInstanceService.listByProject(newProject).get(0);
        AssertionsForClassTypes.assertThat(descriptionRepository.findByDomainIdentAndDomainClassName(imageInstanceFromNewProject.getId(), imageInstanceFromNewProject.getClass().getName())).isPresent();
        AssertionsForInterfaceTypes.assertThat(propertyRepository.findAllByDomainIdent(imageInstanceFromNewProject.getId())).hasSize(1);
        AssertionsForClassTypes.assertThat(propertyRepository.findAllByDomainIdent(imageInstanceFromNewProject.getId()).get(0).getKey()).isEqualTo("key");
        AssertionsForClassTypes.assertThat(propertyRepository.findAllByDomainIdent(imageInstanceFromNewProject.getId()).get(0).getValue()).isEqualTo("value");
        AssertionsForInterfaceTypes.assertThat(tagDomainAssociationRepository.findAllByDomainIdent(imageInstanceFromNewProject.getId())).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(attachedFileRepository.findAllByDomainIdent(imageInstanceFromNewProject.getId())).isNotEmpty();

    }

    public void then_it_has_no_images(Project newProject, Project originalProject) {
        assertThat(imageInstanceService.getAllImageId(originalProject)).isNotEmpty();
        assertThat(imageInstanceService.getAllImageId(newProject)).isEmpty();
    }

    public void then_it_has_same_annotations(Project newProject, Project originalProject) {
        List<UserAnnotation> annotationListFromOriginalProject = userAnnotationRepository.findAll().stream().filter(x -> x.getProject().equals(originalProject)).toList();
        assertThat(annotationListFromOriginalProject).hasSize(1);
        UserAnnotation originalAnnotation = annotationListFromOriginalProject.get(0);

        List<UserAnnotation> annotationListFromNewProject = userAnnotationRepository.findAll().stream().filter(x -> x.getProject().equals(originalProject)).toList();
        assertThat(annotationListFromNewProject).hasSize(1);
        UserAnnotation newAnnotation = annotationListFromOriginalProject.get(0);
        assertThat(newAnnotation.getWktLocation()).isEqualTo(originalAnnotation.getWktLocation());
        assertThat(newAnnotation.getTerms()).containsExactlyElementsOf(originalAnnotation.getTerms());

        AssertionsForClassTypes.assertThat(descriptionRepository.findByDomainIdentAndDomainClassName(newAnnotation.getId(), newAnnotation.getClass().getName())).isPresent();
        AssertionsForInterfaceTypes.assertThat(propertyRepository.findAllByDomainIdent(newAnnotation.getId())).hasSize(1);
        AssertionsForClassTypes.assertThat(propertyRepository.findAllByDomainIdent(newAnnotation.getId()).get(0).getKey()).isEqualTo("key");
        AssertionsForClassTypes.assertThat(propertyRepository.findAllByDomainIdent(newAnnotation.getId()).get(0).getValue()).isEqualTo("value");
        AssertionsForInterfaceTypes.assertThat(tagDomainAssociationRepository.findAllByDomainIdent(newAnnotation.getId())).hasSize(1);
    }

    public void then_it_has_no_annotations(Project newProject, Project originalProject) {
        assertThat(userAnnotationRepository.countByProject(originalProject)).isEqualTo(1);
        assertThat(userAnnotationRepository.countByProject(newProject)).isEqualTo(0);
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

}
