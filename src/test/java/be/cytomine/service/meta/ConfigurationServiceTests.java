package be.cytomine.service.meta;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.service.CommandService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.acls.domain.BasePermission.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ConfigurationServiceTests {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ConfigurationRepository configurationRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    CommandService commandService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    SecurityACLService securityACLService;

    @Test
    void list_all_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");
        assertThat(configuration).isIn(configurationService.list());
    }

    @Test
    void find_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");
        assertThat(configurationService.findByKey("xxx")).contains(configuration);
    }

    @Test
    void find_unexisting_configuration_return_empty() {
        assertThat(configurationService.findByKey("empty")).isEmpty();
    }

    @Test
    void add_valid_configuration_with_success() {
        Configuration configuration = builder.given_a_not_persisted_configuration("xxx");

        CommandResponse commandResponse = configurationService.add(configuration.toJsonObject());

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void add_configuration_with_already_existing_key() {
        Configuration configuration = builder.given_a_configuration("xxx");

        Assertions.assertThrows(AlreadyExistException.class, () -> {
                configurationService.add(configuration.toJsonObject().withChange("id", null));
        });
    }

    @Test
    void edit_valid_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");

        CommandResponse commandResponse = configurationService.update(configuration, configuration.toJsonObject().withChange("value", "NEW VALUE"));

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(configurationService.findByKey("xxx")).isPresent();
        Configuration edited = configurationService.findByKey("xxx").get();
        assertThat(edited.getValue()).isEqualTo("NEW VALUE");
    }


    @Test
    void delete_configuration_with_success() {
        Configuration configuration = builder.given_a_configuration("xxx");

        CommandResponse commandResponse = configurationService.delete(configuration, null, null, true);

        assertThat(commandResponse).isNotNull();
        assertThat(commandResponse.getStatus()).isEqualTo(200);
        assertThat(configurationService.findByKey("xxx").isEmpty());
    }
}
