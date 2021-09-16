package be.cytomine.domain;

import be.cytomine.domain.security.User;
import be.cytomine.utils.DateUtils;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@MappedSuperclass
public abstract class CytomineDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "myGen")
    @SequenceGenerator(name = "myGen", sequenceName = "hibernate_sequence", allocationSize=1)
    protected Long id;

    protected Date created;

    protected Date updated;

    @Version
    protected Integer version = 0;

    public CytomineDomain() {

    }

    public void beforeInsert() {
        if (created!=null) {
            created = new Date();
        }
    }

    public void beforeUpdate() {
        updated = new Date();
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

    public abstract String toJSON();

    public abstract JsonObject toJsonObject();

    public Map<String, Object> getCallBack() {
        return Map.of();
    }
}
