package be.cytomine.service.utils;

import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.ReviewedAnnotation;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.ReviewedAnnotationService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.report.ReportColumn;
import be.cytomine.service.security.SecUserService;
import liquibase.pro.packaged.S;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ReportFormatService {

    private final SecUserService secUserService;

    private final ImageInstanceService imageInstanceService;

    private final TermService termService;

    private final UserAnnotationService userAnnotationService;

    private final ReviewedAnnotationService reviewedAnnotationService;

    /**
     * Transform a List<Map<String,Object>> into a simple Object[][]
     * with headers corresponding to given columns.
     *
     * @param  columns
     * @param  data
     * @param  isAnnotation
     * @return Object[][]
     */
    public Object[][] formatDataForReport(List<ReportColumn> columns, List<Map<String, Object>> data, boolean isAnnotation, boolean isReview){
        Object[] headers = getColumnHeaders(columns);
        Object[][] report = new Object[data.size() + 1][headers.length];
        report[0] = headers;

        for(int i = 0; i < data.size(); i++){
            Map<String, Object> element = data.get(i);
            for(int j = 0; j < headers.length; j++){
                Object value = element.get(headers[j]);

                if(isAnnotation){
                    value = formatAnnotationForReport(value, element, headers[j].toString(), isReview);
                }
                if(value == null){
                    value = "";
                }
                report[i + 1][j] = value;
            }
        }
        headerPropertyToTitle(columns, report);
        return report;
    }

    /**
     * Get value for annotation report
     *
     * @param  value
     * @param  annotation
     * @param  header
     * @return String
     */
    public Object formatAnnotationForReport(Object value, Map<String, Object> annotation, String header, boolean isReview){
        AnnotationDomain userAnnotation;
        Long annotationId = (long) annotation.get("id");
        if(isReview){
            userAnnotation = reviewedAnnotationService.get(annotationId);
        } else{
            userAnnotation = userAnnotationService.get(annotationId);
        }
        DecimalFormat df = new DecimalFormat("0.00");

        switch (header){
            case "user":
                if(value != null){
                    value = secUserService.findUser((long) value).get().humanUsername();
                }
                return value;
            case "filename":
                return imageInstanceService.find((long) annotation.get("image")).get().getBlindInstanceFilename();
            case "term":
                return String.join("- ", getTermsName(value));
            case "area":
                return df.format(userAnnotation.getArea());
            case "perimeter":
                return df.format(userAnnotation.getPerimeter());
            case "X":
                return df.format(userAnnotation.getCentroid().getX());
            case "Y":
                return df.format(userAnnotation.getCentroid().getY());
            default:
                return value;
        }
    }

    /**
     * @param  value Object representing the list of term ids
     * @return String terms name
     */
    public String[] getTermsName(Object value){
        String[] termsId = value.toString()
                .replace("[", "")
                .replace("]", "")
                .split(",");
        String[] termNames = new String[termsId.length];
        if(!termsId[0].trim().isEmpty()){
            int k = 0;
            for (String termId:termsId) {
                termNames[k] = termService.find(Long.parseLong(termId.trim())).get().getName();
                k++;
            }
        }
        if(termsId[0].trim().isEmpty()){
            return new String[] {""};
        }else{
            return termNames;
        }
    }

    /**
     * Get column width from a list of ReportColumn
     *
     * @param  columns list of ReportColumn
     * @return ReportColumn width
     */
    public float[] getColumnWidth(List<ReportColumn> columns){
        float[] columnWidth = new float[columns.size()];
        for(int i=0; i<columns.size(); i++){
            columnWidth[i] = columns.get(i).columnWidth;
        }
        return columnWidth;
    }

    /**
     * Get report column headers from a list of ReportColumn
     *
     * @param  columns list of ReportColumn
     * @return ReportColumn headers
     */
    public static Object[] getColumnHeaders(List<ReportColumn> columns){
        return columns.stream().map(reportColumn -> reportColumn.property).toArray();
    }

    /**
     * Convert data headers (actually property values of columns list)
     * to columns titles.
     *
     * @param  columns
     * @param  data
     * @return
     */
    private static void headerPropertyToTitle(List<ReportColumn> columns, Object[][] data){
        for(int i=0; i<data[0].length; i++){
            data[0][i] = columns.get(i).title;
        }
    }
}
