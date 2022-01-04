package be.cytomine.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTermMapping {

    private Long term;

    private Long user;
}
