package be.cytomine.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.transaction.Transactional;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.stream.Collectors;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@Transactional
public class CasLdapUserDetailsServiceTests {

    @Autowired
    CASLdapUserDetailsService casLdapUserDetailsService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BasicInstanceBuilder builder;

    @BeforeEach
    public void cleanUsers() {
        for (User user : userRepository.findAll()) {
            if (user.getUsername().toLowerCase().contains("ldap")) {
                userRepository.delete(user);
            }
        }
    }

    @Test
    public void loading_existing_user_in_ldap_not_yet_in_database_creates_a_new_user() throws NamingException {
        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeLDAP")).isEmpty();
        UserDetails jdoe = casLdapUserDetailsService.loadUserByUsername("jdoeLDAP");
        assertThat(jdoe).isNotNull();
        assertThat(jdoe.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())).contains("ROLE_GUEST");
        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeLDAP")).isPresent();
    }

    @Test
    public void loading_unexisting_user_in_ldap_not_in_database_fails() throws NamingException {
        Assertions.assertThrowsExactly(UsernameNotFoundException.class, () -> casLdapUserDetailsService.loadUserByUsername("jdoeNotInLDAP"));
//        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeNotInLDAP")).isEmpty();
//        UserDetails jdoe = casLdapUserDetailsService.loadUserByUsername("jdoeNotInLDAP");
////        assertThat(jdoe).isNull();
    }

    @Test
    public void loading_unexisting_user_in_ldap_but_already_in_database_works() throws NamingException {
        builder.given_a_guest("jdoeNotInLDAP");
        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeNotInLDAP")).isPresent();
        UserDetails jdoe = casLdapUserDetailsService.loadUserByUsername("jdoeLDAP");
        assertThat(jdoe).isNotNull();
        assertThat(jdoe.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())).contains("ROLE_GUEST");
    }

    @Test
    public void loading_existing_user_in_ldap_but_already_in_database_works() throws NamingException {
        builder.given_a_guest("jdoeLDAP");
        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeLDAP")).isPresent();
        UserDetails jdoe = casLdapUserDetailsService.loadUserByUsername("jdoeLDAP");
        assertThat(jdoe).isNotNull();
        assertThat(jdoe.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())).contains("ROLE_GUEST");
        assertThat(userRepository.findByUsernameLikeIgnoreCase("jdoeLDAP")).isPresent();
    }




}




