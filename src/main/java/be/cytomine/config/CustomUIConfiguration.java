package be.cytomine.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CustomUIConfiguration {

    CustomUIGlobalConfiguration global;

    CustomUIProjectConfiguration project;
}
