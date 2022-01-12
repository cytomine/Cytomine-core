package be.cytomine.repository.ontology;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.Track;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import liquibase.repackaged.com.opencsv.bean.CsvToBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track>  {

    List<Track> findAllByImage(ImageInstance imageInstance);

    List<Track> findAllByProject(Project project);

    Long countByProject(Project project);

    Long countByProjectAndCreatedAfter(Project project, Date createdMin);

    Long countByProjectAndCreatedBefore(Project project, Date createdMax);

    Long countByProjectAndCreatedBetween(Project project, Date createdMin, Date createdMax);

    Optional<Track> findByNameAndImage(String name, ImageInstance imageInstance);
}
