package be.cytomine.service.database;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.domain.image.Mime;
import be.cytomine.domain.image.server.MimeImageServer;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.processing.ImageFilter;
import be.cytomine.domain.processing.ImagingServer;
import be.cytomine.domain.processing.ParameterConstraint;
import be.cytomine.domain.processing.SoftwareUserRepository;
import be.cytomine.domain.security.*;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.MimeRepository;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.middleware.AmqpQueueRepository;
import be.cytomine.repository.middleware.ImageServerRepository;
import be.cytomine.repository.middleware.MessageBrokerServerRepository;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.processing.ImageFilterRepository;
import be.cytomine.repository.processing.ImagingServerRepository;
import be.cytomine.repository.processing.ParameterConstraintRepository;
import be.cytomine.repository.processing.ProcessingServerRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.amqp.AmqpQueueConfigService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.*;

import static be.cytomine.repository.security.SecRoleRepository.*;

@Service
@Slf4j
public class BootstrapTestsDataService {

    public static final String SUPERADMIN = "SUPER_ADMIN_ACL";

    public static final String ADMIN = "ADMIN_ACL";

    public static final String GUEST = "GUEST_ACL";

    public static final String USER_ACL_READ = "USER_ACL_READ";

    public static final String USER_ACL_CREATE = "USER_ACL_CREATE";

    public static final String USER_ACL_WRITE = "USER_ACL_WRITE";

    public static final String USER_ACL_DELETE = "USER_ACL_DELETE";

    public static final String USER_ACL_ADMIN = "USER_ACL_ADMIN";

    public static final String USER_NO_ACL = "ACL_USER_NO_ACL";

    public static final String CREATOR = "CREATOR";

    @Autowired
    UserRepository userRepository;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    SecUserSecRoleRepository secUserSecRoleRepository;

    public static final Map<String, List<String>> ROLES = new HashMap<>();

    static {
        ROLES.put(SUPERADMIN, List.of(ROLE_SUPER_ADMIN));
        ROLES.put(ADMIN, List.of(ROLE_ADMIN));
        ROLES.put(USER_NO_ACL, List.of(ROLE_USER));
        ROLES.put(USER_ACL_READ, List.of(ROLE_USER));
        ROLES.put(USER_ACL_WRITE, List.of(ROLE_USER));
        ROLES.put(USER_ACL_CREATE, List.of(ROLE_USER));
        ROLES.put(USER_ACL_DELETE, List.of(ROLE_USER));
        ROLES.put(USER_ACL_ADMIN, List.of(ROLE_USER));
        ROLES.put(CREATOR, List.of(ROLE_USER));
        ROLES.put(GUEST, List.of(ROLE_GUEST));
    }


    public SecUser createUserForTests(String login) {
        Optional<User> alreadyExistingUser = userRepository.findByUsernameLikeIgnoreCase(login.toLowerCase());
        if (!ROLES.containsKey(login)) {
            throw new RuntimeException("Cannot execute test because user has not authority defined");
        }
        List<String> authoritiesConstants = ROLES.get(login);

        if (alreadyExistingUser.isPresent()) {
            Set<SecRole> allRoleBySecUser = secUserSecRoleRepository.findAllRoleBySecUser(alreadyExistingUser.get());
            for (String authoritiesConstant : authoritiesConstants) {
                if (!allRoleBySecUser.stream().anyMatch(x -> x.getAuthority().equals(authoritiesConstant))) {
                    throw new RuntimeException("Cannot execute test because already existing user " + login + "  has not same roles: not present - " + authoritiesConstant);
                }
            }
            for (SecRole secRole : allRoleBySecUser) {
                if (!authoritiesConstants.stream().anyMatch(x -> x.equals(secRole.getAuthority()))) {
                    throw new RuntimeException("Cannot execute test because already existing user " + login + " has not same roles: should not be there - " + secRole.getAuthority());
                }
            }
            return alreadyExistingUser.get();
        }

        User user = new User();
        user.setUsername(login);
        user.setEmail(login + "@test.com");
        user.setFirstname("firstname");
        user.setLastname("lastname");
        user.setPublicKey(UUID.randomUUID().toString());
        user.setPrivateKey(UUID.randomUUID().toString());
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("unkown");

        user = userRepository.save(user);
        userRepository.findById(user.getId()); // flush

        for (String authority : authoritiesConstants) {
            SecRole secRole = secRoleRepository.getByAuthority(authority);
            SecUserSecRole secUserSecRole = new SecUserSecRole();
            secUserSecRole.setSecUser(user);
            secUserSecRole.setSecRole(secRole);
            secUserSecRoleRepository.save(secUserSecRole);
        }
        userRepository.findById(user.getId()); // flush
        return user;
    }
}
