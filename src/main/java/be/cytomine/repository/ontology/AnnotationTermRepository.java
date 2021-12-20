package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.*;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnnotationTermRepository extends JpaRepository<AnnotationTerm, Long>, JpaSpecificationExecutor<AnnotationTerm>  {
    List<AnnotationTerm> findAllByUserAnnotation(UserAnnotation annotation);
    List<AnnotationTerm> findAllByUserAnnotationId(Long annotation);


    Optional<AnnotationTerm> findByUserAnnotationAndTermAndUser(UserAnnotation annotation, Term term, SecUser user);


    Optional<AnnotationTerm>  findByUserAnnotationIdAndTermIdAndUserId(Long annotation, Long term, Long user);

}
