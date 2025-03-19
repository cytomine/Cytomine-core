package be.cytomine.repository.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserRepository extends JpaRepository<SecUser, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByUsernameLikeIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByPublicKey(String publicKey);

    @Query("select distinct secUser " +
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, SecUser as secUser "+
            "where aclObjectId.objectId = :projectId " +
            "and aclEntry.aclObjectIdentity = aclObjectId " +
            "and aclEntry.mask = 16 " +
            "and aclEntry.sid = aclSid " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.domain.security.User'")
    List<SecUser> findAllAdminsByProjectId(Long projectId);

    default List<SecUser> findAllUsersByProjectId(Long projectId) {
        return findAllUsersByContainer(projectId);
    }

    default List<SecUser> findAllUsersByStorageId(Long storageId) {
        return findAllUsersByContainer(storageId);
    }

    @Query("select distinct secUser " +
            "from  User as secUser, AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid "+
            "where aclObjectId.objectId = :containerId " +
            "and aclEntry.aclObjectIdentity = aclObjectId " +
            "and aclEntry.sid = aclSid " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.domain.security.User'")
    List<SecUser> findAllUsersByContainer(Long containerId);

    @Query(value = "SELECT DISTINCT sec_user.id \n" +
                " FROM acl_object_identity, acl_entry,acl_sid, sec_user \n" +
                " WHERE acl_object_identity.object_id_identity = :domainId\n" +
                " AND acl_entry.acl_object_identity=acl_object_identity.id\n" +
                " AND acl_entry.sid = acl_sid.id " +
                " AND acl_sid.sid = sec_user.username " +
                " AND sec_user.class = 'be.cytomine.domain.security.User'", nativeQuery = true)
    List<Long> findAllAllowedUserIdList(Long domainId);

    List<SecUser> findAllByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByPublicKeyAndEnabled(String accessKey, boolean enabled);

    @Query(value = "select distinct secUser " +
            "from AclSid as aclSid, AclEntry as aclEntry, SecUser as secUser "+
            "where aclEntry.aclObjectIdentity in (select  aclEntry.aclObjectIdentity from AclEntry as aclEntry where aclEntry.sid.id = :sidId) " +
            "and aclEntry.sid = aclSid and aclSid.sid = secUser.username and aclSid.id <> :sidId")
    List<SecUser> findAllSecUsersSharingAccesToSameProject(Long sidId);

    @Query(value = "SELECT id FROM acl_sid WHERE sid = :username", nativeQuery = true)
    Long getAclSidFromUsername(String username);

    default List<SecUser> findAllSecUsersSharingAccesToSameProject(String username) {
        Long aclId = getAclSidFromUsername(username);
        return findAllSecUsersSharingAccesToSameProject(aclId);
    }
}
