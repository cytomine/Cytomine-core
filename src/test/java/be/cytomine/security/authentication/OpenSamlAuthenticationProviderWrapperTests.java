package be.cytomine.security.authentication;


import be.cytomine.CytomineCoreApplication;
import be.cytomine.security.saml.CasSamlUserDetailsService;
import be.cytomine.security.saml.OpenSamlAuthenticationProviderWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@EnabledIf(expression = "#{environment.acceptsProfiles('saml')}", loadContext = true)
public class OpenSamlAuthenticationProviderWrapperTests {
    @Mock
    private CasSamlUserDetailsService mockUserDetailsService;
    @Mock
    private OpenSamlAuthenticationProvider openSamlAuthenticationProvider;

    private OpenSamlAuthenticationProviderWrapper authenticationProviderWrapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        authenticationProviderWrapper = new OpenSamlAuthenticationProviderWrapper(mockUserDetailsService);
        authenticationProviderWrapper.setOpenSamlAuthenticationProvider(openSamlAuthenticationProvider);
    }

    @Test
    public void authenticate_SuccessfulAuthentication() throws AuthenticationException, NoSuchFieldException, IllegalAccessException {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "username",
                "tempPassword",
                Collections.singleton((GrantedAuthority) () -> "ROLE_USER")
        );
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        samlAttributes.put("employType", Arrays.asList("tourist"));
        Saml2AuthenticatedPrincipal samlPrincipal = new DefaultSaml2AuthenticatedPrincipal("samlPrincipal", samlAttributes);
        Authentication expectedAuth = mock(Authentication.class);

        when(openSamlAuthenticationProvider.authenticate(any())).thenReturn(expectedAuth);
        when(expectedAuth.getPrincipal()).thenReturn(samlPrincipal);
        when(mockUserDetailsService.loadUserBySaml2AuthPrincipal(any(), anyString()))
                .thenReturn(userDetails);
        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        Authentication result = authenticationProviderWrapper.authenticate(authenticationCaptor.capture());
        assertEquals("username", result.getName());
        assertEquals(1, result.getAuthorities().size());
        assertEquals("ROLE_USER", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    public void authenticate_Fails() throws AuthenticationException {

        when(openSamlAuthenticationProvider.authenticate(any())).thenThrow(new RuntimeException("Error"));

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> authenticationProviderWrapper.authenticate(authenticationCaptor.capture())
        );
    }

    @Test
    public void authenticate_ThrowsException_WhenLoadUSerFails() throws AuthenticationException, NoSuchFieldException, IllegalAccessException {

        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        samlAttributes.put("employType", Arrays.asList("tourist"));
        Saml2AuthenticatedPrincipal samlPrincipal = new DefaultSaml2AuthenticatedPrincipal("samlPrincipal", samlAttributes);
        Authentication expectedAuth = mock(Authentication.class);

        when(openSamlAuthenticationProvider.authenticate(any())).thenReturn(expectedAuth);
        when(expectedAuth.getPrincipal()).thenReturn(samlPrincipal);
        when(mockUserDetailsService.loadUserBySaml2AuthPrincipal(any(DefaultSaml2AuthenticatedPrincipal.class), anyString()))
                .thenThrow(new RuntimeException("Error"));

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> authenticationProviderWrapper.authenticate(authenticationCaptor.capture())
        );
    }
}
