package be.cytomine.config;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.config.security.ApiKeyFilter;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.*;
import be.cytomine.config.security.JWTConfigurer;
import be.cytomine.security.jwt.TokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import javax.servlet.http.HttpServletResponse;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final TokenProvider tokenProvider;

    private final DomainUserDetailsService domainUserDetailsService;

    private final SecUserRepository secUserRepository;

    @Value("${application.authentication.jwt.token-validity-in-seconds}")
    Long tokenValidityInSeconds;


    public SecurityConfiguration(TokenProvider tokenProvider, DomainUserDetailsService domainUserDetailsService, SecUserRepository secUserRepository) {
        this.tokenProvider = tokenProvider;
        this.domainUserDetailsService = domainUserDetailsService;
        this.secUserRepository = secUserRepository;
    }

    @Bean
    public SwitchUserFilter switchUserFilter() {
        SwitchUserFilter filter = new SwitchUserFilter();
        filter.setUserDetailsService(domainUserDetailsService);
        filter.setSuccessHandler(switchUserSuccessHandler());
        filter.setFailureHandler(switchUserFailureHandler());
        filter.setUsernameParameter("username");
        filter.setSwitchUserUrl("/api/login/impersonate");
        //filter.setSwitchFailureUrl("/api/login/switchUser");
        //filter.setTargetUrl("/admin/user-management");
        return filter;
    }

    @Bean
    public SwitchUserSuccessHandler switchUserSuccessHandler() {
        return new SwitchUserSuccessHandler(tokenProvider, tokenValidityInSeconds);
    }

    @Bean
    public SwitchUserFailureHandler switchUserFailureHandler() {
        return new SwitchUserFailureHandler();
    }

    @Bean
    public AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler() {
        return new AjaxLogoutSuccessHandler();
    }

        /**
     * Argon2 is intentionally slow: slow-hashing functions are good for storing passwords, because it is time/resource consuming to crack them.
     * SHA-512 is not designed for storing passwords. so insecure and deprecated so in future check if sha256 use it if not use argon2 or bcrypt.
     * recommended way is to use DelegatingPasswordEncoder()
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
//        Digest based password encoding is not considered secure.
//        Have to keep it for old Clients
        return new MessageDigestPasswordEncoder("SHA-256");

//        To-Do: something along these lines ....
//        PasswordEncoder current = new MessageDigestPasswordEncoder("SHA-256");
//
//        String idForEncode = "argon2";
//
//        Map<String,PasswordEncoder> encoders = new HashMap<>();
//        encoders.put("bcrypt", new BCryptPasswordEncoder());
//        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
//        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
//        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
//        encoders.put("sha256", new MessageDigestPasswordEncoder("SHA-256"));
//
//
//        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(domainUserDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
                .antMatchers(HttpMethod.OPTIONS, "/**")
                .antMatchers("/app/**/*.{js,html}")
                .antMatchers("/i18n/**")
                .antMatchers("/content/**")
                .antMatchers("/h2-console/**")
                .antMatchers("/test/**");
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }

    /**
     * Spring Security 6 Require Explicit Saving of SecurityContextRepository
     * @param http the {@link HttpSecurity} to modify
     * @throws Exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
// @formatter:off
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .csrf()
            .disable()
            .addFilterBefore(new ApiKeyFilter(domainUserDetailsService, secUserRepository), BasicAuthenticationFilter.class)
            .exceptionHandling().authenticationEntryPoint(
                    (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
//        .and()
//            .logout()
//            .logoutUrl("/api/logout")
//            .logoutSuccessHandler(ajaxLogoutSuccessHandler())
//            .permitAll()
        .and()
            .authorizeRequests()
            .antMatchers("/api/authenticate").permitAll()
            .antMatchers("/api/register").permitAll()
            .antMatchers("/api/activate").permitAll()
            .antMatchers("/api/account/resetPassword/init").permitAll()
            .antMatchers("/api/account/resetPassword/finish").permitAll()
            .antMatchers("/api/login/impersonate*").hasAuthority("ROLE_ADMIN")
            .antMatchers("/api/**").authenticated()
            .antMatchers("/session/admin/**").authenticated()
            .antMatchers(HttpMethod.GET, "/server/**").permitAll()
            .antMatchers(HttpMethod.POST, "/server/**").permitAll()
            .antMatchers("/**").permitAll()

//        .and()
//            .httpBasic()
        .and()
            .apply(securityConfigurerAdapter())
        .and()
            .addFilter(switchUserFilter())
                .headers()
                .cacheControl().disable()
        // For Spring 6, opting into it within 5.8 as far as it doesn't break my app we can safely migrate
        // Remove me once we migrated as these are the defaults behaviors in Spring Sec 6
        .and()
            .securityContext((securityContext) -> securityContext
                    .requireExplicitSave(true))
            .sessionManagement((sessions) -> sessions
                    .requireExplicitAuthenticationStrategy(true)
            );
        // @formatter:on
    }

//    grails.plugin.springsecurity.interceptUrlMap = [
//        '/admin/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
//        '/admincyto/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
//        '/monitoring/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
//        '/j_spring_security_switch_user': ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
//        '/securityInfo/**': ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
//        '/api/**':      ['IS_AUTHENTICATED_REMEMBERED'],
//        '/lib/**':      ['IS_AUTHENTICATED_ANONYMOUSLY'],
//        '/css/**':      ['IS_AUTHENTICATED_ANONYMOUSLY'],
//        '/images/**':   ['IS_AUTHENTICATED_ANONYMOUSLY'],
//        '/*':           ['IS_AUTHENTICATED_REMEMBERED'], //if cas authentication, active this      //beta comment
//        '/login/**':    ['IS_AUTHENTICATED_ANONYMOUSLY'],
//        '/logout/**':   ['IS_AUTHENTICATED_ANONYMOUSLY'],
//        '/status/**':   ['IS_AUTHENTICATED_ANONYMOUSLY']
//        ]
}
