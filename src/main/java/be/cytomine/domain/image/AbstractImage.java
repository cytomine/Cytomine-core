package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.UserJob;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import be.cytomine.domain.image.UploadedFile;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class AbstractImage extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

    @NotBlank
    private String originalFilename;

    @Min(1)
    private Integer width;

    @Min(1)
    private Integer height;

    @Min(1)
    private Integer depth;

    @Min(1)
    private Integer duration;

    @Min(1)
    private Integer channels;

    @Column(name = "physical_size_x")
    private Double physicalSizeX;

    @Column(name = "physical_size_y")
    private Double physicalSizeY;

    @Column(name = "physical_size_z")
    private Double physicalSizeZ;

    private Double fps;

    private Integer magnification;

    // TODO: should be named bit per color (bpc) <> bit per pixel (bpp) = bit depth
    @Min(1)
    private Integer bitDepth;

    private String colorspace;

    @ManyToOne
    private SecUser user; //owner


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AbstractImage abstractImage = this;
        abstractImage.id = json.getJSONAttrLong("id",null);
        abstractImage.created = json.getJSONAttrDate("created");
        abstractImage.updated = json.getJSONAttrDate("updated");

        abstractImage.originalFilename = json.getJSONAttrStr("originalFilename");

        abstractImage.uploadedFile = (UploadedFile) json.getJSONAttrDomain(entityManager, "uploadedFile", new UploadedFile(), false);

        abstractImage.height = json.getJSONAttrInteger("height", null);
        abstractImage.width = json.getJSONAttrInteger("width", null);
        abstractImage.depth = json.getJSONAttrInteger("depth", 1);
        abstractImage.duration = json.getJSONAttrInteger("duration", 1);
        abstractImage.channels = json.getJSONAttrInteger("channels", 1);
        abstractImage.physicalSizeX = json.getJSONAttrDouble("physicalSizeX", null);
        abstractImage.physicalSizeY = json.getJSONAttrDouble("physicalSizeY", null);
        abstractImage.physicalSizeZ = json.getJSONAttrDouble("physicalSizeZ", null);

        abstractImage.fps = json.getJSONAttrDouble("fps", null);

        abstractImage.magnification = json.getJSONAttrInteger("magnification", null);
        abstractImage.bitDepth = json.getJSONAttrInteger("bitDepth", null);
        abstractImage.colorspace = json.getJSONAttrStr("colorspace", false);


        return uploadedFile;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        AbstractImage abstractImage = (AbstractImage)domain;
        returnArray.put("filename", abstractImage.getFilename());
        returnArray.put("originalFilename", abstractImage.getOriginalFilename());
        returnArray.put("uploadedFile", abstractImage.getUploadedFileId());
        returnArray.put("path", abstractImage.getPath());
        returnArray.put("contentType", abstractImage.getContentType());

        returnArray.put("width", abstractImage.getWidth());
        returnArray.put("height", abstractImage.getHeight());
        returnArray.put("depth", abstractImage.getDepth());
        returnArray.put("duration", abstractImage.getDuration());
        returnArray.put("channels", abstractImage.getChannels());
        returnArray.put("dimensions", abstractImage.getDimensions());

        returnArray.put("physicalSizeX", abstractImage.getPhysicalSizeX());
        returnArray.put("physicalSizeY", abstractImage.getPhysicalSizeY());
        returnArray.put("physicalSizeZ", abstractImage.getPhysicalSizeZ());

        returnArray.put("fps", abstractImage.getFps());
        returnArray.put("zoom", abstractImage.getZoomLevels());  // /!!\ Breaking API : image?.getZoomLevels()?.max


        returnArray.put("magnification", abstractImage.getMagnification());
        returnArray.put("bitDepth", abstractImage.getBitDepth());
        returnArray.put("colorspace", abstractImage.getColorspace());
//        returnArray['thumb'] = UrlApi.getAbstractImageThumbUrlWithMaxSize(image ? (long)image?.id : null, 512)
//        returnArray['preview'] = UrlApi.getAbstractImageThumbUrlWithMaxSize(image ? (long)image?.id : null, 1024)
//        returnArray['macroURL'] = UrlApi.getAssociatedImage(image ? (long)image?.id : null, "macro", image?.uploadedFile?.contentType, 512)
        return returnArray;
    }


    public Long getUploadedFileId() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getId() : null;
    }

    public String getContentType() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getContentType() : null;
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

    public Integer getZoomLevels() {
        if (width==null || height==null) {
            return 1;
        }

        double tmpWidth = width;
        double tmpHeight = height;
        int nbZoom = 0;
        while (tmpWidth > 256 || tmpHeight > 256) {
            nbZoom++;
            tmpWidth /= 2;
            tmpHeight /= 2;
        }
        return nbZoom;
    }

    String getFilename() {
        return originalFilename;
    }

    String getDimensions() {
        List<String> dimensions = new ArrayList<>();
        dimensions.add("X");
        dimensions.add("Y");
        if (channels > 1) {
            dimensions.add("C");
        }
        if (depth > 1) {
            dimensions.add("Z");
        }
        if (duration > 1) {
            dimensions.add("T");
        }
        return String.join("", dimensions);
    }


    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        if (uploadedFile != null) {
            return uploadedFile.container();
        }
        return null;
    }
    
    
    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

//    def hasProfile() {
//        return CompanionFile.countByImageAndType(this, "HDF5") as Boolean
//    }

//    def getSliceCoordinates() {
//        def slices = AbstractSlice.findAllByImage(this)
//
//        return [
//        channels: slices.collect { it.channel }.unique().sort(),
//                zStacks: slices.collect { it.zStack }.unique().sort(),
//                times: slices.collect { it.time }.unique().sort()
//        ]
//    }
//
//    def getReferenceSliceCoordinate() {
//        def coordinates = getSliceCoordinates()
//        return [
//        channel: coordinates.channels[(int) Math.floor(coordinates.channels.size() / 2)],
//                zStack: coordinates.zStacks[(int) Math.floor(coordinates.zStacks.size() / 2)],
//                time: coordinates.times[(int) Math.floor(coordinates.times.size() / 2)],
//        ]
//    }
//
//    def getReferenceSlice() {
//        def coord = getReferenceSliceCoordinate()
//        return AbstractSlice.findByImageAndChannelAndZStackAndTime(this, coord.channel, coord.zStack, coord.time)
//    }

}
