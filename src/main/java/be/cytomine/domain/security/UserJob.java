package be.cytomine.domain.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Data
@DiscriminatorValue("be.cytomine.security.UserJob")
public class UserJob extends SecUser {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    protected User user;

    public UserJob() {
        super();
    }

    @PrePersist
    public void beforeCreate() {
        super.beforeInsert();
    }

    @PreUpdate
    public void beforeUpdate() {
        super.beforeUpdate();
    }

    @Override
    public String toJSON() {
        return "not implemented";
    }

}
