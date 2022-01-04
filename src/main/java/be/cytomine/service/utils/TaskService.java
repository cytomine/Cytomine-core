package be.cytomine.service.utils;

import be.cytomine.repository.utils.TaskRepository;
import be.cytomine.utils.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class TaskService {

    @Autowired
    TaskRepository taskRepository;

    public Task get(Long id) {
        if (id == null) {
            return null;
        }
        return taskRepository.getTaskById(id);
    }

    public void updateTask(Task task, String comment) {
        //TODO
    }

    public void updateTask(Task task, int progress, String comment) {
        //TODO
    }

    public void finishTask(Task task) {
    }
}
