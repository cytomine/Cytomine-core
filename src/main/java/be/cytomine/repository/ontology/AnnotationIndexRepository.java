package be.cytomine.repository.ontology;

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationIndex;
import be.cytomine.domain.security.SecUser;
import be.cytomine.service.dto.AnnotationIndexLightDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnnotationIndexRepository extends JpaRepository<AnnotationIndex, Long>, JpaSpecificationExecutor<AnnotationIndex>  {

    @Query( value = "SELECT user_id as user, slice_id as slice ,count_annotation as countAnnotation,count_reviewed_annotation as countReviewedAnnotation " +
            " FROM annotation_index " +
            " WHERE slice_id = :slice", nativeQuery = true)
    List<AnnotationIndexLightDTO> findAllLightByImageInstance(long slice);


    Optional<AnnotationIndexLightDTO> findOneBySliceAndUser(SliceInstance slice, SecUser user);

    List<AnnotationIndexLightDTO> findAllBySlice(SliceInstance slice);

    void deleteAllBySlice(SliceInstance sliceInstance);
}
