package be.cytomine.service.social;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentProjectConnectionRepository;
import com.mongodb.client.MongoClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.service.social.ProjectConnectionService.DATABASE_NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ProjectConnectionServiceTests {

    @Autowired
    ProjectConnectionService projectConnectionService;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @BeforeEach
    public void cleanDB() {
        persistentProjectConnectionRepository.deleteAll();
        lastConnectionRepository.deleteAll();
    }

    
    @Test
    void test() {
        User user = builder.given_superadmin();
        Project projet = builder.given_a_project();
        Date before = new Date(new Date().getTime()-1000);
        PersistentProjectConnection connection = projectConnectionService.add(user, projet.getId(), "xxx", "linux", "chrome", "123");
        assertThat(connection).isNotNull();
        assertThat(connection.getTime()).isNull();
        Date after = new Date();
        Optional<PersistentProjectConnection> connectionOptional = persistentProjectConnectionRepository.findAllByUserAndProjectAndCreatedLessThan(builder.given_superadmin().getId(), projet.getId(), after,
                PageRequest.of(0, 1, Sort.Direction.DESC, "created")).stream().findFirst();
        assertThat(connectionOptional).isPresent();
        assertThat(connectionOptional.get().getSession()).isEqualTo("xxx");
        assertThat(connectionOptional.get().getTime()).isNull();

        connection = projectConnectionService.add(user, projet.getId(), "yyy", "linux", "chrome", "123");


        connectionOptional = persistentProjectConnectionRepository.findAllByUserAndProjectAndCreatedLessThan(builder.given_superadmin().getId(), projet.getId(), after,
                PageRequest.of(0, 1, Sort.Direction.DESC, "created")).stream().findFirst();
        assertThat(connectionOptional).isPresent();
        assertThat(connectionOptional.get().getSession()).isEqualTo("xxx");
        assertThat(connectionOptional.get().getTime()).isEqualTo(0);

    }


    @Test
    void find_last_user_connection_in_project() {
        Project projet = builder.given_a_project();
        User user = builder.given_superadmin();
        User anotherUser = builder.given_a_user();

        Optional<PersistentProjectConnection> persistentProjectConnection = projectConnectionService.lastConnectionInProject(projet, user.getId(), "created", "desc");
        assertThat(persistentProjectConnection).isEmpty();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(anotherUser, projet);

        persistentProjectConnection = projectConnectionService.lastConnectionInProject(projet, user.getId(), "created", "desc");
        assertThat(persistentProjectConnection).isEmpty();

        Date start = new Date();
        connection = given_a_persistent_connection_in_project(user, projet);
        Date stop = new Date();

        persistentProjectConnection = projectConnectionService.lastConnectionInProject(projet, user.getId(), "created", "desc");

        assertThat(persistentProjectConnection).isPresent();
        assertThat(persistentProjectConnection.get().getCreated()).isBetween(start, stop, true, true);

    }

    @Test
    void find_last_users_connection_in_project() {


        Project projet = builder.given_a_project();
        User user = builder.given_superadmin();
        User anotherUser = builder.given_a_user();


        List<Map<String, Object>> maps = projectConnectionService.lastConnectionInProject(projet, new ArrayList<Long>(), "created", "desc", 0L, 0L);
        assertThat(maps).isEmpty();

        Date start = new Date();
        given_a_persistent_connection_in_project(user, projet);
        Date stop = new Date();

        maps = projectConnectionService.lastConnectionInProject(projet, null, "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(1);
        assertThat(maps.get(0).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(start, stop, true, true);

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(user.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(1);
        assertThat(maps.get(0).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(start, stop, true, true);

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(anotherUser.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(0);

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(user.getId(), anotherUser.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(1);
        assertThat(maps.get(0).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(start, stop, true, true);

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(user.getId(), anotherUser.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(1);
        assertThat(maps.get(0).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(start, stop, true, true);

        Date startSecondConnectionForUser = new Date();
        given_a_persistent_connection_in_project(user, projet);
        Date stopSecondConnectionForUser = new Date();

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(user.getId(), anotherUser.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(1);
        assertThat(maps.get(0).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(stop, new Date());

        Date startConnectionAnotherUser = new Date();
        given_a_persistent_connection_in_project(anotherUser, projet);
        Date stopConnectionAnotherUser = new Date();

        maps = projectConnectionService.lastConnectionInProject(projet, List.of(user.getId(), anotherUser.getId()), "created", "desc", 0L, 0L);
        assertThat(maps).hasSize(2);
        assertThat(maps.get(0).get("user")).isEqualTo(anotherUser.getId());
        assertThat(maps.get(1).get("user")).isEqualTo(user.getId());
        assertThat((Date)maps.get(0).get("created")).isBetween(startConnectionAnotherUser, stopConnectionAnotherUser, true, true);
        assertThat((Date)maps.get(1).get("created")).isBetween(startSecondConnectionForUser, stopSecondConnectionForUser, true, true);

    }

    @Test
    void find_last_connections_of_users_in_project() {
        Project projet = builder.given_a_project();
        User user = builder.given_superadmin();
        User anotherUser = builder.given_a_user();

        given_a_persistent_connection_in_project(user, projet);

        List<Map<String, Object>> results = projectConnectionService.lastConnectionOfGivenUsersInProject
                (projet, List.of(user.getId(), anotherUser.getId()), "created", "desc", 0L, 0L);

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(x -> x.get("user"))).contains(user.getId(), anotherUser.getId());
    }

    @Test
    void fill_project_connection_update_annotations_counter() {
        Project projet = builder.given_a_project();
        User user = builder.given_superadmin();

        PersistentProjectConnection connection = given_a_persistent_connection_in_project(user, projet);
        assertThat(connection.getCountCreatedAnnotations()).isNull();

        UserAnnotation annotation = builder.given_a_not_persisted_user_annotation(projet);
        builder.persistAndReturn(annotation);

        connection = given_a_persistent_connection_in_project(user, projet);
        connection = given_a_persistent_connection_in_project(user, projet);
        List<PersistentProjectConnection> allByUserAndProject = persistentProjectConnectionRepository.findAllByUserAndProject(user.getId(), projet.getId(), PageRequest.of(0, 50, Sort.Direction.DESC, "created"));

        assertThat(allByUserAndProject).hasSize(3);
        assertThat(allByUserAndProject.get(0).getCountCreatedAnnotations()).isNull();
        assertThat(allByUserAndProject.get(1).getCountCreatedAnnotations()).isEqualTo(0);
        assertThat(allByUserAndProject.get(2).getCountCreatedAnnotations()).isEqualTo(1);
    }

    @Test
    void fill_project_connection_update_image_counter() {
        Assertions.fail("todo");
    }

    @Test
    void get_connection_by_user_and_project() {
        Project projet = builder.given_a_project();
        User user = builder.given_superadmin();
        User anotherUser = builder.given_a_user();

        given_a_persistent_connection_in_project(user, projet);
        given_a_persistent_connection_in_project(user, projet);
        given_a_persistent_connection_in_project(anotherUser, projet);
        given_a_last_connection(user, projet);

        List<PersistentProjectConnection> results = projectConnectionService.getConnectionByUserAndProject(user, projet, 50, 0);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getUser()).isEqualTo(user.getId());
        assertThat(results.get(0).getExtraProperties()).containsEntry("online", true);

        results = projectConnectionService.getConnectionByUserAndProject(anotherUser, projet, 50, 0);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getUser()).isEqualTo(anotherUser.getId());
        assertThat(results.get(0).getExtraProperties()).doesNotContainEntry("online", true);
    }

    PersistentProjectConnection given_a_persistent_connection_in_project(User user, Project project) {
        PersistentProjectConnection connection = projectConnectionService.add(user, project.getId(), "xxx", "linux", "chrome", "123");
        return connection;
    }

    LastConnection given_a_last_connection(User user, Project project) {
        LastConnection lastConnection = new LastConnection();
        lastConnection.setProject(project);
        lastConnection.setUser(user);
        return lastConnectionRepository.insert(lastConnection);
    }
}
