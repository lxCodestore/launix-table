package org.ml.table.content;

/**
 * A helper class to represent URL anchor type information attached to a cell as
 * content
 *
 * @author Dr. Matthias Laux
 */
public class UrlAnchor implements Comparable<UrlAnchor> {

    private String address = "";
    private String text = "";

    /**
     * @param address The actual URL
     * @param text The text to be displayed for the URL
     */
    public UrlAnchor(String address, String text) {
        if (address == null) {
            throw new NullPointerException("address may not be null");
        }
        if (text == null) {
            throw new NullPointerException("text may not be null");
        }
        this.address = address;
        this.text = text;
    }

    /**
     * @return
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return
     */
    public String getText() {
        return text;
    }

    /**
     * @param UrlContent
     * @return
     */
    @Override
    public int compareTo(UrlAnchor UrlContent) {
        if (UrlContent == null) {
            throw new NullPointerException("URLContent may not be null");
        }
        return getAddress().compareTo(UrlContent.getAddress());
    }
}
