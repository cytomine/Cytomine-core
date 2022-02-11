package be.cytomine.repository.ontology;


import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AnnotationLight;
import be.cytomine.dto.ReviewedAnnotationStatsEntry;
import be.cytomine.service.UrlApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.Tuple;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface ReviewedAnnotationRepository extends JpaRepository<ReviewedAnnotation, Long>, JpaSpecificationExecutor<ReviewedAnnotation>  {

    Long countByProject(Project project);

//    Long countByUserAndProject(User user, Project project);
//
    Long countByUser(User user);

    Long countByProjectAndCreatedAfter(Project project, Date createdMin);

    Long countByProjectAndCreatedBefore(Project project, Date createdMax);

    Long countByProjectAndCreatedBetween(Project project, Date createdMin, Date createdMax);


    @Query(
            value = "SELECT user_id, count(*), sum(count_reviewed_annotations) as total \n" +
                    "FROM user_annotation ua\n" +
                    "WHERE ua.image_id = :imageId\n" +
                    "GROUP BY user_id\n" +
                    "UNION\n" +
                    "SELECT user_id, count(*), sum(count_reviewed_annotations) as total \n" +
                    "FROM algo_annotation aa\n" +
                    "WHERE aa.image_id = :imageId\n" +
                    "GROUP BY user_id\n" +
                    "ORDER BY total desc;", nativeQuery = true
    )
    List<Tuple> stats(Long imageId);

    @Query(
            value = "SELECT count_reviewed_annotations as total \n" +
                    "FROM user_annotation ua\n" +
                    "WHERE ua.id = :userAnnotationId\n", nativeQuery = true
    )
    long countReviewedAnnotation(Long userAnnotationId);

    default List<ReviewedAnnotationStatsEntry> stats(ImageInstance imageInstance) {
        List<ReviewedAnnotationStatsEntry> reviewedAnnotationStatsEntries = new ArrayList<>();
        for (Tuple tuple : stats(imageInstance.getId())) {
            reviewedAnnotationStatsEntries.add(new ReviewedAnnotationStatsEntry(
                    ((BigInteger)tuple.get(0)).longValue(),
                    ((BigInteger)tuple.get(1)).longValue(),
                    ((BigInteger)tuple.get(2)).longValue())
            );
        }
        return reviewedAnnotationStatsEntries;
    }

    Optional<ReviewedAnnotation> findByParentIdent(Long parentIdent);

    List<ReviewedAnnotation> findAllByImage(ImageInstance image);

    long countAllByTermsContaining(Term term);

    long countAllByProjectAndTerms_Empty(Project project);
//
//    @Query(
//            value = "SELECT a.id id, a.project_id container, '' url FROM user_annotation a, image_instance ii, abstract_image ai WHERE a.image_id = ii.id AND ii.base_image_id = ai.id AND ai.original_filename not like '%ndpi%svs%' AND GeometryType(a.location) != 'POINT' AND st_area(a.location) < 1500000 ORDER BY st_area(a.location) DESC",
//            nativeQuery = true)
//    List<Tuple> listTuplesLight();
//
//    default List<AnnotationLight> listLight() {
//        List<AnnotationLight> annotationLights = new ArrayList<>();
//        for (Tuple tuple : listTuplesLight()) {
//            annotationLights.add(new AnnotationLight(
//                    ((BigInteger)tuple.get("id")).longValue(),
//                    ((BigInteger)tuple.get("container")).longValue(),
//                    UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize(((BigInteger)tuple.get("id")).longValue(), 256, "png"))
//            );
//        }
//        return annotationLights;
//    }
//
//
//    @Query(value = "SELECT a.id id, ST_distance(a.location,ST_GeometryFromText(:geometry))  FROM user_annotation a WHERE project_id = :projectId", nativeQuery = true)
//    List<Tuple> listAnnotationWithDistance(Long projectId, String geometry);


}
