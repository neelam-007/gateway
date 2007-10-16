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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.*;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SAML Assertion Generator for SAML 1.x assertions.
 *
 * <p>This code is moved from the SamlAssertionGenerator.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class SamlAssertionGeneratorSaml1 {

    //- PACKAGE

    Document createStatementDocument(SubjectStatement subjectStatement, SamlAssertionGenerator.Options options, String caDn) throws SignatureException, CertificateException  {
        return assertionToDocument(createXmlBeansAssertion(subjectStatement, options, caDn));
    }

    Document createStatementDocument(SubjectStatement[] subjectStatements, SamlAssertionGenerator.Options options, String caDn) throws SignatureException, CertificateException  {
        return assertionToDocument(createXmlBeansAssertion(subjectStatements, options, caDn));
    }

    //- PRIVATE

    /**
     * Create the statement depending on the SubjectStatement subclass and populating the subject
     *
     * @param subjectStatement the subject statament subclass (authenticatiom, authorization, attribute
     *                         statement
     * @param options          the options, with expiry minutes
     * @return the assertion type containing the requested statement
     * @throws java.security.cert.CertificateException
     */
    private AssertionType createXmlBeansAssertion(SubjectStatement subjectStatement, SamlAssertionGenerator.Options options, String caDn)
      throws CertificateException {
        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        AssertionType assertionType = getGenericAssertion(
                now,
                options.getExpiryMinutes(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                caDn, options.getBeforeOffsetMinutes());
        final SubjectStatementAbstractType subjectStatementAbstractType;

        if (subjectStatement instanceof AuthenticationStatement) {
            AuthenticationStatement as = (AuthenticationStatement)subjectStatement;
            AuthenticationStatementType authStatement = assertionType.addNewAuthenticationStatement();
            authStatement.setAuthenticationMethod(as.getAuthenticationMethod());
            authStatement.setAuthenticationInstant(as.getAuthenticationInstant());

            InetAddress clientAddress = options.getClientAddress();
            if (clientAddress != null) {
                final SubjectLocalityType subjectLocality = authStatement.addNewSubjectLocality();
                subjectLocality.setIPAddress(clientAddress.getHostAddress());
                subjectLocality.setDNSAddress(clientAddress.getCanonicalHostName());
            }
            subjectStatementAbstractType = authStatement;
        } else if (subjectStatement instanceof AuthorizationStatement) {
            AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
            AuthorizationDecisionStatementType atzStatement = assertionType.addNewAuthorizationDecisionStatement();
            atzStatement.setResource(as.getResource());
            atzStatement.setDecision(DecisionType.PERMIT);
            if (as.getAction() != null) {
                ActionType actionType = atzStatement.addNewAction();
                actionType.setStringValue(as.getAction());
                if (as.getActionNamespace() != null) {
                    actionType.setNamespace(as.getActionNamespace());
                }
            }
            subjectStatementAbstractType = atzStatement;

        } else if (subjectStatement instanceof AttributeStatement) {
            AttributeStatement as = (AttributeStatement)subjectStatement;
            AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
            Attribute[] attributes = as.getAttributes();
            for (Attribute attribute : attributes) {
                AttributeType attributeType = attStatement.addNewAttribute();
                attributeType.setAttributeName(attribute.getName());
                if (attribute.getNamespace() != null) {
                    attributeType.setAttributeNamespace(attribute.getNamespace());
                }
                XmlString stringValue = XmlString.Factory.newValue(attribute.getValue());
                attributeType.setAttributeValueArray(new XmlObject[]{stringValue});
            }
            subjectStatementAbstractType = attStatement;
        } else {
            throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
        }

        populateSubjectStatement(subjectStatementAbstractType, subjectStatement);
        return assertionType;
    }

    /**
     * Create the statement depending on the SubjectStatement subclass and populating the subject
     *
     * @param subjectStatements the subject statament subclasses (authenticatiom, authorization, attribute statement
     * @param options          the options, with expiry minutes
     * @return the assertion type containing the requested statement
     * @throws java.security.cert.CertificateException
     */
    private AssertionType createXmlBeansAssertion(SubjectStatement[] subjectStatements,
                                                  SamlAssertionGenerator.Options options,
                                                  String caDn)
      throws CertificateException
    {
        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        AssertionType assertionType = getGenericAssertion(
                now,
                options.getExpiryMinutes(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                caDn, options.getBeforeOffsetMinutes());

        for (SubjectStatement subjectStatement : subjectStatements) {
            final SubjectStatementAbstractType subjectStatementAbstractType;

            if (subjectStatement instanceof AuthenticationStatement) {
                AuthenticationStatement as = (AuthenticationStatement)subjectStatement;
                AuthenticationStatementType authStatement = assertionType.addNewAuthenticationStatement();
                authStatement.setAuthenticationMethod(as.getAuthenticationMethod());
                authStatement.setAuthenticationInstant(as.getAuthenticationInstant());

                InetAddress clientAddress = options.getClientAddress();
                if (clientAddress != null) {
                    final SubjectLocalityType subjectLocality = authStatement.addNewSubjectLocality();
                    subjectLocality.setIPAddress(clientAddress.getHostAddress());
                    subjectLocality.setDNSAddress(clientAddress.getCanonicalHostName());
                }
                subjectStatementAbstractType = authStatement;
            } else if (subjectStatement instanceof AuthorizationStatement) {
                AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
                AuthorizationDecisionStatementType atzStatement = assertionType.addNewAuthorizationDecisionStatement();
                atzStatement.setResource(as.getResource());
                atzStatement.setDecision(DecisionType.PERMIT);
                if (as.getAction() != null) {
                    ActionType actionType = atzStatement.addNewAction();
                    actionType.setStringValue(as.getAction());
                    if (as.getActionNamespace() != null) {
                        actionType.setNamespace(as.getActionNamespace());
                    }
                }
                subjectStatementAbstractType = atzStatement;

            } else if (subjectStatement instanceof AttributeStatement) {
                AttributeStatement as = (AttributeStatement)subjectStatement;
                AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
                Attribute[] attributes = as.getAttributes();
                for (Attribute attribute : attributes) {
                    AttributeType attributeType = attStatement.addNewAttribute();
                    attributeType.setAttributeName(attribute.getName());
                    if (attribute.getNamespace() != null) {
                        attributeType.setAttributeNamespace(attribute.getNamespace());
                    }
                    XmlString stringValue = XmlString.Factory.newValue(attribute.getValue());
                    attributeType.setAttributeValueArray(new XmlObject[]{stringValue});
                }
                subjectStatementAbstractType = attStatement;
            } else {
                throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
            }
            populateSubjectStatement(subjectStatementAbstractType, subjectStatement);
        }

        return assertionType;
    }


    /**
     * Populate the subject statement assertion properties such as subject name, name qualifier
     *
     * @param subjectStatementAbstractType the subject statement abstract type
     * @param subjectStatement
     * @throws java.security.cert.CertificateEncodingException on certificate error
     */
    private void populateSubjectStatement(SubjectStatementAbstractType subjectStatementAbstractType,
                                            SubjectStatement subjectStatement) throws CertificateException {

        SubjectType subjectStatementType = subjectStatementAbstractType.addNewSubject();
        if (subjectStatement.getNameIdentifierType() != NameIdentifierInclusionType.NONE) {
            NameIdentifierType nameIdentifierType = subjectStatementType.addNewNameIdentifier();
            nameIdentifierType.setStringValue(subjectStatement.getName());
            if (subjectStatement.getNameFormat() != null) {
                nameIdentifierType.setFormat(subjectStatement.getNameFormat());
            }
            if (subjectStatement.getNameQualifier() != null) {
                nameIdentifierType.setNameQualifier(subjectStatement.getNameQualifier());
            }
        }

        SubjectConfirmationType subjectConfirmation = subjectStatementType.addNewSubjectConfirmation();
        subjectConfirmation.addConfirmationMethod(subjectStatement.getConfirmationMethod());

        final Object keyInfo = subjectStatement.getKeyInfo();
        if (keyInfo == null || !(keyInfo instanceof X509Certificate)) {
            return;
        }

        X509Certificate cert = (X509Certificate)keyInfo;
        switch(subjectStatement.getSubjectConfirmationKeyInfoType()) {
            case CERT:
                KeyInfoType keyInfoType = subjectConfirmation.addNewKeyInfo();
                X509DataType x509Data = keyInfoType.addNewX509Data();
                x509Data.addX509Certificate(((X509Certificate)keyInfo).getEncoded());
                break;
            case STR_SKI: {
                Element subjConfEl = (Element)subjectConfirmation.getDomNode();
                NamespaceFactory nsf = new NamespaceFactory();
                KeyInfoDetails.makeKeyId(CertUtils.getSKIBytesFromCert(cert), SoapUtil.VALUETYPE_SKI).
                        createAndAppendKeyInfoElement(nsf, subjConfEl);
                break;
            }
            case STR_THUMBPRINT: {
                Element subjConfEl = (Element)subjectConfirmation.getDomNode();
                NamespaceFactory nsf = new NamespaceFactory();
                KeyInfoDetails.makeKeyId(CertUtils.getThumbprintSHA1(cert), true, SoapUtil.VALUETYPE_X509_THUMB_SHA1).
                        createAndAppendKeyInfoElement(nsf, subjConfEl);
                break;
            }
            case NONE:
                // Don't add anything
                break;
        }
    }

    private AssertionType getGenericAssertion(Calendar now, int expiryMinutes, String assertionId, String caDn, int beforeOffsetMinutes) {
        Map caMap = CertUtils.dnToAttributeMap(caDn);
        String caCn = (String)((List)caMap.get("CN")).get(0);

        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(BigInteger.ONE);
        assertion.setMajorVersion(BigInteger.ONE);
        if (assertionId == null)
            assertion.setAssertionID(SamlAssertionGenerator.generateAssertionId(null));
        else
            assertion.setAssertionID(assertionId);
        assertion.setIssuer(caCn);
        assertion.setIssueInstant(now);

        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, (-1 * beforeOffsetMinutes));
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
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);

        return (Document) assertionDocument.newDomNode(xo);
    }
}
