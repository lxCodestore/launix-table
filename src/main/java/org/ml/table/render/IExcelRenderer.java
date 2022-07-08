package org.ml.table.render;

import org.apache.poi.ss.usermodel.Cell;

/**
 * @author Dr. Matthias Laux
 */
public interface IExcelRenderer extends IRenderer {

    /**
     * @param cell     The cell in the workbook to be populated
     * @param dataCell The cell in the Table model containing the data
     */
    void renderCell(Cell cell, org.ml.table.Cell dataCell);
}
