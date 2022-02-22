package be.cytomine.repository.project;

import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectDefaultLayer;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDefaultLayerRepository extends JpaRepository<ProjectDefaultLayer, Long>  {


    Optional<ProjectDefaultLayer> findByProjectAndUser(Project project, User user);

    List<ProjectDefaultLayer> findAllByProject(Project project);

    List<ProjectDefaultLayer> findAllByUser(SecUser user);
}
