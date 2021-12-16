package be.cytomine.service.utils;

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.dto.Kmeans;
import be.cytomine.service.ontology.AnnotationIndexService;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class KmeansGeometryService {

    private final AnnotationIndexService annotationIndexService;

    private final EntityManager entityManager;

    public static final int FULL = 3;
    public static final int KMEANSFULL = 2;
    public static final int KMEANSSOFT = 1;

    public static final int ANNOTATIONSIZE1 = 0;
    public static final int ANNOTATIONSIZE2 = 100;
    public static final int ANNOTATIONSIZE3 = 2000;
    public static final int ANNOTATIONSIZE4 = 10000;
    public static final int ANNOTATIONSIZE5 = 100000;


    public static final Map<Integer, Map<Integer, Integer>> rules =
        Map.of(
                100, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, KMEANSFULL, (int)ANNOTATIONSIZE4, KMEANSFULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                75, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, KMEANSFULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                50, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, KMEANSFULL),
                25, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, FULL),
                0, Map.of((int)ANNOTATIONSIZE1, FULL, (int)ANNOTATIONSIZE2, FULL, (int)ANNOTATIONSIZE3, FULL, (int)ANNOTATIONSIZE4, FULL, (int)ANNOTATIONSIZE5, FULL)

        );


    public List<Kmeans> doKeamsFullRequest(String request) {
        String requestKmeans = "SELECT kmeans, count(*), st_astext(ST_ConvexHull(ST_Collect(location))) \n" +
                "FROM (\n" + request +"\n" +") AS ksub\n" +
                "GROUP BY kmeans\n" +
                "ORDER BY kmeans;";
        return selectAnnotationLightKmeans(requestKmeans);
    }

    public List<Kmeans> doKeamsSoftRequest(String request) {
        String requestKmeans = "SELECT kmeans, count(*), st_astext(ST_Centroid(ST_Collect(location))) \n" +
                "FROM (\n" + request +"\n" +") AS ksub\n" +
                "GROUP BY kmeans\n" +
                "ORDER BY kmeans;";
        return selectAnnotationLightKmeans(requestKmeans);
    }

    private List<Kmeans> selectAnnotationLightKmeans(String request) {
        List<Kmeans> data = new ArrayList<>();

        double max = 1;

        Query nativeQuery = entityManager.createNativeQuery(request, Tuple.class);
        List<Tuple> resultList = nativeQuery.getResultList();
        for (Tuple tuple : resultList) {
            Kmeans kmeans = new Kmeans();
            kmeans.setId((Long)tuple.get(0));
            kmeans.setCount((Long)tuple.get(1));
            kmeans.setLocation((String) tuple.get(2));
            if(kmeans.getCount()>max) {
                max = kmeans.getCount();
            }
        }

        for (Kmeans datum : data) {
            datum.setRatio(((double)datum.getCount()/max));
        }
        return data;
    }

    public int mustBeReduce(Long slice, Long user, String bbox) {
        SliceInstance sliceInstance = entityManager.find(SliceInstance.class, slice);
        SecUser secUser = entityManager.find(SecUser.class, user);
        try {
            return mustBeReduce(sliceInstance, secUser,new WKTReader().read(bbox));
        } catch (ParseException e) {
            throw new WrongArgumentException("Annotation location cannot be converted to geometry: " + bbox);
        }
    }


    public int mustBeReduce(SliceInstance slice, SecUser user, Geometry bbox) {
        if (slice.getImage().getBaseImage().getWidth()==null) {
            return  FULL;
        }

        double imageWidth = slice.getImage().getBaseImage().getWidth();
        double bboxWidth = bbox.getEnvelopeInternal().getWidth();

        double ratio = bboxWidth/imageWidth;

        int ratio25 = ((int)((ratio/25d)*100))*25;

        Map<Integer, Integer> ruleLine = rules.get(Math.min(ratio25,100));

        long numberOfAnnotation = Math.max(0, annotationIndexService.count(slice,user));

        Integer rule = getRuleForNumberOfAnnotations(numberOfAnnotation, ruleLine);
        return rule;
    }

    public Integer getRuleForNumberOfAnnotations(long annotations, Map<Integer, Integer> ruleLine) {
        if (annotations >= ANNOTATIONSIZE5) return ruleLine.get(ANNOTATIONSIZE5);
        if (annotations >= ANNOTATIONSIZE4) return ruleLine.get(ANNOTATIONSIZE4);
        if (annotations >= ANNOTATIONSIZE3) return ruleLine.get(ANNOTATIONSIZE3);
        if (annotations >= ANNOTATIONSIZE2) return ruleLine.get(ANNOTATIONSIZE2);
        if (annotations >= ANNOTATIONSIZE1) return ruleLine.get(ANNOTATIONSIZE1);
        throw new WrongArgumentException("Cannot find rule for annotations count = " + annotations);
    }



}
