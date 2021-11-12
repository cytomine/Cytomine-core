package be.cytomine.repository.middleware;

import be.cytomine.domain.middleware.AmqpQueue;
import be.cytomine.domain.middleware.AmqpQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmqpQueueRepository extends JpaRepository<AmqpQueue, Long>  {

    Optional<AmqpQueue> findByName(String name);
}
