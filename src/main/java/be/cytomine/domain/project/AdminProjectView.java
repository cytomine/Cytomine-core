package be.cytomine.domain.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "admin_project")
@Immutable
@Getter
@Setter
public class AdminProjectView {



//    private Long id;
//
//    private Long userId;

    @EmbeddedId
    AdminProjectPK key;

    public Long getId() {
        return key.getId();
    }

    public Long getUserId() {
        return key.getUserId();
    }

}
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
class AdminProjectPK implements Serializable {
    private Long id;
    private Long userId;
}