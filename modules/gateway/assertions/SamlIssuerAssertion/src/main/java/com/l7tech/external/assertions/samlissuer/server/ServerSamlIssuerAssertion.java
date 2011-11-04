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
import com.l7tech.util.*;
import static com.l7tech.util.Functions.grep;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Attr;
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
            options.setAudienceRestriction(ExpandVariables.process(testAudienceRestriction, vars, getAudit()));
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

    private SubjectStatement makeAuthorizationStatement(LoginCredentials creds, Map<String, Object> vars, String overrideNameValue, String overrideNameFormat, String nameQualifier) {
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
        final String filterExpression = assertion.getAttributeStatement().getFilterExpression();

        // Support variables of Type Element or Message which resolve to saml:Attribute and warn logging anything else found.
        final List<Object> objects = ExpandVariables.processNoFormat(filterExpression, vars, getAudit(), false);

        //todo - check variables resolved from the filter expression for duplicates (name + nameformat)
        //Remove any Elements which are not valid.
        final List<Element> requestAttributeElements = grep(extractElements(objects), new Functions.Unary<Boolean, Element>() {
            @Override
            public Boolean call(Element element) {
                return validateElementIsAttribute(element, version);
            }
        });

        final boolean hasFilter = !requestAttributeElements.isEmpty();

        final String samlNamespace;
        final boolean hasNameFormat; // true for 2.0, false otherwise
        final String nameAttributeName;
        switch (version) {
            case 1:
                samlNamespace = SamlConstants.NS_SAML;
                hasNameFormat = false;
                nameAttributeName = "AttributeName";
                break;
            case 2:
                samlNamespace = SamlConstants.NS_SAML2;
                hasNameFormat = true;
                nameAttributeName = "Name";
                break;
            default:
                //this will likely have occurred before now but adding for future changes.
                throw new IllegalStateException("Unknown version");
        }

        final List<SamlAttributeStatement.Attribute> configuredAttList;
        if (!hasFilter) {
            configuredAttList = new ArrayList<SamlAttributeStatement.Attribute>();
            configuredAttList.addAll(Arrays.asList(assertion.getAttributeStatement().getAttributes()));
        } else {
            // Filter early to avoid misleading logging / auditing messages.
            configuredAttList = grep(
                    Arrays.asList(assertion.getAttributeStatement().getAttributes()),
                    new Functions.Unary<Boolean, SamlAttributeStatement.Attribute>() {
                @Override
                public Boolean call(SamlAttributeStatement.Attribute configAttribute) {
                    //we must find this static attribute in the request to include it
                    //note: incomingElement only contains validated saml:Attribute elements
                    return isConfigAttributeInRequest(configAttribute, requestAttributeElements, nameAttributeName, hasNameFormat, samlNamespace);
                }
            });
        }

        for (SamlAttributeStatement.Attribute attribute : configuredAttList) {

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

    private Boolean isConfigAttributeInRequest(final SamlAttributeStatement.Attribute configAttribute,
                                               final List<Element> incomingElements,
                                               final String nameAttributeName,
                                               final boolean hasNameFormat,
                                               final String samlNamespace) {
        boolean isInIncoming = false;
        for (Element incomingElement : incomingElements) {
            final Attr nameAttr = incomingElement.getAttributeNode(nameAttributeName);

            final String nameAttrValue = nameAttr.getValue();
            if (!configAttribute.getName().equals(nameAttrValue)) {
                continue;
            }

            if (hasNameFormat) {
                // 2.0
                final Attr nameFormatAttr = incomingElement.getAttributeNode("NameFormat");
                final String nameFormatValue;
                if (nameFormatAttr == null) {
                    nameFormatValue = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                } else {
                    nameFormatValue = nameFormatAttr.getValue();
                }

                String expectedNameFormat = configAttribute.getNameFormat();
                if (expectedNameFormat == null || expectedNameFormat.length()==0) {
                    expectedNameFormat = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                }

                if (!expectedNameFormat.equals(nameFormatValue)) {
                    continue;
                }
            } else {
                // 1.x
                final Attr attrNS = incomingElement.getAttributeNode("AttributeNamespace");
                if (!configAttribute.getNamespace().equals(attrNS.getValue())) {
                    continue;
                }
            }

            //does the incoming element have any values to check against?
            final List<Element> attributeValueElms = XmlUtil.findChildElementsByName(incomingElement, samlNamespace, "AttributeValue");
            if (attributeValueElms.isEmpty()) {
                //no values to check
                isInIncoming = true;
                break;
            } else {
                isInIncoming = true;
                //todo element attribute value comparison
            }
        }
        return isInIncoming;
    }


    private boolean validateElementIsAttribute(final Element elmToValidate, final int version) {
        boolean isValid = false;

        final String tagName = elmToValidate.getLocalName();
        final Attr name;
        final String namespaceURI = elmToValidate.getNamespaceURI();
        final String ignoreMsg = "Ignoring variable.";
        switch (version) {
            case 1:
                if (!"Attribute".equals(tagName)) {
                    logger.warning("Expected SAML Attribute Element, found Element with name '" + tagName + "'. " + ignoreMsg);
                    break;
                }

                if (!SamlConstants.NS_SAML.equals(namespaceURI)) {
                    logger.warning("Expected Namespace '"+SamlConstants.NS_SAML+"' found Element with namespace '" + namespaceURI + "'. " + ignoreMsg);
                    break;
                }

                name = elmToValidate.getAttributeNode("AttributeName");
                if (name == null) {
                    logger.warning("Attribute element missing AttributeName attribute. " + ignoreMsg);
                    break;
                }


                final Attr nameNS = elmToValidate.getAttributeNode("AttributeNamespace");
                if (nameNS == null) {
                    logger.warning("Attribute element missing Name attribute. " + ignoreMsg);
                    break;
                }

                isValid = true;
                break;
            case 2:
                if (!"Attribute".equals(tagName)) {
                    logger.warning("Expected SAML Attribute Element, found Element with name '" + tagName + "'. " + ignoreMsg);
                    break;
                }

                if (!SamlConstants.NS_SAML2.equals(namespaceURI)) {
                    logger.warning("Expected Namespace '"+SamlConstants.NS_SAML2+"' found Element with namespace '" + namespaceURI + "'. " + ignoreMsg);
                    break;
                }

                name = elmToValidate.getAttributeNode("Name");
                if (name == null) {
                    logger.warning("Attribute element missing Name attribute." + ignoreMsg);
                    break;
                }

                isValid = true;
                break;
            default:
                isValid = false;
        }

        return isValid;
    }

    private List<Element> extractElements(List<Object> objects) {
        final List<Element> foundElements = new ArrayList<Element>();

        for (Object object : objects) {
            if (object instanceof List) {
                foundElements.addAll(extractElements((List<Object>) object));
            } else if (object instanceof Object[]) {
                foundElements.addAll(extractElements(Arrays.asList((Object [])object)));
            } else if (object instanceof Element) {
                foundElements.add((Element) object);
            } else if (object instanceof Message) {
                final Element element = processMessageVariable((Message) object);
                foundElements.add(element);
            }
        }

        return foundElements;
    }

    /**
     * Convert a Message into XML. No validation of Element's schema type is done.
     *
     * @param message Message to convert.
     * @return Element representing the message. Null if the Message cannot be converted due to invalid content type
     * of if XML is not well formed. If return value is null, then a warning will have been logged and audited.
     */
    private Element processMessageVariable(final Message message) {

        try {
            if (message.isXml()) {
                final XmlKnob xmlKnob = message.getXmlKnob();
                final Document doc = xmlKnob.getDocumentReadOnly();
                return doc.getDocumentElement();
            } else {
                logAndAudit(AssertionMessages.MESSAGE_VARIABLE_NOT_XML_WARNING);
            }
        } catch (IOException e) {
            logAndAudit(AssertionMessages.MESSAGE_VARIABLE_BAD_XML, new String[]{""}, e);
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.MESSAGE_VARIABLE_BAD_XML, new String[]{""}, e);
        }

        return null;
    }

}
