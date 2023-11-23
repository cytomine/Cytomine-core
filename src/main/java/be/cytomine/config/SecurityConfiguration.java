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
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

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
//        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }

      // TODO: we are trying to migrate this to exposing a Bean is it really working? NOT sure more testing is needed
     // Check out: authManager(UserDetailsService detailsService) for how we migrated
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(domainUserDetailsService).passwordEncoder(passwordEncoder());
//    }

    /**
     * configures Spring Security to use your DomainUserDetailsService to fetch user details from a custom source (DB which's SecUserRepository)
     * and to use the provided PasswordEncoder to encode and verify passwords.
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authManager(UserDetailsService detailsService){
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(detailsService);
        daoProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoProvider);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .requestMatchers("/app/**/*.{js,html}")
                .requestMatchers("/i18n/**")
                .requestMatchers("/content/**")
                .requestMatchers("/h2-console/**")
                .requestMatchers("/test/**");
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }

    /**
     * HTTP SECURITY CONFIG
     * Spring Security 6 Require Explicit Saving of SecurityContextRepository
     *
     * @param http the {@link HttpSecurity} to modify
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                .authorizeHttpRequests()
                .requestMatchers("/api/authenticate").permitAll()
                .requestMatchers("/api/register").permitAll()
                .requestMatchers("/api/activate").permitAll()
                .requestMatchers("/api/account/resetPassword/init").permitAll()
                .requestMatchers("/api/account/resetPassword/finish").permitAll()
                .requestMatchers("/api/login/impersonate*").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/session/admin/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/server/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/server/**").permitAll()
                .requestMatchers("/**").permitAll()
                // For spring 6 (this is default behaviour this line was added to simulate that) [It’s recommended that Spring Security secure all dispatch types]
                .shouldFilterAllDispatcherTypes(true)
                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()

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

        return http.build();
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
