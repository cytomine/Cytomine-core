package be.cytomine.service.report;

import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

@Service
@Slf4j
public class SpreadsheetReportService {

    /**
     * Write a CSV report
     *
     * @param  dataArray
     * @return CSV byte array report encoded in base 64
     */
    public byte[] writeCSV(Object[][] dataArray) throws ServerException {
        return writeSpreadsheet(dataArray, ',');
    }

    /**
     * Write a XLS report
     *
     * @param  dataArray
     * @return XLS byte array report encoded in base 64
     */
    public byte[] writeXLS(Object[][] dataArray) throws ServerException {
        return writeSpreadsheet(dataArray, ';');
    }

    /**
     * Write a spreadsheet report with a specific delimiter
     *
     * @param  dataArray
     * @param  delimiter
     * @return Spreadsheet byte array report encoded in base 64
     */
    private byte[] writeSpreadsheet(Object[][] dataArray, char delimiter) throws ServerException {
        log.info(String.format("Generating spread sheet with delimiter: '%s'", delimiter));
        CSVFormat format = CSVFormat.EXCEL.withDelimiter(delimiter);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(output), format)){

            for (Object[] row : dataArray) {
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();

            log.info("Spread sheet file has been generated");
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate spread sheet. Error: %s".format(e.getMessage()));
            throw new ServerException(String.format("Cannot generate spread sheet with params: format=%s. Error: %s",format, e.getMessage()));
        }
    }
}

