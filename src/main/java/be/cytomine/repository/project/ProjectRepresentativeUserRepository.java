package be.cytomine.repository.project;

import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepresentativeUserRepository extends JpaRepository<ProjectRepresentativeUser, Long>  {


    Optional<ProjectRepresentativeUser> findByProjectAndUser(Project project, User user);

    List<ProjectRepresentativeUser> findAllByProject(Project project);
}
