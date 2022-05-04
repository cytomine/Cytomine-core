package be.cytomine.domain.image;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public class AbstractImage extends CytomineDomain {

    @ManyToOne(fetch = FetchType.EAGER)
    private UploadedFile uploadedFile;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private SecUser user; //owner

    @Transient
    private Boolean inProject = false;


    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        AbstractImage abstractImage = this;
        abstractImage.id = json.getJSONAttrLong("id",null);
        abstractImage.created = json.getJSONAttrDate("created");
        abstractImage.updated = json.getJSONAttrDate("updated");

        abstractImage.originalFilename = json.getJSONAttrStr("originalFilename");
        if (originalFilename!=null && originalFilename.trim().equals("")) {
            throw new WrongArgumentException("'originalFilename' property cannot be blank");
        }
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


        return abstractImage;
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
        returnArray.put("thumb", UrlApi.getAbstractImageThumbUrlWithMaxSize(abstractImage.id, 512, "png"));
        returnArray.put("preview", UrlApi.getAbstractImageThumbUrlWithMaxSize(abstractImage.id, 1024, "png"));
        returnArray.put("macroURL", UrlApi.getAssociatedImage(abstractImage.id, "macro", Optional.ofNullable(abstractImage.getUploadedFile()).map(UploadedFile::getContentType).orElse(null), 512, "png"));

        returnArray.put("inProject", abstractImage.getInProject());
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

    public String getImageServerInternalUrl() {
        return this.getUploadedFile()!=null ? this.getUploadedFile().getImageServerInternalUrl() : null;
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
        if (channels!=null && channels > 1) {
            dimensions.add("C");
        }
        if (depth!=null && depth > 1) {
            dimensions.add("Z");
        }
        if (duration!=null && duration > 1) {
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
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }



}
