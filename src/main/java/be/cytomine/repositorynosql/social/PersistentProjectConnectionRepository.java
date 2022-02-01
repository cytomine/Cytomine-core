package be.cytomine.repositorynosql.social;


import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.PersistentProjectConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface PersistentProjectConnectionRepository extends MongoRepository<PersistentProjectConnection, Long> {

    List<PersistentProjectConnection> findAllByUserAndProjectAndCreatedLessThan(Long user, Long project, Date created, PageRequest pageRequest);

    
    @Aggregation(pipeline = {"{$match: {project: ?0, user: ?1, $and : [{created: {$gte: ?3}},{created: {$lte: ?2}}]}},{$sort: {created: 1}},{$project: {dateInMillis: {$subtract: {'$created', ?4}}}}"})
    AggregationResults test(Long project, Long user, Date before, Date after, Date firstDate);


    //        def match = [project : project.id]
//        def sp = searchParameters.find{it.operator.equals("in") && it.property.equals("user")}
//        if(sp) match << [user : [$in :sp.value]]
//
//        def aggregation = [
//                [$match:match],
//                [$sort : ["$sortProperty": sortDirection.equals("desc") ? -1 : 1]],
//                [$group : [_id : '$user', created : [$max :'$created']]],
//                [$skip : offset]
//        ]

//    @Aggregation(pipeline = {"{$match: {project: ?0, user: {$in: ?1}},{$sort: {?2: ?3}},{$group: {_id : '$user', created : {$max :'$created'}}, {$skip: ?4}}"})
//    AggregationResults retrieve(Long project, List<Long> users, String sortProperty, Integer sortDirection, Integer offset);
//
//    @Aggregation(pipeline = {"{$match: {project: ?0}},{$sort: {?2: ?3}},{$group: {_id : '$user', created : {$max :'$created'}}, {$skip: ?4}}"})
//    AggregationResults retrieve(Long project, String sortProperty, Integer sortDirection, Integer offset);

    List<PersistentProjectConnection> findAllByUserAndProject(Long user, Long project, PageRequest pageRequest);

   @Aggregation(pipeline = {"{$match: {project: ?0}},{$sort: {?1: ?2}},{$group: {_id : '$user', created : {$max :'$created'}}}"})
   AggregationResults retrieve(Long project, String sortProperty, Integer sortDirection);


    Long countAllByProjectAndUser(Long project, Long user);

    @Aggregation(pipeline = {"{$group: {_id : '$project', total : {$sum :1}}}"})
    AggregationResults countConnectionByProject();


    Long countByProject(Long project);

    Long countByProjectAndCreatedAfter(Long project, Date createdMin);

    Long countByProjectAndCreatedBefore(Long project, Date createdMax);

    Long countByProjectAndCreatedBetween(Long project, Date createdMin, Date createdMax);

}
