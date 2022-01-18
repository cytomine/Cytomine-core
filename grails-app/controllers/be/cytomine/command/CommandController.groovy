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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.api.RestController
import be.cytomine.security.SecUser

/**
 * Controller for command.
 * A command is an op done by a user (or job).
 * Some command my be undo/redo (e.g. Undo Add annotation x => delete annotation x)
 */
class CommandController extends RestController {
    def springSecurityService
    def messageSource
    def commandService
    def cytomineService
    def securityACLService

    //TODO count the EditCommand with boolean deleted !
    def listDelete() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        String domain
        if(params.domain){
            domain = params.domain;
        }
        Long afterThan;
        if(params.after) afterThan = params.long("after")
        responseSuccess(commandService.list(domain, DeleteCommand, afterThan))
    }
    /**
     * Do undo op on the last command/transaction done by this user
     */
    def undo = {
        SecUser user = SecUser.read(springSecurityService.currentUser.id)

        //Get the last command list with max 1 command
        def lastCommands

        if(params.long("id")) {
            lastCommands = UndoStackItem.findAllByUserAndCommand(user, Command.read(params.long("id")), [sort: "created", order: "desc", max: 1, offset: 0])
        } else {
            lastCommands = UndoStackItem.findAllByUser(user, [sort: "created", order: "desc", max: 1, offset: 0])
        }


        //There is no command, so nothing to undo
        if (lastCommands.isEmpty()) {
            def data = [success: true, message: messageSource.getMessage('be.cytomine.UndoCommand', [] as Object[], Locale.ENGLISH), callback: null, printMessage: true]
            responseSuccess([data])
            return
        }

        //Last command done
        def firstUndoStack = lastCommands.last()
        def results = commandService.undo(firstUndoStack, user)

        if(results.statutes.count {it != 200 && it != 201} == 0) response.status = 200
        else response.status = 400

        responseSuccess(results.data)
    }

    /**
     * Do redo op on the last undo done by this user
     */
    def redo = {
        SecUser user = SecUser.read(springSecurityService.currentUser.id)
        //Get the last undo command
        def lastCommands

        if(params.long("id")) {
            lastCommands = RedoStackItem.findAllByUserAndCommand(user, Command.read(params.long("id")), [sort: "created", order: "desc", max: 1, offset: 0])
        } else {
            lastCommands = RedoStackItem.findAllByUser(user, [sort: "created", order: "desc", max: 1, offset: 0])
        }

        if (lastCommands.size() == 0) {
            def data = [success: true, message: messageSource.getMessage('be.cytomine.RedoCommand', [] as Object[], Locale.ENGLISH), callback: null, printMessage: true]
            responseSuccess([data])
            return
        }

        def lastRedoStack = lastCommands.last()

        def results = commandService.redo(lastRedoStack, user)

        if(results.statutes.count {it != 200 && it != 201} == 0) response.status = 200
        else response.status = 400

        responseSuccess(results.data)
    }



}