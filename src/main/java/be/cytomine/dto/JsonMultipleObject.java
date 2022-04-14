package be.cytomine.dto;

import be.cytomine.utils.JsonObject;
import lombok.Data;
import org.hibernate.mapping.Array;

import java.util.ArrayList;
import java.util.List;

@Data
public class JsonMultipleObject extends ArrayList<JsonObject> implements JsonInput {

}
