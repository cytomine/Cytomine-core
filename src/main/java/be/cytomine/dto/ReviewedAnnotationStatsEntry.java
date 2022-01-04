package be.cytomine.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewedAnnotationStatsEntry {
    private Long user;
    private Long all;
    private Long reviewed;
}
