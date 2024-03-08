package be.cytomine.repository.appengine;

import be.cytomine.domain.appengine.TaskRun;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TaskRunRepository extends JpaRepository<TaskRun, Long>, JpaSpecificationExecutor<TaskRun> {
    Optional<TaskRun> findTaskRunByProjectIdAndTaskRunId(Long projectId,  UUID taskRunId);
}
