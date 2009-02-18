/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import static com.l7tech.policy.assertion.SamlIssuerAssertion.DecorationType.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.security.saml.Attribute;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
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
    private final WssDecorator decorator;
    private final SamlAssertionGenerator samlAssertionGenerator;
    private final SignerInfo signerInfo;
    private final String authMethodUri; // overridden auth method URI or null

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
        this.decorator = (WssDecorator) spring.getBean("wssDecorator");

        this.defaultBeforeOffsetMinutes = sc.getIntProperty("samlBeforeOffsetMinute", 2);
        this.varsUsed = assertion.getVariablesUsed();
        this.confirmationMethod = SubjectStatement.Confirmation.forUri(assertion.getSubjectConfirmationMethodUri());
        try {
            this.signerInfo = ServerAssertionUtils.getSignerInfo(spring, assertion);
        } catch (KeyStoreException e) {
            throw new ServerPolicyException(assertion, "Unable to access configured private key: " + ExceptionUtils.getMessage(e), e);
        }
        this.samlAssertionGenerator = new SamlAssertionGenerator(signerInfo);

        SamlAuthenticationStatement authnSt = assertion.getAuthenticationStatement();
        if (authnSt != null) {
            String[] methods = authnSt.getAuthenticationMethods();
            authMethodUri = methods != null && methods.length > 0 && methods[0] != null
                                ? methods[0]
                                : null;
        } else
            authMethodUri = null;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Generate the SAML assertion
        final TcpKnob tcpKnob = (TcpKnob) context.getRequest().getKnob(TcpKnob.class);
        final String clientAddress = tcpKnob == null ? null : tcpKnob.getRemoteAddress();
        LoginCredentials creds = context.getLastCredentials(); // TODO support some choice of credentials

        final SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        options.setAudienceRestriction(assertion.getAudienceRestriction());
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

        String nameQualifier = assertion.getNameQualifier();
        if (nameQualifier != null) nameQualifier = ExpandVariables.process(nameQualifier, vars, auditor);

        String nameValue;
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

                nameFormat = assertion.getNameIdentifierFormat();
                if (nameFormat != null && nameFormat.equals(SamlConstants.NAMEIDENTIFIER_EMAIL)) {
                    nameValue = u.getEmail();
                } else if (nameFormat != null && nameFormat.equals(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT)) {
                    X509Certificate foundCert = null;
                    for (AuthenticationResult result : context.getAllAuthenticationResults()) {
                        X509Certificate cert = result.getAuthenticatedCert();
                        if (cert != null) {
                            foundCert = cert;
                            break;
                        }
                    }
                    nameValue = foundCert == null ? u.getSubjectDn() : foundCert.getSubjectDN().getName();
                } else {
                    nameValue = u.getLogin();
                }

                if (nameValue == null) nameValue = u.getName();

                break;
            case SPECIFIED:
                String val = assertion.getNameIdentifierValue();
                if (val == null) {
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_MISSING_NIVAL);
                    nameValue = null;
                } else {
                    nameValue = ExpandVariables.process(val, vars, auditor);
                }
                nameFormat = assertion.getNameIdentifierFormat();
                if (creds == null)
                    creds = new LoginCredentials(null, nameValue == null ? null : nameValue.toCharArray(), CredentialFormat.OPAQUETOKEN, SamlIssuerAssertion.class);
                break;
            case NONE:
            default:
                nameValue = null;
                nameFormat = null;
                break;
        }

        final List<SubjectStatement> statements = new LinkedList<SubjectStatement>();
        if (assertion.getAttributeStatement() != null)
            statements.add(makeAttributeStatement(creds, version, vars, nameValue, nameFormat, nameQualifier));
        if (assertion.getAuthenticationStatement() != null)
            statements.add(makeAuthenticationStatement(creds, nameValue, nameFormat, nameQualifier, authMethodUri));
        if (assertion.getAuthorizationStatement() != null)
            statements.add(makeAuthorizationStatement(creds, vars, nameValue, nameFormat, nameQualifier));

        if (statements.isEmpty()) throw new PolicyAssertionException(assertion, "No Subject Statement type selected");

        if (assertion.isSignAssertion()) {
            options.setSignAssertion(true);
            options.setIssuerKeyInfoType(assertion.getSignatureKeyInfoType());
        } else {
            options.setSignAssertion(false);
            options.setIssuerKeyInfoType(KeyInfoInclusionType.NONE);
        }

        try {
            final Element assertionEl = samlAssertionGenerator.createAssertion(statements.toArray(new SubjectStatement[0]), options).getDocumentElement();
            context.setVariable("issuedSamlAssertion", XmlUtil.nodeToString(assertionEl));

            EnumSet<SamlIssuerAssertion.DecorationType> dts = assertion.getDecorationTypes();

            if (dts == null || dts.isEmpty()) {
                // No decoration required, we're done
                auditDone();
                return AssertionStatus.NONE;
            }

            final Message msg;
            if (dts.contains(REQUEST)) {
                msg = context.getRequest();
            } else if (dts.contains(RESPONSE)) {
                msg = context.getResponse();
            } else throw new IllegalStateException("Some decoration was selected, but on neither request nor response");

            try {
                if (!msg.isSoap()) {
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_NOT_SOAP);
                    return AssertionStatus.NOT_APPLICABLE;
                }
            } catch (SAXException e) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
                return AssertionStatus.BAD_REQUEST;
            }

            XmlKnob xk = (XmlKnob) msg.getKnob(XmlKnob.class);
            if (xk == null) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_NOT_XML);
                return AssertionStatus.FAILED;
            }

            final Document messageDoc;
            try {
                messageDoc = xk.getDocumentWritable();
            } catch (SAXException e) {
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
                return AssertionStatus.BAD_REQUEST;
            }

            if (dts.contains(ADD_ASSERTION)) {
                DecorationRequirements dr = new DecorationRequirements();
                dr.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                dr.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                dr.setSecurityHeaderReusable(true);
                dr.setSecurityHeaderActor(null);
                Element sec = SoapUtil.getOrMakeSecurityElement(messageDoc);
                Element newEl = (Element) messageDoc.importNode(assertionEl, true);
                sec.appendChild(newEl);
                if (dts.contains(SIGN_ASSERTION)) dr.getElementsToSign().add(newEl);

                if (dts.contains(SIGN_BODY)) {
                    try {
                        dr.getElementsToSign().add( SoapUtil.getBodyElement(messageDoc));
                    } catch ( InvalidDocumentFormatException e) {
                        auditor.logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
                        return AssertionStatus.BAD_REQUEST;
                    }
                }

                try {
                    decorator.decorateMessage(new Message(messageDoc), dr);
                } catch (Exception e) {
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_CANT_DECORATE, null, e);
                    return AssertionStatus.FAILED;
                }
            }

            auditDone();
            return AssertionStatus.NONE;
        } catch (SignatureException e) {
            return AssertionStatus.FAILED; // TODO AUDIT
        } catch (CertificateException e) {
            return AssertionStatus.FAILED; // TODO AUDIT
        }
    }

    private void auditDone() throws PolicyAssertionException {
        if (assertion.getAttributeStatement() != null) {
            auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_ATTR);
        } else if (assertion.getAuthenticationStatement() != null) {
            auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHN);
        } else if (assertion.getAuthorizationStatement() != null) {
            auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHZ);
        } else {
            throw new PolicyAssertionException(assertion, "No statement selected");
        }
    }

    private SubjectStatement makeAuthorizationStatement(LoginCredentials creds, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat, String nameQualifier) {
        final SamlAuthorizationStatement authz = assertion.getAuthorizationStatement();
        return SubjectStatement.createAuthorizationStatement(
                creds,
                confirmationMethod,
                assertion.getSubjectConfirmationKeyInfoType(),
                ExpandVariables.process(authz.getResource(), vars, auditor),
                ExpandVariables.process(authz.getAction(), vars, auditor),
                ExpandVariables.process(authz.getActionNamespace(), vars, auditor),
                assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat, nameQualifier);
    }

    private SubjectStatement makeAuthenticationStatement(LoginCredentials creds, String overrideNameValue, String overrideNameFormat, String nameQualifier, String overrideAuthnMethodUri) {
        return SubjectStatement.createAuthenticationStatement(
                    creds,
                    confirmationMethod,
                    assertion.getSubjectConfirmationKeyInfoType(),
                    assertion.getNameIdentifierType(),
                    overrideNameValue,
                    overrideNameFormat,
                    nameQualifier,
                    overrideAuthnMethodUri);
    }

    private SubjectStatement makeAttributeStatement(LoginCredentials creds, Integer version, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat, String nameQualifier) throws PolicyAssertionException {
        List<Attribute> outAtts = new ArrayList<Attribute>();
        for (SamlAttributeStatement.Attribute attribute : assertion.getAttributeStatement().getAttributes()) {
            String name = ExpandVariables.process(attribute.getName(), vars, auditor);
            String nameFormatOrNamespace;
            switch(version) {
                case 1:
                    nameFormatOrNamespace = ExpandVariables.process(attribute.getNamespace(), vars, auditor);
                    break;
                case 2:
                    String nf = attribute.getNameFormat();
                    if (nf == null) nf = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                    nameFormatOrNamespace = ExpandVariables.process(nf, vars, auditor);
                    break;
                default:
                    throw new RuntimeException(); // Can't happen
            }

            if (attribute.isRepeatIfMulti()) {
                // Repeat this attribute once for each value
                Object obj = ExpandVariables.processSingleVariableAsDisplayableObject(attribute.getValue(), vars, auditor);
                if (obj instanceof Object[]) {
                    Object[] vals = (Object[]) obj;
                    for (Object val : vals) {
                        final String s = val.toString();
                        outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                        auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    }
                } else {
                    // ExpandVariables will have already thrown/logged a warning if the variable is bad
                    final String s = obj == null ? "" : obj.toString();
                    auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                }
            } else {
                // If it happens to be multivalued, ExpandVariables.process will join the values with a
                // delimiter.
                final String value = ExpandVariables.process(attribute.getValue(), vars, auditor);
                auditor.logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, value);
                outAtts.add(new Attribute(name, nameFormatOrNamespace, value));
            }
        }

        return SubjectStatement.createAttributeStatement(
                creds, confirmationMethod, outAtts.toArray(new Attribute[0]),
                assertion.getSubjectConfirmationKeyInfoType(), assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat, nameQualifier);
    }
}
