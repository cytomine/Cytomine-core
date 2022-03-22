package be.cytomine.domain.security;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Getter
@Setter
public class AuthWithToken {

    @Id
    @GeneratedValue(generator = "myGenerator")
    private Long id;

    @Version
    protected Integer version = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @NotNull
    private Date expiryDate;

    String tokenKey;

    public boolean isValid(){
        return expiryDate.toInstant().isAfter(new Date().toInstant());
    }

}
