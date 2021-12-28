package be.cytomine.repository.image.server;


import be.cytomine.domain.image.server.MimeImageServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface MimeImageServerRepository extends JpaRepository<MimeImageServer, Long>, JpaSpecificationExecutor<MimeImageServer> {



}
