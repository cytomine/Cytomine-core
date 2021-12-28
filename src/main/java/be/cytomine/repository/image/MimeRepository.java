package be.cytomine.repository.image;

import be.cytomine.domain.image.Mime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MimeRepository extends JpaRepository<Mime, Long>, JpaSpecificationExecutor<Mime> {

    Optional<Mime> findByMimeType(String mimeType);
}
