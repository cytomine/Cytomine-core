package be.cytomine.repository.processing;

import be.cytomine.domain.processing.ProcessingServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessingServerRepository extends JpaRepository<ProcessingServer, Long>  {

    Optional<ProcessingServer> findByName(String name);

}
