package be.cytomine.service.utils;

import be.cytomine.CytomineCoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;

import javax.mail.MessagingException;
import javax.transaction.Transactional;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class CytomineMailServiceTests {

    @Autowired
    CytomineMailService cytomineMailService;

    @Test
    public void test_send_email() throws MessagingException {
        cytomineMailService.send(
                "loic.rollus@cytomine.com",
                new String[] {"loic.rollus@cytomine.com"},
                new String[] {},
                new String[] {},
                "Congratulations",
                "Congratulations!!!! Loïc ROLLUS, your best dev just sent an email with Mailgun from Cytomine spring core!  You are truly awesome! "
                );
    }

}
