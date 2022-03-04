package be.cytomine.repository.ontology;


import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AnnotationLight;
import be.cytomine.service.UrlApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.Tuple;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public interface UserAnnotationRepository extends JpaRepository<UserAnnotation, Long>, JpaSpecificationExecutor<UserAnnotation>  {

    List<UserAnnotation> findAllByProjectAndUserIdIn(Project project, List<Long> layers);

    List<UserAnnotation> findAllByUserAndImage(User user, ImageInstance imageInstance);

    Long countByProject(Project project);

    Long countByUserAndProject(User user, Project project);

    Long countByUser(User user);

    Long countByProjectAndCreatedAfter(Project project, Date createdMin);

    Long countByProjectAndCreatedBefore(Project project, Date createdMax);

    Long countByProjectAndCreatedBetween(Project project, Date createdMin, Date createdMax);


    @Query(
            value = "SELECT a.id id, a.project_id container, '' url FROM user_annotation a, image_instance ii, abstract_image ai WHERE a.image_id = ii.id AND ii.base_image_id = ai.id AND ai.original_filename not like '%ndpi%svs%' AND GeometryType(a.location) != 'POINT' AND st_area(a.location) < 1500000 ORDER BY st_area(a.location) DESC",
            nativeQuery = true)
    List<Tuple> listTuplesLight();

    default List<AnnotationLight> listLight() {
        List<AnnotationLight> annotationLights = new ArrayList<>();
        for (Tuple tuple : listTuplesLight()) {
            annotationLights.add(new AnnotationLight(
                    ((BigInteger)tuple.get("id")).longValue(),
                    ((BigInteger)tuple.get("container")).longValue(),
                    UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize(((BigInteger)tuple.get("id")).longValue(), 256, "png"))
            );
        }
        return annotationLights;
    }


    @Query(value = "SELECT a.id id, ST_distance(a.location,ST_GeometryFromText(:geometry))  FROM user_annotation a WHERE project_id = :projectId", nativeQuery = true)
    List<Tuple> listAnnotationWithDistance(Long projectId, String geometry);


    List<UserAnnotation> findAllByImage(ImageInstance image);

    List<UserAnnotation> findAllByUser(User user);
}
