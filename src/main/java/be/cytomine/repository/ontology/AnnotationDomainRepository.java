package be.cytomine.repository.ontology;

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationIndex;
import be.cytomine.domain.security.SecUser;
import be.cytomine.service.dto.AnnotationIndexLightDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnnotationDomainRepository extends JpaRepository<AnnotationDomain, Long>, JpaSpecificationExecutor<AnnotationDomain>  {



}
