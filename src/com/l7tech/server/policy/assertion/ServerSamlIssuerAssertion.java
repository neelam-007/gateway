/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.security.saml.Attribute;
import com.l7tech.common.security.saml.KeyInfoInclusionType;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerSamlIssuerAssertion extends AbstractServerAssertion<SamlIssuerAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSamlIssuerAssertion.class.getName());

    private final Auditor auditor;
    private final int defaultBeforeOffsetMinutes;
    private final SubjectStatement.Confirmation confirmationMethod;
    private final String[] varsUsed;
    private final Integer version;
    private SamlAssertionGenerator samlAssertionGenerator;

    public ServerSamlIssuerAssertion(SamlIssuerAssertion assertion, ApplicationContext spring) throws ServerPolicyException {
        super(assertion);

        {
            final Integer ver = assertion.getVersion();
            if (!(ver == null || ver == 1 || ver == 2)) throw new ServerPolicyException(assertion, "Unsupported SAML version " + ver);
            this.version = ver;
        }

        {
            auditor = new Auditor(this, spring, logger);
        }
        ServerConfig sc = (ServerConfig) spring.getBean("serverConfig");
        X509Certificate serverCert = (X509Certificate) spring.getBean("sslKeystoreCertificate");

        this.defaultBeforeOffsetMinutes = sc.getIntProperty("samlBeforeOffsetMinute", 2);
        this.varsUsed = assertion.getVariablesUsed();
        this.confirmationMethod = SubjectStatement.Confirmation.forUri(assertion.getSubjectConfirmationMethodUri());


        SignerInfo signerInfo;
        if (serverCert == null) throw new IllegalStateException("Unable to locate server certificate");
        X509Certificate[] serverCertChain = new X509Certificate[]{serverCert};
        if (assertion.isSignAssertion()) {
            signerInfo = new SignerInfo((PrivateKey) spring.getBean("sslKeystorePrivateKey"), serverCertChain);
        } else {
            signerInfo = new SignerInfo(null, serverCertChain);
        }
        this.samlAssertionGenerator = new SamlAssertionGenerator(signerInfo);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Generate the SAML assertion
        final TcpKnob tcpKnob = context.getRequest().getTcpKnob();
        final String clientAddress = tcpKnob == null ? null : tcpKnob.getRemoteAddress();
        final LoginCredentials creds = context.getLastCredentials(); // TODO support some choice of credentials

        final SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        if (clientAddress != null) try {
            options.setClientAddress(InetAddress.getByName(clientAddress));
        } catch (UnknownHostException e) {
            throw new PolicyAssertionException(assertion, "Couldn't resolve client IP address", e); // Can't happen (it really is an IP address)
        }
        
        if (version == 2) {
            options.setVersion(SamlAssertionGenerator.Options.VERSION_2);
        }
        options.setBeforeOffsetMinutes(defaultBeforeOffsetMinutes);

        final Map<String, Object> vars;
        if (varsUsed.length == 0) {
            vars = Collections.emptyMap();
        } else {
            vars = context.getVariableMap(varsUsed, auditor);
        }

        // TODO support multiple statements in one Assertion someday (likely only for SAML 2)
        final SubjectStatement subjectStatement;
        final String nameValue;
        final String nameFormat;
        switch(assertion.getNameIdentifierType()) {
            case FROM_CREDS:
                nameValue = null; // SAML generator already has logic to do this
                nameFormat = assertion.getNameIdentifierFormat();
                break;
            case FROM_USER:
                User u = context.getLastAuthenticatedUser(); // TODO support some choice of user
                if (u == null) {
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_AUTH_REQUIRED);
                    return AssertionStatus.AUTH_REQUIRED;
                }

                // TODO choose a different User attribute based on assertion.getNameIdentifierFormat()
                String n = u.getName();
                if (n == null) n = u.getLogin();
                nameValue = n;
                nameFormat = assertion.getNameIdentifierFormat();
                break;
            case SPECIFIED:
                nameValue = ExpandVariables.process(assertion.getNameIdentifierValue(), vars);
                nameFormat = assertion.getNameIdentifierFormat();
                break;
            case NONE:
            default:
                nameValue = null;
                nameFormat = null;
                break;
        }

        if (assertion.getAttributeStatement() != null) {
            subjectStatement = makeAttributeStatement(creds, version, vars, nameValue, nameFormat);
        } else if (assertion.getAuthenticationStatement() != null) {
            subjectStatement = makeAuthenticationStatement(creds, nameValue, nameFormat);
        } else if (assertion.getAuthorizationStatement() != null) {
            subjectStatement = makeAuthorizationStatement(creds, vars, nameValue, nameFormat);
        } else {
            throw new PolicyAssertionException(assertion, "No Subject statement selected");
        }

        if (assertion.isSignAssertion()) {
            options.setSignAssertion(true);
            options.setIssuerKeyInfoType(assertion.getSignatureKeyInfoType());
        } else {
            options.setSignAssertion(false);
            options.setIssuerKeyInfoType(KeyInfoInclusionType.NONE);
        }

        try {
            final Document signedAssertionDoc = samlAssertionGenerator.createAssertion(subjectStatement, options);
            context.setVariable("issuedSamlAssertion", XmlUtil.nodeToString(signedAssertionDoc.getDocumentElement()));
            if (assertion.getAttributeStatement() != null) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_ATTR);
                return AssertionStatus.NONE;
            } else if (assertion.getAuthenticationStatement() != null) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHN);
                return AssertionStatus.NONE;
            } else if (assertion.getAuthorizationStatement() != null) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHZ);
                return AssertionStatus.NONE;
            } else {
                throw new PolicyAssertionException(assertion, "No statement selected");
            }
        } catch (SignatureException e) {
            return AssertionStatus.FAILED; // TODO AUDIT
        } catch (CertificateException e) {
            return AssertionStatus.FAILED; // TODO AUDIT
        }
    }

    private SubjectStatement makeAuthorizationStatement(LoginCredentials creds, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat) {
        final SamlAuthorizationStatement authz = assertion.getAuthorizationStatement();
        return SubjectStatement.createAuthorizationStatement(
                creds,
                confirmationMethod,
                assertion.getSubjectConfirmationKeyInfoType(),
                ExpandVariables.process(authz.getResource(), vars),
                ExpandVariables.process(authz.getAction(), vars),
                ExpandVariables.process(authz.getActionNamespace(), vars),
                assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat);
    }

    private SubjectStatement makeAuthenticationStatement(LoginCredentials creds, String overrideNameValue, String overrideNameFormat) {
        return SubjectStatement.createAuthenticationStatement(
                    creds,
                    confirmationMethod,
                    assertion.getSubjectConfirmationKeyInfoType(),
                    assertion.getNameIdentifierType(),
                    overrideNameValue,
                    overrideNameFormat);
    }

    private SubjectStatement makeAttributeStatement(LoginCredentials creds, Integer version, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat) throws PolicyAssertionException {
        List<Attribute> outAtts = new ArrayList<Attribute>();
        for (SamlAttributeStatement.Attribute attribute : assertion.getAttributeStatement().getAttributes()) {
            String name = ExpandVariables.process(attribute.getName(), vars);
            String nameFormatOrNamespace;
            switch(version) {
                case 1:
                    nameFormatOrNamespace = ExpandVariables.process(attribute.getNamespace(), vars);
                    break;
                case 2:
                    nameFormatOrNamespace = ExpandVariables.process(attribute.getNameFormat(), vars);
                    break;
                default:
                    throw new RuntimeException(); // Can't happen
            }

            if (attribute.isRepeatIfMulti()) {
                // Repeat this attribute once for each value
                Object obj = ExpandVariables.processSingleVariableAsObject(attribute.getValue(), vars);
                if (obj instanceof Object[]) {
                    Object[] vals = (Object[]) obj;
                    for (Object val : vals) {
                        final String s = val.toString();
                        outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                        auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    }
                } else {
                    final String s = obj.toString();
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                }
            } else {
                // If it happens to be multivalued, ExpandVariables.process will join the values with a
                // delimiter.
                final String value = ExpandVariables.process(attribute.getValue(), vars);
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, value);
                outAtts.add(new Attribute(name, nameFormatOrNamespace, value));
            }
        }

        return SubjectStatement.createAttributeStatement(
                creds, confirmationMethod, outAtts.toArray(new Attribute[0]),
                assertion.getSubjectConfirmationKeyInfoType(), assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat);
    }
}
