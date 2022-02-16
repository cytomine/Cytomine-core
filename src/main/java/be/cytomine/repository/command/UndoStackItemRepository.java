package be.cytomine.repository.command;

import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.RedoStackItem;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.command.UndoStackItem;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UndoStackItemRepository extends JpaRepository<UndoStackItem, Long> {

   void deleteAllByCommand_Project(Project project);
}
