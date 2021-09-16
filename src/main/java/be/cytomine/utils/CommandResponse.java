package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;
import lombok.Data;

import java.util.Map;

@Data
public class CommandResponse {

    Integer status;

    CytomineDomain object;

    Map<String, Object> data;

}
