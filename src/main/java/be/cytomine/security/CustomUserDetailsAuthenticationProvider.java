package be.cytomine.security;

import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static be.cytomine.security.DomainUserDetailsService.createSpringSecurityUser;

@Component
public class CustomUserDetailsAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    SecUserRepository secUserRepository;

    PasswordEncoder passwordEncoder;

    public CustomUserDetailsAuthenticationProvider(SecUserRepository secUserRepository, PasswordEncoder passwordEncoder) {
        this.secUserRepository = secUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // check password
        String passwordProvided = (String)authentication.getCredentials();
        String currentEncodedPassword = userDetails.getPassword();

        if (!passwordEncoder.matches(passwordProvided, currentEncodedPassword)) {
            throw new BadCredentialsException("Invalid password");
        }
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        return secUserRepository.findByUsernameLikeIgnoreCase(username).map(x -> createSpringSecurityUser(username, x))
                .orElseThrow(() -> new UsernameNotFoundException("Username "+username+" not found"));
    }
}
