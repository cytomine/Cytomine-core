package be.cytomine.service.dto;

import lombok.Data;

@Data
public class StorageStats {
    Long used;

    Long available;

    Double usedP;

    String hostname;

    String mount;

    String ip;
}
