package be.cytomine.repository.security;


import be.cytomine.domain.security.AuthWithToken;
import be.cytomine.domain.security.ForgotPasswordToken;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface ForgotPasswordTokenRepository extends JpaRepository<ForgotPasswordToken, Long>, JpaSpecificationExecutor<ForgotPasswordToken> {

    Optional<ForgotPasswordToken> findByTokenKeyAndUser(String tokenKey, User user);
}
