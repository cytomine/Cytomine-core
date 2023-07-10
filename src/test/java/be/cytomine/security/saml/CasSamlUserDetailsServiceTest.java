package be.cytomine.security.saml;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@EnabledIf(expression = "#{environment.acceptsProfiles('saml')}", loadContext = true)
class CasSamlUserDetailsServiceTest {

    CasSamlUserDetailsService userDetailsService;
    @Autowired
    BasicInstanceBuilder builder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecRoleRepository secRoleRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private DefaultSaml2AuthenticatedPrincipal saml2AuthenticatedPrincipal;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Mock
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDetailsService = new CasSamlUserDetailsService();
        userDetailsService.setUserRepository(userRepository);
        userDetailsService.setSecRoleRepository(secRoleRepository);
        userDetailsService.setApplicationProperties(applicationProperties);
        userDetailsService.setEntityManager(entityManager);
        userDetailsService.setStorageService(storageService);
    }


    @Test
    void loadUserBySaml2AuthPrincipal_TrowNoSuchFieldException_WhenRegistrationIdIsNotConfigured() {

        User disabledUser = builder.given_a_user();
        disabledUser.setEnabled(false);
        LinkedHashMap<String, List<Object>> attributes = new LinkedHashMap<>();
        attributes.put("cn", Arrays.asList(disabledUser.getUsername()));
        attributes.put("sn", Arrays.asList(disabledUser.getLastname()));
        attributes.put("mail", Arrays.asList(disabledUser.getEmail()));
        attributes.put("employType", Arrays.asList("tourist"));

        when(saml2AuthenticatedPrincipal.getAttributes()).thenReturn(attributes);
        when(saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId()).thenReturn("wrongRegistrationId");

        assertThrows(NoSuchFieldException.class, () ->
                userDetailsService.loadUserBySaml2AuthPrincipal(saml2AuthenticatedPrincipal, "password")
        );
    }

    @Test
    void loadUserBySaml2AuthPrincipal_TrowException_WhenUserNAmeAttributeNotFound() {

        User disabledUser = builder.given_a_user();
        disabledUser.setEnabled(false);
        LinkedHashMap<String, List<Object>> attributes = new LinkedHashMap<>();
        attributes.put("sn", Arrays.asList(disabledUser.getLastname()));
        attributes.put("mail", Arrays.asList(disabledUser.getEmail()));
        attributes.put("employType", Arrays.asList("tourist"));

        when(saml2AuthenticatedPrincipal.getAttributes()).thenReturn(attributes);
        when(saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId()).thenReturn("shibboleth");

        assertThrows(Exception.class, () ->
                userDetailsService.loadUserBySaml2AuthPrincipal(saml2AuthenticatedPrincipal, "password")
        );
    }


    @Test
    void loadUserBySaml2AuthPrincipal_ThrowsDisabledException_WhenUserIsDisabled() {

        User disabledUser = builder.given_a_user();
        disabledUser.setEnabled(false);
        LinkedHashMap<String, List<Object>> attributes = new LinkedHashMap<>();
        attributes.put("cn", Arrays.asList(disabledUser.getUsername()));
        attributes.put("sn", Arrays.asList(disabledUser.getLastname()));
        attributes.put("mail", Arrays.asList(disabledUser.getEmail()));
        attributes.put("employType", Arrays.asList("tourist"));

        when(saml2AuthenticatedPrincipal.getAttributes()).thenReturn(attributes);
        when(saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId()).thenReturn("shibboleth");
        when(userRepository.findByUsernameLikeIgnoreCase(disabledUser.getUsername())).thenReturn(java.util.Optional.of(disabledUser));

        assertThrows(DisabledException.class, () ->
                userDetailsService.loadUserBySaml2AuthPrincipal(saml2AuthenticatedPrincipal, "password")
        );
    }

    @Test
    void loadUserBySaml2AuthPrincipal_ReturnsUserDetails_WhenUserExists() throws NoSuchFieldException, IllegalAccessException {
        User enabledUser = builder.given_a_user();
        enabledUser.setEnabled(true);
        LinkedHashMap<String, List<Object>> attributes = new LinkedHashMap<>();
        attributes.put("cn", Arrays.asList(enabledUser.getUsername()));
        attributes.put("sn", Arrays.asList(enabledUser.getLastname()));
        attributes.put("mail", Arrays.asList(enabledUser.getEmail()));
        attributes.put("employType", Arrays.asList("tourist"));

        when(saml2AuthenticatedPrincipal.getAttributes()).thenReturn(attributes);
        when(saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId()).thenReturn("shibboleth");
        when(userRepository.findByUsernameLikeIgnoreCase(enabledUser.getUsername())).thenReturn(java.util.Optional.of(enabledUser));

        UserDetails userDetails = userDetailsService.loadUserBySaml2AuthPrincipal(saml2AuthenticatedPrincipal, "password");

        assertNotNull(userDetails);
        assertEquals(enabledUser.getUsername(), userDetails.getUsername());
    }

    @Test
    void loadUserBySaml2AuthPrincipal_ThrowsException_WhenCreateUserFails() {
        when(userRepository.findByUsernameLikeIgnoreCase(any())).thenThrow(new RuntimeException("User repository error"));
        assertThrows(RuntimeException.class, () ->
                userDetailsService.loadUserBySaml2AuthPrincipal(saml2AuthenticatedPrincipal, "password")
        );
    }

    @Test
    void createUserFromSamlResults_CreateAdminUser_WhenAttributesAreValid() throws NoSuchFieldException {
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            String username = "testuser";
            LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
            samlAttributes.put("cn", Arrays.asList("dummy"));
            samlAttributes.put("sn", Arrays.asList("dummy"));
            samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
            samlAttributes.put("employType", Arrays.asList("tourist"));
            Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "cn", "lastname", "sn", "mail", "mail", "group", "employType", "admin_role", "tourist");

            String registrationId = "registrationId";
            String password = "password";

            User user = builder.given_a_admin();
            user.setUsername(username);
            user.setLastname("dummy");
            user.setFirstname("dummy");
            user.setEmail("dummy@mail.com");
            user.setEnabled(true);
            user.setPassword(password);
            user.setOrigin(registrationId);
            when(userRepository.save(any())).thenReturn(user);

            Mockito.doNothing().when(entityManager).persist(Mockito.any());
            Mockito.doNothing().when(entityManager).flush();
            Mockito.doNothing().when(entityManager).refresh(Mockito.any());
            when(userRepository.findByUsernameLikeIgnoreCase(any())).thenReturn(Optional.of(user));

            //mock call to static method
            utilities.when(() -> SecurityUtils.doWithAuth(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer((Answer<Void>) invocation -> null);

            User finalUser = userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password);
            assertNotNull(finalUser);
            assertEquals(username, finalUser.getUsername());
            assertEquals(registrationId, finalUser.getOrigin());
        }
    }


    @Test
    void createUserFromSamlResults_CreateGuestUser_WhenAttributesAreValid() throws NoSuchFieldException {
        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        samlAttributes.put("employType", Arrays.asList("tourist"));
        Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "cn", "lastname", "sn", "mail", "mail", "group", "employType", "guest_role", "tourist");

        String registrationId = "registrationId";
        String password = "password";

        User user = builder.given_a_guest();
        user.setUsername(username);
        user.setLastname("dummy");
        user.setFirstname("dummy");
        user.setEmail("dummy@mail.com");
        user.setEnabled(true);
        user.setPassword(password);
        user.setOrigin(registrationId);
        when(userRepository.save(any())).thenReturn(user);

        Mockito.doNothing().when(entityManager).persist(Mockito.any());
        Mockito.doNothing().when(entityManager).flush();
        Mockito.doNothing().when(entityManager).refresh(Mockito.any());
        when(userRepository.findByUsernameLikeIgnoreCase(any())).thenReturn(Optional.of(user));
        User finalUser = userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password);
        assertNotNull(finalUser);
        assertTrue(finalUser.getRoles().stream().anyMatch(userRole -> userRole.getAuthority().equals("ROLE_GUEST")));
        assertEquals(username, finalUser.getUsername());
        assertEquals(registrationId, finalUser.getOrigin());

    }


    @Test
    void createUserFromSamlResults_ThrowsNullPointerException_WhenSamlAttributeMissing() throws NoSuchFieldException {
        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "cn", "lastname", "sn", "mail", "mail", "group", "employType", "admin_role", "tourist");

        String registrationId = "registrationId";
        String password = "password";

        // Mocking the behavior for when group attribute is not found
        assertThrows(NullPointerException.class, () -> {
            userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password);
        });
    }


    @Test
    void createUserFromSamlResults_ThrowsException_WhenAttributeMappingFails() {
        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        samlAttributes.put("employType", Arrays.asList("tourist"));
        Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "wrong", "lastname", "sn", "mail", "mail", "group", "employType");

        String registrationId = "registrationId";
        String password = "password";

        assertThrows(Exception.class, () ->
                userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password)
        );
    }

    @Test
    void createUserFromSamlResults_ThrowsNoSuchFieldException_WhenGroupMappingMissing() {
        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        samlAttributes.put("employType", Arrays.asList("tourist"));
        Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "cn", "lastname", "sn", "mail", "mail", "group", "employType");

        String registrationId = "registrationId";
        String password = "password";
        User user = builder.given_a_user();
        user.setUsername(username);
        user.setLastname("dummy");
        user.setFirstname("dummy");
        user.setEmail("dummy@mail.com");
        user.setEnabled(true);
        user.setPassword(password);
        user.setOrigin(registrationId);
        when(userRepository.save(any())).thenReturn(user);

        assertThrows(NoSuchFieldException.class, () ->
                userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password)
        );
    }

    @Test
    void createUserFromSamlResults_ThrowsNoSuchFieldException_WhenUserRoleMappingMissing() {
        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        samlAttributes.put("cn", Arrays.asList("dummy"));
        samlAttributes.put("sn", Arrays.asList("dummy"));
        samlAttributes.put("mail", Arrays.asList("dummy@mail.com"));
        Map<String, String> attributeMap = Map.of("username", "cn", "firstname", "cn", "lastname", "sn", "mail", "mail", "group", "employType", "admin_role", "admin");

        String registrationId = "registrationId";
        String password = "password";
        User user = builder.given_a_user();
        user.setUsername(username);
        user.setLastname("dummy");
        user.setFirstname("dummy");
        user.setEmail("dummy@mail.com");
        user.setEnabled(true);
        user.setPassword(password);
        user.setOrigin(registrationId);
        when(userRepository.save(any())).thenReturn(user);

        assertThrows(NoSuchFieldException.class, () ->
                userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password)
        );
    }

    @Test
    void createUserFromSamlResults_ThrowsException_WhenUserRepositoryFails() {

        String username = "testuser";
        LinkedHashMap<String, List<Object>> samlAttributes = new LinkedHashMap<>();
        Map<String, String> attributeMap = Map.of("username", "username", "firstname", "firstname", "lastname", "lastname", "mail", "mail");
        String registrationId = "registrationId";
        String password = "password";

        when(userRepository.save(any())).thenThrow(new RuntimeException("User repository error"));

        assertThrows(RuntimeException.class, () ->
                userDetailsService.createUserFromSamlResults(username, samlAttributes, attributeMap, registrationId, password)
        );
    }

}
