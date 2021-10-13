package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface RelationTermRepository extends JpaRepository<RelationTerm, Long>, JpaSpecificationExecutor<Relation>  {

    Optional<RelationTerm> findByRelationAndTerm1AndTerm2(Relation relation, Term term1, Term term2);

    @Query("SELECT rt FROM RelationTerm rt WHERE rt.relation.id = :relation AND rt.term1.id = :term1 AND rt.term2.id = :term2")
    Optional<RelationTerm> findByRelationAndTerm1AndTerm2(Long relation, Long term1, Long term2);

    List<RelationTerm> findAllByTerm1(Term term);
    List<RelationTerm> findAllByTerm2(Term term);

    @Query("SELECT rt FROM RelationTerm rt WHERE rt.term1 = :term OR rt.term2 = :term")
    List<RelationTerm> findAllByTerm(Term term);


}
