package com.l7tech.gateway.common.transport.email;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

/**
 * Bean which holds all information about an email message.
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailMessage {
    private InternetAddress[] toAddresses;
    private InternetAddress[] ccAddresses;
    private InternetAddress[] bccAddresses;
    private InternetAddress fromAddress;
    private String message;
    private String subject;

    public EmailMessage(InternetAddress[] bccAddresses, InternetAddress[] ccAddresses, InternetAddress fromAddress,
                        InternetAddress[] toAddresses, String message, String subject) {
        this.bccAddresses = bccAddresses;
        this.ccAddresses = ccAddresses;
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses;
        this.message = message;
        this.subject = subject;

    }

    public EmailMessage(String bccAddresses, String ccAddresses, String fromAddress, String toAddresses,
                        String message, String subject) {
        setBccAddresses(bccAddresses);
        setCcAddresses(ccAddresses);
        setFromAddresses(fromAddress);
        setToAddresses(toAddresses);
        this.message = message;
        this.subject = subject;

    }

    public InternetAddress[] getBccAddresses() {
        return bccAddresses;
    }

    public void setBccAddresses(InternetAddress[] bccAddresses) {
        this.bccAddresses = bccAddresses;
    }

    public void setBccAddresses(String bccAddresses) {
        try {
            this.bccAddresses = InternetAddress.parse(bccAddresses);
        } catch (AddressException e) {
            this.bccAddresses = null;
        }
    }

    public InternetAddress[] getCcAddresses() {
        return ccAddresses;
    }

    public void setCcAddresses(InternetAddress[] ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    public void setCcAddresses(String ccAddresses) {
        try {
            this.ccAddresses = InternetAddress.parse(ccAddresses);
        } catch (AddressException e) {
            this.ccAddresses = null;
        }
    }

    public InternetAddress getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(InternetAddress fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setFromAddresses(String fromAddresses) {
        try {
            this.fromAddress = new InternetAddress(fromAddresses);
        } catch (AddressException e) {
            this.fromAddress = null;
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public InternetAddress[] getToAddresses() {
        return toAddresses;
    }

    public void setToAddresses(InternetAddress[] toAddresses) {
        this.toAddresses = toAddresses;
    }

    public void setToAddresses(String toAddresses) {
        try {
            this.toAddresses = InternetAddress.parse(toAddresses);
        } catch (AddressException e) {
            this.toAddresses = null;
        }
    }

    /**
     * @return  Returns an array of all receipients email address
     */
    public InternetAddress[] getAllRecipients() {
        InternetAddress[] recipients;
        int size = 0;
        if(toAddresses != null) size += toAddresses.length;
        if(ccAddresses != null) size += ccAddresses.length;
        if(bccAddresses != null) size += bccAddresses.length;
        recipients = new InternetAddress[size];

        int i = 0;
        if(toAddresses != null) {
            for(InternetAddress address : toAddresses) {
                recipients[i++] = address;
            }
        }
        if(ccAddresses != null) {
            for(InternetAddress address : ccAddresses) {
                recipients[i++] = address;
            }
        }
        if(bccAddresses != null) {
            for(InternetAddress address : bccAddresses) {
                recipients[i++] = address;
            }
        }

        return recipients;
    }
}
