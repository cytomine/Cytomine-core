package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.Language;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.UserJob;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.LongArrayToBytesConverter;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class UploadedFile extends CytomineDomain implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private SecUser user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_id", nullable = true)
    private Storage storage;

    //@Lob
    //@Type(type="org.hibernate.type.BinaryType")
    @Convert(converter = LongArrayToBytesConverter.class)
    private Long[] projects;

    private String filename;

    private String originalFilename;

    private String ext;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "image_server_id", nullable = true)
    private ImageServer imageServer;

    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private UploadedFile parent;

    private Long size;

    private int status = 0;

    @Column(columnDefinition = "ltree")
    @Type(type = "be.cytomine.utils.LTreeType")
    private String lTree;

    @Override
    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        UploadedFile uploadedFile = this;
        uploadedFile.id = json.getJSONAttrLong("id",null);
        uploadedFile.created = json.getJSONAttrDate("created");
        uploadedFile.updated = json.getJSONAttrDate("updated");

        SecUser user = (SecUser)json.getJSONAttrDomain(entityManager, "user", new SecUser(), true);
        if (user instanceof UserJob) {
            user = ((UserJob) user).getUser();
        }
        uploadedFile.user = user;

        uploadedFile.parent = (UploadedFile)json.getJSONAttrDomain(entityManager, "parent", new UploadedFile(), false);
        uploadedFile.imageServer = (ImageServer)json.getJSONAttrDomain(entityManager, "imageServer", new ImageServer(), true);
        uploadedFile.storage = (Storage)json.getJSONAttrDomain(entityManager, "storage", new Storage(), true);

        uploadedFile.filename = json.getJSONAttrStr("filename");
        uploadedFile.originalFilename = json.getJSONAttrStr("originalFilename");
        uploadedFile.ext = json.getJSONAttrStr("ext");
        uploadedFile.contentType = json.getJSONAttrStr("contentType");
        uploadedFile.size = json.getJSONAttrLong("size", 0L);

        uploadedFile.status = json.getJSONAttrInteger("status", 0);
        uploadedFile.projects = json.isMissing("projects") ? null : json.getJSONAttrListLong("projects").toArray(new Long[0]);

        return uploadedFile;
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        UploadedFile uploadedFile = (UploadedFile)domain;
        returnArray.put("user", (uploadedFile.getUser()!=null ? uploadedFile.getUser().getId() : null));
        returnArray.put("parent", (uploadedFile.getParent()!=null ? uploadedFile.getParent().getId() : null));
        returnArray.put("imageServer", (uploadedFile.getImageServer()!=null ? uploadedFile.getImageServer().getId() : null));
        returnArray.put("storage", (uploadedFile.getStorage()!=null ? uploadedFile.getStorage().getId() : null));
        returnArray.put("originalFilename", uploadedFile.getOriginalFilename());
        returnArray.put("filename", uploadedFile.getFilename());
        returnArray.put("ext", uploadedFile.getExt());
        returnArray.put("contentType",uploadedFile.getContentType());
        returnArray.put("size",uploadedFile.getSize());
        returnArray.put("path",uploadedFile.getPath());
        returnArray.put("status",uploadedFile.getStatus());
        returnArray.put("statusText",uploadedFile.getStatusText());
        returnArray.put("projects", uploadedFile.getProjects());
        return returnArray;
    }

    String getStatusText() {
        return UploadedFileStatus.findByCode(status).map(Enum::name).orElse(null);
    }

    public String getPath() {
        if (contentType.equals("virtual/stack") || user == null) {
            return null;
        }
        return Paths.get(imageServer.getBasePath(), String.valueOf(user.getId()), filename).toString();
    }

    @PrePersist
    public void beforeCreate() {
        lTree = parent != null ? parent.lTree+"." : "";
        lTree += id;
    }

    @PreUpdate
    public void beforeUpdate() {
        lTree = parent != null ? parent.lTree+"." : "";
        lTree += id;
    }

    public CytomineDomain container() {
        return storage;
    }

    @Override
    public String toJSON() {
        return toJsonObject().toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public String getImageServerUrl() {
        return this.getImageServer()!=null ? this.getImageServer().getUrl() : null;
    }

    public String getImageServerInternalUrl() {
        return this.getImageServer()!=null ? this.getImageServer().getInternalUrl() : null;
    }

}
