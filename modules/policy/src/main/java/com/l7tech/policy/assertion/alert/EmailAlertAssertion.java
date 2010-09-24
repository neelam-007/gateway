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
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.util.ArrayList;
import java.util.Arrays;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An assertion that sends an email base64message.
 */
public class EmailAlertAssertion extends Assertion implements UsesVariables {
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
        String[] hostReferencedNames;
        String[] toRefNames;
        String[] bccRefNames;
        String[] ccRefNames;
        String[] subjectRefNames;
        String[] fromRefNames;
        String[] userRefNames;
        String[] pwdRefNames;
        String[] portRefNames;
        ArrayList<String> list = new ArrayList<String>();


        //Variables originally were only used in the message string.
        //Now let's check all the fields for variables.
        //the vars are: message, host, to, from, cc, bcc, subject, port, username, pwd
        //(also going to keep track of what fields have context vars to speed processing hopefully later)
        //First get the message ones:
        //(note that the original code did not check for null in .messageString() so I left it as is
        String[] messageReferencedNames = Syntax.getReferencedNames(this.messageString());
        if(messageReferencedNames!=null){
            list.addAll(Arrays.asList(messageReferencedNames));
        }

        //Now get: port
        if (this.smtpPort != null){
            portRefNames = Syntax.getReferencedNames(this.smtpPort);
            if(portRefNames!=null){
                list.addAll(Arrays.asList(portRefNames));
            }
        }


        //Now get: host
        if (this.smtpHost != null){
            hostReferencedNames = Syntax.getReferencedNames(this.smtpHost);
            if(hostReferencedNames!=null){
                list.addAll(Arrays.asList(hostReferencedNames));
            }
        }

        //now get: to
        if (this.targetEmailAddress != null){
            toRefNames = Syntax.getReferencedNames(this.targetEmailAddress);
            if(toRefNames!=null){
                list.addAll(Arrays.asList(toRefNames));
            }
            
        }
        //now get bcc
        if (this.targetBCCEmailAddress != null){
            bccRefNames = Syntax.getReferencedNames(this.targetBCCEmailAddress);
            if(bccRefNames!=null){
                list.addAll(Arrays.asList(bccRefNames));
            }
        }
        //now get cc
        if (this.targetCCEmailAddress != null){
            ccRefNames = Syntax.getReferencedNames(this.targetCCEmailAddress);
            if(ccRefNames!=null){
                list.addAll(Arrays.asList(ccRefNames));
            }
        }
        //now get the subject context vars
        if (this.subject != null){
            subjectRefNames = Syntax.getReferencedNames(this.subject);
            if(subjectRefNames!=null){
                list.addAll(Arrays.asList(subjectRefNames));
            }
        }
        //now get the from email vars
        if (this.sourceEmailAddress != null){
            fromRefNames = Syntax.getReferencedNames(this.sourceEmailAddress);
            if(fromRefNames!=null){
                list.addAll(Arrays.asList(fromRefNames));
            }
        }

        if (this.authUsername != null){
            userRefNames = Syntax.getReferencedNames(this.authUsername);
            if(userRefNames!=null){
                list.addAll(Arrays.asList(userRefNames));
            }
        }

        if (this.authPassword != null){
            pwdRefNames = Syntax.getReferencedNames(this.authPassword);
            if(pwdRefNames!=null){
                list.addAll(Arrays.asList(pwdRefNames));
            }
        }


        //(chances are there won't be context vars in all of those fields but\
        //we will check them all anyway

        //amalgamate all the arrays to a single return.
        String endList [] = (String []) list.toArray (new String [list.size ()]);

        return endList;
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
