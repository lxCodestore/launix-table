/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ml.table.render;

import org.ml.table.Cell;

/**
 *
 * @author osboxes
 */
public abstract class AbstractVelocityRenderer implements IVelocityRenderer {

    /**
     *
     * @param cell
     * @return
     */
    @Override
    public String renderCellStyle(Cell cell) {
        if (cell == null) {
            throw new NullPointerException("cell may not be null");
        }
        if (cell.getStyle() != null) {
            return "class=\"" + cell.getStyle() + "\"";
        } else {
            return "";
        }
    }
}
