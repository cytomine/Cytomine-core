package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface OntologyRepository extends JpaRepository<Ontology, Long>, JpaSpecificationExecutor<Ontology>  {

    Optional<Ontology> findByName(String name);


    List<Ontology> findAllByUser(User user);
}
