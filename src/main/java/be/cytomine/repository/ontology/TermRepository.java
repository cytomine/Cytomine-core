package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long>, JpaSpecificationExecutor<Term>  {

    List<Term> findAllByOntology(Ontology ontology);

    @Query("SELECT term FROM Term as term WHERE term.ontology = :ontology AND term.id NOT IN (SELECT DISTINCT rel.term1.id FROM RelationTerm as rel, Term as t WHERE rel.relation = :relation AND t.ontology = :ontology AND t.id=rel.term1.id)")
    List<Term> findAllLeafTerms(Ontology ontology, Relation relation);

    @Query("SELECT term.id FROM Term as term WHERE term.ontology = :ontology")
    List<Long> listAllIds(Ontology ontology);

    Optional<Term> findByNameAndOntology(String name, Ontology ontology);
}
