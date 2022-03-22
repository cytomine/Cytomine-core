package be.cytomine.domain.ontology;

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.SecUser;
import be.cytomine.utils.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class AnnotationIndex {

    @Id
    @GeneratedValue(generator = "myGenerator")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private SecUser user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id", nullable = true)
    private SliceInstance slice;

    @Version
    protected Integer version = 0;

    Long countAnnotation;

    Long countReviewedAnnotation;

    public static JsonObject getDataFromDomain(AnnotationIndex index) {
        JsonObject returnArray = new JsonObject();
        returnArray.put("user", index.getUser()!=null? index.getUser().getId() : null);
        returnArray.put("slice", index.getSlice()!=null? index.getSlice().getId() : null);
        returnArray.put("countAnnotation", index.getCountAnnotation());
        returnArray.put("countReviewedAnnotation", index.getCountReviewedAnnotation());
        return returnArray;
    }
}
