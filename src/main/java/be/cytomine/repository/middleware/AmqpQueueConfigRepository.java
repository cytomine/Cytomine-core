package be.cytomine.repository.middleware;

import be.cytomine.domain.middleware.AmqpQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmqpQueueConfigRepository extends JpaRepository<AmqpQueueConfig, Long>  {

    Optional<AmqpQueueConfig> findByName(String name);
}
