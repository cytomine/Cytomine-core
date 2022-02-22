package be.cytomine.repository.ontology;

import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import liquibase.repackaged.com.opencsv.bean.CsvToBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AnnotationTermRepository extends JpaRepository<AnnotationTerm, Long>, JpaSpecificationExecutor<AnnotationTerm>  {
    List<AnnotationTerm> findAllByUserAnnotation(UserAnnotation annotation);
    List<AnnotationTerm> findAllByUserAnnotationId(Long annotation);


    Optional<AnnotationTerm> findByUserAnnotationAndTermAndUser(UserAnnotation annotation, Term term, SecUser user);


    Optional<AnnotationTerm>  findByUserAnnotationIdAndTermIdAndUserId(Long annotation, Long term, Long user);

    List<AnnotationTerm> findAllByUserAndUserAnnotation(User user, UserAnnotation annotation);

    long countByTerm(Term term);

    List<AnnotationTerm> findAllByUserAnnotation_Project(Project project);

    List<AnnotationTerm> findAllByUser(User user);
}
