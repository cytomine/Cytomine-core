package be.cytomine.utils.filters;

import lombok.Data;

import java.util.Map;

@Data
public class SearchParameterEntry {

    public SearchParameterEntry() {

    }

    public SearchParameterEntry(String property, SearchOperation operation, Object value) {
        this.value = value;
        this.operation = operation;
        this.property = property;
    }

    Object value;

    SearchOperation operation;

    String property;

    String sql;

    Map<String, Object> sqlParameter;
}