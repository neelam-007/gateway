/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.alert;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Configuration for an audit alert action that sends email.
 */
public class EmailNotification extends Notification implements Serializable {
    private Pattern commaMatcher = Pattern.compile(",\\s*");

    public EmailNotification() {
    }

    /** Comma/whitespace-separated email address to include in the To: field of outbound emails */
    private String toAddresses;

    /** Comma/whitespace-separated email address to include in the CC: field of outbound emails */
    private String ccAddresses;

    /** Comma/whitespace-separated email address to include in the BCC: field of outbound emails */
    private String bccAddresses;

    /** String to include in the subject line of outbound emails */
    private String subject;

    /** Text to include in the body of outbound emails */
    private String text;

    public String getToAddresses() {
        return toAddresses;
    }

    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }

    public String getCcAddresses() {
        return ccAddresses;
    }

    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    public String getBccAddresses() {
        return bccAddresses;
    }

    public void setBccAddresses(String bccAddresses) {
        this.bccAddresses = bccAddresses;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


}
