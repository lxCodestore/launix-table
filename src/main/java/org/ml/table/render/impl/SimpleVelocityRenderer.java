package org.ml.table.render.impl;

import org.ml.table.Cell;
import org.ml.table.content.EmailContent;
import org.ml.table.content.UrlAnchor;
import org.ml.table.content.UrlContent;
import static org.ml.table.output.Hint.HINT_PERCENTAGE;
import org.ml.table.render.AbstractVelocityRenderer;
import org.ml.tools.FileType;

/**
 * A renderer with some (hopefully) reasonable default behavior to render a Cell
 * within a Velocity template. Note that this renders only the anonymous content
 * in the Cell object. For more complex scenarios involving multiple named
 * content objects, a specialized rendered needs to be implemented
 *
 * @author mlaux
 */
public class SimpleVelocityRenderer extends AbstractVelocityRenderer {

    public final static String DEFAULT_DOUBLE_FORMAT = "%.2f";
    public final static String DEFAULT_PERCENTAGE_FORMAT = "%.2f";

    private String doubleFormat = DEFAULT_DOUBLE_FORMAT;
    private String percentageFormat = DEFAULT_PERCENTAGE_FORMAT;

    /**
     *
     */
    public SimpleVelocityRenderer() {

    }

    /**
     * @param doubleFormat A formatting string to be used in String.format() for
     * the given data item. This overrides the default
     * @param percentageFormat A formatting string to be used in String.format()
     * for the given data item. This overrides the default
     */
    public SimpleVelocityRenderer(String doubleFormat, String percentageFormat) {
        if (doubleFormat == null) {
            throw new NullPointerException("doubleFormat may not be null");
        }
        if (percentageFormat == null) {
            throw new NullPointerException("percentageFormat may not be null");
        }
        this.doubleFormat = doubleFormat;
        this.percentageFormat = percentageFormat;
    }

    /**
     * @param cell
     * @return
     */
    @Override
    public String renderCell(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("cell may not be null");
        }

        Object content = cell.getContent();

        if (content != null) {

            if (cell.containsHint(HINT_PERCENTAGE)) {

                double val = 0.0;
                if (content instanceof Double) {
                    val = 100.0 * (Double) content;
                } else if (content instanceof Float) {
                    val = 100.0 * (Float) content;
                } else if (content instanceof Integer) {
                    val = 100.0 * (Integer) content;
                } else {
                    throw new UnsupportedOperationException("content contains Hint '" + HINT_PERCENTAGE + "' and is instance of "
                            + content.getClass() + " - don't know how to handle this");
                }
                return String.format(percentageFormat, val) + "%";

            } else {

                if (content instanceof Integer) {
                    return String.valueOf((Integer) content);
                } else if (content instanceof String) {
                    return ((String) content).replaceAll("\n", "<br/>");
                } else if (content instanceof Float) {
                    return String.format(doubleFormat, (Float) content);
                } else if (content instanceof Double) {
                    return String.format(doubleFormat, (Double) content);
                } else if (content instanceof Boolean) {
                    return String.valueOf((Boolean) content);
                } else if (content instanceof EmailContent) {
                    String address = ((EmailContent) content).getAddress();
                    return "<a href=\"mailto:" + address + "\">" + address + "</a>";
                } else if (content instanceof UrlContent) {
                    UrlContent urlContent = (UrlContent) content;
                    StringBuilder sb = new StringBuilder(200);
                    sb.append("<a href=\"").append(urlContent.getAddress());
                    if (urlContent.appendFileExtension()) {
                        sb.append(FileType.HTML.getExtension());
                    }
                    sb.append("\" title=\"").append(urlContent.getTooltip()).append("\">").append(urlContent.getText()).append("</a>");
                    return sb.toString();
                } else if (content instanceof UrlAnchor) {
                    UrlAnchor urlAnchor = (UrlAnchor) content;
                    return "<a name=\"" + urlAnchor.getAddress() + "\">" + urlAnchor.getText() + "</a>";
                } else {
                    return content.toString();
                }
            }
        } else {

            return "";

        }
    }

}
