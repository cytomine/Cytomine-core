package be.cytomine.domain.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.Language;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.UrlApi;
import be.cytomine.utils.JsonObject;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class UserAnnotation extends AnnotationDomain implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    Integer countReviewedAnnotations = 0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "annotation_term",
            joinColumns = { @JoinColumn(name = "user_annotation_id") },
            inverseJoinColumns = { @JoinColumn(name = "term_id") }
    )
    private List<Term> terms = new ArrayList<>();



    @PrePersist
    public void beforeCreate() {
        super.beforeCreate();
    }

    @PreUpdate
    public void beforeUpdate() {
        super.beforeUpdate();
    }

    @Override
    public List<Term> terms() {
        return terms;
    }

    /**
     * Check if annotation is reviewed
     * @return True if annotation is linked with at least one review annotation
     */
    boolean hasReviewedAnnotation() {
        return countReviewedAnnotations > 0;
    }

//TODO:
//    def tracks() {
//        if (this.version != null) {
//            AnnotationTrack.findAllByAnnotationIdentAndDeletedIsNull(this.id).collect { it.track }
//        } else {
//            return []
//        }
//    }

    /**
     * Get all annotation terms id
     * @return Terms id list
     */
    public List<Long> termsId() {
        return terms().stream().map(CytomineDomain::getId).distinct().collect(Collectors.toList());

    }

    // TODO:
//    def tracksId() {
//        return tracks().collect { it.id }.unique()
//    }

    /**
     * Get all terms for automatic review
     * If review is done "for all" (without manual user control), we add these term to the new review annotation
     * @return
     */
    public List<Term> termsForReview() {
        return terms().stream().distinct().collect(Collectors.toList());
    }

    @Override
    Long getUserId() {
        return user.getId();
    }

    /**
     * Check if its an algo annotation
     */
    public boolean isAlgoAnnotation() {
        return false;
    }

    /**
     * Check if its a review annotation
     */
    public boolean isReviewedAnnotation() {
        return false;
    }


// TODO: seems to be not used
//    /**
//     * Get a list of each term link with annotation
//     * For each term, add all users that add this term
//     * [{id: x, term: y, user: [a,b,c]}, {...]
//     */
//    def usersIdByTerm() {
//        def results = []
//        if (this.version != null) {
//            AnnotationTerm.findAllByUserAnnotationAndDeletedIsNull(this).each { annotationTerm ->
//                    def map = [:]
//                map.id = annotationTerm.id
//                map.term = annotationTerm.term?.id
//                map.user = [annotationTerm.user?.id]
//                def item = results.find { it.term == annotationTerm.term?.id }
//                if (!item) {
//                    results << map
//                } else {
//                    item.user.add(annotationTerm.user.id)
//                }
//            }
//        }
//        results
//    }

    public CytomineDomain buildDomainFromJson(JsonObject json, EntityManager entityManager) {
        UserAnnotation annotation = this;
        annotation.id = json.getJSONAttrLong("id",null);

        annotation.slice = (SliceInstance)json.getJSONAttrDomain(entityManager, "slice", new SliceInstance(), true);
        annotation.image = (ImageInstance)json.getJSONAttrDomain(entityManager, "image", new ImageInstance(), true);
        annotation.project = (Project)json.getJSONAttrDomain(entityManager, "project", new Project(), true);
        annotation.user = (User)json.getJSONAttrDomain(entityManager, "user", new User(), true);

        annotation.geometryCompression = json.getJSONAttrDouble("geometryCompression",0D);

        annotation.created = json.getJSONAttrDate("created");
        annotation.updated = json.getJSONAttrDate("updated");


        if (json.containsKey("location") && json.get("location") instanceof Geometry) {
            annotation.location = (Geometry) json.get("location");
        } else {
            try {
                annotation.location = new WKTReader().read(json.getJSONAttrStr("location"));
            }
            catch (com.vividsolutions.jts.io.ParseException ex) {
                throw new WrongArgumentException(ex.toString());
            }
        }

        if (annotation.location==null) {
            throw new WrongArgumentException("Geometry is null: 0 points");
        }

        if (annotation.location.getNumPoints() < 1) {
            throw new WrongArgumentException("Geometry is empty:" + annotation.location.getNumPoints() + " points");
        }

        return annotation;
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }

    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = AnnotationDomain.getDataFromDomain(domain);
        UserAnnotation annotation = (UserAnnotation)domain;
        returnArray.put("cropURL", UrlApi.getUserAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("smallCropURL", UrlApi.getUserAnnotationCropWithAnnotationIdWithMaxSize(annotation.getId(), 256, "png"));
        returnArray.put("url", UrlApi.getUserAnnotationCropWithAnnotationId(annotation.getId(), "png"));
        returnArray.put("imageURL", UrlApi.getAnnotationURL(annotation.getImage().getProject().getId(), annotation.getImage().getId(), annotation.getId()));
        returnArray.put("reviewed", annotation.hasReviewedAnnotation());
        // TODO returnArray.put("track", domain?.tracksId());

        return returnArray;
    }



}
