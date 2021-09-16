package be.cytomine.service.security;

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityACLService {


    private final EntityManager entityManager;

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
}
