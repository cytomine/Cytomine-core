package be.cytomine.service.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.EditingMode;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.social.ProjectConnectionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import static be.cytomine.BasicInstanceBuilder.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.READ;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
public class SecRoleServiceTests {


    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    SecRoleService secRoleService;

    @Test
    public void find_role_from_authority() {
        assertThat(secRoleService.findByAuthority("ROLE_GUEST")).isPresent();
        assertThat(secRoleService.findByAuthority("ROLE_USER")).isPresent();
        assertThat(secRoleService.findByAuthority("ROLE_ADMIN")).isPresent();
    }

    @Test
    public void find_role_from_id() {
        assertThat(secRoleService.find(secRoleService.findByAuthority("ROLE_GUEST").get().getId())).isPresent();
    }

    @Test
    public void list_roles() {
        assertThat(secRoleService.list().stream().map(SecRole::getAuthority))
                .contains("ROLE_GUEST", "ROLE_USER", "ROLE_ADMIN");
    }
}
