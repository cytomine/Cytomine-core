package be.cytomine.domain.acl;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity(name = "AclEntry")
@Table(name = "acl_entry")
@Immutable
public class AclEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ace_order", nullable = false, unique = false)
    private Integer aceOrder;

    @Column(name = "mask", nullable = false, unique = false)
    private Integer mask;

    @Column(name = "granting", nullable = false, unique = false)
    private Boolean granting;

    @Column(name = "audit_success", nullable = false, unique = false)
    private Boolean auditSuccess;

    @Column(name = "audit_failure", nullable = false, unique = false)
    private Boolean auditFailure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acl_object_identity", referencedColumnName = "id", nullable = false, unique = false, insertable = true, updatable = true)
    private AclObjectIdentity aclObjectIdentity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sid", referencedColumnName = "id", nullable = false, unique = false, insertable = true, updatable = true)
    private AclSid sid;

    public AclEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAceOrder() {
        return aceOrder;
    }

    public void setAceOrder(Integer aceOrder) {
        this.aceOrder = aceOrder;
    }

    public Integer getMask() {
        return mask;
    }

    public void setMask(Integer mask) {
        this.mask = mask;
    }

    public Boolean getGranting() {
        return granting;
    }

    public void setGranting(Boolean granting) {
        this.granting = granting;
    }

    public Boolean getAuditSuccess() {
        return auditSuccess;
    }

    public void setAuditSuccess(Boolean auditSuccess) {
        this.auditSuccess = auditSuccess;
    }

    public Boolean getAuditFailure() {
        return auditFailure;
    }

    public void setAuditFailure(Boolean auditFailure) {
        this.auditFailure = auditFailure;
    }

    public AclObjectIdentity getAclObjectIdentity() {
        return aclObjectIdentity;
    }

    public void setAclObjectIdentity(AclObjectIdentity aclObjectIdentity) {
        this.aclObjectIdentity = aclObjectIdentity;
    }

    public AclSid getSid() {
        return sid;
    }

    public void setSid(AclSid sid) {
        this.sid = sid;
    }

    @Override
    public String toString() {
        return "AclEntry [id=" + id + ", aceOrder=" + aceOrder + ", mask="
                + mask + ", granting=" + granting + ", auditSuccess="
                + auditSuccess + ", auditFailure=" + auditFailure
                + ", aclObjectIdentity=" + aclObjectIdentity + ", sid=" + sid
                + "]";
    }

}
