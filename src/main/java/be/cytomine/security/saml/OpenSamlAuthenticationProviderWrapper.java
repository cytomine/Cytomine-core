package be.cytomine.security.saml;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

@Component
@Slf4j
@Transactional
public class OpenSamlAuthenticationProviderWrapper {
    private  OpenSamlAuthenticationProvider openSamlAuthenticationProvider=new OpenSamlAuthenticationProvider();;

   private final CASSamlUserDetailsService casSamlUserDetailsService;

    public OpenSamlAuthenticationProviderWrapper( CASSamlUserDetailsService casSamlUserDetailsService) {
        this.casSamlUserDetailsService = casSamlUserDetailsService;
        this.openSamlAuthenticationProvider.setAuthoritiesMapper(new GrantedAuthoritiesMapper() {
            @Override
            public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
                return authorities;
            }
        });
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth=openSamlAuthenticationProvider.authenticate(authentication);
        String tempPassword= UUID.randomUUID().toString();
        //((DefaultSaml2AuthenticatedPrincipal)auth.getPrincipal()).attributes;
        UserDetails userDetails =casSamlUserDetailsService.loadUserByUsername((DefaultSaml2AuthenticatedPrincipal) auth.getPrincipal(),tempPassword);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                tempPassword,
                userDetails.getAuthorities());
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

