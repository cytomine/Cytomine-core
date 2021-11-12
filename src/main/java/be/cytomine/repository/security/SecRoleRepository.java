package be.cytomine.repository.security;


import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
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
public interface SecRoleRepository extends JpaRepository<SecRole, Long>, JpaSpecificationExecutor<SecRole> {

    SecRole getByAuthority(String authority);

    Optional<SecRole> findByAuthority(String authority);

    default SecRole createIfNotExist(String authority) {
        return findByAuthority(authority).orElseGet(() -> {
            SecRole role = new SecRole();
            role.setAuthority(authority);
            return save(role);
        });
    }

}
