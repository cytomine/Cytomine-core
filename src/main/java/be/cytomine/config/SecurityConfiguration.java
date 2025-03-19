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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
     *
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
//        Digest based password encoding is not considered secure.
//        Have to keep it for old Clients
        return new MessageDigestPasswordEncoder("SHA-256");
    }

    /**
     * configures Spring Security to use your DomainUserDetailsService to fetch user details from a custom source (DB which's SecUserRepository)
     * and to use the provided PasswordEncoder to encode and verify passwords.
     *
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authManager(UserDetailsService detailsService) {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(detailsService);
        daoProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoProvider);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/**", "OPTIONS"))
                .requestMatchers(new AntPathRequestMatcher("/app/**/*.{js,html}"))
                .requestMatchers(new AntPathRequestMatcher("/i18n/**"))
                .requestMatchers(new AntPathRequestMatcher("/content/**"))
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .requestMatchers(new AntPathRequestMatcher("/test/**"));
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
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ApiKeyFilter(domainUserDetailsService, secUserRepository), BasicAuthenticationFilter.class)
                .exceptionHandling((exceptionHandling) ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )

                .authorizeHttpRequests((authorizeHttpRequests) ->
                                authorizeHttpRequests
                                        .requestMatchers("/api/authenticate").permitAll()
                                        .requestMatchers("/api/register").permitAll()
                                        .requestMatchers("/api/activate").permitAll()
                                        .requestMatchers("/api/account/resetPassword/init").permitAll()
                                        .requestMatchers("/api/account/resetPassword/finish").permitAll()
                                        .requestMatchers("/api/login/impersonate*").hasAuthority("ROLE_ADMIN")
                                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                                        .requestMatchers("/session/admin/info.json").authenticated()
                                        .requestMatchers("/session/admin/open.json").authenticated()
                                        .requestMatchers("/session/admin/close.json").authenticated()
                                        .requestMatchers(HttpMethod.GET, "/server/ping").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/server/ping.json").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/server/ping").permitAll()
                                        .requestMatchers(HttpMethod.POST, "/server/ping.json").permitAll()
                                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                )
                // For spring 6 (this is default behaviour this line was added to simulate that) [It’s recommended that Spring Security secure all dispatch types]
//                .shouldFilterAllDispatcherTypes(true)
//                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()

                .apply(securityConfigurerAdapter())
            .and()
                .addFilter(switchUserFilter())
                .headers((headers) ->
                        headers
                                .cacheControl(cache -> cache.disable())
                )

            // For Spring 6, opting into it within 5.8 as far as it doesn't break my app we can safely migrate
            // Remove me once we migrated as these are the defaults behaviors in Spring Sec 6
//                .securityContext((securityContext) -> securityContext
//                        .requireExplicitSave(true))
//                .sessionManagement((sessions) -> sessions
//                        .requireExplicitAuthenticationStrategy(true)
//                )
        ;

        return http.build();
    }
}
