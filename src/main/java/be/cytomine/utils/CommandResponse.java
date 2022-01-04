package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CommandResponse {

    Integer status;

    CytomineDomain object;

    Map<String, Object> data;

}
