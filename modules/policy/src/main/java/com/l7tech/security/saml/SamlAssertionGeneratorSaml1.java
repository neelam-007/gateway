package com.l7tech.security.saml;

import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SoapConstants;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3.x2000.x09.xmldsig.X509IssuerSerialType;
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
import java.util.Map;

/**
 * SAML Assertion Generator for SAML 1.x assertions.
 *
 * <p>This code is moved from the SamlAssertionGenerator.</p>
 */
class SamlAssertionGeneratorSaml1 extends SamlVersionAssertionGenerator{

    //- PACKAGE

    @Override
    public Document createStatementDocument(@NotNull final SamlAssertionGenerator.Options options,
                                            @Nullable final String caDn,
                                            @NotNull final SubjectStatement... subjectStatements)
            throws SignatureException, CertificateException, IllegalStateException {
        return assertionToDocument(createXmlBeansAssertion(subjectStatements, options, caDn));
    }

    //- PRIVATE

    private void buildSubjectStatement( final SubjectStatement subjectStatement,
                                        final SamlAssertionGenerator.Options options,
                                        final AssertionType assertionType ) throws CertificateException {
        final SubjectStatementAbstractType subjectStatementAbstractType;

        if (subjectStatement instanceof AuthenticationStatement ) {
            AuthenticationStatement as = (AuthenticationStatement)subjectStatement;
            AuthenticationStatementType authStatement = assertionType.addNewAuthenticationStatement();
            authStatement.setAuthenticationMethod(as.getAuthenticationMethod());
            authStatement.setAuthenticationInstant(as.getAuthenticationInstant());

            InetAddress clientAddress = options.getClientAddress();
            if (clientAddress != null) {
                final SubjectLocalityType subjectLocality = authStatement.addNewSubjectLocality();
                subjectLocality.setIPAddress(clientAddress.getHostAddress());
                if ( options.isClientAddressDNS() )
                    subjectLocality.setDNSAddress(clientAddress.getCanonicalHostName());
            }
            subjectStatementAbstractType = authStatement;
        } else if (subjectStatement instanceof AuthorizationStatement ) {
            AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
            AuthorizationDecisionStatementType atzStatement = assertionType.addNewAuthorizationDecisionStatement();
            atzStatement.setResource(as.getResource());
            atzStatement.setDecision( DecisionType.PERMIT);
            if (as.getAction() != null) {
                ActionType actionType = atzStatement.addNewAction();
                actionType.setStringValue(as.getAction());
                if (as.getActionNamespace() != null) {
                    actionType.setNamespace(as.getActionNamespace());
                }
            }
            subjectStatementAbstractType = atzStatement;

        } else if (subjectStatement instanceof AttributeStatement ) {
            AttributeStatement as = (AttributeStatement)subjectStatement;
            AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
            Attribute[] attributes = as.getAttributes();
            for (Attribute attribute : attributes) {
                AttributeType attributeType = attStatement.addNewAttribute();
                attributeType.setAttributeName(attribute.getName());
                if (attribute.getNamespace() != null) {
                    attributeType.setAttributeNamespace(attribute.getNamespace());
                }
                final Object objValue = attribute.getValue();
                final XmlObject xmlObject = GeneratorXmlBeansHelper.createXmlObjectForAttributeValueContents(objValue, attribute.getNullBehavior());
                if (xmlObject != null) {
                    attributeType.setAttributeValueArray(new XmlObject[]{xmlObject});
                }
            }
            subjectStatementAbstractType = attStatement;
        } else {
            throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
        }

        populateSubjectStatement(subjectStatementAbstractType, subjectStatement);
    }

    /**
     * Create an SAML Assertion containing Subject Statements for those supplied. This method converts our
     * internal SAML version independent configuration model into the XML beans types created from the SAML schemas.
     *
     * @param subjectStatements the subject statement subclass (authentication, authorization, attribute)
     * @param options    the options, with expiry minutes
     * @param caDn public certs dn. May be null if custom issuer from options should be used.
     * @return the assertion type containing the requested statement
     * @throws CertificateException if problem with signing.
     */
    private AssertionType createXmlBeansAssertion(@NotNull SubjectStatement[] subjectStatements,
                                                  @NotNull SamlAssertionGenerator.Options options,
                                                  @Nullable String caDn)
      throws CertificateException
    {
        Calendar now = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
        final String issuer = (caDn != null) ? getSubjectCn(caDn) : options.getCustomIssuer();
        if (issuer == null) {
            throw new IllegalStateException("No issuer configured"); // coding error
        }

        AssertionType assertionType = getGenericAssertion(
                now, options.getNotAfterSeconds(),
                options.getId() != null ? options.getId() : SamlAssertionGenerator.generateAssertionId(null),
                issuer, options.getNotBeforeSeconds(), options.getAudienceRestriction());

        for (SubjectStatement subjectStatement : subjectStatements) {
            buildSubjectStatement( subjectStatement, options, assertionType );
        }

        return assertionType;
    }


    /**
     * Populate the subject statement assertion properties such as subject name, name qualifier
     *
     * @param subjectStatementAbstractType the subject statement abstract type
     * @param subjectStatement the subject statement
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

        final String confMeth = subjectStatement.getConfirmationMethod();
        if (confMeth == null) {
            // No subject confirmation
            return;
        }

        SubjectConfirmationType subjectConfirmation = subjectStatementType.addNewSubjectConfirmation();
        subjectConfirmation.addConfirmationMethod(confMeth);

        final Object keyInfo = subjectStatement.getKeyInfo();
        if (keyInfo == null || !(keyInfo instanceof X509Certificate)) {
            return;
        }

        X509Certificate cert = (X509Certificate)keyInfo;
        switch(subjectStatement.getSubjectConfirmationKeyInfoType()) {
            case CERT: {
                KeyInfoType keyInfoType = subjectConfirmation.addNewKeyInfo();
                X509DataType x509Data = keyInfoType.addNewX509Data();
                x509Data.addX509Certificate(cert.getEncoded());
                break;
            }
            case ISSUER_SERIAL: {
                KeyInfoType keyInfoType = subjectConfirmation.addNewKeyInfo();
                X509DataType x509Data = keyInfoType.addNewX509Data();
                X509IssuerSerialType xist = x509Data.addNewX509IssuerSerial();
                xist.setX509IssuerName(cert.getIssuerDN().getName());
                xist.setX509SerialNumber(cert.getSerialNumber());
                break;
            }
            case STR_SKI: {
                Element subjConfEl = (Element)subjectConfirmation.getDomNode();
                NamespaceFactory nsf = new NamespaceFactory();
                final byte[] ski = CertUtils.getSKIBytesFromCert(cert);
                if (ski == null)
                    throw new CertificateException("Unable to create SKI reference: no SKI available for certificate [" +
                            CertUtils.getCertIdentifyingInformation(cert) + "]");
                KeyInfoDetails.makeKeyId(ski, SoapConstants.VALUETYPE_SKI).
                        createAndAppendKeyInfoElement(nsf, subjConfEl);
                break;
            }
            case STR_THUMBPRINT: {
                Element subjConfEl = (Element)subjectConfirmation.getDomNode();
                NamespaceFactory nsf = new NamespaceFactory();
                KeyInfoDetails.makeKeyId(CertUtils.getThumbprintSHA1(cert), true, SoapConstants.VALUETYPE_X509_THUMB_SHA1).
                        createAndAppendKeyInfoElement(nsf, subjConfEl);
                break;
            }
            case NONE:
                // Don't add anything
                break;
        }
    }

    private AssertionType getGenericAssertion( @NotNull final Calendar now,
                                               final int expirySeconds,
                                               @Nullable final String assertionId,
                                               @NotNull final String issuer,
                                               final int beforeOffsetSeconds,
                                               @Nullable final Iterable<String> audienceRestriction ) {

        final AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(BigInteger.ONE);
        assertion.setMajorVersion(BigInteger.ONE);
        if (assertionId == null)
            assertion.setAssertionID(SamlAssertionGenerator.generateAssertionId(null));
        else
            assertion.setAssertionID(assertionId);
        assertion.setIssuer(issuer);
        assertion.setIssueInstant(now);

        if ( beforeOffsetSeconds > -1 ||
             expirySeconds > -1 ||
             audienceRestriction != null ) {

            final ConditionsType ct = ConditionsType.Factory.newInstance();

            if ( audienceRestriction != null ) {
                final AudienceRestrictionConditionType ar = ct.addNewAudienceRestrictionCondition();
                for (String audRestriction : audienceRestriction) {
                    ar.addAudience(audRestriction);
                }
            }

            if ( beforeOffsetSeconds > -1 ) {
                final Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, (-1 * beforeOffsetSeconds));
                ct.setNotBefore(calendar);
            }

            if ( beforeOffsetSeconds > -1 ) {
                final Calendar calendar = Calendar.getInstance(SamlAssertionGenerator.utcTimeZone);
                calendar.add(Calendar.SECOND, expirySeconds);
                ct.setNotOnOrAfter(calendar);
            }

            assertion.setConditions(ct);
        }

        return assertion;
    }

    private Document assertionToDocument(AssertionType assertion) {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);

        return (Document) assertionDocument.newDomNode(xo);
    }
}
