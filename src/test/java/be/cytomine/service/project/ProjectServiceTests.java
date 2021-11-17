package be.cytomine.service.project;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectServiceTests {

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

    
    @Test
    void list_user_project_with_success() {
        Project project1 = builder.given_a_project();
        Project projectWhereUserIsMissing = builder.given_a_project();

        builder.addUserToProject(project1, "superadmin");


        ProjectSearchExtension projectSearchExtension = new ProjectSearchExtension();
        projectSearchExtension.setWithCurrentUserRoles(true);
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();

        Page<Map<String, Object>> page = projectService.list(builder.given_superadmin(), projectSearchExtension, searchParameterEntries, "id", "desc", 0L, 0L);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).contains(project1.getId());
        assertThat(page.getContent().stream().map(x -> x.get("id")).collect(Collectors.toList())).doesNotContain(projectWhereUserIsMissing.getId());
    }
}
