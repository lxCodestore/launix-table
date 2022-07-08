package org.ml.table.render;

import org.ml.table.Cell;

/**
 * @author Dr. Matthias Laux
 */
public interface IVelocityRenderer extends IRenderer {

    /**
     * This returns a string representing the cell content that can be embedded in a HTML page
     *
     * @param cell
     * @return
     */
    String renderCell(Cell cell);

    /**
     * This returns a string with styling information, for example a class="name" string
     * @param cell
     * @return
     */
    String renderCellStyle(Cell cell);
}
