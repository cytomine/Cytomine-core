package be.cytomine.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.ConstraintViolation;

@Getter
@Setter
public class ValidationError {

    private String message;

    private String property;

    private Object invalidValue;

    public ValidationError(ConstraintViolation<CytomineDomain> violation) {
        this.message = violation.getMessage();
        this.invalidValue = violation.getInvalidValue();
        this.property = violation.getPropertyPath().toString();
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "message='" + message + '\'' +
                ", property='" + property + '\'' +
                ", invalidValue=" + invalidValue +
                '}';
    }
}
