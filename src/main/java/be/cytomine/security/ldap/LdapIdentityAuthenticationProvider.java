package be.cytomine.security.ldap;

import be.cytomine.domain.meta.Configuration;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.CASLdapUserDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.List;

import static be.cytomine.service.meta.ConfigurationService.*;

@Slf4j
@AllArgsConstructor
@Component
public class LdapIdentityAuthenticationProvider implements AuthenticationProvider {

    private final Environment env;

    private final CASLdapUserDetailsService casLdapUserDetailsService;

    private final ConfigurationRepository configurationRepository;

    @Override
    public Authentication authenticate(Authentication authentication) {
        log.debug("LdapIdentityAuthenticationProvider authentication");
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        LdapClient ldapClient = new LdapClient(configurationRepository);

        String search = configurationRepository.findByKey(CONFIG_KEY_LDAP_SEARCH)
                .map(Configuration::getValue)
                .orElse("NO_LDAP_SEARCH");
        log.debug("LdapIdentityAuthenticationProvider search: {}", search);

        String attributes = configurationRepository.findByKey(CONFIG_KEY_LDAP_ATTRIBUTES)
                .map(Configuration::getValue)
                .orElse("");
        log.debug("LdapIdentityAuthenticationProvider attributes: {}", attributes);
        String passwordAttributeName = configurationRepository.findByKey(CONFIG_KEY_LDAP_PASSWORD_ATTRIBUTE_NAME)
                .map(Configuration::getValue)
                .orElse("NO_LDAP_SEARCH");
        log.debug("LdapIdentityAuthenticationProvider passwordAttributeName: {}", passwordAttributeName);
        String usernameAttributeName = configurationRepository.findByKey(CONFIG_KEY_LDAP_USERNAME_ATTRIBUTE_NAME)
                .map(Configuration::getValue)
                .orElse("cn");
        log.debug("LdapIdentityAuthenticationProvider usernameAttributeName: {}", usernameAttributeName);
        List<String> attrIds = Arrays.stream(attributes.split(",")).toList();

        try {
            if (ldapClient.isInLDAP(search, usernameAttributeName, username, attrIds)) {
                if (ldapClient.hasValidCredential(usernameAttributeName + "=" + username + "," + search, passwordAttributeName, password)) {
                    log.debug("authenticated with LDAP");
                    UserDetails userDetails = casLdapUserDetailsService.loadUserByUsername(username);
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            password,
                            userDetails.getAuthorities());
                } else {
                    throw new BadCredentialsException("Searching '" + usernameAttributeName + "=" + username + "," + search + "' with filter '("+ passwordAttributeName + "=" + password + ")' returned no result in LDAP");
                }
            } else {
                return null;
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(Class<?> authenticationType) {
        // must be call explicitly in code
        return false; //authenticationType.equals(UsernamePasswordAuthenticationToken.class);
    }
}
