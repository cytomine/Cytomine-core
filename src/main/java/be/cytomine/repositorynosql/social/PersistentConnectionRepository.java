package be.cytomine.repositorynosql.social;


import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PersistentConnectionRepository extends MongoRepository<PersistentConnection, Long> {


}
