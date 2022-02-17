package be.cytomine.service.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.dto.NamedCytomineDomain;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.dto.ProjectBounds;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.social.ProjectConnectionService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;

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
public class SecurityAclServiceTests {

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    UserAnnotationService userAnnotationService;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    PermissionService permissionService;

    @Test
    void check_is_user_in_project() {
        Project project = builder.given_a_project();
        User user = builder.given_a_user();
        assertThat(securityACLService.isUserInProject(user, project))
                .isFalse();
        builder.addUserToProject(project, user.getUsername());
        assertThat(securityACLService.isUserInProject(user, project))
                .isTrue();
    }



}
