package org.ml.table;

import org.ml.table.render.IRenderer;
import org.ml.table.render.RenderingContext;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the central class for handling data for tables. Effectively,
 * instances of this class are Java object representations of the HTML table
 * structure, and the goal is that instances of this class hold all the data
 * that is required for the table.
 * <p>
 * A table has a logical row and column numbering that starts at row
 * <code>row0</code> and column <code>col0</code> in the upper left corner.
 * Indices run from <code>row0</code> to <code>rowNumber - 1</code> and from
 * <code>col0</code> to <code>colNumber - 1</code>, respectively.
 */
public class Table {

    /**
     *
     */
    public static final int DEFAULT_TABLE_SIZE = 50;

    /**
     *
     */
    public static final String TAG_EMPTY_VALUE = "";
    /**
     *
     */
    public static final Cell DEFAULT_CELL = new Cell();

    private Cell[][] cells;
    private int rowNumber = 0;
    private Map<Direction, Map<Integer, Map<String, String>>> tags = null;
    private int row0 = 0;
    private int rowEnd = 0;
    private int colNumber = 0;
    private int col0 = 0;
    private int colEnd = 0;
    private boolean[][] visible;
    private boolean[][] def;          // Mark whether a cell contains the default cell
    private final Map<IBoundaryLocation, BoundaryCondition> boundaryConditions = new HashMap<>();
    private final Map<RenderingContext, IRenderer> renderers = new HashMap<>();

    /**
     * @since 1.1
     */
    public enum Direction {

        /**
         * @since 1.1
         */
        ROW,
        /**
         * @since 1.1
         */
        COLUMN
    }

    /**
     * Constructor for a table where the logical indexes for rows and columns
     * start at 0
     *
     * @param rowNumber Number of rows for the table
     * @param colNumber Number of columns for the table
     */
    public Table(int rowNumber, int colNumber) {
        this(0, 0, rowNumber, colNumber);
    }

    /**
     * Constructor for a table where the logical indexes for rows and columns
     * start at 0 and where the number of rows and columns is initially set by
     * DEFAULT_TABLE_SIZE
     */
    public Table() {
        this(0, 0, DEFAULT_TABLE_SIZE, DEFAULT_TABLE_SIZE);
    }

    /**
     * Constructor for a table
     *
     * @param row0 First logical index at upper edge of the table
     * @param col0 First logical index at left edge of the table
     * @param rowNumber Number of rows for the table
     * @param colNumber Number of columns for the table
     */
    public Table(int row0, int col0, int rowNumber, int colNumber) {
        if (rowNumber < 1) {
            throw new IllegalArgumentException("rowNumber must be larger than 0");
        }
        if (colNumber < 1) {
            throw new IllegalArgumentException("colNumber must be larger than 0");
        }

        this.rowNumber = rowNumber;
        this.colNumber = colNumber;
        this.row0 = row0;
        this.col0 = col0;

        rowEnd = row0 + rowNumber - 1;  // Helper
        colEnd = col0 + colNumber - 1;

        cells = new Cell[rowNumber][colNumber];
        visible = new boolean[rowNumber][colNumber];
        def = new boolean[rowNumber][colNumber];

        for (int r = 0; r < rowNumber; r++) {
            for (int c = 0; c < colNumber; c++) {
                visible[r][c] = true;
                def[r][c] = true;
                cells[r][c] = DEFAULT_CELL;
            }
        }

        //.... The default boundary conditions
        boundaryConditions.put(ColumnLocation.LEFT, BoundaryCondition.FIXED);
        boundaryConditions.put(ColumnLocation.RIGHT, BoundaryCondition.FIXED);
        boundaryConditions.put(RowLocation.TOP, BoundaryCondition.FIXED);
        boundaryConditions.put(RowLocation.BOTTOM, BoundaryCondition.FIXED);

    }

    /**
     * Attach a tag (i. e. an arbitrary string) to either a row or a column.
     * Tags can be used to identify entire rows or columns across e. g.
     * coalescing or growth operations.
     *
     * @param tagLocation Either rows or columns can be tagged
     * @param logicalIndex The logical index of either the row or the column to
     * tag
     * @param tagName The actual tag name
     * @param tagValue The actual tag value
     * @since 1.1
     */
    public void addTag(Direction tagLocation, int logicalIndex, String tagName, String tagValue) {
        if (tagName == null) {
            throw new IllegalArgumentException("tag may not be null");
        }
        if (tagValue == null) {
            throw new IllegalArgumentException("tagValue may not be null");
        }
        checkLogicalIndex(tagLocation, logicalIndex);

        //.... Store the tag
        if (tags == null) {
            tags = new HashMap<>();
        }
        if (!tags.containsKey(tagLocation)) {
            tags.put(tagLocation, new HashMap<>());
        }
        if (!tags.get(tagLocation).containsKey(logicalIndex)) {
            tags.get(tagLocation).put(logicalIndex, new HashMap<>());
        }
        tags.get(tagLocation).get(logicalIndex).put(tagName, tagValue);
    }

    /**
     * Attach an empty-valued tag. This is effectively a marker tag.
     *
     * @param tagLocation
     * @param logicalIndex
     * @param tagName
     * @since 1.1
     */
    public void addTag(Direction tagLocation, int logicalIndex, String tagName) {
        addTag(tagLocation, logicalIndex, tagName, TAG_EMPTY_VALUE);
    }

    /**
     * Check for the presence of a tag
     *
     * @param tagLocation Either rows or columns can be tagged
     * @param logicalIndex The logical index of either the row or the column to
     * check for the tag
     * @param tagName The actual tag value
     * @return <code>true</code> if the tag is present, else <code>false</code>
     * @since 1.1
     */
    public boolean hasTag(Direction tagLocation, int logicalIndex, String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("tag may not be null");
        }
        checkLogicalIndex(tagLocation, logicalIndex);

        //.... Search for the tag
        if (tags == null || !tags.containsKey(tagLocation) || !tags.get(tagLocation).containsKey(logicalIndex)) {
            return false;
        }
        return tags.get(tagLocation).get(logicalIndex).containsKey(tagName);
    }

    /**
     * @param tagLocation
     * @param logicalIndex
     * @param tagName
     * @return The string value associated with the tag, or <code>null</code> if
     * the tag is not present
     * @since 1.1
     */
    public String getTag(Direction tagLocation, int logicalIndex, String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("tag may not be null");
        }
        checkLogicalIndex(tagLocation, logicalIndex);

        //.... Search for the tag
        if (tags == null || !tags.containsKey(tagLocation) || !tags.get(tagLocation).containsKey(logicalIndex)) {
            return null;
        }
        return tags.get(tagLocation).get(logicalIndex).get(tagName);
    }

    /**
     * @param tagLocation
     * @param logicalIndex
     */
    private void checkLogicalIndex(Direction tagLocation, int logicalIndex) {
        if (tagLocation == null) {
            throw new IllegalArgumentException("tagLocation may not be null");
        }
        switch (tagLocation) {
            case ROW:
                int r = logicalIndex - row0;
                if (r >= rowNumber || r < 0) {
                    throw new IllegalArgumentException("row must be between " + row0 + " and " + getRowEnd());
                }
                break;
            case COLUMN:
                int c = logicalIndex - col0;
                if (c >= colNumber || c < 0) {
                    throw new IllegalArgumentException("col must be between " + col0 + " and " + getColEnd());
                }
        }
    }

    /**
     * Coalesce cells containing the default cell into one common cell. This is
     * useful to simplify the HTML table structure e. g. after all relevant data
     * has been added to a table. Coalescing can either be along rows or along
     * columns. For example, when coalescing along rows, each row of the table
     * will be checked for consecutive blocks of cells containing the default
     * cell. These blocks will be replaced by one cell covering them all.
     * <p>
     *
     * @param internalLocation The location along which to coalesce. Can either
     * be along rows or along columns
     * @return <code>true</code> if cells were coalesced
     */
    public boolean coalesce(InternalLocation internalLocation) {
        if (internalLocation == null) {
            throw new IllegalArgumentException("internalLocation may not be null");
        }

        Cell cell;
        boolean coalesced = false;

        switch (internalLocation) {

            case ROW:

                for (int r = 0; r < rowNumber; r++) {
                    int cstart = 0;
                    int c = 0;
                    boolean scanning = false;
                    while (c < colNumber) {
                        if (isDefaultCell(r + row0, c + col0)) {
                            if (!scanning) {
                                cstart = c;
                                scanning = true;
                            }
                        } else if (scanning) {

                            cell = new Cell(1, c - cstart);

                            setCell(cell, r + row0, cstart + col0);
                            scanning = false;
                            coalesced = true;

                        }
                        c++;
                    }

                    //.... Final column
                    if (scanning) {

                        cell = new Cell(1, c - cstart);

                        setCell(cell, r + row0, cstart + col0);
                        coalesced = true;

                    }

                }

                break;

            case COLUMN:

                for (int c = 0; c < colNumber; c++) {
                    int rstart = 0;
                    int r = 0;
                    boolean scanning = false;
                    while (r < rowNumber) {
                        if (isDefaultCell(r + row0, c + col0)) {
                            if (!scanning) {
                                rstart = r;
                                scanning = true;
                            }
                        } else if (scanning) {

                            cell = new Cell(r - rstart, 1);

                            setCell(cell, rstart + row0, c + col0);
                            scanning = false;
                            coalesced = true;

                        }
                        r++;
                    }

                    //.... Final column
                    if (scanning) {

                        cell = new Cell(r - rstart, 1);

                        setCell(cell, rstart + row0, c + col0);
                        coalesced = true;

                    }

                }

        }

        return coalesced;

    }

    /**
     * Retrieve the boundary condition at the given boundary location
     *
     * @param boundaryLocation The boundary location where the information is to
     * be retrieved
     * @return The boundary condition at the desired boundary location
     */
    public BoundaryCondition getBoundaryCondition(IBoundaryLocation boundaryLocation) {
        if (boundaryLocation == null) {
            throw new IllegalArgumentException("location may not be null");
        }
        return boundaryConditions.get(boundaryLocation);
    }

    /**
     * Add columns to the table either at the left or at the right end.
     * <p>
     * If columns are inserted at the left edge of the table, the logical start
     * index for the columns is reduced by <code>count</code>. If columns are
     * inserted at the right edge of the table, the logical end index of the
     * columns is increased by <code>count</code>.
     *
     * @param location Whether to add the columns at the left or the right edge
     * @param count The number of columns to add
     */
    public void addColumns(ColumnLocation location, int count) {
        if (location == null) {
            throw new IllegalArgumentException("location may not be null");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        Cell[][] cells_new = new Cell[rowNumber][colNumber + count];
        boolean[][] visible_new = new boolean[rowNumber][colNumber + count];
        boolean[][] def_new = new boolean[rowNumber][colNumber + count];

        switch (location) {

            case LEFT:
                for (int r = 0; r < rowNumber; r++) {
                    for (int c = 0; c < count; c++) {
                        visible_new[r][c] = true;
                        def_new[r][c] = true;
                        cells_new[r][c] = DEFAULT_CELL;
                    }
                    for (int c = 0; c < colNumber; c++) {
                        visible_new[r][c + count] = visible[r][c];
                        def_new[r][c + count] = def[r][c];
                        cells_new[r][c + count] = cells[r][c];
                    }
                }
                col0 -= count;
                break;

            case RIGHT:
                for (int r = 0; r < rowNumber; r++) {
                    for (int c = 0; c < colNumber; c++) {
                        visible_new[r][c] = visible[r][c];
                        def_new[r][c] = def[r][c];
                        cells_new[r][c] = cells[r][c];
                    }
                    for (int c = colNumber; c < count + colNumber; c++) {
                        visible_new[r][c] = true;
                        def_new[r][c] = true;
                        cells_new[r][c] = DEFAULT_CELL;
                    }
                }
                colEnd += count;
                break;

        }

        visible = visible_new;
        def = def_new;
        cells = cells_new;

        colNumber += count;

    }

    /**
     * Add one column to the table either at the left or at the right end.
     * <p>
     * This is a convenience method for adding just one column. See
     * {@link #addColumns(ColumnLocation, int)} for more details.
     *
     * @param location Whether to add the column at the left or the right edge
     */
    public void addColumn(ColumnLocation location) {
        addColumns(location, 1);
    }

    /**
     * Add one row to the table either at the top or at the bottom end.
     * <p>
     * This is a convenience method for adding just one row. See
     * {@link #addRows(RowLocation, int)} for more details.
     *
     * @param location Whether to add the row at the top or the bottom edge
     */
    public void addRow(RowLocation location) {
        addRows(location, 1);
    }

    /**
     * Add rows to the table either at the top or at the bottom end.
     * <p>
     * If rows are inserted at the top edge of the table, the logical start
     * index for the rows is reduced by <code>count</code>. If rows are inserted
     * at the bottom edge of the table, the logical end index of the rows is
     * increased by <code>count</code>.
     *
     * @param location Whether to add the rows at the top or the bottom edge
     * @param count The number of rows to add
     */
    public void addRows(RowLocation location, int count) {
        if (location == null) {
            throw new IllegalArgumentException("location may not be null");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        Cell[][] cells_new = new Cell[rowNumber + count][colNumber];
        boolean[][] visible_new = new boolean[rowNumber + count][colNumber];
        boolean[][] def_new = new boolean[rowNumber + count][colNumber];

        switch (location) {

            case TOP:
                for (int c = 0; c < colNumber; c++) {
                    for (int r = 0; r < count; r++) {
                        visible_new[r][c] = true;
                        def_new[r][c] = true;
                        cells_new[r][c] = DEFAULT_CELL;
                    }
                    for (int r = 0; r < rowNumber; r++) {
                        visible_new[r + count][c] = visible[r][c];
                        def_new[r + count][c] = def[r][c];
                        cells_new[r + count][c] = cells[r][c];
                    }
                }
                row0 -= count;
                break;

            case BOTTOM:
                for (int c = 0; c < colNumber; c++) {
                    for (int r = 0; r < rowNumber; r++) {
                        visible_new[r][c] = visible[r][c];
                        def_new[r][c] = def[r][c];
                        cells_new[r][c] = cells[r][c];
                    }
                    for (int r = rowNumber; r < count + rowNumber; r++) {
                        visible_new[r][c] = true;
                        def_new[r][c] = true;
                        cells_new[r][c] = DEFAULT_CELL;
                    }
                }
                rowEnd += count;
                break;

        }

        visible = visible_new;
        def = def_new;
        cells = cells_new;

        rowNumber += count;

    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        boolean empty = true;
        for (int r = 0; r < rowNumber; r++) {
            for (int c = 0; c < colNumber; c++) {
                if (!def[r][c]) {
                    return false;
                }
            }
        }
        return empty;
    }

    /**
     * Removes empty cells at all four boundary locations.
     * <p>
     * This is a convenience method comprising four individual method calls.
     *
     * @return <code>true</code> if some cells removed
     */
    public boolean compact() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("The table has no cells defined - compacting it would make it disappear. Is this the expected behaviour here?");
        }
        return compact(ColumnLocation.LEFT) | compact(ColumnLocation.RIGHT) | compact(RowLocation.TOP) | compact(RowLocation.BOTTOM);
    }

    /**
     * Removes empty cells at the given locations.
     * <p>
     * This is a convenience method simplifying individual calls to the methods      {@link #compact(RowLocation)},
     * {@link #compact(ColumnLocation)}, and {@link #compact(InternalLocation)}.
     * See these methods for additional details.
     *
     * @param locations The desired locations where to compact the table
     * @return <code>true</code> if some cells removed
     */
    public boolean compact(ILocation... locations) {
        if (isEmpty()) {
            throw new UnsupportedOperationException("The table has no cells defined - compacting it would make it disappear. Is this the expected behaviour here?");
        }
        if (locations == null) {
            throw new IllegalArgumentException("locations may not be null");
        }

        boolean ret = false;

        for (ILocation location : locations) {
            if (location instanceof ColumnLocation) {
                ret = ret | compact((ColumnLocation) location);
            } else if (location instanceof RowLocation) {
                ret = ret | compact((RowLocation) location);
            } else if (location instanceof InternalLocation) {
                ret = ret | compact((InternalLocation) location);
            }
        }

        return ret;

    }

    /**
     * Removes empty cells at the given boundary location.
     * <p>
     * Empty cells are cells which contain the default cell, i. e. they have not
     * been touched as part of a {@link #setCell(Cell, int, int)} method call.
     * This method checks for complete columns with default cells at the given
     * boundary location and removes them from the table.
     *
     * @param columnLocation The desired location where to compact the table
     * @return <code>true</code> if some cells were cut off
     */
    public boolean compact(ColumnLocation columnLocation) {
        if (isEmpty()) {
            throw new UnsupportedOperationException("The table has no cells defined - compacting it would make it disappear. Is this the expected behaviour here?");
        }
        if (columnLocation == null) {
            throw new IllegalArgumentException("columnLocation may not be null");
        }

        int count = 0;
        Cell[][] cells_new;
        boolean[][] visible_new;
        boolean[][] def_new;

        //.... Save this for later check for changes
        int old_row0 = row0;
        int old_col0 = col0;
        int old_rowNumber = rowNumber;
        int old_colNumber = colNumber;

        //.... Left edge
        if (columnLocation.equals(ColumnLocation.LEFT)) {

            int c = 0;
            boolean removable = true;

            //.... Count removable columns on the left end
            do {
                for (int r = 0; r < rowNumber; r++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (removable) {
                    count++;
                    c++;
                }
            } while (removable && c < colNumber);

            if (count > 0) {

                cells_new = new Cell[rowNumber][colNumber - count];
                visible_new = new boolean[rowNumber][colNumber - count];
                def_new = new boolean[rowNumber][colNumber - count];

                for (int c2 = 0; c2 < colNumber - count; c2++) {
                    for (int r = 0; r < rowNumber; r++) {
                        visible_new[r][c2] = visible[r][c2 + count];
                        def_new[r][c2] = def[r][c2 + count];
                        cells_new[r][c2] = cells[r][c2 + count];
                    }
                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                removeTags(Direction.COLUMN, col0, col0 + count);

                col0 += count;

            }

        } else {

            //.... Right edge
            int c = colNumber - 1;
            boolean removable = true;

            do {
                for (int r = 0; r < rowNumber; r++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (removable) {
                    c--;
                    count++;
                }
            } while (removable && c > 0);

            if (count > 0) {

                cells_new = new Cell[rowNumber][colNumber - count];
                visible_new = new boolean[rowNumber][colNumber - count];
                def_new = new boolean[rowNumber][colNumber - count];

                for (int c2 = 0; c2 < colNumber - count; c2++) {
                    for (int r = 0; r < rowNumber; r++) {
                        visible_new[r][c2] = visible[r][c2];
                        def_new[r][c2] = def[r][c2];
                        cells_new[r][c2] = cells[r][c2];
                    }
                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                removeTags(Direction.COLUMN, colEnd - count + 1, colEnd);

                colEnd -= count;

            }

        }

        colNumber -= count;

        //.... Check whether the dimensions of the table have changed
        return row0 != old_row0 || col0 != old_col0 || rowNumber != old_rowNumber || colNumber != old_colNumber;

    }

    /**
     * Removes empty cells at the given boundary location.
     * <p>
     * Empty cells are cells which contain the default cell, i. e. they have not
     * been touched as part of a {@link #setCell(Cell, int, int)} method call.
     * This method checks for complete rows with default cells at the given
     * boundary location and removes them from the table.
     *
     * @param rowLocation The desired location where to compact the table
     * @return <code>true</code> if some cells were cut off
     */
    public boolean compact(RowLocation rowLocation) {
        if (isEmpty()) {
            throw new UnsupportedOperationException("The table has no cells defined - compacting it would make it disappear. Is this the expected behaviour here?");
        }
        if (rowLocation == null) {
            throw new IllegalArgumentException("rowLocation may not be null");
        }

        int count = 0;
        Cell[][] cells_new;
        boolean[][] visible_new;
        boolean[][] def_new;

        //.... Save this for later check for changes
        int old_row0 = row0;
        int old_col0 = col0;
        int old_rowNumber = rowNumber;
        int old_colNumber = colNumber;

        //.... Top edge
        if (rowLocation.equals(RowLocation.TOP)) {

            int r = 0;
            boolean removable = true;

            do {
                for (int c = 0; c < colNumber; c++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (removable) {
                    count++;
                    r++;
                }
            } while (removable);

            if (count > 0) {

                cells_new = new Cell[rowNumber - count][colNumber];
                visible_new = new boolean[rowNumber - count][colNumber];
                def_new = new boolean[rowNumber - count][colNumber];

                for (int c = 0; c < colNumber; c++) {
                    for (int r2 = 0; r2 < rowNumber - count; r2++) {
                        visible_new[r2][c] = visible[r2 + count][c];
                        def_new[r2][c] = def[r2 + count][c];
                        cells_new[r2][c] = cells[r2 + count][c];
                    }
                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                removeTags(Direction.ROW, row0, row0 + count);

                row0 += count;

            }

        } else {

            //.... Bottom edge
            int r = rowNumber - 1;
            boolean removable = true;

            do {
                for (int c = 0; c < colNumber; c++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (removable) {
                    count++;
                    r--;
                }
            } while (removable);

            if (count > 0) {

                cells_new = new Cell[rowNumber - count][colNumber];
                visible_new = new boolean[rowNumber - count][colNumber];
                def_new = new boolean[rowNumber - count][colNumber];

                for (int c = 0; c < colNumber; c++) {
                    for (int r2 = 0; r2 < rowNumber - count; r2++) {
                        visible_new[r2][c] = visible[r2][c];
                        def_new[r2][c] = def[r2][c];
                        cells_new[r2][c] = cells[r2][c];
                    }
                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                removeTags(Direction.ROW, rowEnd - count + 1, rowEnd);

                rowEnd -= count;

            }

        }

        rowNumber -= count;

        //.... Check whether the dimensions of the table have changed
        return row0 != old_row0 || col0 != old_col0 || rowNumber != old_rowNumber || colNumber != old_colNumber;

    }

    /**
     * Removes empty cells at the given internal location.
     * <p>
     * Empty cells are cells which contain the default cell, i. e. they have not
     * been touched as part of a {@link #setCell(Cell, int, int)} method call.
     * This method checks for complete rows or columns in the table (depending
     * on the <code>internalLocation</code> parameter) with default cells and
     * removes them from the table.
     * <p>
     * Note that calls to this method also remove such rows or columns ate the
     * table boundaries, and thus a call to this method is a superset to calls
     * to {@link #compact(RowLocation)} and {@link #compact(ColumnLocation)}.
     *
     * @param internalLocation The desired internal location where to compact
     * the table (effectively by rows or by columns)
     * @return <code>true</code> if some cells were removed
     */
    public boolean compact(InternalLocation internalLocation) {
        if (isEmpty()) {
            throw new UnsupportedOperationException("The table has no cells defined - compacting it would make it disappear. Is this the expected behaviour here?");
        }
        if (internalLocation == null) {
            throw new IllegalArgumentException("internalLocation may not be null");
        }

        int count;
        Cell[][] cells_new;
        boolean[][] visible_new;
        boolean[][] def_new;
        Map<Integer, Map<String, String>> tags_new = null;

        boolean hasColumnTags = false;
        if (tags != null && tags.containsKey(Direction.COLUMN)) {
            hasColumnTags = true;
        }
        boolean hasRowTags = false;
        if (tags != null && tags.containsKey(Direction.ROW)) {
            hasRowTags = true;
        }

        //.... Save this for later check for changes
        int old_row0 = row0;
        int old_col0 = col0;
        int old_rowNumber = rowNumber;
        int old_colNumber = colNumber;

        if (internalLocation.equals(InternalLocation.COLUMN)) {

            //.... Create an index of columns to retain
            List<Integer> columnList = new ArrayList<>();

            for (int c = 0; c < colNumber; c++) {
                boolean removable = true;
                for (int r = 0; r < rowNumber; r++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (!removable) {
                    columnList.add(c);
                }
            }

            //.... Remove the columns
            count = columnList.size();

            if (count > 0) {

                cells_new = new Cell[rowNumber][count];
                visible_new = new boolean[rowNumber][count];
                def_new = new boolean[rowNumber][count];
                if (hasColumnTags) {
                    tags_new = new HashMap<>();
                }

                int c2;
                for (int c = 0; c < count; c++) {

                    c2 = columnList.get(c);
                    for (int r = 0; r < rowNumber; r++) {
                        visible_new[r][c] = visible[r][c2];
                        def_new[r][c] = def[r][c2];
                        cells_new[r][c] = cells[r][c2];
                    }

                    if (hasColumnTags && tags.get(Direction.COLUMN).containsKey(c2 + col0)) {
                        tags_new.put(c + col0 + columnList.get(0), tags.get(Direction.COLUMN).get(c2 + col0));
                    }

                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                col0 += columnList.get(0);
                colNumber = count;
                colEnd = col0 + colNumber - 1;

                if (hasColumnTags) {
                    tags.put(Direction.COLUMN, tags_new);
                }

            } else {
                throw new UnsupportedOperationException("Invocation would remove all columns of the table, i. e. no column would be retained during compaction");
            }

            //.... Remove all empty rows (this includes the TOP and BOTTOM cases)
        } else if (internalLocation.equals(InternalLocation.ROW)) {

            //.... Create an index of rows to retain
            List<Integer> rowList = new ArrayList<>();

            for (int r = 0; r < rowNumber; r++) {
                boolean removable = true;
                for (int c = 0; c < colNumber; c++) {
                    if (!def[r][c]) {
                        removable = false;
                    }
                }
                if (!removable) {
                    rowList.add(r);
                }
            }

            //.... Remove the rows
            count = rowList.size();

            if (count > 0) {

                cells_new = new Cell[count][colNumber];
                visible_new = new boolean[count][colNumber];
                def_new = new boolean[count][colNumber];
                if (hasRowTags) {
                    tags_new = new HashMap<>();
                }

                int r2;
                for (int r = 0; r < count; r++) {

                    r2 = rowList.get(r);
                    for (int c = 0; c < colNumber; c++) {
                        visible_new[r][c] = visible[r2][c];
                        def_new[r][c] = def[r2][c];
                        cells_new[r][c] = cells[r2][c];
                    }

                    if (hasRowTags && tags.get(Direction.ROW).containsKey(r2 + row0)) {
                        tags_new.put(r + row0 + rowList.get(0), tags.get(Direction.ROW).get(r2 + row0));
                    }

                }

                visible = visible_new;
                def = def_new;
                cells = cells_new;

                row0 += rowList.get(0);
                rowNumber = count;
                rowEnd = row0 + rowNumber - 1;

                if (hasRowTags) {
                    tags.put(Direction.ROW, tags_new);
                }

            } else {
                throw new UnsupportedOperationException("Invocation would remove all rows of the table, i. e. no row would be retained during compaction");
            }
        }

        //.... Check whether the dimensions of the table have changed
        return row0 != old_row0 || col0 != old_col0 || rowNumber != old_rowNumber || colNumber != old_colNumber;

    }

    /**
     * Internal helper to remove tags from the map in case rows or columns are
     * deleted
     *
     * @param tagLocation
     * @param startLogicalIndex
     * @param endLogicalIndex The last logical index plus 1 (!)
     * @since 1.1
     */
    private void removeTags(Direction tagLocation, int startLogicalIndex, int endLogicalIndex) {
        if (tagLocation == null) {
            throw new IllegalArgumentException("tagLocation may not be null");
        }
        if (tags == null || !tags.containsKey(tagLocation)) {
            return;
        }
        for (int logicalIndex = startLogicalIndex; logicalIndex < endLogicalIndex; logicalIndex++) {
            removeTags(tagLocation, logicalIndex);
        }
    }

    /**
     * @param tagLocation
     * @param logicalIndex
     * @since 1.1
     */
    private void removeTags(Direction tagLocation, int logicalIndex) {
        if (tagLocation == null) {
            throw new IllegalArgumentException("tagLocation may not be null");
        }
        if (tags == null || !tags.containsKey(tagLocation)) {
            return;
        }
        if (tags.get(tagLocation).containsKey(logicalIndex)) {
            tags.get(tagLocation).remove(logicalIndex);
        }
    }

    /**
     * A convenience method to enable clipping at all four table boundaries.
     *
     * @see BoundaryCondition
     */
    public void setClipping() {
        boundaryConditions.put(ColumnLocation.LEFT, BoundaryCondition.CLIPPING);
        boundaryConditions.put(ColumnLocation.RIGHT, BoundaryCondition.CLIPPING);
        boundaryConditions.put(RowLocation.TOP, BoundaryCondition.CLIPPING);
        boundaryConditions.put(RowLocation.BOTTOM, BoundaryCondition.CLIPPING);
    }

    /**
     * A convenience method to enable auto-grow at all four table boundaries.
     *
     * @see BoundaryCondition
     */
    public void setGrow() {
        boundaryConditions.put(ColumnLocation.LEFT, BoundaryCondition.GROW);
        boundaryConditions.put(ColumnLocation.RIGHT, BoundaryCondition.GROW);
        boundaryConditions.put(RowLocation.TOP, BoundaryCondition.GROW);
        boundaryConditions.put(RowLocation.BOTTOM, BoundaryCondition.GROW);
    }

    /**
     * A convenience method to enable fixed boundaries at all four table
     * boundaries.
     *
     * @see BoundaryCondition
     */
    public void setFixed() {
        boundaryConditions.put(ColumnLocation.LEFT, BoundaryCondition.FIXED);
        boundaryConditions.put(ColumnLocation.RIGHT, BoundaryCondition.FIXED);
        boundaryConditions.put(RowLocation.TOP, BoundaryCondition.FIXED);
        boundaryConditions.put(RowLocation.BOTTOM, BoundaryCondition.FIXED);
    }

    /**
     * Retrieve the logical start index for rows in the table.
     *
     * @return The logical start index for rows
     */
    public int getRow0() {
        return row0;
    }

    /**
     * Retrieve the logical start index for columns in the table.
     *
     * @return The logical start index for columns
     */
    public int getCol0() {
        return col0;
    }

    /**
     * Retrieve the number of rows in the table.
     *
     * @return The number of rows in the table
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * Retrieve the number of columns in the table.
     *
     * @return The number of columns in the table
     */
    public int getColNumber() {
        return colNumber;
    }

    /**
     * Retrieve the cell at the given table location.
     *
     * @param row The logical row index
     * @param col The logical column index
     * @return The cell at the given location
     */
    public Cell getCell(int row, int col) {
        int r = row - row0;
        int c = col - col0;

        if (r >= rowNumber || r < 0) {
            throw new IllegalArgumentException("row must be between " + row0 + " and " + getRowEnd());
        }
        if (c >= colNumber || c < 0) {
            throw new IllegalArgumentException("col must be between " + col0 + " and " + getColEnd());
        }
        return cells[r][c];
    }

    /**
     * Check whether the cell at the given table location is visible.
     * <p>
     * Cells can become invisible when other cells spanning more than one row
     * and/or column cover the particular location in the table. This is
     * important for the rendering of tables since cells which are invisible are
     * not part of the rendering.
     *
     * @param row The logical row index
     * @param col The logical column index
     * @return <code>true</code> if the cell at the given location is visible
     */
    public boolean isVisible(int row, int col) {
        int r = row - row0;
        int c = col - col0;

        if (r >= rowNumber || r < 0) {
            throw new IllegalArgumentException("row must be between " + row0 + " and " + getRowEnd());
        }
        if (c >= colNumber || c < 0) {
            throw new IllegalArgumentException("col must be between " + col0 + " and " + getColEnd());
        }
        return visible[r][c];
    }

    /**
     * Check whether the cell at the given table location is the default cell.
     * <p>
     * At table instance creation time, all cells in the table refer to the
     * default cell. This may change over time as cells are added to the table.
     *
     * @param row The logical row index
     * @param col The logical column index
     * @return <code>true</code> if the cell at the given location is the
     * default cell
     */
    public boolean isDefaultCell(int row, int col) {
        int r = row - row0;
        int c = col - col0;

        if (r >= rowNumber || r < 0) {
            throw new IllegalArgumentException("row must be between " + row0 + " and " + getRowEnd());
        }
        if (c >= colNumber || c < 0) {
            throw new IllegalArgumentException("col must be between " + col0 + " and " + getColEnd());
        }
        return def[r][c];
    }

    /**
     * Insert a cell into the table at the given location.
     * <p>
     * Several cases need to be differentiated when adding a cell to the table.
     * This HTML table shows the different cases that can occur when inserting a
     * cell (orange) into a table (grey). Note that these cases apply both to
     * rows and columns:
     * <p>
     * <p>
     * <table style="text-align: left; width: 500px;" border="1" summary="Insert cell details 1"
     * cellpadding="2" cellspacing="2">
     * <tbody>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">Case<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td colspan="3" rowspan="1"
     * style="vertical-align: top; background-color: rgb(192, 192, 192); text-align: center; font-family: Helvetica,Arial,sans-serif;">Table
     * Extent<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">1<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell</td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">2<br>
     * </td>
     * <td colspan="4" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">3<br>
     * </td>
     * <td colspan="7" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell</td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">4<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell</td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">5<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td colspan="4" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">6<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     * <br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;">&nbsp;&nbsp;&nbsp;&nbsp;
     * <br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;"><br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(192, 192, 192); font-family: Helvetica,Arial,sans-serif;">&nbsp;&nbsp;&nbsp;&nbsp;
     * <br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">&nbsp;&nbsp;&nbsp;&nbsp;
     * &nbsp;<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 204, 51); font-family: Helvetica,Arial,sans-serif; text-align: center;">Cell</td>
     * </tr>
     * </tbody>
     * </table>
     * <p>
     * Depending on the chosen boundary conditions at the boundary locations,
     * the following results occur:
     * <p>
     * <table style="text-align: left;" border="1" cellpadding="2" summary="Insert cell details 2"
     * cellspacing="2">
     * <tbody>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">Case<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">FIXED</td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">CLIPPING<br>
     * </td>
     * <td colspan="1" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">GROW<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">1<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif; text-align: center;">IllegalArgumentException<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">null<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * table expanded<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">3<br>
     * </td>
     * <td colspan="1" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif; text-align: center;">IllegalArgumentException</td>
     * <td colspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * cell clipped<br>
     * </td>
     * <td colspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * table expanded</td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">2<br>
     * </td>
     * <td colspan="1" rowspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif; text-align: center;">IllegalArgumentException</td>
     * <td colspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * cell clipped</td>
     * <td colspan="1"
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * table expanded</td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">4<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">SetResult<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">SetResult<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult</td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">5<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">IllegalArgumentException</td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">SetResult,
     * cell clipped</td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * table expanded</td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif;">6<br>
     * </td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">IllegalArgumentException</td>
     * <td
     * style="vertical-align: top; font-family: Helvetica,Arial,sans-serif; background-color: rgb(255, 255, 255);">null<br>
     * </td>
     * <td
     * style="vertical-align: top; background-color: rgb(255, 255, 255); font-family: Helvetica,Arial,sans-serif;">SetResult,
     * table expanded</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param cell The cell to add to the table
     * @param row The logical row index
     * @param col The logical column index
     * @return A {@link SetResult} instance (or <code>null</code>, see above)
     * @see BoundaryCondition
     */
    public SetResult setCell(Cell cell, int row, int col) {
        if (cell == null) {
            throw new IllegalArgumentException("cell may not be null");
        }

        int r = row - row0;                  // Absolute index  (row, row0 are logical)
        int c = col - col0;                  // Absolute index  (col, col0 are logical)
        int rEnd = r + cell.getRowSpan() - 1;   // Absolute index
        int cEnd = c + cell.getColSpan() - 1;   // Absolute index
        int rowLimit = row0 + rowNumber - cell.getRowSpan();
        int colLimit = col0 + colNumber - cell.getColSpan();

        SetResult result = new SetResult(row, col);  // The default

        //.... Row: Case 1
        if (rEnd < 0) {

            switch (boundaryConditions.get(RowLocation.TOP)) {
                case FIXED:
                    throw new IllegalArgumentException("Cell lies completely outside of the table");
                case CLIPPING:
                    return null;             // Entire contents are clipped
                case GROW:
                    addRows(RowLocation.TOP, -r);
                    r = 0;
                    rEnd = r + cell.getRowSpan() - 1;
            }

        } else if (r < 0) {

            //.... Row: Case 2
            if (rEnd < rowNumber) {

                switch (boundaryConditions.get(RowLocation.TOP)) {
                    case FIXED:
                        if (cell.getRowSpan() > rowNumber) {
                            throw new IllegalArgumentException("Cell has too many rows (" + cell.getRowSpan() + "). Maximum row number is " + rowNumber);
                        }
                        throw new IllegalArgumentException("row must be between " + row0 + " and " + rowLimit);
                    case CLIPPING:
                        r = 0;
                        result.setModified(true);
                        break;
                    case GROW:
                        addRows(RowLocation.TOP, -r);
                        r = 0;
                        rEnd = r + cell.getRowSpan() - 1;
                }

            } else {

                //.... Row: Case 3
                switch (boundaryConditions.get(RowLocation.TOP)) {
                    case FIXED:
                        throw new IllegalArgumentException("Cell has too many rows (" + cell.getRowSpan() + "). Maximum row number is " + rowNumber);
                    case CLIPPING:
                        r = 0;
                        result.setModified(true);
                        break;
                    case GROW:
                        addRows(RowLocation.TOP, -r);
                        r = 0;
                        rEnd = r + cell.getRowSpan() - 1;
                }

                switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                    case FIXED:
                        throw new IllegalArgumentException("Cell has too many rows (" + cell.getRowSpan() + "). Maximum row number is " + rowNumber);
                    case CLIPPING:
                        rEnd = rowNumber - 1;
                        result.setModified(true);
                        break;
                    case GROW:
                        addRows(RowLocation.BOTTOM, rEnd - getRowEnd() + row0);
                        rEnd = rowNumber - 1;
                }

            }

        } else if (r < rowNumber) {

            //.... Row: Case 4
            if (rEnd < rowNumber) {

                //.... Row: Case 5
            } else {

                switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                    case FIXED:
                        if (cell.getRowSpan() > rowNumber) {
                            throw new IllegalArgumentException("Cell has too many rows (" + cell.getRowSpan() + "). Maximum row number is " + rowNumber);
                        } else {
                            throw new IllegalArgumentException("row must be between " + row0 + " and " + rowLimit);
                        }
                    case CLIPPING:
                        rEnd = rowNumber - 1;
                        result.setModified(true);
                        break;
                    case GROW:
                        addRows(RowLocation.BOTTOM, rEnd - getRowEnd() + row0);
                        rEnd = rowNumber - 1;
                }

            }

            //.... Row: Case 6
        } else {

            switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                case FIXED:
                    throw new IllegalArgumentException("Cell lies completely outside of the table");
                case CLIPPING:
                    return null;
                case GROW:
                    addRows(RowLocation.BOTTOM, rEnd - getRowEnd() + row0);
                    rEnd = rowNumber - 1;
            }

        }

        //.... Column: Case 1
        if (cEnd < 0) {

            switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                case FIXED:
                    throw new IllegalArgumentException("Cell lies completely outside of the table");
                case CLIPPING:
                    return null;             // Entire contents are clipped
                case GROW:
                    addColumns(ColumnLocation.LEFT, -c);
                    c = 0;
                    cEnd = c + cell.getColSpan() - 1;
            }

        } else if (c < 0) {

            //.... Column: Case 2
            if (cEnd < colNumber) {

                switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                    case FIXED:
                        if (cell.getColSpan() > colNumber) {
                            throw new IllegalArgumentException("Cell has too many columns (" + cell.getColSpan() + "). Maximum column number is " + colNumber);
                        } else {
                            throw new IllegalArgumentException("col must be between " + col0 + " and " + colLimit);
                        }
                    case CLIPPING:
                        c = 0;
                        result.setModified(true);
                        break;
                    case GROW:
                        addColumns(ColumnLocation.LEFT, -c);
                        c = 0;
                        cEnd = c + cell.getColSpan() - 1;
                }

            } else {

                //.... Column: Case 3
                switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                    case FIXED:
                        throw new IllegalArgumentException("Cell has too many columns (" + cell.getColSpan() + "). Maximum column number is " + colNumber);
                    case CLIPPING:
                        c = 0;
                        result.setModified(true);
                        break;
                    case GROW:
                        addColumns(ColumnLocation.LEFT, -c);
                        c = 0;
                        cEnd = c + cell.getColSpan() - 1;

                }

                switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                    case FIXED:
                        throw new IllegalArgumentException("Cell has too many columns (" + cell.getColSpan() + "). Maximum column number is " + colNumber);
                    case CLIPPING:
                        cEnd = colNumber - 1;
                        result.setModified(true);
                        break;
                    case GROW:
                        addColumns(ColumnLocation.RIGHT, cEnd - getColEnd() + col0);
                        cEnd = colNumber - 1;
                }

            }

        } else if (c < colNumber) {

            //.... Column: Case 4
            if (cEnd < colNumber) {

                //.... Column: Case 5
            } else {

                switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                    case FIXED:
                        if (cell.getColSpan() > colNumber) {
                            throw new IllegalArgumentException("Cell has too many columns (" + cell.getColSpan() + "). Maximum column number is " + colNumber);
                        } else {
                            throw new IllegalArgumentException("col must be between " + col0 + " and " + colLimit);
                        }
                    case CLIPPING:
                        cEnd = colNumber - 1;
                        result.setModified(true);
                        break;
                    case GROW:
                        addColumns(ColumnLocation.RIGHT, cEnd - getColEnd() + col0);
                        cEnd = colNumber - 1;
                }

            }

            //.... Column: Case 6
        } else {

            switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                case FIXED:
                    throw new IllegalArgumentException("Cell lies completely outside of the table");
                case CLIPPING:
                    return null;
                case GROW:
                    addColumns(ColumnLocation.RIGHT, cEnd - getColEnd() + col0);
                    cEnd = colNumber - 1;
            }

        }

        //.... The cell may have to be modified to be displayed correctly now (CLIPPING only)
        if (result.isModified()) {
            cell.setRowSpan(rEnd - r + 1);
            cell.setColSpan(cEnd - c + 1);
        }

        //.... Now actually fill the table where necessary
        for (int rIndex = r; rIndex <= rEnd; rIndex++) {
            for (int cIndex = c; cIndex <= cEnd; cIndex++) {
                if (!def[rIndex][cIndex]) {
                    throw new IllegalArgumentException("Cell conflict when trying to add cell at location (" + rIndex + "/" + cIndex + "): already covered by a cell");
                }
                cells[rIndex][cIndex] = cell;    // The same cell is referenced from all the logical cells
                visible[rIndex][cIndex] = false;
                def[rIndex][cIndex] = false;
            }
        }
        visible[r][c] = true;    // Only this one remains, all others are now hidden

        result.setRow(r + row0);
        result.setCol(c + col0);
        result.setRowEnd(rEnd + row0);
        result.setColEnd(cEnd + col0);

        return result;
    }

    /**
     * @param table
     * @param row0
     * @param col0
     */
    public void addTable(Table table, int row0, int col0) {
        if (table == null) {
            throw new NullPointerException("table may not be null");
        }
        for (int row = table.row0; row <= table.rowEnd; row++) {
            for (int col = table.col0; col <= table.colEnd; col++) {
                if (table.isVisible(row, col)) {
                    setCell(table.getCell(row, col), row0 + row, col0 + col);
                }
            }
        }
    }

    /**
     * A helper enumeration for checks whether a cell can actually be added to a
     * table
     *
     * @since 1.1
     */
    public enum CheckResult {

        /**
         * The cell can not be added as desired
         */
        NO,
        /**
         * The cell can be added technically, but it is fully clipped
         */
        FULLY_CLIPPED,
        /**
         * The cell can be added as desired
         */
        YES
    }

    /**
     * Check whether a cell can be added at the desired location
     *
     * @param cell
     * @param row
     * @param col
     * @return A <code>CheckResult</code> instance indicating the result
     * @since 1.1
     */
    public CheckResult canSetCell(Cell cell, int row, int col) {
        if (cell == null) {
            throw new IllegalArgumentException("cell may not be null");
        }

        int r = row - row0;                  // Absolute index  (row, row0 are logical)
        int c = col - col0;                  // Absolute index  (col, col0 are logical)
        int rEnd = r + cell.getRowSpan() - 1;   // Absolute index
        int cEnd = c + cell.getColSpan() - 1;   // Absolute index

        //.... Row: Case 1
        if (rEnd < 0) {

            switch (boundaryConditions.get(RowLocation.TOP)) {
                case FIXED:
                    return CheckResult.NO;
                case CLIPPING:
                    return CheckResult.FULLY_CLIPPED;
                case GROW:
                    return CheckResult.YES;
            }

        } else if (r < 0) {

            //.... Row: Case 2
            if (rEnd < rowNumber) {

                switch (boundaryConditions.get(RowLocation.TOP)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        r = 0;
                }

            } else {

                //.... Row: Case 3
                switch (boundaryConditions.get(RowLocation.TOP)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        r = 0;
                }

                switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        rEnd = rowNumber - 1;
                }

            }

        } else if (r < rowNumber) {

            //.... Row: Case 4
            if (rEnd < rowNumber) {

                //.... Row: Case 5
            } else {

                switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        rEnd = rowNumber - 1;
                }

            }

            //.... Row: Case 6
        } else {

            switch (boundaryConditions.get(RowLocation.BOTTOM)) {
                case FIXED:
                    return CheckResult.NO;
                case CLIPPING:
                    return CheckResult.FULLY_CLIPPED;
                case GROW:
                    return CheckResult.YES;
            }

        }

        //.... Column: Case 1
        if (cEnd < 0) {

            switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                case FIXED:
                    return CheckResult.NO;
                case CLIPPING:
                    return CheckResult.FULLY_CLIPPED;
                case GROW:
                    return CheckResult.YES;
            }

        } else if (c < 0) {

            //.... Column: Case 2
            if (cEnd < colNumber) {

                switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        c = 0;
                }

            } else {

                //.... Column: Case 3
                switch (boundaryConditions.get(ColumnLocation.LEFT)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        c = 0;

                }

                switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        cEnd = colNumber - 1;
                }

            }

        } else if (c < colNumber) {

            //.... Column: Case 4
            if (cEnd < colNumber) {

                //.... Column: Case 5
            } else {

                switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                    case FIXED:
                        return CheckResult.NO;
                    case CLIPPING:
                    case GROW:
                        cEnd = colNumber - 1;
                }

            }

            //.... Column: Case 6
        } else {

            switch (boundaryConditions.get(ColumnLocation.RIGHT)) {
                case FIXED:
                    return CheckResult.NO;
                case CLIPPING:
                    return CheckResult.FULLY_CLIPPED;
                case GROW:
                    return CheckResult.YES;
            }

        }

        //.... Now check the table for existing cells
        for (int rIndex = r; rIndex <= rEnd; rIndex++) {
            for (int cIndex = c; cIndex <= cEnd; cIndex++) {
                if (!def[rIndex][cIndex]) {
                    return CheckResult.NO;
                }
            }
        }

        return CheckResult.YES;
    }

    /**
     * @param fileName
     * @throws java.io.IOException
     * @since 1.1
     */
    public void dump(String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName may not be null");
        }
        dump(new BufferedWriter(new FileWriter(fileName)));
    }

    /**
     * @throws java.io.IOException
     */
    public void dump() throws IOException {
        dump(new BufferedWriter(new OutputStreamWriter(System.out)));
    }

    /**
     * A simple HTML debug output. The table is dumped to STDOUT and the
     * resulting file can directly be opened in a browser to get a rough idea of
     * the internal table layout and cell structure.
     *
     * @param writer
     * @throws java.io.IOException
     * @since 1.1
     */
    public void dump(Writer writer) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("writer may not be null");
        }

        BufferedWriter buf;
        if (writer instanceof BufferedWriter) {
            buf = (BufferedWriter) writer;
        } else {
            buf = new BufferedWriter(writer);
        }

        buf.write("<html><body>");
        buf.newLine();
        buf.write("<table border=1 style=\"empty-cells:show\">");
        buf.newLine();

        boolean hasColumnTags = false;
        if (tags != null && tags.containsKey(Direction.COLUMN)) {
            hasColumnTags = true;
        }
        boolean hasRowTags = false;
        if (tags != null && tags.containsKey(Direction.ROW)) {
            hasRowTags = true;
        }

        //.... Column tags
        buf.write("<tr>");
        buf.newLine();
        buf.write("<td>");
        buf.newLine();

        for (int c = col0; c <= colEnd; c++) {
            buf.write("<td bgcolor=lightblue>");
            buf.newLine();
            if (hasColumnTags) {
                if (tags.get(Direction.COLUMN).containsKey(c)) {
                    for (String tagName : tags.get(Direction.COLUMN).get(c).keySet()) {
                        String tagValue = tags.get(Direction.COLUMN).get(c).get(tagName);
                        if (tagValue.equals(TAG_EMPTY_VALUE)) {
                            buf.write(tagName + "<br>");
                            buf.newLine();
                        } else {
                            buf.write("TAG: " + tagName + " = " + tagValue + "<br>");
                            buf.newLine();
                        }
                    }
                }
            }
        }

        //.... The actual table
        for (int r = 0; r < rowNumber; r++) {

            buf.write("<tr>");
            buf.newLine();
            buf.write("<td bgcolor=lightblue>");
            buf.newLine();

            if (hasRowTags) {
                if (tags.get(Direction.ROW).containsKey(r + row0)) {
                    for (String tagName : tags.get(Direction.ROW).get(r + row0).keySet()) {
                        String tagValue = tags.get(Direction.ROW).get(r + row0).get(tagName);
                        if (tagValue.equals(TAG_EMPTY_VALUE)) {
                            buf.write(tagName + "<br>");
                            buf.newLine();
                        } else {
                            buf.write("TAG: " + tagName + " = " + tagValue + "<br>");
                            buf.newLine();
                        }
                    }
                }
            }

            for (int c = 0; c < colNumber; c++) {
                if (def[r][c]) {
                    buf.write("<td> (" + r + "/" + c + ")<br>" + def[r][c]);
                    buf.newLine();
                } else {
                    String color = "yellow";
                    if (visible[r][c]) {
                        color = "green";
                    }
                    buf.write("<td bgcolor=" + color + "> (" + r + "/" + c + ")<br> Cell content: </br>");
                    for (String cKey : cells[r][c].getContents().keySet()) {
                        buf.write("<br>" + cKey + ": " + cells[r][c].getContent(cKey) + "</br>");
                    }
                    buf.write("<br>" + cells[r][c].getContent() + "</br>");
                    buf.write("Default: " + def[r][c]);
                    buf.newLine();
                }
            }
        }

        buf.write("</table><p>");
        buf.newLine();
        buf.write("</body></html>");
        buf.newLine();
        buf.flush();
        buf.close();

    }

    /**
     * Get the logical index of the last row in the table.
     *
     * @return The logical index of the last row in the table
     */
    public int getRowEnd() {
        return rowEnd;
    }

    /**
     * Get the logical index of the last column in the table.
     *
     * @return The logical index of the last column in the table
     */
    public int getColEnd() {
        return colEnd;
    }

    /**
     * Set the boundary condition for the given boundary location.
     *
     * @param boundaryLocation The location for which the boundary condition is
     * to be set
     * @param boundaryCondition The boundary condition to establish for this
     * location
     */
    public void setBoundaryCondition(IBoundaryLocation boundaryLocation, BoundaryCondition boundaryCondition) {
        if (boundaryLocation == null) {
            throw new IllegalArgumentException("boundaryLocation may not be null");
        }
        if (boundaryCondition == null) {
            throw new IllegalArgumentException("boundaryCondition may not be null");
        }
        boundaryConditions.put(boundaryLocation, boundaryCondition);
    }

    /**
     *
     */
    /**
     * public void print() { for (int r = 0; r < rowNumber; r++) { for (int c =
     * 0; c < colNumber; c++) { System.out.println(r + " | " + c + " | " +
     * def[r][c] + "|" + visible[r][c] + "|" + cells[r][c]); } } }
     */
    /**
     * @param renderingContext
     * @param renderer
     */
    public void addRenderer(RenderingContext renderingContext, IRenderer renderer) {
        if (renderingContext == null) {
            throw new NullPointerException("renderingContext may not be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer may not be null");
        }
        renderers.put(renderingContext, renderer);
    }

    /**
     * @param renderingContext
     * @return
     */
    public IRenderer getRenderer(RenderingContext renderingContext) {
        if (renderingContext == null) {
            throw new NullPointerException("renderingContext may not be null");
        }
        return renderers.get(renderingContext);
    }

    /**
     * @param renderingContext
     * @return
     */
    public boolean hasRenderer(RenderingContext renderingContext) {
        if (renderingContext == null) {
            throw new NullPointerException("renderingContext may not be null");
        }
        return renderers.get(renderingContext) != null;
    }

}
