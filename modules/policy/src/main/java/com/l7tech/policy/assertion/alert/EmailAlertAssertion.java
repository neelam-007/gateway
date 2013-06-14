/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.alert;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.util.Arrays;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An assertion that sends an email base64message.
 */
public class EmailAlertAssertion extends Assertion implements UsesVariables {
    private static final String META_INITIALIZED = EmailAlertAssertion.class.getName() + ".metadataInitialized";

    public static final String DEFAULT_HOST = "mail";
    public static final int DEFAULT_PORT = 25;
    public static final String DEFAULT_SUBJECT = "Layer 7 Gateway Email Alert";
    public static final String DEFAULT_MESSAGE = "This is an alert message from a Layer 7 Gateway.";
    public static final String DEFAULT_FROM = "L7SSG@NOMAILBOX";

    private String targetEmailAddress = "";
    private String targetCCEmailAddress = "";
    private String targetBCCEmailAddress = "";
    private String sourceEmailAddress = DEFAULT_FROM;
    private String smtpHost = DEFAULT_HOST;
    private String smtpPort = Integer.toString(DEFAULT_PORT);
    private String subject = DEFAULT_SUBJECT;
    private String base64message = "";
    private Protocol protocol = Protocol.PLAIN;
    private boolean authenticate = false;
    private boolean contextVarPassword = false;
    private String authUsername;
    private String authPassword;
    private boolean isTestBean = false;

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

    public boolean isTestBean(){
        return isTestBean;
    }

    public void setIsTestBean(boolean isTestBean){
        this.isTestBean = isTestBean;
    }

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

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    @Deprecated
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = Integer.toString(smtpPort);
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
        return new String(HexUtils.decodeBase64(base64message, true), Charsets.UTF8);
    }

    public void messageString(String text) {
        setBase64message(HexUtils.encodeBase64(HexUtils.encodeUtf8(text), true));
    }

    /**
     * @return the source email address.  May be empty but never null.
     */
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

    public boolean isContextVarPassword() {
        return contextVarPassword;
    }

    public void setContextVarPassword(boolean contextVarPassword) {
        this.contextVarPassword = contextVarPassword;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(
                messageString(),
                smtpPort,
                smtpHost,
                targetEmailAddress,
                targetBCCEmailAddress,
                targetCCEmailAddress,
                subject,
                sourceEmailAddress,
                authUsername,
                contextVarPassword ? authPassword : null
        );
    }

    private final static String baseName = "Send Email Alert";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<EmailAlertAssertion>() {
        @Override
        public String getAssertionName(final EmailAlertAssertion assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

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

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
