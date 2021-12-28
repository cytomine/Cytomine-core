package be.cytomine.repository.processing;

import be.cytomine.domain.processing.SoftwareUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoftwareUserRepositoryRepository extends JpaRepository<SoftwareUserRepository, Long>  {


}
