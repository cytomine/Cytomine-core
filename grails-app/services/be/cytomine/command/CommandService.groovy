package be.cytomine.command

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.security.SecUser
import grails.util.GrailsNameUtils

class CommandService {

    def springSecurityService
    def grailsApplication
    def securityACLService
    def cytomineService

    static final int SUCCESS_ADD_CODE = 200
    static final int SUCCESS_EDIT_CODE = 200
    static final int SUCCESS_DELETE_CODE = 200

    static final int NOT_FOUND_CODE = 404
    static final int TOO_LONG_REQUEST = 413

    /**
     * Execute an 'addcommand' c with json data
     * Store command in undo stack if necessary and in command history
     */
    def processCommand(AddCommand c) throws CytomineException {
        processCommand(c, SUCCESS_ADD_CODE)
    }

    /**
     * Execute an 'editcommand' c with json data
     * Store command in undo stack if necessary and in command history
     */
    def processCommand(EditCommand c) throws CytomineException {
        processCommand(c, SUCCESS_EDIT_CODE)
    }

    /**
     * Execute a 'deletecommand' c with json data
     * Store command in undo stack if necessary and in command history
     */
    def processCommand(DeleteCommand c) throws CytomineException {
        processCommand(c, SUCCESS_DELETE_CODE)
    }

    /**
     * Execute a 'command' c with json data
     * Store command in undo stack if necessary and in command history
     * if success, put http response code as successCode
     */
    def processCommand(Command c, int successCode) throws CytomineException {
        //execute command
        def result = c.execute()
        if (result.status == successCode) {
            if (!c.validate()) {
                log.error c.errors.toString()
            }
            c.save(failOnError: true)
            CommandHistory ch = new CommandHistory(command: c, prefixAction: "", project: c.project,user: c.user, message: c.actionMessage)
            ch.save(failOnError: true);
            if (c.saveOnUndoRedoStack) {
                def item = new UndoStackItem(command: c, user: c.user, transaction: c.transaction)
                item.save(flush: true,failOnError: true)
            }
            result.data.put('command', c.id)
        }
        return result
    }

    def list(String domain, Class commandclass, Long afterThan) {
        securityACLService.checkAdmin(cytomineService.currentUser)
        if(domain) {
            String serviceName = GrailsNameUtils.getPropertyName(domain) + 'Service'

            if(afterThan){
                return commandclass.findAllByServiceNameAndCreatedGreaterThan(serviceName, new Date(afterThan))
            }
            return commandclass.findAllByServiceName(serviceName)
        }
        return commandclass.findAll()
    }

    def undo(UndoStackItem undoItem, SecUser user) {
        def data = []
        def statutes = []
        def result

        def transaction =  undoItem.transaction
        log.debug "FirstUndoStack=" + undoItem

        if (!transaction) {
            log.debug "Transaction not in progress"
            //Not Transaction, no other command must be deleted
            result = undoItem.getCommand().undo()
            //An undo command must be move to redo stack
            moveToRedoStack(undoItem)
            data << result.data
            statutes << result.status
        } else {
            log.debug "Transaction in progress"
            //Its a transaction, many other command will be deleted
            def undoStacks = UndoStackItem.findAllByUserAndTransaction(user, transaction, [sort: "created", order: "desc"])
            for (undoStack in undoStacks) {
                //browse all command and undo it while its the same transaction
                log.debug "Undo stack transaction:" + transaction?.id
                if(undoStack.getCommand().refuseUndo) {
                    //responseError(new ObjectNotFoundException("You cannot delete your last operation!")) //undo delete project is not possible
                    throw new ObjectNotFoundException("You cannot delete your last operation!") //undo delete project is not possible
                    return
                }
                result = undoStack.getCommand().undo()
                log.info "Undo stack transaction: UNDO " + undoStack?.command?.actionMessage
                data << result.data
                statutes << result.status
                moveToRedoStack(undoStack)
            }
        }

        return [data : data, statutes : statutes]
    }

    def redo(RedoStackItem redoItem, SecUser user) {
        def data = []
        def statutes = []
        def result

        def transaction =  redoItem.transaction
        log.debug "LastRedoStack=" + redoItem

        if (!transaction) {
            log.debug "Transaction not in progress"
            //Not Transaction, no other command must be deleted
            result = redoItem.getCommand().redo()
            //An undo command must be move to redo stack
            moveToUndoStack(redoItem)
            data << result.data
            statutes << result.status
        } else {
            log.debug "Transaction in progress"
            //Its a transaction, many other command will be deleted
            def redoStacks = RedoStackItem.findAllByUserAndTransaction(user, transaction, [sort: "created", order: "desc"])
            for (redoStack in redoStacks) {
                //Redo each command from the same transaction
                result = redoStack.getCommand().redo()
                moveToUndoStack(redoItem)
                data << result.data
                statutes << result.status
            }
        }

        return [data : data, statutes : statutes]
    }

    /**
     * Move an undo stack item to redo stack
     * @param firstUndoStack Undo stack item to move
     */
    private def moveToRedoStack(UndoStackItem firstUndoStack) {
        //create new redo stack item
        new RedoStackItem(
                command: firstUndoStack.getCommand(),
                user: firstUndoStack.getUser(),
                transaction: firstUndoStack.transaction
        ).save(flush: true)
        //save to history stack
        new CommandHistory(command: firstUndoStack.getCommand(), prefixAction: "UNDO", project: firstUndoStack.getCommand().project, user: firstUndoStack.user, message: firstUndoStack.command.actionMessage).save(failOnError: true)
        //delete from undo stack
        firstUndoStack.delete(flush: true)
    }

    /**
     * Move redo item to the undo stack
     * @param lastRedoStack redo item to move
     */
    private def moveToUndoStack(RedoStackItem lastRedoStack) {
        //create the new undo item
        new UndoStackItem(
                command: lastRedoStack.getCommand(),
                user: lastRedoStack.getUser(),
                transaction: lastRedoStack.transaction,
        ).save(flush: true)
        //add to history stack
        new CommandHistory(command: lastRedoStack.getCommand(), prefixAction: "REDO", project: lastRedoStack.getCommand().project,user: lastRedoStack.user,message: lastRedoStack.command.actionMessage).save(failOnError: true);
        //delete the redo item
        lastRedoStack.delete(flush: true)
    }

}
