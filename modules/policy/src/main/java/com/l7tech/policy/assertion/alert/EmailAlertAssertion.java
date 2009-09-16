/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.alert;

import com.l7tech.util.HexUtils;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.io.IOException;
import java.util.Arrays;

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
    private String targetCCEmailAddress = "";
    private String targetBCCEmailAddress = "";
    private String sourceEmailAddress = DEFAULT_FROM;
    private String smtpHost = DEFAULT_HOST;
    private int smtpPort = DEFAULT_PORT;
    private String subject = DEFAULT_SUBJECT;
    private String base64message = "";
    private Protocol protocol = Protocol.PLAIN;
    private boolean authenticate = false;
    private String authUsername;
    private String authPassword;

    public static enum Protocol {
        PLAIN("Plain SMTP"),
        SSL("SMTP over SSL"),
        STARTTLS("SMTP with STARTTLS"),;

        private final String description;

        private Protocol(String s) {
            this.description = s;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

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

    public String getTargetCCEmailAddress() {
        return targetCCEmailAddress;
    }

    public void setTargetCCEmailAddress(String targetCCEmailAddress) {
        if (targetCCEmailAddress == null) throw new NullPointerException();
        this.targetCCEmailAddress = targetCCEmailAddress;
    }

    public String getTargetBCCEmailAddress() {
        return targetBCCEmailAddress;
    }

    public void setTargetBCCEmailAddress(String targetBCCEmailAddress) {
        if (targetBCCEmailAddress == null) throw new NullPointerException();
        this.targetBCCEmailAddress = targetBCCEmailAddress;
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
            return new String(HexUtils.decodeBase64(base64message, true), "UTF-8");
        } catch (IOException e) {
            return base64message;
        }
    }

    public void messageString(String text) {
        setBase64message(HexUtils.encodeBase64(HexUtils.encodeUtf8(text), true));
    }

    /** @return the source email address.  May be empty but never null. */
    public String getSourceEmailAddress() {
        return sourceEmailAddress;
    }

    public void setSourceEmailAddress(String sourceEmailAddress) {
        if (sourceEmailAddress == null) sourceEmailAddress = DEFAULT_FROM;
        this.sourceEmailAddress = sourceEmailAddress;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(this.messageString());
    }

    private final static String baseName = "Send Email Alert";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<EmailAlertAssertion>(){
        @Override
        public String getAssertionName( final EmailAlertAssertion assertion, final boolean decorate) {
            return baseName;            
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Send an email message to predetermined recipients.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EmailAlertAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Email Alert Properties");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(Protocol.class, "Protocol")
        )));

        return meta;
    }
}
