package be.cytomine.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.ConstraintViolation;

@Data
public class ValidationError {

    private String message;

    private String property;

    private Object invalidValue;

    public ValidationError(ConstraintViolation<CytomineDomain> violation) {
        this.message = violation.getMessage();
        this.invalidValue = violation.getInvalidValue();
        this.property = violation.getPropertyPath().toString();
    }

}
