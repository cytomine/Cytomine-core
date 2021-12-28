package be.cytomine.repository.middleware;

import be.cytomine.domain.meta.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long>  {


}
