package be.cytomine.repository.ontology;

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OntologyRepository extends JpaRepository<Ontology, Long>, JpaSpecificationExecutor<Ontology>  {


}
