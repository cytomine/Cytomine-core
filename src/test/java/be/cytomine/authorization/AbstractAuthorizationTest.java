package be.cytomine.authorization;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.PermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static be.cytomine.BasicInstanceBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

@Transactional
public abstract class AbstractAuthorizationTest {



    public static final String SUPERADMIN = "SUPER_ADMIN_ACL";

    public static final String ADMIN = "ADMIN_ACL";

    public static final String GUEST = "GUEST_ACL";

    public static final String USER_ACL_READ = "USER_ACL_READ";

    public static final String USER_ACL_CREATE = "USER_ACL_CREATE";

    public static final String USER_ACL_WRITE = "USER_ACL_WRITE";

    public static final String USER_ACL_DELETE = "USER_ACL_DELETE";

    public static final String USER_ACL_ADMIN = "USER_ACL_ADMIN";

    public static final String USER_NO_ACL = "ACL_USER_NO_ACL";

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
        ROLES.put(GUEST, List.of(ROLE_GUEST));
    }

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected SecUserSecRoleRepository secUserSecRoleRepository;

    @Autowired
    protected SecRoleRepository secRoleRepository;

    @Autowired
    protected PermissionService permissionService;
//    protected final EntityManager entityManager;
//
//    protected final UserRepository userRepository;
//
//    protected final SecUserSecRoleRepository secUserSecRoleRepository;
//
//    protected final SecRoleRepository secRoleRepository;
//
//    protected final PermissionService permissionService;

    public SecUser given_a_user(String login) throws Exception {
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

        User user = BasicInstanceBuilder.given_a_user_not_persisted();
        user.setUsername(login);
        user.setEmail(login + "@test.com");

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

    protected void initUser() throws Exception {
        SecUser superadmin = given_a_user(SUPERADMIN);
        SecUser userWithRead = given_a_user(USER_ACL_READ);
        SecUser userWithWrite = given_a_user(USER_ACL_WRITE);
        SecUser userWithCreate = given_a_user(USER_ACL_CREATE);
        SecUser userWithDelete = given_a_user(USER_ACL_DELETE);
        SecUser userWithAdmin = given_a_user(USER_ACL_ADMIN);
        SecUser userNoAcl = given_a_user(USER_NO_ACL);
        SecUser userGuest = given_a_user(GUEST);
    }

    protected void initACL(CytomineDomain container) {
        permissionService.addPermission(container, USER_ACL_READ, BasePermission.READ);
        permissionService.addPermission(container, USER_ACL_WRITE, BasePermission.WRITE);
        permissionService.addPermission(container, USER_ACL_CREATE, BasePermission.CREATE);
        permissionService.addPermission(container, USER_ACL_DELETE, BasePermission.DELETE);
        permissionService.addPermission(container, USER_ACL_ADMIN, BasePermission.ADMINISTRATION);
        permissionService.addPermission(container, GUEST, BasePermission.READ);
    }

    protected void expectForbidden(Executable executable) {
        Assertions.assertThrows(ForbiddenException.class, executable) ;
    }

    protected void expectOK(Executable executable) {
        Assertions.assertDoesNotThrow(executable);
    }


}
