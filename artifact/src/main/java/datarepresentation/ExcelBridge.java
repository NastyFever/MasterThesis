package datarepresentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    private void writeEntryToRow(Entry entry, WritableSheet sheet, int row) throws WriteException {
        Label label;
        label = new Label(FIRST_COLUMN, row, entry.time);
        sheet.addCell(label);
        label = new Label(SECOND_COLUMN, row, entry.activeChannels);
        sheet.addCell(label);
        label = new Label(THIRD_COLUMN, row, entry.finishedJobs);
        sheet.addCell(label);
        label = new Label(FOURTH_COLUMN, row, entry.comeBackRate);
        sheet.addCell(label);
        label = new Label(FIFTH_COLUMN, row, entry.jobTime);
        sheet.addCell(label);
    }

    public void writeEntries(ArrayList<Entry> entries) {
        try {
            Workbook copy = Workbook.getWorkbook(new File(FILE_NAME));
            WritableWorkbook workbook = Workbook.createWorkbook(new File(FILE_NAME), copy);
            WritableSheet sheet = workbook.getSheet(0);
            for(Entry entry : entries) {
                writeEntryToRow(entry, sheet, numberOfEntries);
                ++numberOfEntries;
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
