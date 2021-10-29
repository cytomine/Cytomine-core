package be.cytomine.utils.filters;

import be.cytomine.domain.CytomineDomain;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class SpecificationBuilder {


    public static <T extends CytomineDomain> Specification<T> getSpecificationFromFilters(List<SearchParameterEntry> filters){
        if (filters.size() > 0) {
            Specification<T> specification = createSpecification(filters.get(0));
            if (filters.size() > 1) {
                for (int i = 1 ; i<filters.size() ; i++) {
                    specification = specification.and(createSpecification(filters.get(i)));
                }
            }
            return specification;
        } else {
           return Specification.where(null);
        }
    }

    public static <T extends CytomineDomain> Specification<T> createSpecification(SearchParameterEntry input) {
        switch (input.getOperation()){

            case equals:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(input.getProperty()),
                                castToRequiredType(root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            case nequals:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.notEqual(root.get(input.getProperty()),
                                castToRequiredType(root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            case gte:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.ge(root.get(input.getProperty()),
                                (Number) castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            case lte:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.le(root.get(input.getProperty()),
                                (Number) castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            case like:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(root.get(input.getProperty()),
                                "%"+input.getValue()+"%");

            case ilike:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(input.getProperty())),
                                "%"+input.getValue().toString().toLowerCase()+"%");

            case in:
                return (root, query, criteriaBuilder) ->
                        criteriaBuilder.in(root.get(input.getProperty()))
                                .value(castToRequiredType(
                                        root.get(input.getProperty()).getJavaType(),
                                        input.getValue()));

            default:
                throw new RuntimeException("Operation not supported yet");
        }
    }

    public static Object castToRequiredType(Class fieldType, Object value) {
        if(fieldType.isAssignableFrom(Double.class)) {
            return Double.valueOf(value.toString());
        } else if(fieldType.isAssignableFrom(Integer.class)) {
            return Integer.valueOf(value.toString());
        } else if(Enum.class.isAssignableFrom(fieldType)) {
            return Enum.valueOf(fieldType, value.toString());
        }
        return null;
    }

}
