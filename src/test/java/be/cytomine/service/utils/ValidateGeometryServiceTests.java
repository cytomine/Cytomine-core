package be.cytomine.service.utils;

import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.ontology.UserAnnotation;
import com.vividsolutions.jts.io.ParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import javax.transaction.Transactional;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class ValidateGeometryServiceTests {

    @Autowired
    ValidateGeometryService validateGeometryService;

    @Test
    public void validate_valid_polygon() throws ParseException {

        assertThat(validateGeometryService.tryToMakeItValidIfNotValid(
                "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"
        )).isEqualTo("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))");
    }


}
