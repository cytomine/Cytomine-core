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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    /**
     * Argon2 is intentionally slow: slow-hashing functions are good for storing passwords, because it is time/resource consuming to crack them.
     * SHA-512 is not designed for storing passwords. so insecure and deprecated so in future check if sha256 use it if not use argon2 or bcrypt.
     * recommended way is to use DelegatingPasswordEncoder()
     *
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() { //TODO IAM: delete
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

    /**
     * configures Spring Security to use your DomainUserDetailsService to fetch user details from a custom source (DB which's SecUserRepository)
     * and to use the provided PasswordEncoder to encode and verify passwords.
     *
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authManager(UserDetailsService detailsService) {
        // TODO IAM: adapt
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(detailsService);
        daoProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(daoProvider);
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
                .exceptionHandling((exceptionHandling) ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .authorizeHttpRequests((authorizeHttpRequests) ->
                                authorizeHttpRequests
                                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                                        .requestMatchers("/session/admin/info.json").authenticated()
                                        .requestMatchers("/session/admin/open.json").authenticated()
                                        .requestMatchers("/session/admin/close.json").authenticated()
                                        .requestMatchers(HttpMethod.GET, "/server/ping").permitAll() // TODO IAM: remove
                                        .requestMatchers(HttpMethod.GET, "/server/ping.json").permitAll() // TODO IAM: remove
                                        .requestMatchers(HttpMethod.POST, "/server/ping").permitAll() // TODO IAM: remove
                                        .requestMatchers(HttpMethod.POST, "/server/ping.json").permitAll() // TODO IAM: remove
                                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll() // TODO IAM: remove ?
                );
        return http.build();
    }
}
