package be.cytomine.repository.processing;

import be.cytomine.domain.processing.ImageFilter;
import be.cytomine.domain.processing.ImagingServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImagingServerRepository extends JpaRepository<ImagingServer, Long>, JpaSpecificationExecutor<ImagingServer> {

    Optional<ImagingServer> findByUrl(String url);
}
