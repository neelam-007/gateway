/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.alert;

import com.l7tech.policy.assertion.Assertion;

/**
 * An assertion that sends an email message.
 */
public class EmailAlertAssertion extends Assertion {
    public static final String DEFAULT_HOST = "mail";
    public static final int DEFAULT_PORT = 25;
    public static final String DEFAULT_SUBJECT = "Layer 7 SecureSpan Gateway Email Alert";
    public static final String DEFAULT_MESSAGE = "This is an alert message from a Layer 7 SecureSpan Gateway.";

    private String targetEmailAddress = "";
    private String smtpHost = DEFAULT_HOST;
    private int smtpPort = DEFAULT_PORT;
    private String subject = DEFAULT_SUBJECT;
    private String message = DEFAULT_MESSAGE;

    public EmailAlertAssertion() {
    }

    public EmailAlertAssertion(String subject, String message, String targetEmailAddress, String snmpServer) {
        if (targetEmailAddress == null || snmpServer == null)
            throw new NullPointerException();
        if (subject == null) subject = "";
        if (message == null) message = "";
        this.subject = subject;
        this.message = message;
        this.targetEmailAddress = targetEmailAddress;
        this.smtpHost = snmpServer;
    }

    public String getTargetEmailAddress() {
        return targetEmailAddress;
    }

    public void setTargetEmailAddress(String targetEmailAddress) {
        if (targetEmailAddress == null) throw new NullPointerException();
        this.targetEmailAddress = targetEmailAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        if (smtpHost == null) throw new NullPointerException();
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        if (smtpPort < 0 || smtpPort > 65535) throw new IllegalArgumentException();
        this.smtpPort = smtpPort;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        if (subject == null) subject = "";
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null) message = "";
        this.message = message;
    }
}
