package be.cytomine.domain.command;

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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
public class CommandHistory extends CytomineDomain {


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "command_id", nullable = true)
    protected Command command;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected SecUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    protected Project project;

    @Column(nullable = true)
    protected String message;

    @Column(nullable = false)
    protected String prefixAction;

    public CommandHistory() {

    }

    public CommandHistory(Command command) {
        this.setCommand(command);
        this.setPrefixAction("");
        this.setProject(command.getProject());
        this.setUser(command.getUser());
        this.setMessage(command.getActionMessage());
    }

    public CommandHistory(UndoStackItem undoStackItem) {
        this.setCommand(undoStackItem.getCommand());
        this.setPrefixAction("UNDO");
        this.setProject(undoStackItem.getCommand().getProject());
        this.setUser(undoStackItem.getUser());
        this.setMessage(undoStackItem.getCommand().getActionMessage());
    }

    public CommandHistory(RedoStackItem redoStackItem) {
        this.setCommand(redoStackItem.getCommand());
        this.setPrefixAction("REDO");
        this.setProject(redoStackItem.getCommand().getProject());
        this.setUser(redoStackItem.getUser());
        this.setMessage(redoStackItem.getCommand().getActionMessage());
    }




//    @PrePersist
//    public void beforeCreate() {
//        super.beforeInsert();
//    }
//
//    @PreUpdate
//    public void beforeUpdate() {
//        super.beforeUpdate();
//    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        throw new RuntimeException("Not supported");
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        CommandHistory commandHistory = (CommandHistory)domain;
        returnArray.put("command", commandHistory.getCommand()!=null? commandHistory.getCommand().toJsonObject() : null);
        returnArray.put("prefixAction", commandHistory.prefixAction);
        returnArray.put("user", commandHistory.user);
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
