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
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findById(String id);

    Optional<User> findByUsernameLikeIgnoreCase(String username);

    Optional<User> findByEmailLikeIgnoreCase(String email);

    Optional<User> findByPublicKeyAndEnabled(String publicKey, Boolean enabled);

}
