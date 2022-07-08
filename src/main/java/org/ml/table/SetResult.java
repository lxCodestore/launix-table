/*
 * The MIT License
 *
 * Copyright 2019 Dr. Matthias Laux.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.ml.table;

/**
 * Result information of a {@link Table#setCell(Cell, int, int)} operation.
 * <p>
 * The information returned in an instance of this class is useful in cases where
 * the boundaries of the table are managed dynaically since then the cell as such may
 * have been clipped and thus, the starting and end indices of rows and columns may
 * have changed.
 */
public class SetResult {

    private int row = 0;                // The logical row where the actual insert occurred
    private int col = 0;                // The logical col where the actual insert occurred
    private int rowEnd = 0;             // The logical index of the end row for the cell
    private int colEnd = 0;             // The logical index of the end col for the cell
    private boolean modified = false;   // True if rowSpan and/or colSpan had to be modified

    /**
     * Create a new instance with the given row and column information.
     *
     * @param row The logical row where the actual insert of the cell occurred
     * @param col The logical column where the actual insert of the cell occurred
     */
    public SetResult(int row, int col) {
        this.setRow(row);
        this.setCol(col);
    }

    /**
     * Retrieve the logical index of the row where the actual insert of the
     * cell occurred
     *
     * @return The logical index of the row where the actual insert of the
     * cell occurred
     */
    public int getRow() {
        return row;
    }

    /**
     * Set the logical index of the row where the actual insert of the cell
     * occurred. Sometimes it is necessary to modify the value established
     * in the constructor.
     *
     * @param row The logical index of the row where the actual insert of
     *            the cell occurred
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * Retrieve the logical index of the column where the actual insert of
     * the cell occurred
     *
     * @return The logical index of the column where the actual insert
     * of the cell occurred
     */
    public int getCol() {
        return col;
    }

    /**
     * Set the logical index of the column where the actual insert of the cell
     * occurred. Sometimes
     * it is necessary to modify the value established in the constructor.
     *
     * @param col The logical index of the column where the actual insert of
     *            the cell occurred
     */
    public void setCol(int col) {
        this.col = col;
    }

    /**
     * Returns a boolean indicating whether the original values of the cell (row and
     * column number) and /or the insertion point (the arguments to the
     * {@link Table#setCell(Cell, int, int)} method) have been modified in the course
     * of the insertion process.
     *
     * @return A boolean indicating whether the original values of the cell have
     * been modified in the course of the insertion process
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Set the boolean indicating whether the cell parameters have been changed
     * in the course
     * of the insertion process into the table
     *
     * @param modified The desired boolean value
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Retrieve the actual row end index of the cell in the table after the
     * insertion process.
     * This value may be different from he expected value if clipping is
     * activated at the
     * boundaries.
     *
     * @return The actual row end index of the cell in the table
     */
    public int getRowEnd() {
        return rowEnd;
    }

    /**
     * Set the actual logical row end index of the cell in the table after the
     * insertion process.
     *
     * @param rowEnd The actual logical row end index of the cell in the table
     */
    public void setRowEnd(int rowEnd) {
        this.rowEnd = rowEnd;
    }

    /**
     * Retrieve the actual logical end column index of the cell in the table
     * after the insertion process.
     * This value may be different from he expected value if clipping is
     * activated at the
     * boundaries.
     *
     * @return The actual logical column end index of the cell in the table
     */
    public int getColEnd() {
        return colEnd;
    }

    /**
     * Set the actual logical column end index of the cell in the table after
     * the insertion process.
     *
     * @param colEnd The actual logical column end index of the cell in the table
     */
    public void setColEnd(int colEnd) {
        this.colEnd = colEnd;
    }

    /**
     * The overridden {@link Object#toString()} method.
     *
     * @return A string representation of the instance with all relevant data
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SetResult: row = ");
        sb.append(row);
        sb.append(" / col = ");
        sb.append(col);
        sb.append(" / rowEnd = ");
        sb.append(rowEnd);
        sb.append(" / colEnd = ");
        sb.append(colEnd);
        sb.append(" / modified = ");
        sb.append(modified);
        return sb.toString();
    }
}
