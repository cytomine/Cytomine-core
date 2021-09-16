package be.cytomine.repository.security;


import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserRepository extends JpaRepository<SecUser, Long>, JpaSpecificationExecutor<User> {

    Optional<SecUser> findById(String id);

    Optional<SecUser> findByUsernameLikeIgnoreCase(String username);

//    Optional<SecUser> findByEmailLikeIgnoreCase(String email);

//    Optional<SecUser> findByPublicKeyAndEnabled(String publicKey, Boolean enabled);

}
