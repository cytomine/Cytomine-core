package be.cytomine.repository.utils;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.NamedCytomineDomain;
import be.cytomine.utils.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Project, Long>  {

    @Query(value = "SELECT id,progress,project_id,user_id FROM task where id = :id", nativeQuery = true)
    Task getTaskById(Long id);


}
