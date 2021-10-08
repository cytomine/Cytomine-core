package be.cytomine.repository.security;

import be.cytomine.domain.security.SecUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AclRepository extends JpaRepository<SecUser, Long> {

    @Query(value = "SELECT mask FROM acl_object_identity aoi, acl_sid sid, acl_entry ae " +
            "WHERE aoi.object_id_identity = :domainId " +
            "AND sid.sid = :humanUsername "  +
            "AND ae.acl_object_identity = aoi.id "+
            "AND ae.sid = sid.id ", nativeQuery = true)
    List<Integer> listMaskForUsers(Long domainId, String humanUsername);

    @Query(value = "SELECT mask FROM acl_object_identity aoi, acl_sid sid, acl_entry ae " +
            "WHERE aoi.object_id_identity = :domainId " +
            "AND sid.sid = :humanUsername "  +
            "AND ae.acl_object_identity = aoi.id "+
            "AND ae.sid = sid.id ", nativeQuery = true)
    List<Integer> listMaskForUsers(Long domainId);
}
