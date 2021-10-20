package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class Ontology extends CytomineDomain {

    @NotNull
    @NotBlank
    @Column(nullable = false, unique = true)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy="ontology")
//    @JoinColumn(referencedColumnName = "ontology_id", nullable = true)
    protected Set<Project> projects = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy="ontology")
//    @JoinColumn(referencedColumnName = "ontology_id", nullable = true)
    protected Set<Term> terms = new HashSet<>();

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
        Ontology ontology = (Ontology)this;
        ontology.id = json.getJSONAttrLong("id",null);
        ontology.name = json.getJSONAttrStr("name");
        ontology.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);
        ontology.created = json.getJSONAttrDate("created");
        ontology.updated = json.getJSONAttrDate("updated");
        return ontology;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        Ontology ontology = (Ontology)domain;
        returnArray.put("name", ontology.getName());
        returnArray.put("user", ontology.getUserId());
        returnArray.put("title", ontology.getName());
        returnArray.put("attr",JsonObject.of("id", ontology.getId(), "type", ontology.getClass()));
        returnArray.put("data",ontology.getName());
        returnArray.put("isFolder",true);
        returnArray.put("key",ontology.getId());
        returnArray.put("hideCheckbox",true);

        returnArray.put("state","open");
        returnArray.put("projects", ontology.projects.stream().map(Project::getDataFromDomain).collect(Collectors.toSet()));
        if(domain.getVersion()!=null) {
            returnArray.put("children",ontology.tree());
        } else {
            returnArray.put("children",new ArrayList<>());
        }
        returnArray.put("user", (ontology.getUser()!=null ? ontology.getUser().getId() : null));
        return returnArray;
    }

    private Long getUserId() {
        return (user != null ? user.getId() : null);
    }

    /**
     * Get all ontology terms
     * @return Term list
     */
    Set<Term> terms() {
        return terms;
    }

    /**
     * Get the full ontology (with term) formatted in tree
     * @return List of root parent terms, each root parent term has its own child tree
     */
    public List<Map<String,Object>> tree() {
        List<Map<String,Object>> rootTerms = new ArrayList<>();
        for(Term term : this.terms()) {
            if (term.isRoot()) {
                rootTerms.add(branch(term));
            }
        }
        return rootTerms;
    }

    /**
     * Get the term branch
     * @param term Root term
     * @return Branch with all term children as tree
     */
    Map<String,Object> branch(Term term) {
        Map<String,Object> t = new HashMap<>();
        t.put("name", term.getName());
        t.put("id", term.getId());
        t.put("title",term.getName());
        t.put("data",term.getName());
        t.put("color",term.getColor());
        t.put("class",term.getClass());
        t.put("parent",term.parent().orElse(null));
        t.put("attr",Map.of("id", term.getId(), "type", term.getClass()));
        t.put("checked", false);

        t.put("key", term.getId());
        List<Map<String,Object>> children = new ArrayList<>();
        boolean isFolder = false;
        for (Term child : term.children()) {
            isFolder = true;
            Map<String,Object> childTree = branch(child);
            children.add(childTree);
        }
        children = children.stream().sorted(Comparator.comparing(a -> ((String) a.get("name")))).collect(Collectors.toList());
        t.put("children", children);

        t.put("isFolder", isFolder);
        t.put("hideCheckbox", isFolder);
        return t;
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    @Override
    public CytomineDomain container() {
        return this;
    }


    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    @Override
    public String toString() {
        return "Ontology{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
