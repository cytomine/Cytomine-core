package be.cytomine.domain.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Getter
@Setter
public class Mime extends CytomineDomain {

    @Size(min=1, max = 5)
    private String extension;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String mimeType;


    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

//    def imageServers() {
//        MimeImageServer.findAllByMime(this).collect {it.imageServer}
//    }
}
