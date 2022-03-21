package be.cytomine.service.report;

import be.cytomine.exceptions.ServerException;
import be.cytomine.service.dto.AnnotationResult;
import be.cytomine.service.utils.ReportFormatService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ReportService {

    public static final boolean HAS_PAGINATION = true;
    public static final boolean HAS_HEADER = true;
    public static final List<ReportColumn> REPORT_COLUMNS = new ArrayList<>(){{
        add(new ReportColumn("id", "Id", (float) 0.05));
        add(new ReportColumn("area", "Area (microns²)", (float) 0.10));
        add(new ReportColumn("perimeter", "Perimeter (mm)", (float) 0.10));
        add(new ReportColumn("X", "X", (float) 0.05));
        add(new ReportColumn("Y", "Y", (float) 0.05));
        add(new ReportColumn("image", "Image Id", (float) 0.07));
        add(new ReportColumn("filename", "Image Filename", (float) 0.10));
        add(new ReportColumn("user", "User", (float) 0.05));
        add(new ReportColumn("term", "Term", (float) 0.05));
        add(new ReportColumn("cropURL", "View annotation picture", (float) 0.19));
        add(new ReportColumn("imageURL", "View annotation on image", (float) 0.19));
    }};

    private final PDFReportService pdfReportService;

    private final SpreadsheetReportService spreadsheetReportService;

    private final ReportFormatService reportFormatService;

    public byte[] generateReport(String projectName, Set<String> terms, Set<String> users, List<AnnotationResult> annotations, String format) throws ServerException {

        Object[][] data = reportFormatService.formatDataForReport(REPORT_COLUMNS, annotations);
        float[] columnWidth = reportFormatService.getColumnWidth(REPORT_COLUMNS);

        return generateReport(getReportTitle(projectName, terms, users), data, columnWidth, format);
    }

    public byte[] generateReport(String title, Object[][] data, float[] columnWidth, String format) throws ServerException {
        switch (format){
            case "csv":
                return spreadsheetReportService.writeCSV(data);
            case "xls":
                return spreadsheetReportService.writeXLS(data);
            case "pdf":
                return pdfReportService.writePDF(data, title, columnWidth, HAS_PAGINATION, HAS_HEADER);
            default :
                log.error("Format should be one of these types: 'pdf', 'csv' or 'xls'.");
                throw new ServerException("Failed to generate report, invalid format. Format should be one of these types: 'pdf', 'csv' or 'xls'.");
        }
    }

    private String getReportTitle(String projectName, Set<String> terms, Set<String> users) {
        DateFormat DFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault());
        return "Annotations in " + projectName + " created by " + String.join(" or ", users) + " and associated with " + String.join(" or ", terms) + " @ " + DFormat.format(new Date());
    }

    public String getReportFileName(String format, Long projectId){
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String datePrefix = simpleFormat.format(new Date());

        return datePrefix + "_annotations_project" + projectId + "." + format;
    }
}
