/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for an Email {@link NotificationRule}.
 */
public class EmailNotificationRule extends NotificationRule {
    private String smtpHost;
    private int port = 25;
    private CryptoType cryptoType = CryptoType.PLAIN;
    private AuthInfo authInfo = null;
    private String from = "nomailbox@NOWHERE";
    private List<String> to = new ArrayList<String>();
    private List<String> cc = new ArrayList<String>();
    private List<String> bcc = new ArrayList<String>();
    private List<Pair<String, String>> extraHeaders = new ArrayList<Pair<String, String>>();
    private String subject;
    private String text;

    public EmailNotificationRule(MonitoringConfiguration configuration) {
        super(configuration, Type.EMAIL);
    }

    protected EmailNotificationRule() {
        super(Type.EMAIL);
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public CryptoType getCryptoType() {
        return cryptoType;
    }

    public void setCryptoType(CryptoType cryptoType) {
        this.cryptoType = cryptoType;
    }

    /**
     * The username and password to use for authentication, or null if authentication is not required
     */
    public AuthInfo getAuthInfo() {
        return authInfo;
    }

    public void setAuthInfo(AuthInfo authInfo) {
        this.authInfo = authInfo;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
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

    public List<Pair<String, String>> getExtraHeaders() {
        return extraHeaders;
    }

    /**
     * Additional headers to include in the message
     */
    public void setExtraHeaders(List<Pair<String, String>> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    public enum CryptoType {
        PLAIN, SSL, STARTTLS
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EmailNotificationRule that = (EmailNotificationRule) o;

        if (port != that.port) return false;
        if (authInfo != null ? !authInfo.equals(that.authInfo) : that.authInfo != null) return false;
        if (bcc != null ? !bcc.equals(that.bcc) : that.bcc != null) return false;
        if (cc != null ? !cc.equals(that.cc) : that.cc != null) return false;
        if (cryptoType != that.cryptoType) return false;
        if (extraHeaders != null ? !extraHeaders.equals(that.extraHeaders) : that.extraHeaders != null) return false;
        if (smtpHost != null ? !smtpHost.equals(that.smtpHost) : that.smtpHost != null) return false;
        if (subject != null ? !subject.equals(that.subject) : that.subject != null) return false;
        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (to != null ? !to.equals(that.to) : that.to != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (smtpHost != null ? smtpHost.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (cryptoType != null ? cryptoType.hashCode() : 0);
        result = 31 * result + (authInfo != null ? authInfo.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (cc != null ? cc.hashCode() : 0);
        result = 31 * result + (bcc != null ? bcc.hashCode() : 0);
        result = 31 * result + (extraHeaders != null ? extraHeaders.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
