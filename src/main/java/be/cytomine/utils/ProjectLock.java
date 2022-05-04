package be.cytomine.utils;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
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
            log.debug("Try to lock project " + project.getId());
            projectLocks.putIfAbsent(project.getId(), new ReentrantLock());
            log.debug("Project {} current lock {}", project.getId(), projectLocks.get(project.getId()).isLocked());
            boolean result = projectLocks.get(project.getId()).tryLock(60, TimeUnit.SECONDS);
            log.debug("Project {} lock result {}", project.getId(), result);
            return result;
        } catch (InterruptedException e) {
            throw new ServerException("Cannot acquire lock on domain " + project, e);
        }
    }

    public void unlock(Project project) {
        log.debug("Unlock project " + project.getId());
        projectLocks.get(project.getId()).unlock();
    }
}
