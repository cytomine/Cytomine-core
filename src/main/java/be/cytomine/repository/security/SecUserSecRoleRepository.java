package be.cytomine.repository.security;


import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.SecUserSecRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserSecRoleRepository extends JpaRepository<SecUserSecRole, Long>, JpaSpecificationExecutor<SecUserSecRole> {

    Optional<SecUser> findById(String id);

    @Query("select distinct s.secRole from SecUserSecRole s where s.secUser = ?1")
    Set<SecRole> findAllRoleBySecUser(SecUser user);

}
