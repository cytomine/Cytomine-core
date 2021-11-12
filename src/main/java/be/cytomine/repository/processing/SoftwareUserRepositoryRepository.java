package be.cytomine.repository.processing;

import be.cytomine.domain.processing.ProcessingServer;
import be.cytomine.domain.processing.SoftwareUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SoftwareUserRepositoryRepository extends JpaRepository<SoftwareUserRepository, Long>  {


}
