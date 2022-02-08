package be.cytomine.repositorynosql.social;


import be.cytomine.domain.social.AnnotationAction;
import be.cytomine.domain.social.PersistentConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface AnnotationActionRepository extends MongoRepository<AnnotationAction, Long> {

    Long countByProject(Long project);

    Long countByProjectAndCreatedAfter(Long project, Date createdMin);

    Long countByProjectAndCreatedBefore(Long project, Date createdMax);

    Long countByProjectAndCreatedBetween(Long project, Date createdMin, Date createdMax);

    void deleteAllByImage(Long id);
}
