package datarepresentation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ExcelBridge {

    final static int FIRST_COLUMN = 0;
    final static int SECOND_COLUMN = 1;
    final static int THIRD_COLUMN = 2;
    final static int FOURTH_COLUMN = 3;
    final static int FIFTH_COLUMN = 4;

    private int numberOfEntries = 0;
    private String FILE_NAME = "output" + System.currentTimeMillis() + ".ods";

    public ExcelBridge () throws BiffException{
        try {
            WritableWorkbook workbook = Workbook.createWorkbook(new File(FILE_NAME));
            workbook.createSheet("Sheet1", 0);
            workbook.write();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }

    public void writeList(List<String> listOfTimes, List<String> listOfQueueLevels, List<String> listOfFinishedJobs,
                          List<String> listOfTCRUpdateTimes, List<String> listOfTCR) {
        try {
            Workbook copy = Workbook.getWorkbook(new File(FILE_NAME));
            WritableWorkbook workbook = Workbook.createWorkbook(new File(FILE_NAME), copy);
            WritableSheet sheet = workbook.getSheet(0);
            writeListToColumn(listOfTimes, sheet, FIRST_COLUMN, numberOfEntries);
            writeListToColumn(listOfQueueLevels, sheet, SECOND_COLUMN, numberOfEntries);
            writeListToColumn(listOfFinishedJobs, sheet, THIRD_COLUMN, numberOfEntries);
            writeListToColumn(listOfTCRUpdateTimes, sheet, FOURTH_COLUMN, numberOfEntries);
            writeListToColumn(listOfTCR, sheet, FIFTH_COLUMN, numberOfEntries);
            copy.close();
            workbook.write();
            workbook.close();
        } catch (WriteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    private void writeListToColumn(List<String> listOfTimes, WritableSheet sheet, int column, int startOnRow) throws WriteException {
        Label label;
        for(String time : listOfTimes) {
            label = new Label(column, startOnRow, time);
            sheet.addCell(label);
            ++startOnRow;
        }
    }
}
