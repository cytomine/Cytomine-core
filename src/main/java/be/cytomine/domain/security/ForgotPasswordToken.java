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
public class ForgotPasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "myGenerator")
    @GenericGenerator(name = "myGenerator", strategy = "be.cytomine.config.CustomIdentifierGenerator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @NotNull
    private Date expiryDate;

    private String tokenKey;

    public boolean isValid(){
        return expiryDate.toInstant().isAfter(new Date().toInstant());
    }

}
