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
     * Write a spreadsheet report (xls / csv)
     *
     * @param  dataArray
     * @return Spreadsheet byte array report encoded in base 64
     */
    public byte[] writeSpreadsheet(Object[][] dataArray) throws ServerException {
        log.info(String.format("Generating spread sheet with delimiter: '%s'", ";"));
        CSVFormat format = CSVFormat.EXCEL.withDelimiter(';');
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

