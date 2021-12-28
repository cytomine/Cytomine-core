package be.cytomine.service.ontology;

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.SecUser;
import be.cytomine.repository.ontology.AnnotationIndexRepository;
import be.cytomine.service.dto.AnnotationIndexLightDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnnotationIndexService {

    private final AnnotationIndexRepository annotationIndexRepository;

    public List<AnnotationIndexLightDTO> list(SliceInstance sliceInstance) {
        return annotationIndexRepository.findAllLightByImageInstance(sliceInstance.getId());
    }

    /**
     * Return the number of annotation created by this user for this slice
     * If user is null, return the number of reviewed annotation for this slice
     */
    public Long count(SliceInstance slice, SecUser user) {
        if (user!=null) {
            return annotationIndexRepository.findOneBySliceAndUser(slice, user)
                    .map(AnnotationIndexLightDTO::getCountAnnotation).orElse(0L);
        } else {
            return annotationIndexRepository.findAllBySlice(slice)
                    .stream().mapToLong(AnnotationIndexLightDTO::getCountReviewedAnnotation).sum();
        }
    }
}
