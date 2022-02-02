package be.cytomine.service.social;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.domain.social.PersistentUserPosition;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repositorynosql.social.*;
import be.cytomine.service.AnnotationListingService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.dto.AreaDTO;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.domain.social.PersistentUserPosition.getJtsPolygon;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class UserPositionService {

    public static final String DATABASE_NAME = "cytomine";
    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    ProjectConnectionRepository projectConnectionRepository;

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    PersistentProjectConnectionRepository persistentProjectConnectionRepository;

    @Autowired
    AnnotationListingService annotationListingService;

    @Autowired
    LastConnectionRepository lastConnectionRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    SequenceService sequenceService;

//
//    public LastUserPosition add(SecUser user, SliceInstance sliceInstance) {
//
//    }
//
//    public LastUserPosition add(SecUser user, ImageInstance imageInstance) {
//
//    }

    public PersistentUserPosition add(
            Date created,
            SecUser user,
            SliceInstance sliceInstance,
            ImageInstance imageInstance,
            AreaDTO area,
            Integer zoom,
            Double rotation,
            Boolean broadcast) {

        LastUserPosition position = new LastUserPosition();
        position.setId(sequenceService.generateID());
        position.setUser(user.getId());
        position.setImage(imageInstance.getId());
        position.setSlice(sliceInstance.getId());
        position.setProject(imageInstance.getProject().getId());
        position.setLocation(area.toMongodbLocation().getCoordinates());
        position.setZoom(zoom);
        position.setRotation(rotation);
        position.setBroadcast(broadcast);
        position.setCreated(created);
        position.setUpdated(created);
        position.setImageName(imageInstance.getBlindInstanceFilename());
        lastUserPositionRepository.insert(position);

        PersistentUserPosition persistedPosition = new PersistentUserPosition();
        persistedPosition.setId(sequenceService.generateID());
        persistedPosition.setUser(user.getId());
        persistedPosition.setImage(imageInstance.getId());
        persistedPosition.setSlice(sliceInstance.getId());
        persistedPosition.setProject(imageInstance.getProject().getId());
        persistedPosition.setLocation(area.toMongodbLocation().getCoordinates());
        persistedPosition.setZoom(zoom);
        persistedPosition.setRotation(rotation);
        persistedPosition.setBroadcast(broadcast);
        persistedPosition.setCreated(created);
        persistedPosition.setUpdated(created);
        persistedPosition.setImageName(imageInstance.getBlindInstanceFilename());
        persistentUserPositionRepository.insert(persistedPosition);

        return persistedPosition;
    }

    public Optional<LastUserPosition> lastPositionByUser(ImageInstance image, SliceInstance slice, SecUser user, boolean broadcast) {
        securityACLService.check(image,READ);

        Query query = new Query();
        query.addCriteria(Criteria.where("user").is(user.getId()));
        query.addCriteria(Criteria.where("image").is(image.getId()));
        if (slice!=null) {
            query.addCriteria(Criteria.where("slice").is(slice.getId()));
        }
        if (broadcast) {
            query.addCriteria(Criteria.where("broadcast").is(true));
        }
        query.with(Sort.by(Sort.Direction.DESC, "created"));
        query.limit(1);

        List<LastUserPosition> lastUserPositions = mongoTemplate.find(query, LastUserPosition.class);
        return lastUserPositions.stream().findFirst();
    }

    public List<Long> listOnlineUsersByImage(ImageInstance image, SliceInstance slice, boolean broadcast) {
        securityACLService.check(image,READ);
        Date thirtySecondsAgo = DateUtils.addSeconds(new Date(), -30);

        List<Bson> request = new ArrayList<>();
        request.add(match(eq("image", image.getId())));
        request.add(match(gte("created", thirtySecondsAgo)));
        if (broadcast) {
            request.add(match(eq("broadcast", true)));
        }
        if (slice!=null) {
            request.add(match(eq("slice", slice.getId())));
        }

        request.add(project(Projections.fields(Projections.computed("user", "$user"))));
        request.add(group("$user"));


        MongoCollection<Document> persistentProjectConnection = mongoClient.getDatabase(DATABASE_NAME).getCollection("lastUserPosition");

        List<Document> results = persistentProjectConnection.aggregate(request)
                .into(new ArrayList<>());

        return results.stream().map(x -> x.getLong("_id")).collect(Collectors.toList());
    }

    List<PersistentUserPosition> list(ImageInstance image, User user, SliceInstance slice, Long afterThan, Long beforeThan, int max, int offset){
        securityACLService.check(image,WRITE);

        Query query = new Query();
        if (user!=null) {
            query.addCriteria(Criteria.where("user").is(user.getId()));
        }
        if (image!=null) {
            query.addCriteria(Criteria.where("image").is(image.getId()));
        }
        if (slice!=null) {
            query.addCriteria(Criteria.where("slice").is(slice.getId()));
        }
        if (afterThan!=null && beforeThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)).lte(new Date(beforeThan)));
        } else if (afterThan!=null) {
            query.addCriteria(Criteria.where("created").gte(new Date(afterThan)));
        } else if (beforeThan!=null) {
            query.addCriteria(Criteria.where("created").lte(new Date(beforeThan)));
        }
        query.with(Sort.by(Sort.Direction.DESC, "created"));
        query.limit(max);
        query.skip(offset);

        List<PersistentUserPosition> lastUserPositions = mongoTemplate.find(query, PersistentUserPosition.class);
        return lastUserPositions;

    }

    public List<Map<String, Object>> summarize(ImageInstance image, User user, SliceInstance slice, Long afterThan, Long beforeThan) {
        securityACLService.check(image, WRITE);

        List<Bson> request = new ArrayList<>();
        request.add(match(eq("image", image.getId())));
        if (afterThan != null) {
            request.add(match(gte("created", new Date(afterThan))));
        }
        if (beforeThan != null) {
            request.add(match(lte("created", new Date(beforeThan))));
        }
        if (user != null) {
            request.add(match(eq("user", user.getId())));
        }
        if (slice != null) {
            request.add(match(eq("slice", slice.getId())));
        }



        request.add(group(Document.parse("{location: '$location', zoom: '$zoom', rotation: '$rotation'}"),
                Accumulators.sum("frequency", 1), Accumulators.first("image", "$image")));


        MongoCollection<Document> persistentUserPosition = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentUserPosition");

        List<Document> results = persistentUserPosition.aggregate(request)
                .into(new ArrayList<>());

        return results.stream().map(x ->
                JsonObject.of("location", ((Document) x.get("_id")).get("location"),
                        "zoom", ((Document) x.get("_id")).get("zoom"),
                        "rotation", ((Document) x.get("_id")).get("rotation"),
                        "rotation", ((Document) x.get("_id")).get("rotation"),
                        "frequency", x.get("frequency"),
                        "image", x.get("image"))).collect(Collectors.toList());

    }
}
