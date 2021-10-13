package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.UserJob;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityACLService {


    private final EntityManager entityManager;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    private final AclRepository aclRepository;

    public void check(Long id, String className, Permission permission) {
        try {
            check(id, Class.forName(className),permission);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot check ACL for class " + className);
        }
    }

    public void check(Long id, Class className, Permission permission) {
        try {
            CytomineDomain domain = (CytomineDomain)entityManager.find(className, id);
            if (domain!=null) {
                check(domain,permission);
            } else {
                throw new ObjectNotFoundException("ACL error: " + className + " with id "+ id + " was not found! Unable to process auth checking");
            }
        } catch(IllegalArgumentException ex) {
            throw new ObjectNotFoundException("ACL error: " + className + " with id "+ id + " was not found! Unable to process auth checking");
        }

    }

//    public void check(Long id, Class className, String method, Permission permission) {
//        CytomineDomain domain = (CytomineDomain)entityManager.find(className, id);
//        if (domain!=null) {
//            def containerObject = simpleObject."$method"()
//            check(containerObject,permission)
//        } else {
//            throw new ObjectNotFoundException("ACL error: ${className} with id ${id} was not found! Unable to process auth checking")
//        }
//    }

    public void check(CytomineDomain domain, Permission permission) {
        if (domain!=null) {
            if (!checkPermission(domain.container(), permission, currentRoleService.isAdminByNow(currentUserService.getCurrentUser()))) {
                throw new ForbiddenException("You don't have the right to read or modify this resource! "  + domain.getClass() + " " + domain.getId());
            }
        } else {
            throw new ObjectNotFoundException("ACL error: domain is null! Unable to process project auth checking");
        }

    }

    /**
     * Check if user has permission on the curret domain
     * @param permission Permission to check (READ,...)
     * @return true if user has this permission on current domain
     */
    boolean checkPermission(CytomineDomain domain, Permission permission, boolean isAdmin) {
        boolean right = hasACLPermission(domain, permission) || isAdmin;
        return right;
    }

//    /**
//     * Check if user has ACL entry for this permission and this domain.
//     * IT DOESN'T CHECK IF CURRENT USER IS ADMIN
//     * @param permission Permission to check (READ,...)
//     * @return true if user has this permission on current domain
//     */
//    boolean hasACLPermission(Permission permission) {
//        try {
//            return hasACLPermission(this,permission);
//        } catch (Exception e) {}
//        return false;
//    }

    boolean hasACLPermission(CytomineDomain domain, Permission permission) {
        List<Integer> masks = getPermissionInACL(domain,currentUserService.getCurrentUsername());
        return masks.stream().max(Integer::compare).orElse(-1) >= permission.getMask();
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, User user) {
         return aclRepository.listMaskForUsers(domain.getId(), user.humanUsername());
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, String username) {
        return aclRepository.listMaskForUsers(domain.getId(), username);
    }


    public List<Storage> getStorageList(SecUser user, boolean adminByPass) {
        return getStorageList(user, adminByPass, null);
    }

    public List<Storage> getStorageList(SecUser user, boolean adminByPass, String searchString) {
        // adminByPass TODO
        // userjob.user TODO

        Query query = entityManager.createQuery(
                "select distinct storage "+
                        "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid,  Storage as storage "+
                        "where aclObjectId.objectId = storage.id " +
                        "and aclEntry.aclObjectIdentity = aclObjectId.id "+
                        "and aclEntry.sid = aclSid.id and aclSid.sid like '"+user.getUsername() +"'" + (StringUtils.isNotBlank(searchString)? " and lower(storage.name) like '%" + searchString.toLowerCase() + "%'" : ""));
        List<Storage> storages = query.getResultList();
        return storages;
    }

    public List<String> getProjectUsers(Project project) {
        // adminByPass TODO
        // userjob.user TODO

        Query query = entityManager.createQuery(
                "select distinct aclSid.sid "+
                        "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid,  Project as project "+
                        "where aclObjectId.objectId = project.id " +
                        "and aclEntry.aclObjectIdentity = aclObjectId.id "+
                        "and aclEntry.sid = aclSid.id and project.id = " + project.getId());
        List<String> usernames = query.getResultList();
        return usernames;
    }


    public void checkIsCurrentUserSameUser(SecUser user) {
        checkIsSameUser(user, currentUserService.getCurrentUser());
    }

    public void checkIsSameUser(SecUser user,SecUser currentUser) {
        boolean sameUser = (user.getId() == currentUser.getId());
        sameUser |= currentRoleService.isAdminByNow(currentUser);
        sameUser |= (currentUser instanceof UserJob && user.getId()==((UserJob)currentUser).getUser().getId());
        if (!sameUser) {
            throw new ForbiddenException("You don't have the right to read this resource! You must be the same user!");
        }
    }

    public void checkAdmin(SecUser user) {
        if (!currentRoleService.isAdminByNow(user)) {
            throw new ForbiddenException("You don't have the right to read this resource! You must be admin!");
        }
    }



}
