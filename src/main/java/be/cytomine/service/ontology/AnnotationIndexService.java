package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ConstraintException;
import be.cytomine.repository.ontology.AnnotationIndexRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.dto.AnnotationIndexLightDTO;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.*;

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
