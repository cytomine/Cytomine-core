package be.cytomine.repository.middleware;

import be.cytomine.domain.middleware.MessageBrokerServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageBrokerServerRepository extends JpaRepository<MessageBrokerServer, Long>  {

    Optional<MessageBrokerServer> findByName(String name);
}
