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

    public void writeList(List<String> listOfQueueLevels, List<String> listOfFinishedJobs) {
        try {
            Workbook copy = Workbook.getWorkbook(new File(FILE_NAME));
            WritableWorkbook workbook = Workbook.createWorkbook(new File(FILE_NAME), copy);
            WritableSheet sheet = workbook.getSheet(0);
            Label label1;
            int oldNumberOftries = numberOfEntries;
            for(String element : listOfQueueLevels) {
                label1 = new Label(FIRST_COLUMN, numberOfEntries, element);
                sheet.addCell(label1);
                numberOfEntries++;
            }
            numberOfEntries = oldNumberOftries;
            Label label2;
            for(String element : listOfFinishedJobs) {
                label2 = new Label(SECOND_COLUMN, numberOfEntries, element);
                sheet.addCell(label2);
                numberOfEntries++;
            }
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
}
