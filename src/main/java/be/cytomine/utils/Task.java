package be.cytomine.utils;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * A task provide info about a command.
 * The main info is the progress status
 * THIS CLASS CANNOT BE A DOMAIN! Because it cannot works with hibernate transaction.
 */
@Getter
@Setter
public class Task {

    private Long id;

    /**
     * Request progress between 0 and 100
     */
    private int progress = 0;

    /**
     * Project updated by the command task
     */
    private Long projectIdent = -1L;

    /**
     * User that ask the task
     */
    private Long userIdent;

    private boolean printInActivity = false;

//
//    def getMap(taskService) {
//        def map = [:]
//        map.id = id
//        map.progress = progress
//        map.project = projectIdent
//        map.user = userIdent
//        map.printInActivity = printInActivity
//        map.comments = taskService.getLastComments(this,5)
//        return map
//    }

}
