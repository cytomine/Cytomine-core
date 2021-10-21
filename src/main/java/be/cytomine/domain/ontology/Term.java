package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static be.cytomine.domain.ontology.RelationTerm.PARENT;

@Entity
@Getter
@Setter
public class Term extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String name;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ontology_id", nullable = true)
    private Ontology ontology;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "term1")
    private Set<RelationTerm> relationsLeft = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "term2")
    private Set<RelationTerm> relationsRight  = new HashSet<>();

    private String color;


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
        Term term = this;
        term.id = json.getJSONAttrLong("id",null);
        term.name = json.getJSONAttrStr("name");
        term.comment = json.getJSONAttrStr("comment");
        term.color = json.getJSONAttrStr("color");
        term.ontology = (Ontology)json.getJSONAttrDomain(entityManager, "ontology", new Ontology(), true);
        term.created = json.getJSONAttrDate("created");
        term.updated = json.getJSONAttrDate("updated");
        return term;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Term term = (Term)domain;
        returnArray.put("name", term.getName());
        returnArray.put("comment", term.getComment());
        returnArray.put("ontology", term.getOntology()!=null ? term.getOntology().getId() : null);
        returnArray.put("color", term.getColor());
        returnArray.put("parent", term.parent().map(Term::getId).orElse(null));
        return returnArray;
    }

    public Optional<Term> parent() {
        if (this.relationsRight==null) {
            return Optional.empty();
        }
        return this.relationsRight.stream().filter(x -> x.getRelation().name.equals(PARENT)).map(RelationTerm::getTerm1).findFirst();
    }

    public Set<Term> children() {
        if (this.relationsRight==null) {
            return Set.of();
        }
        return this.relationsLeft.stream().filter(x -> x.getRelation().name.equals(PARENT)).map(RelationTerm::getTerm2).collect(Collectors.toSet());
    }

    /**
     * Check if this term has no parent
     * @return True if term has no parent
     */
    boolean isRoot() {
        return parent().isEmpty();
    }


    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public CytomineDomain container() {
        return ontology.container();
    }

    public Map<String, Object> getCallBack() {
        return Map.of("ontologyID", this.ontology.getId());
    }

    @Override
    public String toString() {
        return "Term{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
