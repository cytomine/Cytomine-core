package be.cytomine.authorization.meta;

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
import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.service.PermissionService;
import be.cytomine.service.meta.DescriptionService;
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

@AutoConfigureMockMvc
@SpringBootTest(classes = CytomineCoreApplication.class)
@Transactional
public class DescriptionAuthorizationTest extends CRUDAuthorizationTest {


    private Description descriptionForProject = null;
    private Description descriptionForAnnotation = null;
    private Description descriptionForAbstractImage = null;

    private Project project = null;
    private AnnotationDomain annotationDomain = null;
    private AbstractImage abstractImage = null;


    @Autowired
    DescriptionService descriptionService;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @BeforeEach
    public void before() throws Exception {
        if (descriptionForProject == null) {
            project = builder.given_a_project();
            annotationDomain = builder.given_a_user_annotation();
            abstractImage = builder.given_an_abstract_image();

            descriptionForProject = builder.given_a_description(project);
            descriptionForAnnotation = builder.given_a_description(annotationDomain);
            descriptionForAbstractImage = builder.given_a_description(abstractImage);

            ;
            initACL(project);
            initACL(annotationDomain.getProject());
            initACL(abstractImage.getUploadedFile().getStorage());
        }
        project.setMode(EditingMode.CLASSIC);
        annotationDomain.getProject().setMode(EditingMode.CLASSIC);
    }

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_list() {
        expectOK (() -> { descriptionService.list(); });
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_list(){
        expectForbidden(() -> {
            descriptionService.list();
        });
    }


    // ANNOTATIONS
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_readonly_mode(){
        project.setMode(EditingMode.READ_ONLY);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode(){
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_annotation(){
        project.setMode(EditingMode.RESTRICTED);
        expectForbidden(() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_canadd_in_restricted_mode_for_annotation_if_owner(){
        annotationDomain.getProject().setMode(EditingMode.RESTRICTED);
        ((UserAnnotation)annotationDomain).setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> when_i_add_domain());
    }


    @Override
    public void when_i_get_domain() {
        descriptionService.findByDomain(project);
        descriptionService.findByDomain("AnnotationDomain", annotationDomain.getId());
        descriptionService.findByDomain(abstractImage);
    }

    @Override
    protected void when_i_add_domain() {
        AnnotationDomain annotationDomain = builder.persistAndReturn(builder.given_a_not_persisted_user_annotation(project));
        descriptionService.add(builder.given_a_not_persisted_description(annotationDomain).toJsonObject());
    }

    @Override
    protected void when_i_edit_domain() {
       descriptionService.update(descriptionForAnnotation, descriptionForAnnotation.toJsonObject());
    }


    @Override
    protected void when_i_delete_domain() {
        Description description = builder.given_a_description(annotationDomain);
        descriptionService.delete(description, null, null, true);
    }

    //IMAGE
    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_cannot_add_in_restricted_mode_for_image(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        annotationDomain.getProject().setMode(EditingMode.RESTRICTED);
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_can_add_in_restricted_mode_for_image_if_owner(){
        ImageInstance imageInstance=builder.given_an_image_instance(project);
        imageInstance.getProject().setMode(EditingMode.RESTRICTED);
        imageInstance.setUser(userRepository.findByUsernameLikeIgnoreCase(USER_ACL_READ).get());
        expectOK(() -> descriptionService.add(builder.given_a_not_persisted_description(imageInstance).toJsonObject()));
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_image(){
        expectForbidden(() -> descriptionService.add(builder.given_a_not_persisted_description(builder.given_an_image_instance()).toJsonObject()));
    }

    @Override
    protected Optional<Permission> minimalPermissionForCreate() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForDelete() {
        return Optional.of(BasePermission.READ);
    }

    @Override
    protected Optional<Permission> minimalPermissionForEdit() {
        return Optional.of(BasePermission.READ);
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
