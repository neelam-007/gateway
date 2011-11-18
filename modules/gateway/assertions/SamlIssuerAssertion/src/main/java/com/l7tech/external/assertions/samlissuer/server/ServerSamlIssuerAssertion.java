package com.l7tech.external.assertions.samlissuer.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlissuer.SamlIssuerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.security.saml.Attribute;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.l7tech.policy.assertion.SamlIssuerConfiguration.DecorationType.*;
import static com.l7tech.util.Functions.grep;

/**
 * @author alex
 */
public class ServerSamlIssuerAssertion extends AbstractServerAssertion<SamlIssuerAssertion> {
    private final int defaultBeforeOffsetMinutes;
    private final int defaultAfterOffsetMinutes;
    private final SubjectStatement.Confirmation confirmationMethod;
    private final String[] varsUsed;
    private final Integer version;
    private final WssDecorator decorator;
    private final SamlAssertionGenerator samlAssertionGenerator;
    private final SignerInfo signerInfo;
    private final String authMethodUri; // overridden auth method URI or null

    private final boolean validateUris = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.samlissuer.validateUris", true );

    public ServerSamlIssuerAssertion(SamlIssuerAssertion assertion, ApplicationContext spring) throws ServerPolicyException {
        super(assertion);

        {
            final Integer ver = assertion.getVersion();
            if (!(ver == null || ver == 1 || ver == 2)) throw new ServerPolicyException(assertion, "Unsupported SAML version " + ver);
            this.version = ver;
        }

        Config sc = spring.getBean("serverConfig", Config.class);
        this.decorator = spring.getBean("wssDecorator", WssDecorator.class);

        this.defaultBeforeOffsetMinutes = sc.getIntProperty("samlBeforeOffsetMinute", 2);
        this.defaultAfterOffsetMinutes = sc.getIntProperty("samlAfterOffsetMinute", 5);
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

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Generate the SAML assertion
        final TcpKnob tcpKnob = context.getRequest().getKnob(TcpKnob.class);
        final String clientAddress = tcpKnob == null ? null : tcpKnob.getRemoteAddress();
        LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials(); // TODO support some choice of credentials

        final Map<String, Object> vars;
        if (varsUsed.length == 0) {
            vars = Collections.emptyMap();
        } else {
            vars = context.getVariableMap(varsUsed, getAudit());
        }
        
        final SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        final String testAudienceRestriction = assertion.getAudienceRestriction();
        if( testAudienceRestriction != null && !testAudienceRestriction.isEmpty() ) {
            final List<String> allResolvedStrings = grep(ContextVariableUtils.getAllResolvedStrings(testAudienceRestriction,
                    vars, getAudit(), TextUtils.URI_STRING_SPLIT_PATTERN,
                    new Functions.UnaryVoid<Object>() {
                        @Override
                        public void call(Object unexpectedNonString) {
                            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Found non string value for audience restriction: " + unexpectedNonString);
                        }
                    }), new Functions.Unary<Boolean, String>() {
                @Override
                public Boolean call(String possibleUri) {
                    final boolean isValidUri = ValidationUtils.isValidUri(possibleUri);
                    if (validateUris && !isValidUri) {
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Ignoring invalid URI value for audience restriction '" + possibleUri + "'");
                    }
                    return !validateUris || isValidUri;
                }
            });

            options.setAudienceRestriction(allResolvedStrings);
        }
        if (clientAddress != null) try {
            options.setClientAddress(InetAddress.getByName(clientAddress));
        } catch (UnknownHostException e) {
            throw new PolicyAssertionException(assertion, "Couldn't resolve client IP address", e); // Can't happen (it really is an IP address)
        }

        if (version == 2) {
            options.setVersion(SamlAssertionGenerator.Options.VERSION_2);
        }

        int assertionNotBeforeSeconds = assertion.getConditionsNotBeforeSecondsInPast();
        options.setNotBeforeSeconds(assertionNotBeforeSeconds != -1 ? assertionNotBeforeSeconds : defaultBeforeOffsetMinutes * 60);
        int assertionNotAfterSeconds = assertion.getConditionsNotOnOrAfterExpirySeconds();
        options.setNotAfterSeconds(assertionNotAfterSeconds != -1 ? assertionNotAfterSeconds : defaultAfterOffsetMinutes * 60);

        String nameQualifier = assertion.getNameQualifier();
        if (nameQualifier != null) nameQualifier = ExpandVariables.process(nameQualifier, vars, getAudit());

        String nameValue;
        final String nameFormat;
        switch(assertion.getNameIdentifierType()) {
            case FROM_CREDS:
                nameValue = null; // SAML generator already has logic to do this
                nameFormat = assertion.getNameIdentifierFormat();
                break;
            case FROM_USER:
                User u = context.getDefaultAuthenticationContext().getLastAuthenticatedUser(); // TODO support some choice of user
                if (u == null) {
                    logAndAudit(AssertionMessages.SAML_ISSUER_AUTH_REQUIRED);
                    return AssertionStatus.AUTH_REQUIRED;
                }

                nameFormat = assertion.getNameIdentifierFormat();
                if (nameFormat != null && nameFormat.equals(SamlConstants.NAMEIDENTIFIER_EMAIL)) {
                    nameValue = u.getEmail();
                } else if (nameFormat != null && nameFormat.equals(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT)) {
                    X509Certificate foundCert = null;
                    for (AuthenticationResult result : context.getDefaultAuthenticationContext().getAllAuthenticationResults()) {
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
                    logAndAudit(AssertionMessages.SAML_ISSUER_MISSING_NIVAL);
                    nameValue = null;
                } else {
                    nameValue = ExpandVariables.process(val, vars, getAudit());
                }
                nameFormat = assertion.getNameIdentifierFormat();
                if (creds == null)
                    creds = LoginCredentials.makeLoginCredentials(new OpaqueSecurityToken(nameValue == null ? "" : nameValue, null), SamlIssuerAssertion.class);
                break;
            case NONE:
            default:
                nameValue = null;
                nameFormat = null;
                break;
        }

        if (creds == null) {
            // if creds are null, then they are missing and required. Fail early.
            logAndAudit(AssertionMessages.IDENTITY_NO_CREDS);
            return AssertionStatus.FALSIFIED;
        }

        final List<SubjectStatement> statements = new LinkedList<SubjectStatement>();
        if (assertion.getAttributeStatement() != null)
            statements.add(makeAttributeStatement(creds, version, vars, nameValue, nameFormat, nameQualifier));
        if (assertion.getAuthenticationStatement() != null)
            statements.add(makeAuthenticationStatement(creds, nameValue, nameFormat, nameQualifier, authMethodUri,
                    assertion.getAuthenticationStatement().isIncludeAuthenticationContextDeclaration()));
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

        options.setSubjectConfirmationDataAddress( nullSafeExpand( assertion.getSubjectConfirmationDataAddress(), vars) );
        options.setSubjectConfirmationDataInResponseTo( nullSafeExpand( assertion.getSubjectConfirmationDataInResponseTo(), vars) );
        options.setSubjectConfirmationDataRecipient( nullSafeExpand( assertion.getSubjectConfirmationDataRecipient(), vars) );
        options.setSubjectConfirmationDataNotBeforeSecondsInPast( assertion.getSubjectConfirmationDataNotBeforeSecondsInPast() );
        options.setSubjectConfirmationDataNotOnOrAfterExpirySeconds( assertion.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() );

        options.setCustomIssuer(nullSafeExpand(assertion.getCustomizedIssuerValue(), vars));
        options.setCustomIssuerNameQualifier(nullSafeExpand(assertion.getCustomizedIssuerNameQualifier(), vars));
        options.setCustomIssuerNameFormatUri(assertion.getCustomizedIssuerNameFormat());

        try {
            final Element assertionEl = samlAssertionGenerator.createAssertion(statements.toArray(new SubjectStatement[statements.size()]), options).getDocumentElement();
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
                    logAndAudit(AssertionMessages.SAML_ISSUER_NOT_SOAP);
                    return AssertionStatus.NOT_APPLICABLE;
                }
            } catch (SAXException e) {
                logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
                return AssertionStatus.BAD_REQUEST;
            }

            XmlKnob xk = msg.getKnob(XmlKnob.class);
            if (xk == null) {
                logAndAudit(AssertionMessages.SAML_ISSUER_NOT_XML);
                return AssertionStatus.FAILED;
            }

            final Document messageDoc;
            try {
                messageDoc = xk.getDocumentWritable();
            } catch (SAXException e) {
                logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
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
                        logAndAudit(AssertionMessages.SAML_ISSUER_BAD_XML, null, e);
                        return AssertionStatus.BAD_REQUEST;
                    }
                }

                try {
                    decorator.decorateMessage(new Message(messageDoc), dr);
                } catch (Exception e) {
                    logAndAudit(AssertionMessages.SAML_ISSUER_CANT_DECORATE, null, e);
                    return AssertionStatus.FAILED;
                }
            }

            auditDone();
            return AssertionStatus.NONE;
        } catch (GeneralSecurityException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to issue assertion: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }
    }

    private String nullSafeExpand( final String text, final Map<String,Object> variables ) {
        String value = null;

        if ( text != null ) {
            value = ExpandVariables.process( text, variables, getAudit() );
        }

        return value;
    }


    private void auditDone() throws PolicyAssertionException {
        if (assertion.getAttributeStatement() != null) {
            logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_ATTR);
        } else if (assertion.getAuthenticationStatement() != null) {
            logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHN);
        } else if (assertion.getAuthorizationStatement() != null) {
            logAndAudit(AssertionMessages.SAML_ISSUER_ISSUED_AUTHZ);
        } else {
            throw new PolicyAssertionException(assertion, "No statement selected");
        }
    }

    private SubjectStatement makeAuthorizationStatement(@NotNull LoginCredentials creds, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat, String nameQualifier) {
        final SamlAuthorizationStatement authz = assertion.getAuthorizationStatement();
        return SubjectStatement.createAuthorizationStatement(
                creds,
                confirmationMethod,
                assertion.getSubjectConfirmationKeyInfoType(),
                ExpandVariables.process(authz.getResource(), vars, getAudit()),
                ExpandVariables.process(authz.getAction(), vars, getAudit()),
                ExpandVariables.process(authz.getActionNamespace(), vars, getAudit()),
                assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat, nameQualifier);
    }

    private SubjectStatement makeAuthenticationStatement(LoginCredentials creds,
                                                         String overrideNameValue,
                                                         String overrideNameFormat,
                                                         String nameQualifier,
                                                         String overrideAuthnMethodUri,
                                                         boolean includeAuthenticationContextDeclaration) {
        return SubjectStatement.createAuthenticationStatement(
                    creds,
                    confirmationMethod,
                    assertion.getSubjectConfirmationKeyInfoType(),
                    assertion.getNameIdentifierType(),
                    overrideNameValue,
                    overrideNameFormat,
                    nameQualifier,
                    overrideAuthnMethodUri,
                    includeAuthenticationContextDeclaration);
    }

    private SubjectStatement makeAttributeStatement(final LoginCredentials creds,
                                                    final Integer version,
                                                    final Map<String, Object> vars,
                                                    final String overrideNameValue,
                                                    final String overrideNameFormat,
                                                    final String nameQualifier) throws PolicyAssertionException {
        List<Attribute> outAtts = new ArrayList<Attribute>();
        for (SamlAttributeStatement.Attribute attribute : assertion.getAttributeStatement().getAttributes()) {

            String name = ExpandVariables.process(attribute.getName(), vars, getAudit());
            String nameFormatOrNamespace;
            switch(version) {
                case 1:
                    nameFormatOrNamespace = ExpandVariables.process(attribute.getNamespace(), vars, getAudit());
                    break;
                case 2:
                    String nf = attribute.getNameFormat();
                    if (nf == null) nf = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                    nameFormatOrNamespace = ExpandVariables.process(nf, vars, getAudit());
                    break;
                default:
                    throw new RuntimeException(); // Can't happen
            }

            if (attribute.isRepeatIfMulti()) {
                // Repeat this attribute once for each value
                Object obj = ExpandVariables.processSingleVariableAsDisplayableObject(attribute.getValue(), vars, getAudit());
                if (obj instanceof Object[]) {
                    Object[] vals = (Object[]) obj;
                    for (Object val : vals) {
                        final String s = val.toString();
                        outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                        logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    }
                } else {
                    // ExpandVariables will have already thrown/logged a warning if the variable is bad
                    final String s = obj == null ? "" : obj.toString();
                    logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, s);
                    outAtts.add(new Attribute(name, nameFormatOrNamespace, s));
                }
            } else {
                // If it happens to be multivalued, ExpandVariables.process will join the values with a
                // delimiter.
                final String value = ExpandVariables.process(attribute.getValue(), vars, getAudit());
                logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, name, value);
                outAtts.add(new Attribute(name, nameFormatOrNamespace, value));
            }
        }

        return SubjectStatement.createAttributeStatement(
                creds, confirmationMethod, outAtts.toArray(new Attribute[outAtts.size()]),
                assertion.getSubjectConfirmationKeyInfoType(), assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat, nameQualifier);
    }

}
