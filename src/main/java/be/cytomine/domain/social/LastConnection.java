package be.cytomine.domain.social;

import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

//@Entity
@Getter
@Setter
@Document
//@CompoundIndex(def = "{'date' : 2, 'expireAfterSeconds': 60}")
/**
 * Info on last user connection on Cytomine
 * User x connect to poject y the 2013/01/01 at xxhyymin
 */
public class LastConnection {

//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @CreatedDate
//    @Indexed(name="last_connection_expiration", expireAfterSeconds=300)
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    protected Date date;

    protected Long user;

    private Long project;

    private Integer version = 0;
}
