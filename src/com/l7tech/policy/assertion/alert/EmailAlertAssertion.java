/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.alert;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.common.util.HexUtils;

import java.io.IOException;

/**
 * An assertion that sends an email base64message.
 */
public class EmailAlertAssertion extends Assertion implements UsesVariables {
    public static final String DEFAULT_HOST = "mail";
    public static final int DEFAULT_PORT = 25;
    public static final String DEFAULT_SUBJECT = "Layer 7 SecureSpan Gateway Email Alert";
    public static final String DEFAULT_MESSAGE = "This is an alert message from a Layer 7 SecureSpan Gateway.";
    public static final String DEFAULT_FROM = "L7SSG@NOMAILBOX";

    private String targetEmailAddress = "";
    private String sourceEmailAddress = DEFAULT_FROM;
    private String smtpHost = DEFAULT_HOST;
    private int smtpPort = DEFAULT_PORT;
    private String subject = DEFAULT_SUBJECT;
    private String base64message = "";

    public EmailAlertAssertion() {
    }

//    public EmailAlertAssertion(String subject, String message, String targetEmailAddress, String snmpServer) {
//        if (targetEmailAddress == null || snmpServer == null)
//            throw new NullPointerException();
//        if (subject == null) subject = "";
//        if (message == null) message = "";
//        this.subject = subject;
//        this.base64message = message;
//        this.targetEmailAddress = targetEmailAddress;
//        this.smtpHost = snmpServer;
//    }

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

    public String getBase64message() {
        return base64message;
    }

    public void setBase64message(String message) {
        if (message == null) message = "";
        base64message = message;
    }

    public String messageString() {
        try {
            return new String(HexUtils.decodeBase64(base64message, true));
        } catch (IOException e) {
            return base64message;
        }
    }

    public void messageString(String text) {
        setBase64message(HexUtils.encodeBase64(text.getBytes(), true));
    }

    /** @return the source email address.  May be empty but never null. */
    public String getSourceEmailAddress() {
        return sourceEmailAddress;
    }

    public void setSourceEmailAddress(String sourceEmailAddress) {
        if (sourceEmailAddress == null) sourceEmailAddress = DEFAULT_FROM;
        this.sourceEmailAddress = sourceEmailAddress;
    }

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(this.messageString());
    }
}