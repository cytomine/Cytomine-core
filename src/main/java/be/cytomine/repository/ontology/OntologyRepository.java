package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Ontology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OntologyRepository extends JpaRepository<Ontology, Long>, JpaSpecificationExecutor<Ontology>  {

    Optional<Ontology> findByName(String name);


}
