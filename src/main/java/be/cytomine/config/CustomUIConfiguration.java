package be.cytomine.config;

import lombok.Data;

@Data
public class CustomUIConfiguration {

    CustomUIGlobalConfiguration global;

    CustomUIProjectConfiguration project;
}
