package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import saml.support.ds.SignatureType;
import saml.v2.assertion.AttributeType;
import saml.v2.assertion.EncryptedElementType;
import saml.v2.assertion.NameIDType;
import saml.v2.assertion.SubjectType;
import saml.v2.protocol.AttributeQueryType;

import javax.xml.bind.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.*;

import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.SUPPORTED_ATTRIBUTE_NAMEFORMATS;
import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.SUPPORTED_SUBJECT_FORMATS;
import static com.l7tech.external.assertions.samlpassertion.server.ProtocolRequestUtilities.*;
import static com.l7tech.util.Functions.grep;

/**
 * Server assertion for Process SAML Attribute Query Request assertion
 */
public class ServerProcessSamlAttributeQueryRequestAssertion extends AbstractMessageTargetableServerAssertion<ProcessSamlAttributeQueryRequestAssertion> {

    public ServerProcessSamlAttributeQueryRequestAssertion(final ProcessSamlAttributeQueryRequestAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        if (assertion.getSamlVersion() != SamlVersion.SAML2) {
            //todo updated when SAML 1.1 is supported.
            throw new PolicyAssertionException(assertion, "Unsupported SAML version");
        }

        if (assertion.getVariablePrefix() == null || assertion.getVariablePrefix().isEmpty()) {
            throw new PolicyAssertionException(assertion, "Variable prefix is required"); // should not happen
        }

        variablesUsed = assertion.getVariablesUsed();
        configuredSubjectFormats = getUriListFromProperty(
                assertion.getSubjectFormats(), "Subject Format", SUPPORTED_SUBJECT_FORMATS);
        configuredAttrNameFormatUris = getUriListFromProperty(
                assertion.getAttributeNameFormats(), "Attribute NameFormat", SUPPORTED_ATTRIBUTE_NAMEFORMATS);
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                             final Message message,
                                             final String messageDescription,
                                             final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {

        final String samlProtocolNS = SamlConstants.NS_SAMLP2; // update when SAML 1.1 is supported.
        final Element attributeQueryElement = getAttributeQueryElement(message, messageDescription, samlProtocolNS);

        final AttributeQueryType attributeQueryType = unmarshallV2(attributeQueryElement);

        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        validateAndSetVariablesV2(attributeQueryType, context, vars, attributeQueryElement);

        return AssertionStatus.NONE;
    }

    // - PRIVATE

    private final String[] variablesUsed;
    private final List<String> configuredSubjectFormats;
    private final List<String> configuredAttrNameFormatUris;
    private static final String ATTRIBUTE_QUERY_LOCAL_NAME = "AttributeQuery";
    private static final boolean validateUris = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.samlpassertion.server.validateUris", true);
    private static final boolean trimStrings = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.samlpassertion.server.trimStringValues", true);

    @NotNull
    private Element getAttributeQueryElement(final Message message,
                                             final String messageDescription,
                                             final String samlProtocolNS)
            throws IOException, AssertionStatusException {

        final Element attributeQueryElement;
        try {
            if (assertion.isSoapEncapsulated() && !message.isSoap()) {
                logAndAudit(AssertionMessages.MESSAGE_NOT_SOAP, messageDescription, "");
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
            if (!message.isXml()) {
                logAndAudit(AssertionMessages.MESSAGE_NOT_XML, messageDescription, "");
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }

            final Element documentElement = message.getXmlKnob().getDocumentReadOnly().getDocumentElement();
            if (assertion.isSoapEncapsulated()) {
                final Element soapBodyElm = XmlUtil.findFirstChildElementByName(documentElement, documentElement.getNamespaceURI(), "Body");
                if (soapBodyElm == null) {
                    logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_NOT_SOAP_ENCAPSULATED);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                } else {
                    attributeQueryElement = XmlUtil.findFirstChildElementByName(soapBodyElm, samlProtocolNS, ATTRIBUTE_QUERY_LOCAL_NAME);
                    if (attributeQueryElement == null) {
                        logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_NOT_SOAP_ENCAPSULATED);
                        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                    }
                }
            } else {
                attributeQueryElement = documentElement;
            }
        } catch (SAXException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID,
                    new String[]{"Error parsing request - " + ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        return attributeQueryElement;
    }

    @NotNull
    private AttributeQueryType unmarshallV2(final Element attributeQueryElement)
            throws AssertionStatusException {
        final AttributeQueryType attributeQueryType;
        if (ATTRIBUTE_QUERY_LOCAL_NAME.equals(attributeQueryElement.getLocalName())
                && SamlConstants.NS_SAMLP2.equals(attributeQueryElement.getNamespaceURI())) {
            try {
                final Unmarshaller unmarshallerV2 = JaxbUtil.getUnmarshallerV2();
                unmarshallerV2.setEventHandler(new ValidationEventHandler() {
                    @Override
                    public boolean handleEvent(ValidationEvent event) {
                        // instruct Unmarshaller to stop processing for any event and to throw the correct exception
                        return false;
                    }
                });

                final Object unmarshalObj = unmarshallerV2.unmarshal(attributeQueryElement);
                if (unmarshalObj instanceof JAXBElement &&
                        AttributeQueryType.class.isAssignableFrom(((JAXBElement) unmarshalObj).getDeclaredType())) {
                    attributeQueryType = (AttributeQueryType) ((JAXBElement) unmarshalObj).getValue();
                } else {
                    //this should never happen
                    logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID, "Unexpected type found.");
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            } catch (JAXBException e) {
                logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID,
                        new String[]{ExceptionUtils.getMessage(e)},
                        ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        } else {
            logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID, "Unsupported Message found where an AttributeQuery was expected");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        return attributeQueryType;
    }

    /**
     * Validate the received AttributeQuery according to assertion configuration
     *
     * @param attributeQueryType query to validate
     * @param context            PEC
     * @param vars               available variables
     * @throws com.l7tech.server.policy.assertion.AssertionStatusException
     *          throw if any configuration validation rules
     *          are not satisfied
     */
    private void validateAndSetVariablesV2(final AttributeQueryType attributeQueryType,
                                           final PolicyEnforcementContext context,
                                           final Map<String, Object> vars,
                                           final Element attributeQueryElement)
            throws AssertionStatusException {

        final NameIDType issuer = attributeQueryType.getIssuer();
        if (assertion.isRequireIssuer() && issuer == null) {
            throwForMissing("Issuer");
        }

        final SignatureType signature = attributeQueryType.getSignature();
        if (assertion.isRequireSignature() && signature == null) {
            throwForMissing("Signature");
        }

        // Ensure all variables are set, even with null, so that no WARNING / Exceptions are thrown (depending on strict)
        // if a variable is referenced in policy which refers to an element / attribute not set in the AttributeQuery msg.
        context.setVariable(prefix(SUFFIX_ISSUER), maybeTrim((issuer != null)? issuer.getValue(): null));
        context.setVariable(prefix(SUFFIX_ISSUER_NAME_QUALIFIER), getNameQualifier(issuer));
        context.setVariable(prefix(SUFFIX_ISSUER_SP_NAME_QUALIFIER), getSPNameQualifier(issuer));
        context.setVariable(prefix(SUFFIX_ISSUER_FORMAT), getNameFormat(issuer));
        context.setVariable(prefix(SUFFIX_ISSUER_SP_PROVIDED_ID), getSPProvidedID(issuer));

        final String id = attributeQueryType.getID();
        context.setVariable(prefix(SUFFIX_ID), id);

        if (assertion.isRequireId() && id == null) {
            throwForMissing("ID");
        }

        final String version = attributeQueryType.getVersion();
        context.setVariable(prefix(SUFFIX_VERSION), version);

        if (assertion.isRequireVersion() && version == null) {
            throwForMissing("Version");
        }

        final XMLGregorianCalendar issueInstant = attributeQueryType.getIssueInstant();
        context.setVariable(prefix(SUFFIX_ISSUE_INSTANT), getIsoTime(issueInstant));

        if (assertion.isRequireIssueInstant() && issueInstant == null) {
            throwForMissing("IssueInstant");
        }

        final String consent = attributeQueryType.getConsent();
        context.setVariable(prefix(SUFFIX_CONSENT), consent);

        if (assertion.isRequireConsent() && consent == null) {
            throwForMissing("Consent");
        }

        final String destination = attributeQueryType.getDestination();
        context.setVariable(prefix(SUFFIX_DESTINATION), destination);

        if (assertion.isRequireDestination() && destination == null) {
            throwForMissing("Destination");
        }

        //validate allowed destinations if required
        final String destinationExpression = assertion.getDestination();
        if (destinationExpression != null && !destinationExpression.trim().isEmpty()) {
            final List<String> allowedDestinations = getRuntimeUriRestrictions("destination", destinationExpression, vars);
            if (!allowedDestinations.contains(destination)) {
                throwForValueNoMatch("destination", destination, allowedDestinations);
            }
        }

        final SubjectType subject = attributeQueryType.getSubject();
        if (subject == null) {
            throwForMissing("Subject");
        }

        NameIDType subjectNameID = null;

        if (assertion.isAllowNameId()) {
            subjectNameID = getSubjectNameID(subject);
        }

        Element encryptedIdElm = null;
        if (subjectNameID == null && assertion.isAllowEncryptedId()) {
            // check encrypted id if no name id has been found.
            // decrypt
            final EncryptedElementType subjectEncryptedID = getSubjectEncryptedID(subject);
            subjectNameID = null;
            //todo [Donal] implement
        }

        if (subjectNameID == null) {
            // A Subject is always required.
            throwForMissing("Subject NameID or EncryptedID");
        }

        context.setVariable(prefix(SUFFIX_SUBJECT), maybeTrim(getSubject(subject)));
        context.setVariable(prefix(SUFFIX_SUBJECT_NAME_QUALIFIER), getNameQualifier(subjectNameID));
        context.setVariable(prefix(SUFFIX_SUBJECT_SP_NAME_QUALIFIER), getSPNameQualifier(subjectNameID));

        final String testFormat = getNameFormat(subjectNameID);
        if (assertion.isRequireSubjectFormat() && testFormat == null) {
            throwForMissing("Subject Format attribute");
        }

        final String nameIdFormat = testFormat != null? testFormat: SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;


        final List<String> allSupportedFormat = new ArrayList<String>(configuredSubjectFormats);
        final String customSubjectFormatsExpr = assertion.getCustomSubjectFormats();
        if (customSubjectFormatsExpr != null) {
            final List<String> customSubjectFormats = getRuntimeUriRestrictions("Subject NameID Format", customSubjectFormatsExpr, vars);
            allSupportedFormat.addAll(customSubjectFormats);
        }

        if (nameIdFormat != null && !allSupportedFormat.contains(nameIdFormat)) {
            // if supplied, it must match a configured value
            throwForValueNoMatch("Subject NameID Format", nameIdFormat, allSupportedFormat);
        } else if (nameIdFormat == null && assertion.isRequireSubjectFormat()) {
            // if not supplied and it's required, then throw
            throwForMissing("Subject NameID Format");
        }

        context.setVariable(prefix(SUFFIX_SUBJECT_FORMAT), (nameIdFormat != null)? nameIdFormat: SamlConstants.NAMEIDENTIFIER_UNSPECIFIED);
        context.setVariable(prefix(SUFFIX_SUBJECT_SP_PROVIDED_ID), getSPProvidedID(subjectNameID));

        final List<AttributeType> attributes = attributeQueryType.getAttribute();
        if (assertion.isRequireAttributes() && attributes.isEmpty()) {
            throwForMissing("Attributes");
        }

        if (attributes.isEmpty()) {
            context.setVariable(prefix(SUFFIX_ATTRIBUTES), null);
        } else {
            // extract the Elements
            final NodeList attNodeList = attributeQueryElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");
            List<Element> allAttributes = new ArrayList<Element>();
            for (int i = 0; i < attNodeList.getLength(); i++) {
                allAttributes.add((Element) attNodeList.item(i));
            }
            context.setVariable(prefix(SUFFIX_ATTRIBUTES), allAttributes);
        }

        if (assertion.isVerifyAttributesAreUnique()) {
            final List<AttributeType> duplicates = getDuplicates(attributes);

            if (!duplicates.isEmpty()) {
                logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID, "Duplicate Attribute elements found:" + getDuplicateNames(duplicates));
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }
        }

        if (assertion.isRequireAttributeNameFormat()) {
            for (AttributeType attribute : attributes) {
                if (attribute.getNameFormat() == null) {
                    throwForMissing("NameFormat attribute for Attribute [" + attribute.getName() + "]");
                }
            }
        }

        final List<String> allSupportedNameFormats = new ArrayList<String>(configuredAttrNameFormatUris);
        if (assertion.getCustomAttributeNameFormats() != null) {
            allSupportedNameFormats.addAll(getRuntimeUriRestrictions("Custom NameFormat", assertion.getCustomAttributeNameFormats(), vars));
        }

        for (AttributeType attribute : attributes) {
            final String nameFormat = (attribute.getNameFormat() != null)? attribute.getNameFormat(): SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
            if (!allSupportedNameFormats.contains(nameFormat)) {
                throwForValueNoMatch("an Attribute's NameFormat", nameFormat, allSupportedNameFormats);
            }
        }
    }

    private String getDuplicateNames(List<AttributeType> duplicates) {
        final List<Object> dupList = Functions.map(duplicates, new Functions.Unary<Object, AttributeType>() {
            @Override
            public Object call(AttributeType duplicate) {
                StringBuilder sb = new StringBuilder("[");
                sb.append("Name=");
                sb.append(duplicate.getName());
                final String nameFormat = duplicate.getNameFormat();
                if (nameFormat != null) {
                    sb.append(" NameFormat=");
                    sb.append(nameFormat);
                }
                sb.append("]");
                return sb.toString();
            }
        });

        return CollectionUtils.mkString(dupList, ", ");
    }

    private List<AttributeType> getDuplicates(List<AttributeType> attributes) {
        final Set<String> uniqueCheck = new HashSet<String>();
        final List<AttributeType> duplicates = new ArrayList<AttributeType>();
        for (AttributeType attributeType : attributes) {
            final String name = attributeType.getName();
            final String nameFormat = attributeType.getNameFormat();
            final String key = name + nameFormat; // it is ok if nameFormat is null. It's just a token for uniqueness
            if (uniqueCheck.contains(key)) {
                duplicates.add(attributeType);
            } else {
                uniqueCheck.add(key);
            }
        }
        return duplicates;
    }

    private List<String> getRuntimeUriRestrictions(final String restrictionFieldName,
                                                   final String fieldExpression,
                                                   final Map<String, Object> vars) {

        return grep(ContextVariableUtils.getAllResolvedStrings(fieldExpression,
                vars, getAudit(), TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object unexpectedNonString) {
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Found non string value for " + restrictionFieldName + " restriction: " + unexpectedNonString);
                    }
                }), new Functions.Unary<Boolean, String>() {
            @Override
            public Boolean call(String possibleUri) {
                final boolean isValidUri = ValidationUtils.isValidUri(possibleUri);
                if (validateUris && !isValidUri) {
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Ignoring invalid URI value for " + restrictionFieldName + " restriction '" + possibleUri + "'");
                }
                return !validateUris || isValidUri;
            }
        });
    }

    private void throwForMissing(String missingItem) throws AssertionStatusException {
        logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID, "Missing " + missingItem);
        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
    }

    private void throwForValueNoMatch(final String missingItem,
                                      final String foundValue,
                                      final List<String> expectedValues) throws AssertionStatusException {
        logAndAudit(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_NOT_SUPPORTED_VALUE, missingItem, foundValue, expectedValues.toString());
        throw new AssertionStatusException(AssertionStatus.FALSIFIED);
    }

    private String prefix(@NotNull final String variableName) {
        return assertion.getVariablePrefix() + "." + variableName;
    }

    private List<String> getUriListFromProperty(@Nullable final String propValue,
                                                @NotNull final String propLoggingName,
                                                @NotNull final Set validateSet) {
        final List<String> returnList;
        if (propValue != null) {
            final Set<String> subjectFormatSet = new HashSet<String>(
                    grep(Arrays.asList(TextUtils.URI_STRING_SPLIT_PATTERN.split(propValue)),
                            new Functions.Unary<Boolean, String>() {
                                @Override
                                public Boolean call(String s) {
                                    final boolean isSupported = validateSet.contains(s);
                                    if (!isSupported) {
                                        //only happens if policy xml is manually edited.
                                        logger.warning("Unsupported URI value found for " + propLoggingName + " : '" + s + "'");
                                    }
                                    return isSupported;
                                }
                            }));

            returnList = Collections.unmodifiableList(new ArrayList<String>(subjectFormatSet));
        } else {
            returnList = Collections.emptyList();
        }

        return returnList;
    }

    private static String maybeTrim(@Nullable final String variableValue) {
        return (variableValue != null && trimStrings) ? variableValue.trim() : variableValue;
    }
}
