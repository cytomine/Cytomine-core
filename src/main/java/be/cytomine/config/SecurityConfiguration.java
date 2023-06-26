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
import be.cytomine.security.saml.OpenSamlAuthenticationProviderWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfiguration {

    private final TokenProvider tokenProvider;

    private final DomainUserDetailsService domainUserDetailsService;

    private final CASLdapUserDetailsService casLdapUserDetailsService;


    private final SecUserRepository secUserRepository;
    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    @Autowired
    private OpenSamlAuthenticationProviderWrapper openSamlAuthenticationProviderWrapper;



    @Value("${application.authentication.jwt.token-validity-in-seconds}")
    Long tokenValidityInSeconds;

    @Value("${application.authentication.ldap.enabled}")
    Boolean ldapEnabled;

//    @Value("${security.saml2.relyingparty.registration.shibboleth.assertingparty.metadata-uri}")
//    private String metadataUri;
//
//    @Value("${security.saml2.relyingparty.registration.registration-id}")
//    private String registrationId;


    public SecurityConfiguration(TokenProvider tokenProvider, DomainUserDetailsService domainUserDetailsService, CASLdapUserDetailsService casLdapUserDetailsService, SecUserRepository secUserRepository, RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this.tokenProvider = tokenProvider;
        this.domainUserDetailsService = domainUserDetailsService;
        this.casLdapUserDetailsService = casLdapUserDetailsService;
        this.secUserRepository = secUserRepository;
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MessageDigestPasswordEncoder("SHA-256");
    }



    //@Override
    AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) throws Exception {
        if (ldapEnabled) {
            log.info("LDAP authentication configuration");
            return auth.userDetailsService(casLdapUserDetailsService).passwordEncoder(passwordEncoder()).and().build();
        } else {
            log.info("Custom authentication configuration");
            return  auth.userDetailsService(domainUserDetailsService).passwordEncoder(passwordEncoder()).and().build();
        }
    }


//    @Bean
//    public void configure(WebSecurity web) {
//        web.ignoring()
//                .antMatchers(HttpMethod.OPTIONS, "/**")
//                .antMatchers("/app/**/*.{js,html}")
//                .antMatchers("/i18n/**")
//                .antMatchers("/content/**")
//                .antMatchers("/h2-console/**")
//                .antMatchers("/test/**");
//    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**")
                .antMatchers("/app/**/*.{js,html}")
                .antMatchers("/i18n/**")
                .antMatchers("/content/**")
                .antMatchers("/h2-console/**")
                .antMatchers("/test/**");
    }


    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider);
    }

//    @Bean
//    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
//        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
//        authenticationProvider.setResponseAuthenticationConverter(groupsConverter());
//// @formatter:off
//        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
//            .csrf()
//            .disable()
//            .addFilterBefore(new ApiKeyFilter(domainUserDetailsService, secUserRepository), BasicAuthenticationFilter.class)
//            .exceptionHandling().authenticationEntryPoint(
//                    (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
////        .and()
////            .logout()
////            .logoutUrl("/api/logout")
////            .logoutSuccessHandler(ajaxLogoutSuccessHandler())
////            .permitAll()
//        .and()
//            .authorizeRequests()
//            .antMatchers("/api/configuration/key/LOGIN_WELCOME.json").permitAll()
//            .antMatchers("/api/configuration/key/DEMO_ACCOUNT.json").permitAll()
//            .antMatchers("/api/token.json").permitAll()
//            .antMatchers("/api/authenticate").permitAll()
//            .antMatchers("/api/register").permitAll()
//            .antMatchers("/api/activate").permitAll()
//            .antMatchers("/api/account/resetPassword/init").permitAll()
//            .antMatchers("/api/account/resetPassword/finish").permitAll()
//            .antMatchers("/api/login/impersonate*").hasAuthority("ROLE_ADMIN")
//            .antMatchers("/api/**").authenticated()
//            .antMatchers("/session/admin/**").authenticated()
//            .antMatchers(HttpMethod.GET, "/server/**").permitAll()
//            .antMatchers(HttpMethod.POST, "/server/**").permitAll()
//            .antMatchers("/**").permitAll()
////        .and()
////            .httpBasic()
////        .and()
////            .apply(securityConfigurerAdapter())
//                .and()
//            .addFilter(switchUserFilter())
//                .headers()
//                .cacheControl().disable().and().saml2Login(saml2 -> saml2
//                        .authenticationManager(new ProviderManager(authenticationProvider)))
//                .saml2Logout(withDefaults());
//        // @formatter:on
//        return http.build();
//    }

//    @Bean
//    public SAMLAuthenticationProvider samlAuthenticationProvider() {
//        final OpenSamlAuthenticationProvider samlAuthenticationProvider = new OpenSamlAuthenticationProvider();
//        samlAuthenticationProvider..setUserDetails            (new MySamlUserDetailsService());
//        samlAuthenticationProvider.setForcePrincipalAsString (false);
//        return samlAuthenticationProvider;
//    }

//    @Bean
//    protected RelyingPartyRegistrationRepository relyingPartyRegistrations() throws Exception {
//     //  RelyingPartyRegistration oktaRegistration = RelyingPartyRegistrations.fromMetadataLocation("https://dev-78925480.okta.com/app/exk9parbhynfPgFhL5d7/sso/saml/metadata").registrationId("okta").build();
////    // RelyingPartyRegistration oktaRegistration = RelyingPartyRegistrations.fromMetadataLocation("https://dev-78925480.okta.com/app/exk9r8de3gVOdEvUR5d7/sso/saml/metadata").registrationId("okta").build();
////        RelyingPartyRegistration oktaRegistration = RelyingPartyRegistrations.fromMetadataLocation("http://localhost:777/idp/shibboleth").registrationId("shibboleth").build();
////
////        //https://dev-78925480.okta.com/app/exk9parbhynfPgFhL5d7/sso/saml/metadata
////       List<RelyingPartyRegistration> registrationList = new ArrayList<>();
////        registrationList.add(oktaRegistration);
//        return this.relyingPartyRegistrationRepository;
//    }

    @Bean
    SecurityFilterChain configure(HttpSecurity http) throws Exception {


        DefaultRelyingPartyRegistrationResolver relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(this.relyingPartyRegistrationRepository);

        Saml2MetadataFilter filter = new Saml2MetadataFilter((RelyingPartyRegistrationResolver)
                relyingPartyRegistrationResolver,
                new OpenSamlMetadataResolver());
       // OpenSamlAuthenticationProviderWrapper p = new OpenSamlAuthenticationProviderWrapper();

//
//        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
//        authenticationProvider.set
//        authenticationProvider.setResponseAuthenticationConverter(groupsConverter());

        http//.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .csrf().disable()
                .addFilterBefore(new ApiKeyFilter(domainUserDetailsService, secUserRepository), BasicAuthenticationFilter.class)
              //  .exceptionHandling().authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .authorizeRequests(authorize -> authorize
                        .antMatchers("/api/configuration/key/DEMO_ACCOUNT.json").permitAll()
                        .antMatchers("/api/configuration/key/LOGIN_WELCOME.json").permitAll()
                        .antMatchers("/api/token.json").permitAll()
                        .antMatchers("/api/authenticate").permitAll()
                        .antMatchers("/api/register").permitAll()
                        .antMatchers("/api/activate").permitAll()
                        .antMatchers("/login/loginWithToken").permitAll()
                        .antMatchers("/saml2/service-provider-metadata/**").permitAll()
                        .antMatchers("/api/account/resetPassword/init").permitAll()
                        .antMatchers("/api/account/resetPassword/finish").permitAll()
                        .antMatchers("/api/login/impersonate*").hasAuthority("ROLE_ADMIN")
                        .antMatchers("/api/**").authenticated()
                        .antMatchers("/session/admin/**").authenticated()
                        .antMatchers(HttpMethod.GET, "/server/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/server/**").permitAll()
                        .anyRequest().authenticated()
                )
                .saml2Login(saml2 -> saml2
                        .authenticationManager(a -> {
                            return openSamlAuthenticationProviderWrapper.authenticate(a);
                            // transform the result however you want

                        })).addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class)
                .saml2Logout(withDefaults());
              //  .exceptionHandling().authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED));

        return http.build();
    }

    private Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> groupsConverter() {

        Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> delegate =
                OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();

        return (responseToken) -> {
            Saml2Authentication authentication = delegate.convert(responseToken);
            Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            List<String> groups = principal.getAttribute("groups");
            Set<GrantedAuthority> authorities = new HashSet<>();
            if (groups != null) {
                groups.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
            } else {
                authorities.addAll(authentication.getAuthorities());
            }
            return new Saml2Authentication(principal, authentication.getSaml2Response(), authorities);
        };
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
//]
}
