package be.cytomine.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageStats {
    Long used;

    Long available;

    Double usedP;

    String hostname;

    String mount;

    String ip;
}
