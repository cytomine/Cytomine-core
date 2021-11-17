package be.cytomine.service.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.WRITE;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
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

        Page<Map<String, Object>> page = secUserService.listUsersByProject(projectWhereUserIsManager, new ArrayList<>(), "id", "desc", 0L, 0L);
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
}
