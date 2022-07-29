package be.cytomine.security.authentication;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.CustomUserDetailsAuthenticationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.transaction.Transactional;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class CustomUserDetailsAuthenticationProviderTests {

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    BasicInstanceBuilder builder;

    User user;

    CustomUserDetailsAuthenticationProvider customUserDetailsAuthenticationProvider;

    @BeforeEach
    public void initUser() {
        user = builder.given_a_user();
        user.setPassword(passwordEncoder.encode("validPassword"));
        secUserRepository.save(user);
        customUserDetailsAuthenticationProvider
                = new CustomUserDetailsAuthenticationProvider(secUserRepository, passwordEncoder);
    }

    @Test
    public void valid_username_valid_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(user.getUsername(), "validPassword");

        Authentication authenticate = customUserDetailsAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);

        assertThat(authenticate).isNotNull();
        assertThat(authenticate.getPrincipal()).isInstanceOf(UserDetails.class);
        assertThat(((UserDetails)authenticate.getPrincipal()).getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    public void bad_username_valid_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken("badUsername", "validPassword");

        Assertions.assertThrows(BadCredentialsException.class, () -> {
            customUserDetailsAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);
        });
    }

    @Test
    public void valid_username_bad_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(user.getUsername(), "badPassword");

        Assertions.assertThrows(BadCredentialsException.class, () -> {
            customUserDetailsAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);
        });
    }

}
