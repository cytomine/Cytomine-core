package be.cytomine.config.security;

import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.DomainUserDetailsService;
import be.cytomine.security.jwt.TokenProvider;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class ApiKeyConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final DomainUserDetailsService domainUserDetailsService;

    private final SecUserRepository secUserRepository;


    public ApiKeyConfigurer(DomainUserDetailsService domainUserDetailsService, SecUserRepository secUserRepository) {
        this.domainUserDetailsService = domainUserDetailsService;
        this.secUserRepository = secUserRepository;
    }

    @Override
    public void configure(HttpSecurity http) {
        ApiKeyFilter customFilter = new ApiKeyFilter(domainUserDetailsService, secUserRepository);
        http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
