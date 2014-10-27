package com.l7tech.security.saml;

import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SoapConstants;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3.x2000.x09.xmldsig.X509IssuerSerialType;
import org.w3c.dom.*;
import x0Assertion.oasisNamesTcSAML2.*;

import javax.xml.XMLConstants;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SAML Assertion Generator for SAML 2.x assertions.
 */
class SamlAssertionGeneratorSaml2 extends SamlVersionAssertionGenerator {

    //- PUBLIC

    @Override
    public Document createStatementDocument(@NotNull final SamlAssertionGenerator.Options options,
                                            @Nullable final String caDn,
                                            @NotNull final SubjectStatement ... subjectStatements)
            throws SignatureException, CertificateException, IllegalStateException {
        return assertionToDocument(createXmlBeansAssertion(subjectStatements, options, caDn));
    }

    //- PRIVATE

    /**
     * Populate the subject statement assertion properties such as subject name, name qualifier
     *
     * @param subjectStatement the subject statement abstract type
     * @throws CertificateEncodingException
     */
    private void populateSubjectStatement(SubjectType subjectType,
                                          SubjectStatement subjectStatement,
                                          String alternateNameId,
                                          SamlAssertionGenerator.Options options) throws CertificateException {

        if (subjectStatement.getNameIdentifierType() != NameIdentifierInclusionType.NONE) {
            NameIDType nameIdentifierType = subjectType.addNewNameID();
            nameIdentifierType.setStringValue(subjectStatement.getName());
            if (subjectStatement.getNameFormat() != null) {
                nameIdentifierType.setFormat(subjectStatement.getNameFormat());
            }
            if (subjectStatement.getNameQualifier() != null) {
                nameIdentifierType.setNameQualifier(subjectStatement.getNameQualifier());
            }
        }

        String confirmationMethod = subjectStatement.getConfirmationMethod();
        String confirmationNameId = null;
        if (SamlConstants.CONFIRMATION_BEARER.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_BEARER;
        }
        else if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY;
        }
        else if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES;
            confirmationNameId = alternateNameId;
        }

        if (confirmationMethod != null) {
            SubjectConfirmationType subjectConfirmation = subjectType.addNewSubjectConfirmation();
            subjectConfirmation.setMethod(confirmationMethod);

            if (confirmationNameId != null) {
                NameIDType confirmationNameIdType = subjectConfirmation.addNewNameID();
                confirmationNameIdType.setStringValue(confirmationNameId);
            }

            final Object keyInfo = subjectStatement.getKeyInfo();
            final boolean hasKeyInfo = keyInfo instanceof X509Certificate;
            final boolean hasSubjectConfirmationDataOption =
                    options.getSubjectConfirmationDataAddress() != null ||
                    options.getSubjectConfirmationDataInResponseTo() != null ||
                    options.getSubjectConfirmationDataRecipient() != null ||
                    options.getSubjectConfirmationDataNotBeforeSecondsInPast() >= 0 ||
                    options.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() >= 0;

            if ( !hasKeyInfo && !hasSubjectConfirmationDataOption ) {
                return;
            }

            final SubjectConfirmationDataType subjectConfirmationData = hasKeyInfo ?
                    KeyInfoConfirmationDataType.Factory.newInstance(getXmlOptions()) :
                    SubjectConfirmationDataType.Factory.newInstance(getXmlOptions());

            // Set optional subject confirmation data
            if ( options.getSubjectConfirmationDataAddress() != null ) {
                subjectConfirmationData.setAddress( options.getSubjectConfirmationDataAddress() );
            }
            if ( options.getSubjectConfirmationDataInResponseTo() != null ) {
                subjectConfirmationData.setInResponseTo( options.getSubjectConfirmationDataInResponseTo() );
            }
            if ( options.getSubjectConfirmationDataRecipient() != null ) {
                subjectConfirmationData.setRecipient( options.getSubjectConfirmationDataRecipient() );
            }
            if ( options.getSubjectConfirmationDataNotBeforeSecondsInPast() > -1 ) {
                Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, (-1 * options.getSubjectConfirmationDataNotBeforeSecondsInPast()));
                subjectConfirmationData.setNotBefore(calendar);
            }
            if ( options.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() > -1) {
                Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, options.getSubjectConfirmationDataNotOnOrAfterExpirySeconds());
                subjectConfirmationData.setNotOnOrAfter(calendar);
            }

            if ( hasKeyInfo ) {
                KeyInfoConfirmationDataType kicdt = (KeyInfoConfirmationDataType) subjectConfirmationData;
                X509Certificate cert = (X509Certificate)keyInfo;
                switch(subjectStatement.getSubjectConfirmationKeyInfoType()) {
                    case CERT: {
                        KeyInfoType keyInfoType = kicdt.addNewKeyInfo();
                        X509DataType x509Data = keyInfoType.addNewX509Data();
                        x509Data.addX509Certificate(cert.getEncoded());
                        break;
                    }
                    case ISSUER_SERIAL: {
                        KeyInfoType keyInfoType = kicdt.addNewKeyInfo();
                        X509DataType x509Data = keyInfoType.addNewX509Data();
                        X509IssuerSerialType xist = x509Data.addNewX509IssuerSerial();
                        xist.setX509IssuerName(cert.getIssuerDN().getName());
                        xist.setX509SerialNumber(cert.getSerialNumber());
                        break;
                    }
                    case STR_THUMBPRINT: {
                        Node node = kicdt.getDomNode();
                        NamespaceFactory nsf = new NamespaceFactory();
                        KeyInfoDetails kid = KeyInfoDetails.makeKeyId(CertUtils.getThumbprintSHA1(cert),
                                                 true,
                                                 SoapConstants.VALUETYPE_X509_THUMB_SHA1);
                        kid.createAndAppendKeyInfoElement(nsf, node);
                        break;
                    }
                    case STR_SKI: {
                        Node node = kicdt.getDomNode();
                        NamespaceFactory nsf = new NamespaceFactory();
                        final byte[] ski = CertUtils.getSKIBytesFromCert(cert);
                        if (ski == null)
                            throw new CertificateException("Unable to create SKI reference: no SKI available for " +
                                    "certificate [" + CertUtils.getCertIdentifyingInformation(cert) + "]");
                        KeyInfoDetails kid = KeyInfoDetails.makeKeyId(ski,
                                                 SoapConstants.VALUETYPE_SKI);
                        kid.createAndAppendKeyInfoElement(nsf, node);
                        break;
                    }
                    case NONE:
                }
            }

            subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        }
    }

    private AssertionType getGenericAssertion(@NotNull final Calendar now,
                                              final int expirySeconds,
                                              @Nullable final String assertionId,
                                              @NotNull final String issuer,
                                              @Nullable final String issuerNameFormat,
                                              @Nullable final String issuerNameQualifier,
                                              final int beforeOffsetSeconds,
                                              @Nullable final Iterable<String> audienceRestriction) {

        final AssertionType assertion = AssertionType.Factory.newInstance(getXmlOptions());

        assertion.setVersion("2.0");
        if (assertionId == null)
            assertion.setID(SamlAssertionGenerator.generateAssertionId(null));
        else
            assertion.setID(assertionId);
        assertion.setIssueInstant(now);

        final NameIDType issuerNameIdType = NameIDType.Factory.newInstance(getXmlOptions());
        issuerNameIdType.setStringValue(issuer);

        if (issuerNameFormat != null) {
            issuerNameIdType.setFormat(issuerNameFormat);
        }

        if (issuerNameQualifier != null) {
            issuerNameIdType.setNameQualifier(issuerNameQualifier);
        }

        assertion.setIssuer(issuerNameIdType);

        if ( beforeOffsetSeconds > -1 ||
             expirySeconds > -1 ||
             audienceRestriction != null ) {

            final ConditionsType ct = ConditionsType.Factory.newInstance(getXmlOptions());

            if ( audienceRestriction != null ) {
                final AudienceRestrictionType audienceRestrictionType = ct.addNewAudienceRestriction();
                for (String audRestriction : audienceRestriction) {
                    audienceRestrictionType.addAudience(audRestriction);
                }
            }

            if ( beforeOffsetSeconds > -1 ) {
                Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, (-1 * beforeOffsetSeconds));
                ct.setNotBefore(calendar);
            }

            if ( expirySeconds > -1 ) {
                Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, expirySeconds);
                ct.setNotOnOrAfter(calendar);
            }

            assertion.setConditions(ct);
        }

        return assertion;
    }

    private Document assertionToDocument(AssertionType assertion) {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance(getXmlOptions());
        assertionDocument.setAssertion(assertion);

        return fixPrefixes((Document) assertionDocument.newDomNode(getXmlOptions()), getNamespaceMap());
    }

    private Map<String,String> getNamespaceMap() {
        final Map<String, String> namespaces = new LinkedHashMap<String, String>();
        namespaces.put(SamlConstants.NS_SAML2, SamlConstants.NS_SAML2_PREFIX);
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_XMLDSIG, "saccxds");
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_PASSWORD, "saccpwd");
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_TLS_CERT, "sacctlsc");
        namespaces.put(SoapConstants.DIGSIG_URI, "ds");
        return namespaces;
    }

    private XmlOptions getXmlOptions() {
        XmlOptions xo = new XmlOptions();
        xo.setSaveSuggestedPrefixes(getNamespaceMap());
        return xo;
    }

    /**
     * Seems like a bug in XmlBeans that the prefixes for some elements are not set.
     */
    private Document fixPrefixes(Document document, Map<String, String> namespaces) {
        for (Map.Entry<String, String> nsAndPrefix : namespaces.entrySet()) {
            fixPrefixes(document.getDocumentElement(), nsAndPrefix.getValue(), null, nsAndPrefix.getKey());
        }
        return document;
    }

    private void fixPrefixes(Element element, String prefix, String replacedPrefix, String namespace) {
        // fix any ns decls on this element
        final NamedNodeMap attributes = element.getAttributes();
        for (int n=0; n<attributes.getLength(); n++) {
            Attr attr = (Attr) attributes.item(n);
            if (namespace.equals(attr.getValue()) &&
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                replacedPrefix = attr.getPrefix();
                element.removeAttributeNode(attr);
                element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespace);
                break;
            }
        }

        // fix prefix for this element
        if (namespace.equals(element.getNamespaceURI())) {
            element.setPrefix(prefix);
        }

        // fix any xsi:type attribute
        final Attr attr = element.getAttributeNodeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
        if ( attr != null ) {
            final String value = attr.getValue();
            if ( value != null && !value.isEmpty() ) {
                if ( value.indexOf( ":" ) < 0 && replacedPrefix==null ) {
                    attr.setValue( prefix + ":" + value );
                } else if ( value.indexOf( ":" ) > 0 && replacedPrefix != null ) {
                    attr.setValue( prefix + value.substring(value.indexOf( ":" )));
                }
            }
        }

        // fix children
        NodeList children = element.getChildNodes();
        for (int n=0; n<children.getLength(); n++) {
            Node node = children.item(n);
            if (node.getNodeType()==Node.ELEMENT_NODE) {
                fixPrefixes((Element)node, prefix, replacedPrefix, namespace);
            }
        }
    }

    /**
     * Create an SAML Assertion containing Subject Statements for those supplied. This method converts our
     * internal SAML version independent configuration model into the XML beans types created from the SAML schemas.
     *
     * @param subjectStatements the subject statement subclass (authentication, authorization, attribute)
     * @param options    the options, with expiry minutes
     * @param caDn public certs dn. May be null if custom issuer from options should be used.
     * @return the assertion type containing the requested statement
     * @throws CertificateEncodingException if problem with signing.
     */
    private AssertionType createXmlBeansAssertion(@NotNull final SubjectStatement[] subjectStatements,
                                                  @NotNull final SamlAssertionGenerator.Options options,
                                                  @Nullable final String caDn) throws CertificateException {
        final String issuer = (caDn != null) ? getSubjectCn(caDn) : options.getCustomIssuer();
        if (issuer == null) {
            throw new IllegalStateException("No issuer configured"); // coding error
        }

        final String nameFormatUri = options.getCustomIssuerFormat();
        final String nameQualifier = options.getCustomIssuerNameQualifier();

        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        AssertionType assertionType = getGenericAssertion(
                now, options.getNotAfterSeconds(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                issuer, nameFormatUri, nameQualifier, options.getNotBeforeSeconds(), options.getAudienceRestriction()
        );
        final SubjectType subjectStatementAbstractType = assertionType.addNewSubject();

        populateSubjectStatement(subjectStatementAbstractType, subjectStatements[0], caDn, options);

        for (SubjectStatement subjectStatement : subjectStatements) {
            if (subjectStatement instanceof AuthenticationStatement) {
                AuthenticationStatement authenticationStatement = (AuthenticationStatement) subjectStatement;
                AuthnStatementType as = assertionType.addNewAuthnStatement();
                as.setAuthnInstant(authenticationStatement.getAuthenticationInstant());

                addAuthenticationMethod(authenticationStatement, as);

                InetAddress clientAddress = options.getClientAddress();
                if (clientAddress != null) {
                    final SubjectLocalityType subjectLocality = as.addNewSubjectLocality();
                    subjectLocality.setAddress(clientAddress.getHostAddress());
                    if (options.isClientAddressDNS())
                        subjectLocality.setDNSName(clientAddress.getCanonicalHostName());
                }
            } else if (subjectStatement instanceof AuthorizationStatement) {
                AuthorizationStatement as = (AuthorizationStatement) subjectStatement;
                AuthzDecisionStatementType atzStatement = assertionType.addNewAuthzDecisionStatement();
                atzStatement.setResource(as.getResource());
                atzStatement.setDecision(DecisionType.PERMIT);
                if (as.getAction() != null) {
                    ActionType actionType = atzStatement.addNewAction();
                    actionType.setStringValue(as.getAction());
                    if (as.getActionNamespace() != null) {
                        actionType.setNamespace(as.getActionNamespace());
                    }
                }
            } else if (subjectStatement instanceof AttributeStatement) {
                AttributeStatement as = (AttributeStatement) subjectStatement;
                AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
                Attribute[] attributes = as.getAttributes();
                for (Attribute attribute : attributes) {
                    AttributeType attributeType = attStatement.addNewAttribute();
                    attributeType.setName(attribute.getName());
                    attributeType.setNameFormat(attribute.getNamespace());
                    final Object objValue = attribute.getValue();
                    final XmlObject xmlObject = GeneratorXmlBeansHelper.createXmlObjectForAttributeValueContents(objValue, attribute.getNullBehavior());
                    if (xmlObject != null) {
                        attributeType.setAttributeValueArray(new XmlObject[]{xmlObject});
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
            }
        }

        return assertionType;
    }

    private void addAuthenticationMethod( final AuthenticationStatement authenticationStatement, 
                                          final AuthnStatementType as ) {
        String authenticationMethod = authenticationStatement.getAuthenticationMethod();
        XmlObject authnContextDecl = null;
        if ( SamlConstants.PASSWORD_AUTHENTICATION.equals(authenticationMethod) ||
            SamlConstants.AUTHENTICATION_SAML2_PASSWORD.equals(authenticationMethod)) {
            authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_PASSWORD;

            x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType passwdContextDecl =
                    x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType.Factory.newInstance();

            x0AcClassesPassword.oasisNamesTcSAML2.AuthnMethodBaseType authnMethod =
                    passwdContextDecl.addNewAuthnMethod();

            x0AcClassesPassword.oasisNamesTcSAML2.AuthenticatorBaseType abt = authnMethod.addNewAuthenticator();
            x0AcClassesPassword.oasisNamesTcSAML2.PasswordType pt = abt.addNewRestrictedPassword();
            x0AcClassesPassword.oasisNamesTcSAML2.LengthType lt = pt.addNewLength();
            lt.setMin( BigInteger.valueOf(3));

            authnContextDecl = passwdContextDecl;
        }
        else if (SamlConstants.XML_DSIG_AUTHENTICATION.equals(authenticationMethod) ||
            SamlConstants.AUTHENTICATION_SAML2_XMLDSIG.equals(authenticationMethod)) {
            authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_XMLDSIG;

            x0AcClassesXMLDSig.oasisNamesTcSAML2.AuthnContextDeclarationBaseType digSigContextDecl =
                    x0AcClassesXMLDSig.oasisNamesTcSAML2.AuthnContextDeclarationBaseType.Factory.newInstance();

            x0AcClassesXMLDSig.oasisNamesTcSAML2.AuthnMethodBaseType authnMethod =
                    digSigContextDecl.addNewAuthnMethod();

            x0AcClassesXMLDSig.oasisNamesTcSAML2.AuthenticatorBaseType abt = authnMethod.addNewAuthenticator();
            XmlString kv = XmlString.Factory.newInstance();
            kv.setStringValue(SamlConstants.AUTHENTICATION_SAML2_X509);
            abt.addNewDigSig().setKeyValidation(kv);

            authnContextDecl = digSigContextDecl;
        }
        else if (SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION.equals(authenticationMethod) ||
            SamlConstants.AUTHENTICATION_SAML2_TLS_CERT.equals(authenticationMethod)) {
            authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_TLS_CERT;

            x0AcClassesTLSClient.oasisNamesTcSAML2.AuthnContextDeclarationBaseType tlsContextDecl =
                    x0AcClassesTLSClient.oasisNamesTcSAML2.AuthnContextDeclarationBaseType.Factory.newInstance();

            x0AcClassesTLSClient.oasisNamesTcSAML2.AuthnMethodBaseType authnMethod =
                    tlsContextDecl.addNewAuthnMethod();

            x0AcClassesTLSClient.oasisNamesTcSAML2.AuthenticatorBaseType abt = authnMethod.addNewAuthenticator();
            XmlString kv = XmlString.Factory.newInstance();
            kv.setStringValue(SamlConstants.AUTHENTICATION_SAML2_X509);
            abt.addNewDigSig().setKeyValidation(kv);

            x0AcClassesTLSClient.oasisNamesTcSAML2.AuthenticatorTransportProtocolType atpt =
                    authnMethod.addNewAuthenticatorTransportProtocol();
            atpt.addNewSSL();

            authnContextDecl = tlsContextDecl;
        }
        else if (SamlConstants.UNSPECIFIED_AUTHENTICATION.equals(authenticationMethod)) {
            authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_UNSPECIFIED;
        }

        if (authenticationMethod != null) {
            AuthnContextType act = as.addNewAuthnContext();
            act.setAuthnContextClassRef(authenticationMethod);
            if (authnContextDecl != null && authenticationStatement.isIncludeAuthnContextDecl()) {
                act.setAuthnContextDecl(authnContextDecl);
            }
        }
    }
}
