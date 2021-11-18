package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;

@Entity
@Getter
@Setter
public class CompanionFile extends CytomineDomain{

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

    @ManyToOne(fetch = FetchType.EAGER)
    private AbstractImage image;

    @NotBlank
    private String originalFilename;
    
    @NotBlank
    private String filename;
    
    private  String type;
    
    private  Integer progress = 0;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        CompanionFile companionFile = this;
        companionFile.id = json.getJSONAttrLong("id",null);
        companionFile.created = json.getJSONAttrDate("created");
        companionFile.updated = json.getJSONAttrDate("updated");

        companionFile.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);
        companionFile.image = (AbstractImage) json.getJSONAttrDomain(entityManager, "image", new AbstractImage(), true);


        companionFile.originalFilename = json.getJSONAttrStr("originalFilename", true);
        companionFile.filename = json.getJSONAttrStr("filename", true);
        companionFile.type = json.getJSONAttrStr("type", true);

        companionFile.progress = json.getJSONAttrInteger("progress", 0);

        return companionFile;
    }



    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        CompanionFile companionFile = (CompanionFile)domain;
        returnArray.put("uploadedFile", companionFile.getUploadedFileId());
        returnArray.put("path", companionFile.getPath());
        returnArray.put("image", companionFile.getImageId());
        returnArray.put("originalFilename", companionFile.getOriginalFilename());

        returnArray.put("filename", companionFile.getFilename());
        returnArray.put("type", companionFile.getType());
        returnArray.put("progress", companionFile.getProgress());
        returnArray.put("status", companionFile.getUploadedFileStatus());
        returnArray.put("statusText", companionFile.getUploadedFileStatusText());
        return returnArray;
    }

    public Long getImageId() {
        return this.getImage()!=null ? this.getImage().getId() : null;
    }


    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public Integer getUploadedFileStatus() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getStatus() : null;
    }


    public String getUploadedFileStatusText() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getStatusText() : null;
    }

    public String getPath() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getPath() : null;
    }


    @Override
    public CytomineDomain container() {
        return image.container();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public String getImageServerUrl() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getImageServerUrl() : null;
    }

    public String getImageServerInternalUrl() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getImageServerInternalUrl() : null;
    }
    
    
    
}
