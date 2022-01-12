package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.SharedAnnotation;
import be.cytomine.domain.ontology.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SharedAnnotationRepository extends JpaRepository<SharedAnnotation, Long>, JpaSpecificationExecutor<SharedAnnotation>  {

    List<SharedAnnotation> findAllByAnnotationIdentOrderByCreatedDesc(Long annotationId);
}
