package be.cytomine.repository.middleware;

import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.NamedCytomineDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageServerRepository extends JpaRepository<ImageServer, Long>  {


}
