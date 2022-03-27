package be.cytomine.service.report;

import be.cytomine.CytomineCoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_SUPER_ADMIN", username = "superadmin")
@Transactional
public class SpreadsheetReportServiceTests {

    @Autowired
    SpreadsheetReportService spreadsheetReportService;

    Object [][] validData = {
            {"ID", "Area (microns²)", "Perimeter (mm)", "X", "Y", "Image Filename", "View annotation picture", "View annotation on image"},
            {'2', (float) 23298.5267702416, 0.614768015861511, 736, true, (long) 6836067, "https://...", "https://...", "0.614768015861511", "736", "1978.375", "6836067"},
    };
    String validCsvDataResult = "ID,Area (microns²),Perimeter (mm),X,Y,Image Filename,View annotation picture,View annotation on image\r\n2,23298.527,0.614768015861511,736,true,6836067,https://...,https://...,0.614768015861511,736,1978.375,6836067\r\n";

    Object [][] csvDelimiterInData = {{"ID", null, "Perimeter , (mm)"},};
    String csvDelimiterInDataResult = "ID,,\"Perimeter , (mm)\"\r\n";
    Object [][] xlsDelimiterInData = {{"ID", null, "Perimeter ; (mm)"},};
    String xlsDelimiterInDataResult = "ID;;\"Perimeter ; (mm)\"\r\n";

    Object [][] nullData = {{"ID", null, "Perimeter (mm)"},};
    String nullCsvDataResult = "ID,,Perimeter (mm)\r\n";

    Object [][] emptyData = {{}};
    String emptyCsvDataResult = "\r\n";

    @Test
    public void generate_spreadsheets_with_valid_data_works() {
        String validXlsDataResult = validCsvDataResult.replace(',', ';');
        assertEquals(createXLSSpreadsheet(validData), validXlsDataResult);
        assertEquals(createCSVSpreadsheet(validData), validCsvDataResult);
    }

    @Test
    public void generate_spreadsheets_with_delimiter_in_data_works() {
        assertEquals(createXLSSpreadsheet(xlsDelimiterInData), xlsDelimiterInDataResult);
        assertEquals(createCSVSpreadsheet(csvDelimiterInData), csvDelimiterInDataResult);
    }

    @Test
    public void null_data_return_empty_string() {
        assertEquals(createCSVSpreadsheet(nullData), nullCsvDataResult);
    }

    @Test
    public void empty_data_array_return_empty_string() {
        assertEquals(createCSVSpreadsheet(emptyData), emptyCsvDataResult);
    }

    private String createCSVSpreadsheet(Object[][] data) {
        byte[] csvByteArray = spreadsheetReportService.writeCSV(data);
        return new String(csvByteArray);
    }

    private String createXLSSpreadsheet(Object[][] data) {
        byte[] xlsByteArray = spreadsheetReportService.writeXLS(data);
        return new String(xlsByteArray);
    }

}
