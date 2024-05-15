package be.cytomine.domain.appengine;

import be.cytomine.converter.UUIDConverter;
import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

import java.util.Optional;

import java.util.UUID;

@Entity
@Getter
@Setter
public class TaskRun extends CytomineDomain {

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user; // launcher

    @NotNull
    @Column(nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID taskRunId;

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        TaskRun taskRun = (TaskRun) domain;
        returnArray.put("taskRunId", taskRun.getTaskRunId().toString());
        returnArray.put("project", taskRun.getProjectId());
        returnArray.put("user", taskRun.getUserId());
        return returnArray;
    }

    private Long getProjectId() {
        return Optional.ofNullable(this.getProject()).map(CytomineDomain::getId).orElse(null);
    }

    private Long getUserId() {
        return Optional.ofNullable(this.getUser()).map(CytomineDomain::getId).orElse(null);
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public CytomineDomain container() {
        return project.container();
    }
}
