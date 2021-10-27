package be.cytomine.repository.image;


import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.UploadedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the abstract image entity.
 */
@Repository
public interface AbstractImageRepository extends JpaRepository<AbstractImage, Long>, JpaSpecificationExecutor<AbstractImage> {

    List<AbstractImage> findAllByUploadedFile(UploadedFile uploadedFile);

}
