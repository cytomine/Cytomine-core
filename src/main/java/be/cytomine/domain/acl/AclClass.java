package be.cytomine.domain.acl;

import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "AclClass")
@Table(name = "acl_class")
@Immutable
public class AclClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "class", length = 100, nullable = false, unique = true)
    private String className;

    public AclClass() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "AclClass [id=" + id + ", className=" + className + "]";
    }

}

