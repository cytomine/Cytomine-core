package be.cytomine.service;

import be.cytomine.domain.ValidationError;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.EditCommand;
import be.cytomine.exceptions.*;
import be.cytomine.utils.CommandResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;

@Slf4j
@Service
public class CommandService {

//    def springSecurityService
//    def grailsApplication
//    def securityACLService
//    def cytomineService

    @Autowired
    EntityManager entityManager;

    static final int SUCCESS_ADD_CODE = 200;
    static final int SUCCESS_EDIT_CODE = 200;
    static final int SUCCESS_DELETE_CODE = 200;

    static final int NOT_FOUND_CODE = 404;
    static final int TOO_LONG_REQUEST = 413;

//    /**
//     * Execute an 'addcommand' c with json data
//     * Store command in undo stack if necessary and in command history
//     */
//    CommandResponse processCommand(AddCommand c) throws CytomineException {
//        return processCommand(c, SUCCESS_ADD_CODE);
//    }
//
//    /**
//     * Execute an 'editcommand' c with json data
//     * Store command in undo stack if necessary and in command history
//     */
//    CommandResponse processCommand(EditCommand c) throws CytomineException {
//        return processCommand(c, SUCCESS_EDIT_CODE);
//    }
//
//    /**
//     * Execute a 'deletecommand' c with json data
//     * Store command in undo stack if necessary and in command history
//     */
//    CommandResponse processCommand(DeleteCommand c) throws CytomineException {
//        return processCommand(c, SUCCESS_DELETE_CODE);
//    }

    CommandResponse processCommand(Command c, ModelService service) throws CytomineException {
        if (c instanceof AddCommand) {
            return processCommand(c, service, SUCCESS_ADD_CODE);
        }
        if (c instanceof EditCommand) {
            return processCommand(c, service, SUCCESS_EDIT_CODE);
        }
        if (c instanceof DeleteCommand) {
            return processCommand(c, service, SUCCESS_DELETE_CODE);
        }
        throw new ObjectNotFoundException("Command not supported");
    }

    /**
     * Execute a 'command' c with json data
     * Store command in undo stack if necessary and in command history
     * if success, put http response code as successCode
     */
    CommandResponse processCommand(Command c, ModelService service, int successCode) throws CytomineException {
        //execute command
        CommandResponse result = c.execute(service);
        if (result.getStatus() == successCode) {
            List<ValidationError> validationErrors = c.validate();
            if (!validationErrors.isEmpty()) {
                log.error(validationErrors.toString());
            }
            entityManager.persist(c);

            //TODO:
//            CommandHistory ch = new CommandHistory(command: c, prefixAction: "", project: c.project,user: c.user, message: c.actionMessage)
//            ch.save(failOnError: true);
            if (c.isSaveOnUndoRedoStack()) {
                 // TODO:
//                def item = new UndoStackItem(command: c, user: c.user, transaction: c.transaction)
//                item.save(flush: true,failOnError: true)
            }
            result.getData().put("command", c.getId());
        }
        return result;
    }
}
