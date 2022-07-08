package org.ml.table;

/**
 * An enum constant for the different supported boundary conditions.
 */
public enum BoundaryCondition {

    /**
     * Any cell location outside of the predefined area leads to an exception.
     * This is the default setting
     */
    FIXED,
    /**
     * Cells are truncated when necessary
     */
    CLIPPING,
    /**
     * The table grows when necessary to accommodate additional columns/rows
     */
    GROW
}
