package be.cytomine.repository.command;

import be.cytomine.domain.command.RedoStackItem;
import be.cytomine.domain.command.UndoStackItem;
import be.cytomine.domain.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedoStackItemRepository extends JpaRepository<RedoStackItem, Long> {

   void deleteAllByCommand_Project(Project project);
}
