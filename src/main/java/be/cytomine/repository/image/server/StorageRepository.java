package be.cytomine.repository.image.server;


import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface StorageRepository extends JpaRepository<Storage, Long>, JpaSpecificationExecutor<Storage> {

    List<Storage> findAllByUser(SecUser user);

}
