package be.cytomine.domain.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class TagDomainAssocitation extends CytomineDomain {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    protected Tag tag;

    @NotNull
    private String domainClassName;

    @NotNull
    private Long domainIdent;

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
