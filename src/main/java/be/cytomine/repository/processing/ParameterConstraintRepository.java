package be.cytomine.repository.processing;

import be.cytomine.domain.processing.ParameterConstraint;
import be.cytomine.domain.processing.ProcessingServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParameterConstraintRepository extends JpaRepository<ParameterConstraint, Long>  {

    Optional<ParameterConstraint> findByNameAndDataType(String name, String dataType);

}
