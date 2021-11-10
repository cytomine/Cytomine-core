package be.cytomine.repository.image;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.Mime;
import be.cytomine.domain.image.NestedImageInstance;
import be.cytomine.domain.project.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MimeRepository extends JpaRepository<Mime, Long>, JpaSpecificationExecutor<Mime> {

    Optional<Mime> findByMimeType(String mimeType);
}
