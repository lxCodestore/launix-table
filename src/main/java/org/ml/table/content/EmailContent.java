package org.ml.table.content;

/**
 * @author Dr. Matthias Laux
 */
public class EmailContent implements Comparable<EmailContent> {

    private String address = "";

    /**
     * @param address
     */
    public EmailContent(String address) {
        if (address == null) {
            throw new NullPointerException("address may not be null");
        }
        this.address = address;
    }

    /**
     * @return
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param emailContent
     * @return
     */
    @Override
    public int compareTo(EmailContent emailContent) {
        if (emailContent == null) {
            throw new NullPointerException("emailContent may not be null");
        }
        return getAddress().compareTo(emailContent.getAddress());
    }
}
