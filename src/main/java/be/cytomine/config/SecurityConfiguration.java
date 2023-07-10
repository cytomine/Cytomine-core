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
import be.cytomine.config.security.JWTConfigurer;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.*;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.saml.OpenSamlAuthenticationProviderWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.*;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfiguration {


    //SAML2 configuration, only endpoints containing /saml2/** or /saml/** are protected by SAML
    //Is conditionally enabled by the presence of the saml profile
    @Configuration
    @Profile({"saml"})
    @Order(1)
    public static class SamlAuthWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private OpenSamlAuthenticationProviderWrapper openSamlAuthenticationProviderWrapper;
        @Autowired
        private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            DefaultRelyingPartyRegistrationResolver relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(this.relyingPartyRegistrationRepository);

            //this filter will expose the sp metadata
            Saml2MetadataFilter filter = new Saml2MetadataFilter((RelyingPartyRegistrationResolver)
                    relyingPartyRegistrationResolver,
                    new OpenSamlMetadataResolver());
            //override default sp metadata endpoint
            filter.setRequestMatcher(new AntPathRequestMatcher("/saml2/sp-metadata/{registrationId}", "GET"));

            http.requestMatchers().antMatchers("/saml/**", "/saml2/**", "/login/saml2/**",  "/logout/saml2/**").and()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .saml2Login(saml2 -> saml2
                            //override the default saml2 auth provider to use our custom one
                            .authenticationManager(a -> openSamlAuthenticationProviderWrapper.authenticate(a)))
                    .addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class)
                    .saml2Logout(withDefaults());
        }
    }


    //JWT configuration, all other endpoints are protected by JWT
    @Configuration
    @Order(2)
    public static class BasicAuthWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Value("${application.authentication.jwt.token-validity-in-seconds}")
        Long tokenValidityInSeconds;
        @Value("${application.authentication.ldap.enabled}")
        Boolean ldapEnabled;
        @Autowired
        private TokenProvider tokenProvider;
        @Autowired
        private DomainUserDetailsService domainUserDetailsService;
        @Autowired
        private CASLdapUserDetailsService casLdapUserDetailsService;
        @Autowired
        private SecUserRepository secUserRepository;

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

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new MessageDigestPasswordEncoder("SHA-256");
        }


        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            if (ldapEnabled) {
                log.info("LDAP authentication configuration");
                auth.userDetailsService(casLdapUserDetailsService).passwordEncoder(passwordEncoder());
            } else {
                log.info("Custom authentication configuration");
                auth.userDetailsService(domainUserDetailsService).passwordEncoder(passwordEncoder());
            }
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


        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .csrf()
                    .disable()
                    .authorizeRequests()
                    .and()
                    .authorizeRequests()
                    .antMatchers("/api/configuration/key/LOGIN_WELCOME.json").permitAll()
                    .antMatchers("/api/configuration/key/DEMO_ACCOUNT.json").permitAll()
                    .antMatchers("/api/token.json").permitAll()
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
                    .and()
                    .addFilterBefore(new ApiKeyFilter(domainUserDetailsService, secUserRepository), BasicAuthenticationFilter.class)
                    .exceptionHandling().authenticationEntryPoint(
                            (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
//                    .and()
//                    .logout()
//                    .logoutUrl("/api/logout")
//                    .logoutSuccessHandler(ajaxLogoutSuccessHandler())
//                    .permitAll()
                    .and()
                    .apply(securityConfigurerAdapter())
                    .and()
                    .addFilter(switchUserFilter())
                    .headers()
                    .cacheControl().disable();

        }
    }


}
