package be.cytomine.service.dto;

import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JobLayerDTO {

    private Long id;

    private String username;

    private String softwareName;

    private String softwareVersion;

    private Date created;

    private boolean algo = true;

    private Long job;

    public static JsonObject getDataFromDomain(JobLayerDTO jobLayer) {
        JsonObject returnArray = new JsonObject();
        returnArray.put("id", jobLayer.getId());
        returnArray.put("username", jobLayer.getUsername());
        returnArray.put("softwareName",
                (jobLayer.getSoftwareVersion()==null || jobLayer.getSoftwareVersion().trim().equals("") ?
                        jobLayer.getSoftwareName() :
                        jobLayer.getSoftwareName() + " (" + jobLayer.getSoftwareVersion() + ")"
                )
        );
        returnArray.put("created", jobLayer.getCreated().getTime());
        returnArray.put("algo", jobLayer.isAlgo());
        returnArray.put("job", jobLayer.getJob());
        return returnArray;
    }



}
