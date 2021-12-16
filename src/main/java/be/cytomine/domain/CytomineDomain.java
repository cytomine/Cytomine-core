package be.cytomine.domain;

import be.cytomine.domain.security.Language;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CytomineDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "myGenerator")
    //@SequenceGenerator(name = "myGen", sequenceName = "hibernate_sequence", allocationSize=1)
    @GenericGenerator(name = "myGenerator", strategy = "be.cytomine.config.CustomIdentifierGenerator")
    protected Long id;

    @CreatedDate
    protected Date created;

    @LastModifiedDate
    protected Date updated;

    @Version
    protected Integer version = 0;

    public CytomineDomain() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CytomineDomain that = (CytomineDomain) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject jsonObject = new JsonObject();
        if (domain != null) {
            jsonObject.put("class", domain.getClass());
            jsonObject.put("id", domain.getId());
            jsonObject.put("created", DateUtils.getTimeToString(domain.created));
            jsonObject.put("updated", DateUtils.getTimeToString(domain.updated));
        }
        return jsonObject;
    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        return null;
    }

//    public static CytomineDomain buildDomainFromJson(JsonObject json) {
//        return null;
//    }
//
//    public static CytomineDomain buildDomainFromJson(JsonObject json, CytomineDomain domain) {
//        return null;
//    }

    public List<ValidationError> validate() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<CytomineDomain>> violations = validator.validate(this);
        return violations.stream().map(ValidationError::new).collect(Collectors.toList());
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return null;
    }

    public SecUser userDomainCreator() {
        return null;
    }

    public abstract JsonObject toJsonObject();

    public Map<String, Object> getCallBack() {
        return Map.of();
    }

    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    public boolean canUpdateContent() {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + id +
                '}';
    }
}
