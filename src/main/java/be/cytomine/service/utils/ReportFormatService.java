package be.cytomine.service.utils;

import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.service.dto.AnnotationResult;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.ontology.TermService;
import be.cytomine.service.ontology.UserAnnotationService;
import be.cytomine.service.report.ReportColumn;
import be.cytomine.service.security.SecUserService;
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

    /**
     * Transform a List<Map<String,Object>> into a simple Object[][]
     * with headers corresponding to given columns.
     *
     * @param  columns
     * @param  annotations
     * @return Object[][]
     */
    public Object[][] formatDataForReport(List<ReportColumn> columns, List<AnnotationResult> annotations){
        Object[] headers = getColumnHeaders(columns);
        Object[][] reportFormat = new Object[annotations.size() + 1][headers.length];
        reportFormat[0] = headers;

        for(int i = 0; i < annotations.size(); i++){
            Map<String, Object> annotation = annotations.get(i);

            for(int j = 0; j < headers.length; j++){
                Object value = annotation.get(headers[j]);
                UserAnnotation userAnnotation = userAnnotationService.get((long) annotation.get("id"));
                DecimalFormat df = new DecimalFormat("0.00");

                switch (headers[j].toString()){
                    case "user":
                        if(value != null){
                            value = secUserService.findUser((long) value).get().humanUsername();
                        }
                        break;
                    case "filename":
                        value = imageInstanceService.find((long) annotation.get("image")).get().getBlindInstanceFilename();
                        break;
                    case "term":
                        value = String.join("- ", getTermsName(value));
                        break;
                    case "area":
                        value = df.format(userAnnotation.getArea());
                        break;
                    case "perimeter":
                        value = df.format(userAnnotation.getPerimeter());
                        break;
                    case "X":
                        value = df.format(userAnnotation.getCentroid().getX());
                        break;
                    case "Y":
                        value = df.format(userAnnotation.getCentroid().getY());
                        break;
                    default:
                        break;
                }
                if(value == null){
                    value = "";
                }
                reportFormat[i + 1][j] = value;
            }
        }
        headerPropertyToTitle(columns, reportFormat);
        return reportFormat;
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
