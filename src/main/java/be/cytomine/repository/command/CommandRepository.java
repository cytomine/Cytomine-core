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
public interface CommandRepository extends JpaRepository<Command, Long> {

    @Query("SELECT usi FROM UndoStackItem usi WHERE usi.transaction = :transaction AND usi.user = :user ORDER BY usi.created DESC")
    List<UndoStackItem> findAllUndoOrderByCreatedDesc(SecUser user, Transaction transaction);

    @Query("SELECT rsi FROM RedoStackItem rsi WHERE rsi.transaction = :transaction AND rsi.user = :user ORDER BY rsi.created DESC")
    List<RedoStackItem> findAllRedoOrderByCreatedDesc(SecUser user, Transaction transaction);


    @Query("SELECT usi FROM UndoStackItem usi WHERE usi.command = :command AND usi.user = :user ORDER BY usi.created DESC")
    Page<UndoStackItem> findLastUndoStackItems(SecUser user, Command command, Pageable pageable);

    default Optional<UndoStackItem> findLastUndoStackItem(SecUser user, Command command) {
        return findLastUndoStackItems(user, command, PageRequest.of(0,1)).stream().findFirst();
    }

    @Query("SELECT usi FROM UndoStackItem usi WHERE usi.user = :user ORDER BY usi.created DESC")
    Page<UndoStackItem> findLastUndoStackItems(SecUser user, Pageable pageable);

    default Optional<UndoStackItem> findLastUndoStackItem(SecUser user) {
        return findLastUndoStackItems(user, PageRequest.of(0,1)).stream().findFirst();
    }

    @Query("SELECT usi FROM RedoStackItem usi WHERE usi.command = :command AND usi.user = :user ORDER BY usi.created DESC")
    Page<RedoStackItem> findLastRedoStackItems(SecUser user, Command command, Pageable pageable);

    default Optional<RedoStackItem> findLastRedoStackItem(SecUser user, Command command) {
        return findLastRedoStackItems(user, command, PageRequest.of(0,1)).stream().findFirst();
    }

    @Query("SELECT usi FROM RedoStackItem usi WHERE usi.user = :user ORDER BY usi.created DESC")
    Page<RedoStackItem> findLastRedoStackItems(SecUser user, Pageable pageable);

    default Optional<RedoStackItem> findLastRedoStackItem(SecUser user) {
        return findLastRedoStackItems(user, PageRequest.of(0,1)).stream().findFirst();
    }


    void deleteAllByProject(Project project);
}
