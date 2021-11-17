package be.cytomine.repository.security;


import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserRepository extends JpaRepository<SecUser, Long>, JpaSpecificationExecutor<User> {

    Optional<SecUser> findById(String id);

    Optional<SecUser> findByUsernameLikeIgnoreCase(String username);

    Optional<SecUser> findByPublicKey(String publicKey);

//    Optional<SecUser> findByPublicKeyAndEnabled(String publicKey, Boolean enabled);


    @Query("select distinct secUser " +
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, SecUser as secUser "+
            "where aclObjectId.objectId = :projectId " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id " +
            "and aclEntry.mask = 16 " +
            "and aclEntry.sid = aclSid.id " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.security.User'")
    List<SecUser> findAllAdminsByProjectId(Long projectId);

    @Query("select distinct secUser " +
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, User as secUser "+
            "where aclObjectId.objectId = :projectId " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id " +
            "and aclEntry.sid = aclSid.id " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.security.User'")
    List<SecUser> findAllUsersByProjectId(Long projectId);


}
