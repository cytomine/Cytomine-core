package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Entity
@Getter
@Setter
public class AbstractSlice extends CytomineDomain {

    //TODO:      sort([time: 'asc', zStack: 'asc', channel: 'asc'])

    @ManyToOne
    private AbstractImage image;

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

    @ManyToOne(fetch = FetchType.EAGER)
    private Mime mime;

    private Integer channel;

    private Integer zStack;

    private Integer time;

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AbstractSlice abstractSlice = this;
        abstractSlice.id = json.getJSONAttrLong("id",null);
        abstractSlice.created = json.getJSONAttrDate("created");
        abstractSlice.updated = json.getJSONAttrDate("updated");

        abstractSlice.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);
        abstractSlice.image = (AbstractImage) json.getJSONAttrDomain(entityManager, "image", new AbstractImage(), true);
        abstractSlice.mime = (Mime) json.getJSONAttrDomain(entityManager, "mime", new Mime(), "mimeType", "String", true);

        
        abstractSlice.channel = json.getJSONAttrInteger("channel", 0);
        abstractSlice.zStack = json.getJSONAttrInteger("zStack", 0);
        abstractSlice.time = json.getJSONAttrInteger("time", 0);
        
        return abstractSlice;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AbstractSlice abstractSlice = (AbstractSlice)domain;
        returnArray.put("uploadedFile", abstractSlice.getUploadedFileId());
        returnArray.put("path", abstractSlice.getPath());
        returnArray.put("image", abstractSlice.getImageId());
        returnArray.put("mime", abstractSlice.getMimeType());

        returnArray.put("channel", abstractSlice.getChannel());
        returnArray.put("zStack", abstractSlice.getZStack());
        returnArray.put("time", abstractSlice.getTime());
        returnArray.put("rank", abstractSlice.getRank());
         return returnArray;
    }

    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public Long getImageId() {
        return this.getImage()!=null ? this.getImage().getId() : null;
    }

    public String getMimeType() {
        return this.getMime()!=null ? this.getMime().getMimeType() : null;
    }

    public String getPath() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getPath() : null;
    }
    public String getImageServerUrl() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getImageServerUrl() : null;
    }

    public String getImageServerInternalUrl(boolean useHTTPInternally) {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getImageServerInternalUrl(useHTTPInternally) : null;
    }

    public Integer getRank() {
        return this.channel +
                Optional.ofNullable(this.image.getChannels()).orElse(0) *
                        (this.zStack + Optional.ofNullable(this.image.getDepth()).orElse(0) * this.time);
    }

    @Override
    public CytomineDomain container() {
        return image.container();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
