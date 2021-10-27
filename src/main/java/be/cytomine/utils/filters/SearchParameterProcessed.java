package be.cytomine.utils.filters;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SearchParameterProcessed {

    List<SearchParameterEntry> data;

    Map<String, Object> sqlParameters;
}
