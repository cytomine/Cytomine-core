package be.cytomine.security.saml;

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.security.Language;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.ResourcesUtils;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static be.cytomine.security.DomainUserDetailsService.createSpringSecurityUser;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CASSamlUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecRoleRepository secRoleRepository;
    @Autowired
    private Environment env;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StorageService storageService;



    public UserDetails loadUserByUsername(DefaultSaml2AuthenticatedPrincipal saml2AuthenticatedPrincipal, String password)
            throws UsernameNotFoundException {

        LinkedHashMap<String, List<Object>> attributes = (LinkedHashMap<String, List<Object>>) saml2AuthenticatedPrincipal.getAttributes();
        Map<String, String> attributeMap = ResourcesUtils.getPropertiesMap(applicationProperties.getAuthentication().getSaml2().getAttributeFile());
        String registrationId = saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId();
        String username=(String) attributes.get(attributeMap.get("username")).get(0);
        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseGet(() -> {
                    try {
                        return createUserFromSamlResults(username, attributes, attributeMap,registrationId, password);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }); //User does not exists in our database

        if(!user.getEnabled()) {
            throw new DisabledException("Disabled user");
        }
        return createSpringSecurityUser(username, user);
    }

    public User createUserFromSamlResults(String username,  LinkedHashMap<String, List<Object>> samlAttributes,Map<String, String> attributeMap, String registrationId,String password) {
        String firstname;
        String lastname;
        String mail;
        try {
            firstname = (String)samlAttributes.get(attributeMap.get("firstname")).get(0);
            lastname = (String)samlAttributes.get(attributeMap.get("lastname")).get(0);
            mail = (String)samlAttributes.get(attributeMap.get("mail")).get(0);
        } catch(Exception e){
            log.error(e.getMessage());
            e.printStackTrace();
            throw e;
        }

        User user = new User();
        user.setUsername(username);
        user.setLastname(lastname);
        user.setFirstname(firstname);
        user.setEmail(mail);
        user.setEnabled(true);
        user.setPassword(password);
        user.setOrigin(registrationId);
        user.setLanguage(Language.valueOf(applicationProperties.getDefaultLanguage()));
        user.generateKeys();

        user = userRepository.save(user);

        String group_attribute=attributeMap.get("group");
        String group = (String)samlAttributes.get(group_attribute).get(0);
        SecRole userRole;
        if(group.equals(attributeMap.get("admin_role"))){
            userRole=secRoleRepository.getAdmin();
        }
        else if(group.equals(attributeMap.get("user_role"))){
            userRole = secRoleRepository.getUser();
        }else{
            userRole = secRoleRepository.getGuest();
        }

        SecUserSecRole secUsersecRole = new SecUserSecRole();
        secUsersecRole.setSecUser(user);
        secUsersecRole.setSecRole(userRole);

        entityManager.persist(secUsersecRole);
        entityManager.flush();
        entityManager.refresh(user);

        User finalUser = userRepository.findByUsernameLikeIgnoreCase(user.getUsername()).get();
        if(userRole.getAuthority().equals("ROLE_ADMIN")||userRole.getAuthority().equals("ROLE_USER")) {
            SecurityUtils.doWithAuth(applicationContext, "admin", () -> storageService.createStorage(finalUser));
        }
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
