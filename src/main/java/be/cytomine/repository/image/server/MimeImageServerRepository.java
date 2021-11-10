package be.cytomine.repository.image.server;


import be.cytomine.domain.image.server.MimeImageServer;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface MimeImageServerRepository extends JpaRepository<MimeImageServer, Long>, JpaSpecificationExecutor<MimeImageServer> {



}
