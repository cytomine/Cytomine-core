package be.cytomine.dto.search;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {
    private String query;
    private List<String> storage;
    private String index;
    private List<List<Object>> similarities;
}
