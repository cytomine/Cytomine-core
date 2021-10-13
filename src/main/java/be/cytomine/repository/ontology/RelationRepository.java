package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RelationRepository extends JpaRepository<Relation, Long>, JpaSpecificationExecutor<Relation>  {

    Relation getByName(String name);

    default Relation getParent() {
        return getByName(RelationTerm.PARENT);
    }
}
