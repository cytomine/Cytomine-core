package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Optional;

@Entity
@Getter
@Setter
public class SliceInstance extends CytomineDomain implements Serializable {

    @ManyToOne(fetch = FetchType.EAGER)
    private AbstractSlice baseSlice;

    @ManyToOne
    private ImageInstance image;

    @ManyToOne
    private Project project;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        SliceInstance sliceInstance = this;
        sliceInstance.id = json.getJSONAttrLong("id",null);
        sliceInstance.created = json.getJSONAttrDate("created");
        sliceInstance.updated = json.getJSONAttrDate("updated");
        
        sliceInstance.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);
        sliceInstance.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        sliceInstance.baseSlice = (AbstractSlice)json.getJSONAttrDomain(entityManager, "baseSlice", new AbstractSlice(), true);
        
        return sliceInstance;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = ImageInstance.getDataFromDomain(domain);
        SliceInstance sliceInstance = (SliceInstance) domain;
        
        
        
        returnArray.put("uploadedFile", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getUploadedFileId).orElse(null));

        returnArray.put("imageServerUrl", sliceInstance.getImageServerUrl());
        returnArray.put("project", Optional.ofNullable(sliceInstance.project).map(CytomineDomain::getId).orElse(null));
        returnArray.put("baseSlice", Optional.ofNullable(sliceInstance.baseSlice).map(CytomineDomain::getId).orElse(null));
        returnArray.put("image", Optional.ofNullable(sliceInstance.image).map(CytomineDomain::getId).orElse(null));
        returnArray.put("path", sliceInstance.getPath());

        returnArray.put("mime", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getMimeType).orElse(null));
        returnArray.put("channel", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getChannel).orElse(null));
        returnArray.put("zStack", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getZStack).orElse(null));
        returnArray.put("time", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getTime).orElse(null));
        returnArray.put("rank", Optional.ofNullable(sliceInstance.baseSlice).map(AbstractSlice::getRank).orElse(null));
        return returnArray;
    }


    public String getPath() {
        return Optional.ofNullable(this.baseSlice).map(AbstractSlice::getPath).orElse(null);
    }

    public String getImageServerUrl() {
        return Optional.ofNullable(this.baseSlice).map(AbstractSlice::getImageServerUrl).orElse(null);
    }

    public String getMimeType(){
        return Optional.ofNullable(this.baseSlice).map(AbstractSlice::getMimeType).orElse(null);
    }

    @Override
    public CytomineDomain container() {
        return project.container();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }


}
