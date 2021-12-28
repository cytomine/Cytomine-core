package be.cytomine.repository.middleware;

import be.cytomine.domain.middleware.ImageServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageServerRepository extends JpaRepository<ImageServer, Long>  {

    Optional<ImageServer> findByUrl(String url);
}
