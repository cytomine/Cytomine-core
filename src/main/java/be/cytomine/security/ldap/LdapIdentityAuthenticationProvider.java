package be.cytomine.security.ldap;

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

@Slf4j
@AllArgsConstructor
@Component
public class LdapIdentityAuthenticationProvider implements AuthenticationProvider {

    private final LdapClient ldapClient;

    private final Environment env;

    private final CASLdapUserDetailsService casLdapUserDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) {
        log.debug("LdapIdentityAuthenticationProvider authentication");
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        String search = env.getProperty("application.authentication.ldap.search", "NO_LDAP_SEARCH");
        List<String> attrIds = Arrays.stream(env.getProperty("application.authentication.ldap.attributes").split(",")).toList();

        try {
            if (ldapClient.isInLDAP(search, username, attrIds)) {
                if (ldapClient.hasValidCredential("cn="+username +"," + search, env.getProperty("application.authentication.ldap.passwordAttributeName", "NO_LDAP_SEARCH"), password)) {
                    UserDetails userDetails = casLdapUserDetailsService.loadUserByUsername(username);
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            password,
                            userDetails.getAuthorities());
                } else {
                    throw new BadCredentialsException("Password wrong for username " + username + " in LDAP");
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
