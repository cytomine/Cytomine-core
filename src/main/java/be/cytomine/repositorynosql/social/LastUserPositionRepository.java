package be.cytomine.repositorynosql.social;


import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.domain.social.PersistentUserPosition;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface LastUserPositionRepository extends MongoRepository<LastUserPosition, Long> {

    void deleteAllByImage(Long id);

}
