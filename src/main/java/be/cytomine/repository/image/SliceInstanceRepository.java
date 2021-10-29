package be.cytomine.repository.image;


import be.cytomine.domain.image.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the abstract image entity.
 */
@Repository
public interface SliceInstanceRepository extends JpaRepository<SliceInstance, Long>, JpaSpecificationExecutor<SliceInstance> {

    Optional<SliceInstance> findByBaseSliceAndImage(AbstractSlice abstractSlice, ImageInstance imageInstance);
}
