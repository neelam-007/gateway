package com.l7tech.common.security.saml;

import com.l7tech.common.security.xml.KeyInfoDetails;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.NamespaceFactory;
import com.l7tech.common.util.SoapUtil;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlString;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
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
import java.util.List;
import java.util.Map;

/**
 * SAML Assertion Generator for SAML 2.x assertions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SamlAssertionGeneratorSaml2 {
    //- PACKAGE

    Document createStatementDocument(SubjectStatement subjectStatement, SamlAssertionGenerator.Options options, String caDn) throws SignatureException, CertificateException  {
        return assertionToDocument(createStatementType(subjectStatement, options, caDn));
    }

    //- PRIVATE

    /**
     * Create the statement depending on the SubjectStatement subclass and populating the subject
     *
     * @param subjectStatement the subject statament subclass (authenticatiom, authorization, attribute
     *                         statement
     * @param options          the options, with expiry minutes
     * @return the assertion type containing the requested statement
     * @throws CertificateEncodingException
     */
    private AssertionType createStatementType(SubjectStatement subjectStatement,
                                              SamlAssertionGenerator.Options options,
                                              String caDn) throws CertificateEncodingException {
        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        AssertionType assertionType = getGenericAssertion(
                now, options.getExpiryMinutes(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                caDn, options.getBeforeOffsetMinutes(), options.getAudienceRestriction());
        final SubjectType subjectStatementAbstractType = assertionType.addNewSubject();

        if (subjectStatement instanceof AuthenticationStatement) {
            AuthenticationStatement authenticationStatement = (AuthenticationStatement) subjectStatement;
            AuthnStatementType as = assertionType.addNewAuthnStatement();
            as.setAuthnInstant(authenticationStatement.getAuthenticationInstant());

            String authenticationMethod = authenticationStatement.getAuthenticationMethod();
            XmlObject authnContextDecl = null;
            if (SamlConstants.PASSWORD_AUTHENTICATION.equals(authenticationMethod) ||
                SamlConstants.AUTHENTICATION_SAML2_PASSWORD.equals(authenticationMethod)) {
                authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_PASSWORD;

                x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType passwdContextDecl =
                        x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType.Factory.newInstance();

                x0AcClassesPassword.oasisNamesTcSAML2.AuthnMethodBaseType authnMethod =
                        passwdContextDecl.addNewAuthnMethod();

                x0AcClassesPassword.oasisNamesTcSAML2.AuthenticatorBaseType abt = authnMethod.addNewAuthenticator();
                x0AcClassesPassword.oasisNamesTcSAML2.PasswordType pt = abt.addNewRestrictedPassword();
                x0AcClassesPassword.oasisNamesTcSAML2.LengthType lt = pt.addNewLength();
                lt.setMin(BigInteger.valueOf(3));

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
                if (authnContextDecl != null) act.setAuthnContextDecl(authnContextDecl);
            }

            InetAddress clientAddress = options.getClientAddress();
            if (clientAddress != null) {
                final SubjectLocalityType subjectLocality = as.addNewSubjectLocality();
                subjectLocality.setAddress(clientAddress.getHostAddress());
                subjectLocality.setDNSName(clientAddress.getCanonicalHostName());
            }
        } else if (subjectStatement instanceof AuthorizationStatement) {
            AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
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
            AttributeStatement as = (AttributeStatement)subjectStatement;
            AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
            for (Attribute attribute : as.getAttributes()) {
                AttributeType attributeType = attStatement.addNewAttribute();
                attributeType.setName(attribute.getName());
                attributeType.setNameFormat(attribute.getNamespace());
                XmlString stringValue = XmlString.Factory.newValue(attribute.getValue());
                attributeType.setAttributeValueArray(new XmlObject[]{stringValue});
            }
        } else {
            throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
        }

        populateSubjectStatement(subjectStatementAbstractType, subjectStatement, caDn);
        return assertionType;
    }

    /**
     * Populate the subject statement assertion properties such as subject name, name qualifier
     *
     * @param subjectStatement the subject statement abstract type
     * @throws CertificateEncodingException
     */
    private void populateSubjectStatement(SubjectType subjectType,
                                          SubjectStatement subjectStatement,
                                          String alternateNameId) throws CertificateEncodingException {

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
            if (keyInfo == null || !(keyInfo instanceof X509Certificate)) {
                return;
            }

            subjectConfirmation.setSubjectConfirmationData(KeyInfoConfirmationDataType.Factory.newInstance());
            KeyInfoConfirmationDataType kicdt = (KeyInfoConfirmationDataType) subjectConfirmation.getSubjectConfirmationData();

            X509Certificate cert = (X509Certificate)keyInfo;
            switch(subjectStatement.getSubjectConfirmationKeyInfoType()) {
                case CERT:
                    KeyInfoType keyInfoType = kicdt.addNewKeyInfo();
                    X509DataType x509Data = keyInfoType.addNewX509Data();
                    x509Data.addX509Certificate(cert.getEncoded());
                    break;
                case STR_THUMBPRINT: {
                    Node node = kicdt.getDomNode();
                    NamespaceFactory nsf = new NamespaceFactory();
                    KeyInfoDetails kid = KeyInfoDetails.makeKeyId(CertUtils.getThumbprintSHA1(cert),
                                             true,
                                             SoapUtil.VALUETYPE_X509_THUMB_SHA1);
                    kid.createAndAppendKeyInfoElement(nsf, node);
                    break;
                }
                case STR_SKI: {
                    Node node = kicdt.getDomNode();
                    NamespaceFactory nsf = new NamespaceFactory();
                    KeyInfoDetails kid = KeyInfoDetails.makeKeyId(CertUtils.getSKIBytesFromCert(cert),
                                             SoapUtil.VALUETYPE_SKI);
                    kid.createAndAppendKeyInfoElement(nsf, node);
                    break;
                }
                case NONE:
            }
        }
    }

    private AssertionType getGenericAssertion(Calendar now, int expiryMinutes, String assertionId, String caDn, int beforeOffsetMinutes, String audienceRestriction) {
        Map caMap = CertUtils.dnToAttributeMap(caDn);
        String caCn = (String)((List)caMap.get("CN")).get(0);

        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setVersion("2.0");
        if (assertionId == null)
            assertion.setID(SamlAssertionGenerator.generateAssertionId(null));
        else
            assertion.setID(assertionId);
        assertion.setIssueInstant(now);

        NameIDType issuer = NameIDType.Factory.newInstance();
        issuer.setStringValue(caCn);
        assertion.setIssuer(issuer);

        ConditionsType ct = ConditionsType.Factory.newInstance();
        if (audienceRestriction != null) ct.addNewAudienceRestriction().addAudience(audienceRestriction);
        Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, (-1 * beforeOffsetMinutes)); //bzilla #3616
        ct.setNotBefore(calendar);
        Calendar c2 = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        c2.add(Calendar.MINUTE, expiryMinutes);
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);
        return assertion;
    }

    private Document assertionToDocument(AssertionType assertion) {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map<String, String> namespaces = new LinkedHashMap<String, String>();
        namespaces.put(SamlConstants.NS_SAML2, SamlConstants.NS_SAML2_PREFIX);
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_XMLDSIG, "saccxds");
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_PASSWORD, "saccpwd");
        namespaces.put(SamlConstants.AUTHENTICATION_SAML2_TLS_CERT, "sacctlsc");
        namespaces.put(SoapUtil.DIGSIG_URI, "dsig");
        xo.setSaveSuggestedPrefixes(namespaces);

        return fixPrefixes((Document) assertionDocument.newDomNode(xo), namespaces);
    }

    /**
     * Seems like a bug in XmlBeans that the prefixes for some elements are not set.
     */
    private Document fixPrefixes(Document document, Map<String, String> namespaces) {
        for (Map.Entry<String, String> nsAndPrefix : namespaces.entrySet()) {
            fixPrefixes(document.getDocumentElement(), nsAndPrefix.getValue(), nsAndPrefix.getKey());
        }
        return document;
    }

    /**
     * NOTE: Only required for Statements, not known types
     *
     * Ensure that any statements have the required xsi:type information.
     * /
    private Document fixTypes(Document document) {
        Element documentElement = document.getDocumentElement();

        // Add XSI Namespace
        documentElement.setAttributeNS(
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                XMLConstants.XMLNS_ATTRIBUTE + ":xsi",
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);

        // Add any xsi:type attributes.
        String[] statementElements = {"AuthnStatement", "AuthzDecisionStatement", "AttributeStatement"};
        for (int i = 0; i < statementElements.length; i++) {
            String statementElementName = statementElements[i];
            NodeList statementNodes = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, statementElementName);
            if (statementNodes != null) {
                for (int n=0; n<statementNodes.getLength(); n++) {
                    Element statementElement = (Element) statementNodes.item(n);
                    statementElement.setAttributeNS(
                            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                            "xsi:type",
                            SamlConstants.NS_SAML_PREFIX + ":" + statementElementName + "Type");
                }
            }
        }

        return document;
    }

    /**
     * WARNING - This only works when you know that the prefix is not already in use.
     */
    private void fixPrefixes(Element element, String prefix, String namespace) {
        // fix any ns decls on this element
        NamedNodeMap attributes = element.getAttributes();
        for (int n=0; n<attributes.getLength(); n++) {
            Attr attr = (Attr) attributes.item(n);
            if (namespace.equals(attr.getValue()) &&
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                element.removeAttributeNode(attr);
                element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, namespace);
                break;
            }
        }

        // fix prefix for this element
        if (namespace.equals(element.getNamespaceURI())) {
            element.setPrefix(prefix);
        }

        // fix children
        NodeList children = element.getChildNodes();
        for (int n=0; n<children.getLength(); n++) {
            Node node = children.item(n);
            if (node.getNodeType()==Node.ELEMENT_NODE) {
                fixPrefixes((Element)node, prefix, namespace);
            }
        }
    }

    public Document createStatementDocument(SubjectStatement[] statements, SamlAssertionGenerator.Options options, String caDn) throws CertificateEncodingException {
        return assertionToDocument(createXmlBeansAssertion(statements, options, caDn));
    }

    private AssertionType createXmlBeansAssertion(SubjectStatement[] statements, SamlAssertionGenerator.Options options, String caDn) throws CertificateEncodingException {
        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        AssertionType assertionType = getGenericAssertion(
                now, options.getExpiryMinutes(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                caDn, options.getBeforeOffsetMinutes(), options.getAudienceRestriction()
        );
        final SubjectType subjectStatementAbstractType = assertionType.addNewSubject();

        populateSubjectStatement(subjectStatementAbstractType, statements[0], caDn);

        for (SubjectStatement subjectStatement : statements) {
            if (subjectStatement instanceof AuthenticationStatement) {
                AuthenticationStatement authenticationStatement = (AuthenticationStatement) subjectStatement;
                AuthnStatementType as = assertionType.addNewAuthnStatement();
                as.setAuthnInstant(authenticationStatement.getAuthenticationInstant());

                String authenticationMethod = authenticationStatement.getAuthenticationMethod();
                XmlObject authnContextDecl = null;
                if (SamlConstants.PASSWORD_AUTHENTICATION.equals(authenticationMethod) ||
                    SamlConstants.AUTHENTICATION_SAML2_PASSWORD.equals(authenticationMethod)) {
                    authenticationMethod = SamlConstants.AUTHENTICATION_SAML2_PASSWORD;

                    x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType passwdContextDecl =
                            x0AcClassesPassword.oasisNamesTcSAML2.AuthnContextDeclarationBaseType.Factory.newInstance();

                    x0AcClassesPassword.oasisNamesTcSAML2.AuthnMethodBaseType authnMethod =
                            passwdContextDecl.addNewAuthnMethod();

                    x0AcClassesPassword.oasisNamesTcSAML2.AuthenticatorBaseType abt = authnMethod.addNewAuthenticator();
                    x0AcClassesPassword.oasisNamesTcSAML2.PasswordType pt = abt.addNewRestrictedPassword();
                    x0AcClassesPassword.oasisNamesTcSAML2.LengthType lt = pt.addNewLength();
                    lt.setMin(BigInteger.valueOf(3));

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
                    if (authnContextDecl != null) act.setAuthnContextDecl(authnContextDecl);
                }

                InetAddress clientAddress = options.getClientAddress();
                if (clientAddress != null) {
                    final SubjectLocalityType subjectLocality = as.addNewSubjectLocality();
                    subjectLocality.setAddress(clientAddress.getHostAddress());
                    subjectLocality.setDNSName(clientAddress.getCanonicalHostName());
                }
            } else if (subjectStatement instanceof AuthorizationStatement) {
                AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
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
                AttributeStatement as = (AttributeStatement)subjectStatement;
                AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
                Attribute[] attributes = as.getAttributes();
                for (Attribute attribute : attributes) {
                    AttributeType attributeType = attStatement.addNewAttribute();
                    attributeType.setName(attribute.getName());
                    attributeType.setNameFormat(attribute.getNamespace());
                    XmlString stringValue = XmlString.Factory.newValue(attribute.getValue());
                    attributeType.setAttributeValueArray(new XmlObject[]{stringValue});
                }
            } else {
                throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
            }
        }

        return assertionType;
    }
}
