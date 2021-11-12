package be.cytomine.repository.processing;

import be.cytomine.domain.image.Mime;
import be.cytomine.domain.processing.ImageFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageFilterRepository extends JpaRepository<ImageFilter, Long>, JpaSpecificationExecutor<ImageFilter> {

    Optional<ImageFilter> findByName(String name);
}
