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
import com.l7tech.policy.variable.Syntax;
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

import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueAddBehavior.STRING_CONVERT;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueComparison.STRING_COMPARE;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;

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
        } else {
            authMethodUri = null;
        }

        // validate AttributeValue configuration when 'attribute.isRepeatIfMulti()' is true.
        final SamlAttributeStatement attributeStatement = assertion.getAttributeStatement();
        if (attributeStatement != null) {
            final SamlAttributeStatement.Attribute[] attributes = attributeStatement.getAttributes();
            for (SamlAttributeStatement.Attribute attribute : attributes) {
                if (attribute.isRepeatIfMulti()) {
                    final String value = attribute.getValue();
                    final String errorMsg = "Invalid AttributeValue value configuration. When repeat if Multivalued is configured only a single variable may be referenced:  '" + value + "'";
                    if (value != null && !Syntax.isOnlyASingleVariableReferenced(value)) {
                        throw new ServerPolicyException(assertion, errorMsg);
                    }
                }
            }
        }
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
        final List<Attribute> outAtts = new ArrayList<Attribute>();
        final String filterExpression = assertion.getAttributeStatement().getFilterExpression();

        // Support variables of Type Element or Message which resolve to saml:Attribute and warn logging anything else found.
        final List<Object> objects = ExpandVariables.processNoFormat(filterExpression, vars, getAudit(), false);
        //todo - check variables resolved from the filter expression for duplicates (name + nameformat)
        final List<Element> requestAttributeElements = extractElements(objects);
        final boolean hasFilter = !requestAttributeElements.isEmpty();

        //build map of request Attributes
        //TODO - the request (filter attributes) may have individual Attributes (like assertions we generate) - if this is the case they need to be merged
        final Map<String, XmlObject> requestAttributeMap = buildRequestAttributeMap(requestAttributeElements, version);

        final List<SamlAttributeStatement.Attribute> configuredAttList = Arrays.asList(assertion.getAttributeStatement().getAttributes());

        for (SamlAttributeStatement.Attribute configAttribute : configuredAttList) {
            String resolvedName = ExpandVariables.process(configAttribute.getName(), vars, getAudit());
            String nameFormatOrNamespace;
            switch (version) {
                case 1:
                    nameFormatOrNamespace = ExpandVariables.process(configAttribute.getNamespace(), vars, getAudit());
                    break;
                case 2:
                    String nf = configAttribute.getNameFormat();
                    if (nf == null) nf = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                    nameFormatOrNamespace = ExpandVariables.process(nf, vars, getAudit());
                    break;
                default:
                    throw new RuntimeException(); // Can't happen
            }

            // Process the Config AttributeValue's value - result depends on Config Attribute configuration.
            final List<Object> allResolvedObjectsForConfigAttribute = resolveObjectsForAttributeValue(configAttribute, vars);

            final boolean isXmlConvert = configAttribute.getAddBehavior() == ADD_AS_XML;
            //TODO - fail when some are not included.

            final String mapKey = resolvedName + nameFormatOrNamespace;

            final List<String> requestAttributeValues;
            // First filter - If request attributes exist, filter any config attributes not received in request.
            if (hasFilter) {
                if (!requestAttributeMap.containsKey(mapKey)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Filtering out attribute based on filter attribute variables: " + getAttributeDisplayString(configAttribute, version));
                    }
                    continue;
                }

                //there may be a value to check - if Version 2.0
                if (version == 2) {
                    final XmlObject requestXmlObject = requestAttributeMap.get(mapKey);
                    requestAttributeValues = getRequestAttributeValuesAsComparableStrings(requestXmlObject, configAttribute.getValueComparison());
                } else {
                    requestAttributeValues = Collections.emptyList();
                }
            } else {
                requestAttributeValues = Collections.emptyList();
            }

            for (Object resolvedObject : allResolvedObjectsForConfigAttribute) {
                //TODO support null and support empty string
                //TODO support missing when configuration for comparisons

                if (!requestAttributeValues.isEmpty()) {
                    // The matching requests Attribute contained an AttributeValue (1 or more). Validate the resolved value
                    // is contained within the values.
                    final String resolvedString = getStringForResolvedObject(configAttribute.getValueComparison(), resolvedObject);
                    if (!requestAttributeValues.contains(resolvedString)) {
                        logger.log(Level.FINE, "Resolved value for Attribute '" + configAttribute.getName() + "' was filtered as it's value '" + resolvedString + "' was not included in the filter attribute variables.");
                        continue;
                    }
                }

                // note at some point here or elsewhere this should be multiple AttributeValues and not Attributes. See bug 11200
                if (!isXmlConvert) {
                    final String s = resolvedObject.toString();
                    outAtts.add(new Attribute(resolvedName, nameFormatOrNamespace, s));
                    logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, resolvedName, s);
                } else {
                    final boolean isXmlVariable = resolvedObject instanceof Message || resolvedObject instanceof Element;
                    if (isXmlVariable) {
                        outAtts.add(new Attribute(resolvedName, nameFormatOrNamespace, resolvedObject));
                        logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, resolvedName, "Value of type " + ((resolvedObject instanceof Message) ? "Message" : "Element"));
                    } else if (resolvedObject instanceof List) {
                        outAtts.add(new Attribute(resolvedName, nameFormatOrNamespace, resolvedObject));
                        logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, resolvedName, "Value of type List");
                    } else {
                        final String s = resolvedObject.toString();
                        outAtts.add(new Attribute(resolvedName, nameFormatOrNamespace, s));
                        logAndAudit(AssertionMessages.SAML_ISSUER_ADDING_ATTR, resolvedName, s);
                    }
                }
            }
        }

        return SubjectStatement.createAttributeStatement(
                creds, confirmationMethod, outAtts.toArray(new Attribute[outAtts.size()]),
                assertion.getSubjectConfirmationKeyInfoType(), assertion.getNameIdentifierType(), overrideNameValue, overrideNameFormat, nameQualifier);
    }

    private String getStringForResolvedObject(final SamlAttributeStatement.Attribute.AttributeValueComparison valueComparison,
                                              final Object resolvedObject) {
        final String resolvedString;
        if (resolvedObject instanceof Element) {
            final Element resolvedElement = (Element) resolvedObject;
            if (valueComparison == STRING_COMPARE) {
                resolvedString = DomUtils.getTextValue(resolvedElement);
            } else {
                ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();
                try {
                    XmlUtil.canonicalize(resolvedElement, byteOutExpected);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot canonicalize AttributeValue element: " + ExceptionUtils.getMessage(e));
                }
                resolvedString = new String(byteOutExpected.toByteArray());
            }
        } else if (resolvedObject instanceof List) {
            List<Object> containedList = (List<Object>) resolvedObject;
            StringBuilder sb = new StringBuilder();
            for (Object obj : containedList) {
                final String objToString = getStringForResolvedObject(valueComparison, obj);
                sb.append(objToString);
            }
            resolvedString = sb.toString();
        } else {
            //String or anything else
            resolvedString = resolvedObject.toString();
        }

        return resolvedString;
    }

    /**
     * Process the Attribute's value. The result depends on the following configuration:
     * <ul>
     *     <li>Repeat if Multivalued - A List will be returned if a multi valued variable is referenced</li>
     *     <li>Behavior when Message / Element - works with above also. If not repeat then a single item which will
     *     be a list of each referenced item (e.g. String, Message, String, Element...etc for mixed content) or a List
     *     of Elements</li>
     * </ul>
     *
     *
     * Pre 6.2 behavior for isMulti is as follows: If true and a single variable is referenced, then multiple
     * attributes are added. If false, all values are concatenated. Pre 6.2 if more than one variable was referenced
     * then nothing was added. This behavior is kept except that the assertion no longer supports invalid multi
     * valued variable references.
     *
     * @param configAttribute The config Attribute
     * @param vars Available variables
     * @return List of Object which when repeat is not configured, will always contain a single item, otherwise it will
     * have multiple items, 1 for each item found in a multi valued variable.
     */
    private List<Object> resolveObjectsForAttributeValue(final SamlAttributeStatement.Attribute configAttribute,
                                                         final Map<String, Object> vars){
        final List<Object> allResolvedObjectsForConfigAttribute;

        final boolean isMulti = configAttribute.isRepeatIfMulti();
        if (configAttribute.getAddBehavior() == STRING_CONVERT && !isMulti) {
            //stringify everything and turn it into a single record.
            final Object value = ExpandVariables.process(configAttribute.getValue(), vars, getAudit());
            allResolvedObjectsForConfigAttribute = Collections.singletonList(value);
        } else if (configAttribute.getAddBehavior() == STRING_CONVERT && isMulti) {
            //stringify everything and turn it into a list of records.
            final Object obj = ExpandVariables.processSingleVariableAsDisplayableObject(configAttribute.getValue(), vars, getAudit());
            if (obj == null) {
                //keep existing behavior where null is converted to an empty string
                final Object o = "";
                allResolvedObjectsForConfigAttribute = Collections.singletonList(o);
            } else {
                //obj may be a single Object or an Object []
                if (obj instanceof Object[]) {
                    allResolvedObjectsForConfigAttribute = new ArrayList<Object>();
                    final Object[] obj1 = (Object[]) obj;
                    allResolvedObjectsForConfigAttribute.addAll(Arrays.asList(obj1));
                } else {
                    allResolvedObjectsForConfigAttribute = Collections.singletonList(obj);
                }
            }
        } else {
            final List<Object> flatten = flatten(ExpandVariables.processNoFormat(configAttribute.getValue(), vars, getAudit(), false));
            if (isMulti) {
                // all of these items will be added as a separate Attribute.
                allResolvedObjectsForConfigAttribute = flatten;
            } else {
                // all items in a single element of the list will be added as mixed content.
                final Object listObj = flatten;//so compiler is happy with singletonList
                allResolvedObjectsForConfigAttribute = Collections.singletonList(listObj);
            }
        }

        return allResolvedObjectsForConfigAttribute;
    }

    private Map<String, XmlObject> buildRequestAttributeMap(final List<Element> requestAttributeElements, final int version) {
        final Map<String, XmlObject> nameAndFormatToAttribute = new HashMap<String, XmlObject>();
        for (Element attElement : requestAttributeElements) {
            try {
                final String mapKey;
                final String name; // Name or AttributeName value depending on version
                final String nameFormatOrNamespace; // NameFormat or Namespace depending on version
                final XmlObject attType;
                final String warningMsg = "Configured variable did not contain a SAML Attribute. Ignoring value.";
                switch (version) {
                    case 1:
                        //TODO - Need a container type for SAML 1.1 - create XMLBeans jar for 1.1 protocol schema if needed to use the AttributeQueryType`
                        final String tagName = attElement.getLocalName();
                        final String ignoreMsg = "Ignoring variable.";

                        if (!"Attribute".equals(tagName) && !"AttributeDesignator".equals(tagName)) {
                            logger.warning("Expected AttributeDesignator Element, found Element with name '" + tagName + "'. " + ignoreMsg);
                            continue;
                        }

                        final String namespaceURI = attElement.getNamespaceURI();
                        if (!SamlConstants.NS_SAML.equals(namespaceURI)) {
                            logger.warning("Expected Namespace '"+SamlConstants.NS_SAML+"' found Element with namespace '" + namespaceURI + "'. " + ignoreMsg);
                            continue;
                        }

                        Attr nameAtt = attElement.getAttributeNode("AttributeName");
                        if (nameAtt == null) {
                            logger.warning("Attribute element missing AttributeName attribute. " + ignoreMsg);
                            continue;
                        }

                        final Attr nameNS = attElement.getAttributeNode("AttributeNamespace");
                        if (nameNS == null) {
                            logger.warning("Attribute element missing Name attribute. " + ignoreMsg);
                            continue;
                        }

                        name = nameAtt.getValue();
                        nameFormatOrNamespace = nameNS.getValue();
                        mapKey = name + nameFormatOrNamespace;

                        // manually create desired type after extracting required values
                        x0Assertion.oasisNamesTcSAML1.AttributeDesignatorType desType = x0Assertion.oasisNamesTcSAML1.AttributeDesignatorType.Factory.newInstance();
                        desType.setAttributeName(name);
                        desType.setAttributeNamespace(nameFormatOrNamespace);

                        attType = desType;
                        break;
                    case 2:
                        //Use a type which can contain an <saml:Attribute>
                        AttributeStatementType attStmtTypeV2 = AttributeStatementType.Factory.parse(attElement);
                        final AttributeType[] attArrayV2 = attStmtTypeV2.getAttributeArray();
                        if (attArrayV2.length > 0) {
                            AttributeType attributeTypeV2 = attArrayV2[0];
                            name = attributeTypeV2.getName();
                            nameFormatOrNamespace = attributeTypeV2.getNameFormat(); //optional - add default if null
                            mapKey = name + ((nameFormatOrNamespace == null) ? SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED : nameFormatOrNamespace);
                            attType = attributeTypeV2;
                            break;
                        } else {
                            logger.warning(warningMsg);
                            continue;
                        }

                    default:
                        throw new IllegalStateException("Unsupported version: " + version);//should never happen here.
                }

                if (nameAndFormatToAttribute.containsKey(mapKey)) {
                    // If the SAML Attributes filter contains more
                    StringBuilder sb = new StringBuilder("Duplicate SAML ");
                    if (version == 1) {
                        sb.append("AttributeDesignator: ");
                        sb.append("Name = ").append(name);
                        sb.append(" Namespace = ").append(nameFormatOrNamespace);
                    } else {
                        sb.append("Attribute: ");
                        sb.append("Name = ").append(name);
                        sb.append(" NameFormat = ").append(nameFormatOrNamespace);
                    }

                    logger.warning(sb.toString());
                }

                nameAndFormatToAttribute.put(mapKey, attType);
            } catch (XmlException e) {
                logger.warning("Ignoring invalid SAML Attribute element: " + ExceptionUtils.getMessage(e));
                break;
            }
        }
        return nameAndFormatToAttribute;
    }

    /**
     * Get the runtime value of a Config Attribute's AttributeValue processed such that it can be compared using
     * String.equals().
     *
     * @param xmlObject An XmlObject which must be an AttributeType (V1 or V2) instance.
     * @param attValueCompare How should the value of this XmlObject be extracted and processed if necessary? If
     * {@link com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueComparison#STRING_COMPARE} then the text of the node is
     * extracted. If {@link com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueComparison#CANONICALIZE} then the contents
     * of the XmlObject are extracted and then canonicalized.
     * @return String representation of the XmlObject
     */
    @NotNull
    private List<String> getRequestAttributeValuesAsComparableStrings(XmlObject xmlObject,
                                                                      SamlAttributeStatement.Attribute.AttributeValueComparison attValueCompare){
        final List<String> allAttributeValues = new ArrayList<String>();

        final XmlObject[] attributeValueArray;
        if (xmlObject instanceof x0Assertion.oasisNamesTcSAML1.AttributeType) {
            x0Assertion.oasisNamesTcSAML1.AttributeType attributeType = (x0Assertion.oasisNamesTcSAML1.AttributeType) xmlObject;
            attributeValueArray = attributeType.getAttributeValueArray();
        } else if (xmlObject instanceof AttributeType) {
            AttributeType attributeType = (AttributeType) xmlObject;
            attributeValueArray = attributeType.getAttributeValueArray();
        } else {
            attributeValueArray = null;
        }

        if (attributeValueArray != null) {
            for (XmlObject object : attributeValueArray) {
                //Can only include Attribute with a value which matches an incoming AttributeValue for that attribute.
                //Collect every value from the Attribute's AttributeValues
                final Node domNode = object.getDomNode();

                final String comparisonValue;
                if (attValueCompare == STRING_COMPARE) {
                    comparisonValue = DomUtils.getTextValue((Element) domNode);
                } else {
                    //object is an saml:AttributeValue element - process each child node individually
                    ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();
                    try {
                        XmlUtil.canonicalize(domNode, byteOutExpected);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Cannot canonicalize AttributeValue element: " + ExceptionUtils.getMessage(e));
                        continue;
                    }
                    final String includingWrapper = new String(byteOutExpected.toByteArray());
                    //remove the saml:AttributeValue wrapper.
                    // <saml:AttributeValue></saml:AttributeValue>
                    final int firstClosing = includingWrapper.indexOf(">");
                    final int lastOpening = includingWrapper.lastIndexOf("<");

                    if (firstClosing < 0 || lastOpening < 0) {
                        //sanity check - this should never happen here
                        logger.log(Level.WARNING, "Invalid AttributeValue element found.");
                        continue;
                    }

                    comparisonValue = includingWrapper.substring(firstClosing + 1, lastOpening);
                }
                allAttributeValues.add(comparisonValue);
            }
        }

        return allAttributeValues;
    }

    private String getAttributeDisplayString(final SamlAttributeStatement.Attribute configAttribute,
                                             final int version) {

        StringBuilder sb = new StringBuilder();
        switch (version) {
            case 1:
                sb.append("[Attribute=");
                sb.append(configAttribute.getName());
                sb.append(" ");
                sb.append("Namespace=");
                sb.append(configAttribute.getNamespace());
                sb.append("]");
                break;
            case 2:
                sb.append("[Attribute=");
                sb.append(configAttribute.getName());
                sb.append(" ");
                sb.append("NameFormat=");
                sb.append((configAttribute.getNameFormat() == null? SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED:configAttribute.getNameFormat()));
                sb.append("]");
                break;

            default:
                throw new IllegalStateException("Unknown version");
        }

        return sb.toString();
    }

    private List<Object> flatten(final List<Object> objects) {

        final List<Object> extractedValues = new ArrayList<Object>();
        for (Object object : objects) {
            if (object instanceof List) {
                @SuppressWarnings({"unchecked"})
                final List<Object> objects1 = flatten((List<Object>) object);
                extractedValues.addAll(objects1);
            } else if (object instanceof Object[]) {
                extractedValues.addAll(flatten(Arrays.asList((Object[]) object)));
            } else {
                if (object instanceof Message) {
                    //convert to Element if XML
                    Message msg = (Message) object;
                    Object objToAdd;
                    try {
                        final XmlKnob xmlKnob = msg.getXmlKnob();
                        if (xmlKnob != null) {
                            final Element documentElement = xmlKnob.getDocumentReadOnly().getDocumentElement();
                            objToAdd = documentElement;
                        } else {
                            //Not xml - then add as string - if something else is desired, then .mainpart should have been used.
                            objToAdd = msg.toString();
                        }
                    } catch (Exception e) {
                        if (e instanceof RuntimeException) {
                            throw new RuntimeException(e);
                        }
                        logger.warning("Invalid XML message referenced with in Attribute configuration: " + ExceptionUtils.getMessage(e));
                        // this is equivalent to referencing ${messageVar} and forgetting to leave out the '.mainpart' suffix.
                        objToAdd = msg.toString();
                    }
                    extractedValues.add(objToAdd);
                } else {
                    extractedValues.add(object);
                }
            }
        }

        return extractedValues;
    }

    /**
     *  Any Message variables are converted to Element if XML, otherwise toString is called on the Message.
     * @param objects
     * @return
     */
    private List<Element> extractElements(List<Object> objects) {
        final List<Element> foundElements = new ArrayList<Element>();

        for (Object object : objects) {
            if (object instanceof List) {
                foundElements.addAll(extractElements((List<Object>) object));
            } else if (object instanceof Object[]) {
                foundElements.addAll(extractElements(Arrays.asList((Object[]) object)));
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
