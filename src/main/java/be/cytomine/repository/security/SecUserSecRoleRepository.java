package be.cytomine.repository.security;


import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserSecRoleRepository extends JpaRepository<SecUser, Long>, JpaSpecificationExecutor<User> {

    Optional<SecUser> findById(String id);

    @Query("select distinct s.secRole from SecUserSecRole s where s.secUser = ?1")
    Set<SecRole> findAllRoleBySecUser(SecUser user);

}
