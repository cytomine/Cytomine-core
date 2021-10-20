package be.cytomine.service;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.UserJob;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final EntityManager entityManager;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    private final AclRepository aclRepository;

    public boolean hasACLPermission(CytomineDomain domain, String username, Permission permission) {
        List<Integer> masks = getPermissionInACL(domain,username);
        return masks.stream().max(Integer::compare).orElse(-1) >= permission.getMask();
    }

    public boolean hasACLPermission(CytomineDomain domain, Permission permission) {
        return hasACLPermission(domain, currentUserService.getCurrentUsername(), permission);
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, User user) {
        return aclRepository.listMaskForUsers(domain.getId(), user.humanUsername());
    }

    List<Integer> getPermissionInACL(CytomineDomain domain, String username) {
        return aclRepository.listMaskForUsers(domain.getId(), username);
    }


    public void deletePermission(CytomineDomain domain, String username, Permission permission) {
        if (hasACLPermission(domain, username, permission)) {
            log.info("Delete permission for {}, {}, {}", username, permission.getMask(), domain.getId());
            Long aclObjectIdentity = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
            int mask = permission.getMask();
            Long sid = aclRepository.getAclSid(username);

            if(aclObjectIdentity==null || sid==null) {
                throw new ObjectNotFoundException("User " + username + " or Object " + domain.getId() + " are not in ACL");
            }
            aclRepository.deleteAclEntry(aclObjectIdentity, mask, sid);
        }
    }

    /**
     * Add Permission right
     * @param domain
     * @param username
     * @param permission
     */
    public void addPermission(CytomineDomain domain, String username, int permission) {
        addPermission(domain, username, readFromMask(permission));
    }

    public void addPermission(CytomineDomain domain, String username, Permission permission) {
        addPermission(domain,username,permission,currentUserService.getCurrentUser());
    }

    public void addPermission(CytomineDomain domain, String username, Permission permission, SecUser user) {
        if (!hasACLPermission(domain, username, permission)) {
            //get domain class id
            Long aclClassId = getAclClassId(domain);

            //get acl sid for current user (run request)
            Long sidCurrentUser = getAclSid(user.getUsername());

            //get acl object id
            Long aclObjectIdentity = getAclObjectIdentity(domain, aclClassId, sidCurrentUser);

            //get acl sid for the user
            Long sid = getAclSid(username);

            //get acl entry
            createAclEntry(aclObjectIdentity, sid, permission.getMask());
        }
    }




    public Long createAclEntry(Long aoi, Long sid, Integer mask) {
        Long aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
        if (aclEntryId == null) {
            Integer max = aclRepository.getMaxAceOrder(aoi);
            if(max==null) {
                max=0;
            } else {
                max = max+1;
            }
            aclRepository.insertAclEntry(max, aoi, mask, sid);
            aclEntryId = aclRepository.getAclEntryId(aoi, sid, mask);
        }
        return aclEntryId;
    }

    public Long getAclObjectIdentity(CytomineDomain domain, Long aclClassId, Long aclSidId) {
        Long aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
        if (aclObjectIdentityId == null) {
            aclRepository.insertAclObjectIdentity(aclClassId, domain.getId(), aclSidId);
            aclObjectIdentityId = aclRepository.getAclObjectIdentityFromDomainId(domain.getId());
        }
        return aclObjectIdentityId;
    }

    public Long getAclSid(String username) {
        Long id = aclRepository.getAclSidFromUsername(username);
        if (id == null) {
            aclRepository.insertAclSid(username);
            id = aclRepository.getAclSidFromUsername(username);
        }
        return id;
    }

    public Long getAclClassId(CytomineDomain domain) {
        Long id = aclRepository.getAclClassId(domain.getClass().getName());
        if (id == null) {
            aclRepository.insertAclClassId(domain.getClass().getName());
            id = aclRepository.getAclClassId(domain.getClass().getName());
        }
        return id;
    }

    Permission readFromMask(int mask) {
        switch (mask) {
            case 1:
                return BasePermission.READ;
            case 2:
                return BasePermission.WRITE;
            case 4:
                return BasePermission.CREATE;
            case 8:
                return BasePermission.DELETE;
            case 16:
                return BasePermission.ADMINISTRATION;
        }
        throw new RuntimeException("Mask " + mask + " not supported");
    }
}
