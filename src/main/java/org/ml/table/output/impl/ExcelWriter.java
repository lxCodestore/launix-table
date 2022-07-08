package org.ml.table.output.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.ml.table.Table;
import org.ml.table.render.IExcelRenderer;
import org.ml.table.render.RenderingContext;
import org.ml.table.render.impl.SimpleExcelRenderer;
import org.ml.tools.FileType;
import org.ml.tools.excel.ExcelTools;
import org.ml.tools.logging.LoggerFactory;

/**
 *
 * @author osboxes
 */
public class ExcelWriter {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class.getName());
    private final FileType excelFileType = FileType.XLSX;
    private final String sheetName = "Data";
    private Map<Enum, CellStyle> styleMap;
    private Workbook workbook;

    /**
     *
     */
    public ExcelWriter() {

    }

    /**
     *
     * @param workbook
     * @param styleMap
     */
    public ExcelWriter(Workbook workbook, Map<Enum, CellStyle> styleMap) {
        if (styleMap == null) {
            throw new NullPointerException("styleMap may not be null");
        }
        this.workbook = workbook;
        this.styleMap = styleMap;
    }

    /**
     *
     * @param table
     * @param fileName
     * @throws Exception
     */
    public void write(Table table, String fileName) throws Exception {
        if (table == null) {
            throw new NullPointerException("table may not be null");
        }
        if (fileName == null) {
            throw new NullPointerException("fileName may not be null");
        }

        IExcelRenderer renderer = null;
        if (table.getRenderer(RenderingContext.EXCEL) != null) {
            renderer = (IExcelRenderer) table.getRenderer(RenderingContext.EXCEL);
        } else {
            renderer = new SimpleExcelRenderer();
        }

        if (workbook == null) {
            workbook = ExcelTools.getNewWorkbook(excelFileType);
        }
        Sheet sheet = workbook.createSheet(sheetName);

        //.... Populate the excel sheet
        for (int r = table.getRow0(); r <= table.getRowEnd(); r++) {
            Row row = sheet.createRow(r);
            for (int c = table.getCol0(); c <= table.getColEnd(); c++) {

                Cell cell = row.createCell(c);

                //.... If a logical call spans more than 1 row and/or column, we only show the one that is actually visible and hide the others
                if (table.isVisible(r, c)) {

                    org.ml.table.Cell dataCell = table.getCell(r, c);

                    if (styleMap != null) {
                        Enum style = dataCell.getStyle();
                        if (style != null) {
                            if (styleMap.containsKey(style)) {
                                cell.setCellStyle(styleMap.get(style));
                            }
                        }
                    }
                    renderer.renderCell(cell, dataCell);

                    //.... Add a merged region in Excel
                    if (dataCell.getRowSpan() > 1 || dataCell.getColSpan() > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(r, r + dataCell.getRowSpan() - 1, c, c + dataCell.getColSpan() - 1));
                    }
                }
            }
        }

        //.... Check if the base directory exists; create it if not
        File file = new File(fileName);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        //.... Write the data to the file now
        try (FileOutputStream stream = new FileOutputStream(file)) {
            LOGGER.log(Level.INFO, "Writing output file {0}", file);
            workbook.write(stream);
            stream.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "{0}:{1}", new Object[]{ex.getClass(), ex.getMessage()});
        }

    }

    /**
     *
     * @param tables
     * @param fileName
     * @throws Exception
     */
    public void write(Map<String, Table> tables, String fileName) throws Exception {
        if (tables == null) {
            throw new NullPointerException("tables may not be null");
        }
        if (fileName == null) {
            throw new NullPointerException("fileName may not be null");
        }

        if (workbook == null) {
            workbook = ExcelTools.getNewWorkbook(excelFileType);
        }

        IExcelRenderer renderer = null;

        for (String tableName : tables.keySet()) {

            Table table = tables.get(tableName);
            if (table.getRenderer(RenderingContext.EXCEL) != null) {
                renderer = (IExcelRenderer) table.getRenderer(RenderingContext.EXCEL);
            } else {
                renderer = new SimpleExcelRenderer();
            }

            Sheet sheet = workbook.createSheet(tableName);
            LOGGER.log(Level.INFO, "Adding sheet name ''{0}''", tableName);

            //.... Populate the excel sheet
            for (int r = table.getRow0(); r <= table.getRowEnd(); r++) {
                Row row = sheet.createRow(r);
                for (int c = table.getCol0(); c <= table.getColEnd(); c++) {
                    Cell cell = row.createCell(c);
                    org.ml.table.Cell dataCell = table.getCell(r, c);
                    if (styleMap != null) {
                        Enum style = dataCell.getStyle();
                        if (style != null) {
                            if (styleMap.containsKey(style)) {
                                cell.setCellStyle(styleMap.get(style));
                            }
                        }
                    }
                    renderer.renderCell(cell, dataCell);
                }
            }
        }

        //.... Check if the base directory exists; create it if not
        File file = new File(fileName);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        //.... Write the data to the file now
        try (FileOutputStream stream = new FileOutputStream(file)) {
            LOGGER.log(Level.INFO, "Writing output file {0}", file);
            workbook.write(stream);
            stream.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "{0}:{1}", new Object[]{ex.getClass(), ex.getMessage()});
        }

    }

}
