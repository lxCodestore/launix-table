package org.ml.table.render.impl;

import org.ml.table.Cell;
import org.ml.table.output.Style;

/**
 *
 */
public class ClassVelocityRenderer extends SimpleVelocityRenderer {

    /**
     *
     */
    public ClassVelocityRenderer() {
        super();
    }

    /**
     * @param cell
     * @return
     */
    @Override
    public String renderCellStyle(Cell cell) {
        if (cell == null) {
            throw new NullPointerException("cell may not be null");
        }
        if (cell.getStyle() != null) {
            return "class=\"" + ((Style) cell.getStyle()).getDescription() + "\"";
        } else {
            return "";
        }
    }

}
