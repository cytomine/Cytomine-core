package be.cytomine.security.authentication;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.security.User;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.CASLdapUserDetailsService;
import be.cytomine.security.CustomUserDetailsAuthenticationProvider;
import be.cytomine.security.ldap.LdapClient;
import be.cytomine.security.ldap.LdapIdentityAuthenticationProvider;
import be.cytomine.service.meta.ConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class LdapIdentityAuthenticationProviderTests {

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    Environment env;

    @Autowired
    CASLdapUserDetailsService casLdapUserDetailsService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    BasicInstanceBuilder builder;

    LdapIdentityAuthenticationProvider ldapIdentityAuthenticationProvider;

    @Autowired
    ConfigurationRepository configurationRepository;

    @BeforeEach
    public void initUser() {
        ldapIdentityAuthenticationProvider
                = new LdapIdentityAuthenticationProvider(env, casLdapUserDetailsService, configurationRepository);

        configurationRepository.deleteAll();
        configurationRepository.findAll(); // force refresh
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_SERVER, BasicInstanceBuilder.LDAP_URL);
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_PRINCIPAL, "CN=admin,OU=users,DC=mtr,DC=com");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_PASSWORD, "itachi");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_SEARCH, "OU=users,DC=mtr,DC=com");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_ATTRIBUTES, "cn,sn,givenname,mail");
        builder.given_a_configuration(ConfigurationService.CONFIG_KEY_LDAP_PASSWORD_ATTRIBUTE_NAME, "password");
    }

    @Test
    public void valid_username_valid_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken("jdoeLDAP", "goodPassword");

        Authentication authenticate = ldapIdentityAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);

        assertThat(authenticate).isNotNull();
        assertThat(authenticate.getPrincipal()).isInstanceOf(UserDetails.class);
        assertThat(((UserDetails)authenticate.getPrincipal()).getUsername()).isEqualTo("jdoeLDAP");
    }

    @Test
    public void bad_username_valid_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken("badUsername", "goodPassword");

        // if username not in ldap, fallback to simple custom auth
        assertThat(ldapIdentityAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken)).isNull();
    }

    @Test
    public void valid_username_bad_password() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken("jdoeLDAP", "badPassword");

        Assertions.assertThrows(BadCredentialsException.class, () -> {
            ldapIdentityAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);
        });
    }

}
