package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saml.support.ds.KeyInfoType;
import saml.support.ds.X509DataType;
import saml.support.ds.X509IssuerSerialType;
import saml.v2.assertion.*;
import saml.v2.protocol.RequestAbstractType;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for SAMLP v2 MessageGenerator implementations.  This class contains
 * common helper methods that are re-used all subclasses.
 *
 * @author: vchan
 */
public abstract class AbstractSamlp2MessageGenerator<SAMLP_MSG extends RequestAbstractType>
    implements SamlpMessageGenerator<SamlpRequestBuilderAssertion, SAMLP_MSG>, SamlpRequestConstants
{
    private static final Logger logger = Logger.getLogger(AbstractSamlp2MessageGenerator.class.getName());

    @Nullable
    private final NameIdentifierResolver issuerNameResolver;

    protected final Map<String, Object> variablesMap;
    protected final Audit auditor;

    protected SAMLP_MSG samlpMessage;
    protected SamlpRequestBuilderAssertion assertion;
    protected saml.v2.assertion.ObjectFactory samlFactory;
    protected saml.v2.protocol.ObjectFactory samlpFactory;
    protected saml.support.ds.ObjectFactory digsigFactory;

    protected NameIdentifierResolver nameResolver;
    protected InetAddressResolver addressResolver;
    protected MessageValueResolver<String> authnMethodResolver;
    protected MessageValueResolver<X509Certificate> clientCertResolver;
    protected EvidenceBlockResolver evidenceBlockResolver;

    private DatatypeFactory xmltypeFactory;

    public AbstractSamlp2MessageGenerator(@NotNull final Map<String, Object> variablesMap,
                                          @NotNull final Audit auditor,
                                          @Nullable final NameIdentifierResolver issuerNameResolver)
        throws SamlpAssertionException
    {
        this.variablesMap = variablesMap;
        this.auditor = auditor;
        this.issuerNameResolver = issuerNameResolver;

        this.samlFactory = new saml.v2.assertion.ObjectFactory();
        this.samlpFactory = new saml.v2.protocol.ObjectFactory();
        this.digsigFactory = new saml.support.ds.ObjectFactory();
        
        this.samlpMessage = createMessageInstance();

        try {
            this.xmltypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dcex) {
            throw new SamlpAssertionException("Error while initializing Samlp message generator", dcex);
        }
    }
    
    @Override
    public SAMLP_MSG create(final SamlpRequestBuilderAssertion assertion) {

        if (assertion.getVersion() != 2)
            throw new IllegalArgumentException("Incompatable Saml Version");

        this.assertion = assertion;

        buildCommonMessageParts();
        buildSpecificMessageParts();

        // finally convert the samlpMessage into XML tree
        return samlpMessage;
    }

    /**
     * Sets the NameIdentifier resolver.
     *
     * @param nameResolver the resolver to set
     */
    public void setNameResolver(NameIdentifierResolver nameResolver) {
        this.nameResolver = nameResolver;
    }

    /**
     * Sets the InetAddressResolver for the SubjectLocality name.
     *
     * @param addressResolver the resolver to set
     */
    public void setAddressResolver(InetAddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    /**
     * Sets the InetAddress resolver for the Authentication statement.
     *
     * @param authnMethodResolver the resolver to set
     */
    public void setAuthnMethodResolver(MessageValueResolver<String> authnMethodResolver) {
        this.authnMethodResolver = authnMethodResolver;
    }

    /**
     * Sets the X509 client cert resolver used for the subjectConfirmation.
     *
     * @param clientCertResolver the resolver to set
     */
    public void setClientCertResolver(MessageValueResolver<X509Certificate> clientCertResolver) {
        this.clientCertResolver = clientCertResolver;
    }

    /**
     * Sets the Evidence variable block resolver for the authorization statement.
     *
     * @param evidenceBlockResolver the resolver to set
     */
    public void setEvidenceBlockResolver(EvidenceBlockResolver evidenceBlockResolver) {
        this.evidenceBlockResolver = evidenceBlockResolver;
    }

    /**
     * Instantiate an instance of the samlpMessage type.
     *
     * @return
     */
    protected abstract SAMLP_MSG createMessageInstance();

    protected abstract void buildSpecificMessageParts();

    /**
     * Sets all common elements for SAMLP messages (i.e. RequestId, issueInstant, etc).
     */
    protected void buildCommonMessageParts() {

        // mandatory attributes
        samlpMessage.setID( getRequestId() );
        samlpMessage.setVersion( getSamlVersion() );
        samlpMessage.setIssueInstant( getIssueInstant() );

        // optional attributes
        if (assertion.getDestinationAttribute() != null)
            samlpMessage.setDestination( getVariableValue(assertion.getDestinationAttribute()) );
        if (assertion.getConsentAttribute() != null)
            samlpMessage.setConsent( getVariableValue(assertion.getConsentAttribute()) );

        // elements
        samlpMessage.setIssuer( getIssuer() );
    }

    protected SubjectType buildSubject() {

        SubjectType subj = samlFactory.createSubjectType();

        NameIDType nameIdentifier = getNameIdentifier();

        JAXBElement<NameIDType> nameId = samlFactory.createNameID(nameIdentifier);
        subj.getContent().add(nameId);

        if (assertion.getSubjectConfirmationMethodUri() != null) {
            subj.getContent().add(buildSubjectConfirmation());
        }
        return subj;
    }

    protected JAXBElement<SubjectConfirmationType> buildSubjectConfirmation() {

        String confirmationMethod = SubjectStatement.Confirmation.forUri(assertion.getSubjectConfirmationMethodUri()).getUri();
//        String confirmationNameId = null;
        if (SamlConstants.CONFIRMATION_BEARER.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_BEARER;
        }
        else if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY;
        }
        else if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(confirmationMethod)) {
            confirmationMethod = SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES;
//            confirmationNameId = "???";
        }

        SubjectConfirmationType subjConf = samlFactory.createSubjectConfirmationType();
        subjConf.setNameID(getNameIdentifier());
        subjConf.setMethod(confirmationMethod);
        
        if (SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY.equals(confirmationMethod)) {
            subjConf.setSubjectConfirmationData(buildSubjectConfirmationData(true));
        } else if ( confirmationMethod != null ) {
            subjConf.setSubjectConfirmationData(buildSubjectConfirmationData(false));
        }

        return samlFactory.createSubjectConfirmation(subjConf);
    }


    protected SubjectConfirmationDataType buildSubjectConfirmationData( final boolean includeKeyInfo ) {
        SubjectConfirmationDataType data = null;
        JAXBElement<?> dataContents = null;
        if (includeKeyInfo && clientCertResolver != null && clientCertResolver.getValue() != null) {
            X509Certificate cert = clientCertResolver.getValue();

            try {
                byte[] certSki = null;
                KeyInfoInclusionType confType = assertion.getSubjectConfirmationKeyInfoType();
                if (confType == KeyInfoInclusionType.STR_SKI) {
                     certSki = CertUtils.getSKIBytesFromCert(cert);
                    if (certSki == null) {
                        logger.log(Level.FINE, "No SKI available for cert -- switching to embedded cert");
                        confType = KeyInfoInclusionType.CERT;
                    }
                }
                switch(confType) {
                    case CERT: {
                        dataContents = digsigFactory.createX509DataTypeX509Certificate(cert.getEncoded());
                        break;
                    }
                    case ISSUER_SERIAL: {
                        X509IssuerSerialType issuer = digsigFactory.createX509IssuerSerialType();
                        issuer.setX509IssuerName(cert.getIssuerDN().getName());
                        issuer.setX509SerialNumber(cert.getSerialNumber());
                        dataContents = digsigFactory.createX509DataTypeX509IssuerSerial(issuer);
                        break;
                    }
                    case STR_SKI: {
                        dataContents = digsigFactory.createX509DataTypeX509SKI(certSki);
                        break;
                    }
                    case STR_THUMBPRINT: {
                        dataContents = digsigFactory.createX509DataTypeX509SKI(CertUtils.getThumbprintSHA1(cert).getBytes());
                        break;
                    }
                    case NONE:
                }
            } catch (CertificateEncodingException cex) {
                logger.log(Level.INFO, "Unable to parse client cert to build SubjectConfirmation: {0}", ExceptionUtils.getMessage(cex));
                dataContents = null;
            }
        }

        final boolean hasSubjectConfirmationDataOption =
                assertion.getSubjectConfirmationDataAddress() != null ||
                assertion.getSubjectConfirmationDataInResponseTo() != null ||
                assertion.getSubjectConfirmationDataRecipient() != null ||
                assertion.getSubjectConfirmationDataNotBeforeSecondsInPast() >= 0 ||
                assertion.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() >= 0;

        if ( dataContents != null ) {
            X509DataType x509Data = digsigFactory.createX509DataType();
            x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(dataContents);

            // create KeyInfo
            KeyInfoType keyInfo = digsigFactory.createKeyInfoType();
            keyInfo.getContent().add(digsigFactory.createX509Data(x509Data));

            data = samlFactory.createKeyInfoConfirmationDataType();
            data.getContent().add(digsigFactory.createKeyInfo(keyInfo));
        } else if ( hasSubjectConfirmationDataOption ) {
            data = samlFactory.createSubjectConfirmationDataType();
        }

        if ( data != null ) {
            // Set optional subject confirmation data
            if ( assertion.getSubjectConfirmationDataAddress() != null ) {
                data.setAddress( getVariableValue(assertion.getSubjectConfirmationDataAddress()) );
            }
            if ( assertion.getSubjectConfirmationDataInResponseTo() != null ) {
                data.setInResponseTo( getVariableValue(assertion.getSubjectConfirmationDataInResponseTo()) );
            }
            if ( assertion.getSubjectConfirmationDataRecipient() != null ) {
                data.setRecipient( getVariableValue(assertion.getSubjectConfirmationDataRecipient()) );
            }
            if ( assertion.getSubjectConfirmationDataNotBeforeSecondsInPast() >= 0 ) {
                GregorianCalendar calendar = (GregorianCalendar)GregorianCalendar.getInstance();
                calendar.add(GregorianCalendar.SECOND, (-1 * assertion.getSubjectConfirmationDataNotBeforeSecondsInPast()));
                calendar.set(GregorianCalendar.MILLISECOND, 0);
                data.setNotBefore( xmltypeFactory.newXMLGregorianCalendar(calendar) );
            }
            if ( assertion.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() >= 0) {
                GregorianCalendar calendar = (GregorianCalendar)GregorianCalendar.getInstance();
                calendar.add(GregorianCalendar.SECOND, assertion.getSubjectConfirmationDataNotOnOrAfterExpirySeconds());
                data.setNotOnOrAfter( xmltypeFactory.newXMLGregorianCalendar(calendar) );
            }
        }

        return data;
    }


    protected String getRequestId() {
        
        if (assertion.getRequestId() == SAMLP_REQUEST_ID_FROM_VAR) {
            return getVariableValue(assertion.getRequestIdVariable());
        }
        return generateRequestId();
    }

    protected XMLGregorianCalendar getIssueInstant() {
        return this.getIssueInstant((GregorianCalendar) GregorianCalendar.getInstance());
    }

    protected XMLGregorianCalendar getIssueInstant(GregorianCalendar cal) {
        return xmltypeFactory.newXMLGregorianCalendar(cal);
    }

    protected NameIDType getIssuer() {

        NameIDType issuer = null;

        if (issuerNameResolver != null) {
            issuer = samlFactory.createNameIDType();
            issuer.setValue(issuerNameResolver.getNameValue());
            issuer.setFormat(issuerNameResolver.getNameFormat());
            issuer.setNameQualifier(issuerNameResolver.getNameQualifier());
            // ignore the rest
//            issuer.setNameQualifier("qualifier");
//            issuer.setSPNameQualifier("spn-name-gateway");
//            issuer.setSPProvidedID("spprov-id-1");
        }
        return issuer;
    }

    protected String getSamlVersion() {
        return SAML_VERSION_2_0;
    }

    protected String generateRequestId() {
        StringBuilder sb = new StringBuilder( SAMLP_V2_REQUEST_ID_PREFIX );
        return sb.append(generateHexBytesForId()).toString();
    }

    protected String generateAssertionId() {
        StringBuilder sb = new StringBuilder( SAMLP_V2_REQUEST_ASSN_ID_PREFIX );
        return sb.append(generateHexBytesForId()).toString();
    }

    protected String generateHexBytesForId() {
        return HexUtils.generateRandomHexId(16);
    }

    protected NameIDType getNameIdentifier() {
        NameIDType value = null;

        /*
         * Note, this method should not always rebuild the NameId from scratch...
         */
        if (!NameIdentifierInclusionType.NONE.equals(assertion.getNameIdentifierType()) && nameResolver != null) {
            value = samlFactory.createNameIDType();
            value.setValue(nameResolver.getNameValue());
            value.setFormat(nameResolver.getNameFormat());

            if (assertion.getNameQualifier() != null) {
                value.setNameQualifier( getVariableValue(assertion.getNameQualifier()) );
            }
        }
        return value;
    }

    protected String getVariableValue(String var) {
        if (var != null)
            return ExpandVariables.process(var, variablesMap, auditor);
        return null;
    }

    protected Object[] getVariableValues(String var) {

        if (var != null) {
            Object expanded = ExpandVariables.processSingleVariableAsObject(var, variablesMap, auditor);
            if (expanded != null) {
                if (expanded instanceof Object[])
                    return (Object[]) expanded;
                // assume it's a single object, so return as array
                return new Object[] { expanded };
            }
        }
        return new Object[0];
    }

}
