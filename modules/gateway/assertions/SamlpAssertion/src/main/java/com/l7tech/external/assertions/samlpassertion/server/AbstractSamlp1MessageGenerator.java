package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.KeyInfoInclusionType;
import saml.support.ds.KeyInfoType;
import saml.support.ds.X509DataType;
import saml.support.ds.X509IssuerSerialType;
import saml.v1.assertion.NameIdentifierType;
import saml.v1.assertion.SubjectConfirmationType;
import saml.v1.assertion.SubjectType;
import saml.v1.protocol.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for SAMLP v1 MessageGenerator implementations.  This class contains
 * common helper methods that are re-used all subclasses.
 *
 * @author: vchan
 */
public abstract class AbstractSamlp1MessageGenerator<REQ_MSG extends SubjectQueryAbstractType>
    implements SamlpMessageGenerator<SamlpRequestBuilderAssertion, RequestType>, SamlpRequestConstants
{
    private static final Logger logger = Logger.getLogger(AbstractSamlp1MessageGenerator.class.getName());
    
    protected static final BigInteger MAJOR_VERSION = BigInteger.valueOf(1);
    protected static final BigInteger MINOR_VERSION = BigInteger.valueOf(1);

    protected final Map<String, Object> variablesMap;
    protected final Auditor auditor;

    protected REQ_MSG samlpMessage;
    protected SamlpRequestBuilderAssertion assertion;
    protected saml.v1.assertion.ObjectFactory samlFactory;
    protected saml.v1.protocol.ObjectFactory samlpFactory;
    protected saml.support.ds.ObjectFactory digsigFactory;

    protected NameIdentifierResolver nameResolver;
    protected NameIdentifierResolver issuerNameResolver;
    protected InetAddressResolver addressResolver;
    protected MessageValueResolver<String> authnMethodResolver;
    protected MessageValueResolver<X509Certificate> clientCertResolver;
    protected EvidenceBlockResolver evidenceBlockResolver;

    private DatatypeFactory xmltypeFactory;
    private static final Random rand = new SecureRandom();

    public AbstractSamlp1MessageGenerator(final Map<String, Object> variablesMap, final Auditor auditor)
        throws SamlpAssertionException
    {
        this.variablesMap = variablesMap;
        this.auditor = auditor;

        this.samlFactory = new saml.v1.assertion.ObjectFactory();
        this.samlpFactory = new saml.v1.protocol.ObjectFactory();
        this.digsigFactory = new saml.support.ds.ObjectFactory();

        this.samlpMessage = createMessageInstance();

        try {
            this.xmltypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dcex) {
            throw new SamlpAssertionException("Error while initializing Samlp message generator", dcex);
        }
    }

    public RequestType create(final SamlpRequestBuilderAssertion assertion) {

        if (assertion.getSamlVersion() != 1)
            throw new IllegalArgumentException("Incompatable Saml Version");

        this.assertion = assertion;

        // build the message common parts
        RequestType finalReq = samlpFactory.createRequestType();
        buildCommonMessageParts(finalReq);

        // build the specific message payload
        buildSpecificMessageParts();

        // set query request into request
        if (samlpMessage instanceof AuthenticationQueryType)
            finalReq.setAuthenticationQuery((AuthenticationQueryType) samlpMessage);
        else if (samlpMessage instanceof AuthorizationDecisionQueryType)
            finalReq.setAuthorizationDecisionQuery((AuthorizationDecisionQueryType) samlpMessage);
        else if (samlpMessage instanceof AttributeQueryType)
            finalReq.setAttributeQuery((AttributeQueryType) samlpMessage);
        else {
            // should fail
        }

        return finalReq;
    }

    public JAXBElement<RequestType> createJAXBElement(RequestType samlpMsg) {
        return samlpFactory.createRequest(samlpMsg);
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
     * Sets the NameIdentifier resolver for the Issuer name.
     *
     * @param issuerNameResolver the resolver to set
     */
    public void setIssuerNameResolver(NameIdentifierResolver issuerNameResolver) {
        this.issuerNameResolver = issuerNameResolver;
    }

    /**
     * Sets the InetAddress resolver for the SubjectLocality element.
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
     * Sets the InetAddress resolver for the Authentication statement.
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
    protected abstract REQ_MSG createMessageInstance();

    protected abstract void buildSpecificMessageParts();

    /**
     * Sets all common elements for SAMLP messages (i.e. RequestId, issueInstant, etc).
     */
    protected void buildCommonMessageParts(RequestType request) {

        // mandatory attributes
        request.setRequestID( getRequestId() );
        request.setMajorVersion(MAJOR_VERSION);
        request.setMinorVersion(MINOR_VERSION);
        request.setIssueInstant( getIssueInstant() );
    }

    protected SubjectType buildSubject() {

        SubjectType subj = samlFactory.createSubjectType();
        
        NameIdentifierType nameIdentifier = getNameIdentifier();

        JAXBElement<NameIdentifierType> nameId = samlFactory.createNameIdentifier(nameIdentifier);
        subj.getContent().add(nameId);

        if (assertion.getSubjectConfirmationMethodUri() != null) {
            subj.getContent().add(buildSubjectConfirmation());
        }
        return subj;
    }

    protected JAXBElement<SubjectConfirmationType> buildSubjectConfirmation() {

        String confirmationMethod =
                SubjectStatement.Confirmation.forUri(assertion.getSubjectConfirmationMethodUri()).getUri();

        SubjectConfirmationType subjConf = samlFactory.createSubjectConfirmationType();
        subjConf.getConfirmationMethod().add(confirmationMethod);

        if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(confirmationMethod)) {
            X509DataType x509Data = buildSubjectConfirmationData();

//            subjConf.setSubjectConfirmationData(x509Data);
            // Rather than setting the subjectConf data, we'll set the keyInfo
            if (x509Data != null) {
                KeyInfoType keyInfo = digsigFactory.createKeyInfoType();
                keyInfo.getContent().add(digsigFactory.createX509Data(x509Data));
                subjConf.setKeyInfo(keyInfo);
            }
        }

        return samlFactory.createSubjectConfirmation(subjConf);
    }

    protected X509DataType buildSubjectConfirmationData() {

        if (clientCertResolver == null || clientCertResolver.getValue() == null) {
            return null;
        }

        X509Certificate cert = clientCertResolver.getValue();
        JAXBElement<?> dataContents = null;

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

        if (dataContents != null) {
            X509DataType x509Data = digsigFactory.createX509DataType();
            x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(dataContents);
            return x509Data;
        }
        return null;
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

    protected String generateRequestId() {
        StringBuffer sb = new StringBuffer(SAMLP_V1_REQUEST_ID_PREFIX);
        return sb.append(generateHexBytesForId()).toString();
    }
    
    protected String generateAssertionId() {
        StringBuffer sb = new StringBuffer(SAMLP_V1_REQUEST_ASSN_ID_PREFIX);
        return sb.append(generateHexBytesForId()).toString();
    }

    protected String generateHexBytesForId() {
        byte[] disambig = new byte[16];
        rand.nextBytes(disambig);
        return HexUtils.hexDump(disambig);
    }

    protected NameIdentifierType getNameIdentifier() {
        NameIdentifierType value = null;

        /*
         * Note, this method should not always rebuild the NameId from scratch...
         */
        if (assertion.getNameIdentifierType() != NameIdentifierInclusionType.NONE && nameResolver != null) {
            value = samlFactory.createNameIdentifierType();
            value.setValue(nameResolver.getNameValue());
            value.setFormat(nameResolver.getNameFormat());              

            if (assertion.getNameQualifier() != null && assertion.getNameQualifier().length() > 0) {
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

}
