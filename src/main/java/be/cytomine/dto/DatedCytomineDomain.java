package be.cytomine.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DatedCytomineDomain {
    private Long id;
    private Date date;
}
