package be.cytomine.domain.social;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.CytomineSocialDomain;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.domain.ontology.RelationTerm.PARENT;

//@Entity
@Getter
@Setter
@Document
//@CompoundIndex(def = "{'user' : 1, 'created' : -1}")
public class PersistentConnection extends CytomineSocialDomain {

    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    protected User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Project project;

    private String session;

    @Override
    public JsonObject toJsonObject() {
        throw new WrongArgumentException("getDataFromDomain is not implemented for this class");
    }
}
