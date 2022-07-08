package org.ml.table.render.impl;

import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Cell;
import org.ml.table.content.EmailContent;
import org.ml.table.content.UrlAnchor;
import org.ml.table.content.UrlContent;
import org.ml.table.render.IExcelRenderer;

/**
 * A renderer with some (hopefully) reasonable default behavior to render a Cell
 * for an Excel sheet. Note that this renders only the anonymous content in the
 * Cell object. For more complex scenarios involving multiple named content
 * objects, a specialized rendered needs to be implemented
 *
 * @author mlaux
 */
public class SimpleExcelRenderer implements IExcelRenderer {

    /**
     *
     */
    public SimpleExcelRenderer() {

    }

    /**
     * @param excelCell
     * @param cell
     */
    @Override
    public void renderCell(Cell excelCell, org.ml.table.Cell cell) {
        if (excelCell == null) {
            throw new NullPointerException("excelCell may not be null");
        }
        if (cell == null) {
            throw new NullPointerException("cell may not be null");
        }

        try {

            if (cell.getContent() != null) {

                Object content = cell.getContent();

                if (content instanceof Integer) {
                    excelCell.setCellValue((Integer) content);
                } else if (content instanceof Long) {
                    excelCell.setCellValue((Long) content);
                } else if (content instanceof Short) {
                    excelCell.setCellValue((Short) content);
                } else if (content instanceof Float) {
                    excelCell.setCellValue((Float) content);
                } else if (content instanceof Double) {
                    excelCell.setCellValue((Double) content);
                } else if (content instanceof Boolean) {
                    excelCell.setCellValue((Boolean) content);
                } else if (content instanceof LocalDate) {
                    excelCell.setCellValue((LocalDate) content);
                } else if (content instanceof String) {
                    excelCell.setCellValue((String) content);
                } else if (content instanceof EmailContent) {
                    String address = ((EmailContent) content).getAddress();
                    excelCell.setCellValue("mailto:" + address);
                } else if (content instanceof UrlContent) {
                    UrlContent urlContent = (UrlContent) content;
                    excelCell.setCellValue(urlContent.getText());
                } else if (content instanceof UrlAnchor) {
                    UrlAnchor urlAnchor = (UrlAnchor) content;
                    excelCell.setCellValue(urlAnchor.getText());
                } else {
                    excelCell.setCellValue(content.toString());
                }

            }

        } catch (ClassCastException ex) {
            throw new UnsupportedOperationException(ex.getMessage() + " / Content value : " + cell.getContent());
        }
    }
}
