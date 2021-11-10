package be.cytomine.domain.image.server;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.Mime;
import be.cytomine.domain.middleware.ImageServer;
import be.cytomine.domain.processing.ImagingServer;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Getter
@Setter
public class MimeImageServer extends CytomineDomain {

    @ManyToOne(optional = false)
    private ImageServer imageServer;

    @ManyToOne(optional = false)
    private Mime mime;

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
