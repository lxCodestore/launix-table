/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ml.table.output;

/**
 *
 * @author osboxes
 */
public enum Style {

    TABLE_HEADER("tableHeader"),
    TABLE_FOOTER("tableFooter"),
    TABLE_SUB_HEADER("tableSubHeader"),
    CELL_OK("cellOk"),
    CELL_WARNING("cellWarning"),
    CELL_ERROR("cellError"),
    CELL_HIGHLIGHT_1("cellHighlight1"),
    CELL_HIGHLIGHT_2("cellHighlight2"),
    CELL_HIGHLIGHT_3("cellHighlight3"),
    CELL_HIGHLIGHT_4("cellHighlight4"),
    CELL_STANDARD("cellStandard");

    String description;

    /**
     *
     * @param description
     */
    Style(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }
}
