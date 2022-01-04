package be.cytomine.domain.security;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@DiscriminatorValue("be.cytomine.security.UserJob")
public class UserJob extends SecUser {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    public UserJob() {
        super();
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

    @Override
    public String toJSON() {
        return "not implemented";
    }

}
