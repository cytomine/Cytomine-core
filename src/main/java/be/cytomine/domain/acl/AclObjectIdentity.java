package be.cytomine.domain.acl;

import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Immutable;


@Entity(name = "AclObjectIdentity")
@Table(name = "acl_object_identity", uniqueConstraints = @UniqueConstraint(name = "uk_acl_object_identity", columnNames = {
        "object_id_class", "object_id_identity" }))
@Immutable
public class AclObjectIdentity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "object_id_identity", nullable = false, unique = false)
    private Long objectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id_class", referencedColumnName = "id", nullable = false, unique = false, insertable = true, updatable = true)
    private AclClass objectIdClass;

    @Column(name = "entries_inheriting", nullable = false, unique = false)
    private Boolean entriesInheriting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_object", referencedColumnName = "id", nullable = true, unique = false, insertable = true, updatable = true)
    private AclObjectIdentity parentObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_sid", referencedColumnName = "id", nullable = true, unique = false, insertable = true, updatable = true)
    private AclSid ownerSid;

    @OneToMany(targetEntity = AclEntry.class, fetch = FetchType.LAZY, mappedBy = "aclObjectIdentity", cascade = CascadeType.REMOVE)
    private Set<AclEntry> aclEntries = new HashSet<AclEntry>();

    public AclObjectIdentity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public Boolean getEntriesInheriting() {
        return entriesInheriting;
    }

    public void setEntriesInheriting(Boolean entriesInheriting) {
        this.entriesInheriting = entriesInheriting;
    }

    public AclObjectIdentity getParentObject() {
        return parentObject;
    }

    public void setParentObject(AclObjectIdentity parentObject) {
        this.parentObject = parentObject;
    }

    public AclClass getObjectIdClass() {
        return objectIdClass;
    }

    public void setObjectIdClass(AclClass objectIdClass) {
        this.objectIdClass = objectIdClass;
    }

    public AclSid getOwnerSid() {
        return ownerSid;
    }

    public void setOwnerSid(AclSid ownerSid) {
        this.ownerSid = ownerSid;
    }

    public Set<AclEntry> getAclEntries() {
        return aclEntries;
    }

    public void setAclEntries(Set<AclEntry> aclEntries) {
        this.aclEntries = aclEntries;
    }

}
