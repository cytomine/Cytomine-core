package be.cytomine.security.saml;


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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/*
This is a wrapper for OpenSamlAuthenticationProvider that allows us to intercept the authentication process
 */
@Component
@Slf4j
@Transactional
@Data
public class OpenSamlAuthenticationProviderWrapper {
    private final CasSamlUserDetailsService casSamlUserDetailsService;
    private OpenSamlAuthenticationProvider openSamlAuthenticationProvider = new OpenSamlAuthenticationProvider();

    public OpenSamlAuthenticationProviderWrapper(CasSamlUserDetailsService casSamlUserDetailsService) {
        this.casSamlUserDetailsService = casSamlUserDetailsService;
        this.openSamlAuthenticationProvider.setAuthoritiesMapper(new GrantedAuthoritiesMapper() {
            @Override
            public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
                return authorities;
            }
        });
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        try {
            log.debug("attempting to authenticate with saml2");
            Authentication auth = openSamlAuthenticationProvider.authenticate(authentication);

            log.debug("authenticated with saml2");
            String tempPassword = UUID.randomUUID().toString();
            UserDetails userDetails = casSamlUserDetailsService.loadUserBySaml2AuthPrincipal((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal(), tempPassword);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(userDetails.getUsername(),
                    tempPassword,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            log.debug("jwt token generated and successfully set in security context");
            return newAuth;
        } catch (Exception e) {
            log.error("error authenticating with saml2", e);
            throw new RuntimeException(e);
        }
    }


    public void setResponseElementsDecrypter(Consumer<OpenSamlAuthenticationProvider.ResponseToken> responseElementsDecrypter) {
        openSamlAuthenticationProvider.setResponseElementsDecrypter(responseElementsDecrypter);
    }

    public void setAssertionValidator(Converter<OpenSamlAuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> assertionValidator) {
        openSamlAuthenticationProvider.setAssertionValidator(assertionValidator);
    }

    public void setAssertionElementsDecrypter(Consumer<OpenSamlAuthenticationProvider.AssertionToken> assertionDecrypter) {
        openSamlAuthenticationProvider.setAssertionElementsDecrypter(assertionDecrypter);
    }

    public void setResponseAuthenticationConverter(Converter<OpenSamlAuthenticationProvider.ResponseToken, ? extends AbstractAuthenticationToken> responseAuthenticationConverter) {
        openSamlAuthenticationProvider.setResponseAuthenticationConverter(responseAuthenticationConverter);
    }

    public void setAuthoritiesExtractor(Converter<Assertion, Collection<? extends GrantedAuthority>> authoritiesExtractor) {
        openSamlAuthenticationProvider.setAuthoritiesExtractor(authoritiesExtractor);
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        openSamlAuthenticationProvider.setAuthoritiesMapper(authoritiesMapper);
    }

    public void setResponseTimeValidationSkew(Duration responseTimeValidationSkew) {
        openSamlAuthenticationProvider.setResponseTimeValidationSkew(responseTimeValidationSkew);
    }


    public boolean supports(Class<?> authentication) {
        return openSamlAuthenticationProvider.supports(authentication);
    }

}

