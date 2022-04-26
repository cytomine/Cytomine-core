package be.cytomine.utils;

import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ServerException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ProjectLock {

    private Map<Long, ReentrantLock> projectLocks = new ConcurrentHashMap<>();
    private static ProjectLock projectLock = null;

    private ProjectLock() {

    }

    public static ProjectLock getInstance() {
        synchronized (ProjectLock.class) {
            if (projectLock==null) {
                projectLock = new ProjectLock();
            }
            return projectLock;
        }
    }

    public boolean lock(Project project) {
        try {
            projectLocks.putIfAbsent(project.getId(), new ReentrantLock());
            return projectLocks.get(project.getId()).tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ServerException("Cannot acquire lock on domain " + project, e);
        }
    }

    public void unlock(Project project) {
        projectLocks.get(project.getId()).unlock();
    }
}
