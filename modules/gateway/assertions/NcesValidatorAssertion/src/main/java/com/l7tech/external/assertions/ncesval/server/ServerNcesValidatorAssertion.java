package com.l7tech.external.assertions.ncesval.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.CertificateInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.token.*;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Server side implementation of the NcesValidatorAssertion.
 *
 * @see com.l7tech.external.assertions.ncesval.NcesValidatorAssertion
 */
public class ServerNcesValidatorAssertion extends AbstractServerAssertion<NcesValidatorAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNcesValidatorAssertion.class.getName());

    private final Auditor auditor;
    private final CertValidationProcessor certValidationProcessor;
    private final SecurityTokenResolver securityTokenResolver;
    private final TrustedCertServices trustedCertServices;
    private final boolean checkSingleSignature;

    public ServerNcesValidatorAssertion( final NcesValidatorAssertion assertion,
                                         final ApplicationContext context ) throws PolicyAssertionException {
        this( assertion,
              context,
              null,
              (CertValidationProcessor) context.getBean("certValidationProcessor"),
              (SecurityTokenResolver)context.getBean("securityTokenResolver"),
              (TrustedCertServices)context.getBean("trustedCertServices"),
              SyspropUtil.getBoolean(ServerNcesValidatorAssertion.class.getName() + ".checkSingleSigner", true) );
    }

    protected ServerNcesValidatorAssertion( final NcesValidatorAssertion assertion,
                                            final ApplicationContext context,
                                            final Auditor auditor,
                                            final CertValidationProcessor certValidationProcessor,
                                            final SecurityTokenResolver securityTokenResolver,
                                            final TrustedCertServices trustedCertServices,
                                            final boolean checkSingleSignature ) throws PolicyAssertionException {
        super(assertion);
        this.auditor = auditor!=null ? auditor : new Auditor(this, context, logger);        
        this.certValidationProcessor = certValidationProcessor;
        this.securityTokenResolver = securityTokenResolver;
        this.trustedCertServices = trustedCertServices;
        this.checkSingleSignature = checkSingleSignature;
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Message msg;
        try {
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        }
        final String what = assertion.getTargetName();

        try {
            if (!msg.isSoap(true)) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_NOT_SOAP, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_BAD_XML, new String[] { what, ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.BAD_REQUEST;
        }

        final ProcessorResult wssResult = WSSecurityProcessorUtils.getWssResults(msg, what, securityTokenResolver, auditor);
        if (wssResult == null) {
            // WssProcessorUtil.getWssResults() has already audited the message
            return AssertionStatus.NOT_APPLICABLE;
        }

        SamlAssertion saml = null;
        X509Certificate cert = null;
        for (XmlSecurityToken token : wssResult.getXmlSecurityTokens()) {
            if (token instanceof SamlAssertion) {
                if (assertion.isSamlRequired()) {
                    saml = (SamlAssertion)token;
                }
            } else  if (token instanceof X509SigningSecurityToken) {
                X509SigningSecurityToken x509Token = (X509SigningSecurityToken)token;
                if ( x509Token.isPossessionProved() ) {
                    cert = x509Token.getMessageSigningCertificate();
                }
            }
        }

        SigningSecurityToken samlSigner = null;
        SigningSecurityToken timestampSigner = null;
        SigningSecurityToken messageIdSigner = null;
        SigningSecurityToken bodySigner = null;

        for (SignedElement signedElement : wssResult.getElementsThatWereSigned()) {
            final SigningSecurityToken sst = signedElement.getSigningSecurityToken();
            final Element el = signedElement.asElement();
            if ( saml != null && saml.asElement().isEqualNode(el) ) {
                if ( samlSigner != null ) {
                    auditor.logAndAudit(AssertionMessages.NCESVALID_NO_SAML, what);
                    return AssertionStatus.BAD_REQUEST;
                }
                samlSigner = sst;
            } else if ( isWsAddressingMessageID(el) ) {
                if ( messageIdSigner != null ) {
                    auditor.logAndAudit(AssertionMessages.NCESVALID_NO_MESSAGEID, what);
                    return AssertionStatus.BAD_REQUEST;
                }
                messageIdSigner = sst;
            } else if ( timestampSigner == null && wssResult.getTimestamp() != null && el == wssResult.getTimestamp().asElement() ) {
                timestampSigner = sst;
            } else if ( bodySigner==null ) {
                try {
                    if ( SoapUtil.isBody(el)) bodySigner = sst;
                } catch ( InvalidDocumentFormatException e) {
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Can't find SOAP Body element", e);
                }
            }
        }

        if (assertion.isSamlRequired() && samlSigner == null) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_NO_SAML, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if (timestampSigner == null) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_NO_TIMESTAMP, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if (messageIdSigner == null) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_NO_MESSAGEID, what);
            return AssertionStatus.BAD_REQUEST;
        }

        if (bodySigner == null) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_BODY_NOT_SIGNED, what);
            return AssertionStatus.BAD_REQUEST;
        }

        boolean ok;
        if (assertion.isSamlRequired()) {
            ok = samlSigner == timestampSigner && timestampSigner == messageIdSigner && messageIdSigner == bodySigner &&
                 (!checkSingleSignature || WSSecurityProcessorUtils.isSameSignature( samlSigner, timestampSigner, messageIdSigner, bodySigner ));
        } else {
            ok = timestampSigner == messageIdSigner && messageIdSigner == bodySigner &&
                 (!checkSingleSignature || WSSecurityProcessorUtils.isSameSignature( timestampSigner, messageIdSigner, bodySigner ));
        }

        if (!ok) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_DIFF_SIGNATURES, what);
            return AssertionStatus.BAD_REQUEST;
        }

        // Check certificate was used for signing and that it is a certificate trusted by this assertion
        if ( cert == null ) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_NO_CERTIFICATE, what);
            return AssertionStatus.FALSIFIED;
        } else if ( !(timestampSigner instanceof X509SigningSecurityToken ) ) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_NOT_USED, what);
            return AssertionStatus.FALSIFIED;
        } else {
            X509Certificate signingCertificate = ((X509SigningSecurityToken)timestampSigner).getMessageSigningCertificate();
            if ( !CertUtils.certsAreEqual( cert, signingCertificate )) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_NOT_USED, what);
                return AssertionStatus.FALSIFIED;
            }

            if ( certificateValid( cert, what ) ) {
                return AssertionStatus.NONE;
            } else {
                return AssertionStatus.FALSIFIED;
            }
        }
    }

    /**
     * Check if the given element is a WS-Addressing MessageID 
     */
    private boolean isWsAddressingMessageID( final Element element ) {
        boolean isMessageID = false;

        try {
            if ( DomUtils.elementInNamespace(element, SoapConstants.WSA_NAMESPACE_ARRAY) &&
                 SoapConstants.MESSAGEID_EL_NAME.equals(element.getLocalName()) &&
                 SoapUtil.isHeader(element.getParentNode() ) ) {
                isMessageID = true;
            }
        } catch ( InvalidDocumentFormatException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Can't find SOAP Header element", e);
        }

        return isMessageID;
    }

    /**
     * Ensure signing certificate is valid / trusted
     */
    private boolean certificateValid( final X509Certificate cert, final String what ) {
        boolean valid = false;

        // This can be null, in which case the facilities value is used
        CertificateValidationType validationType = assertion.getCertificateValidationType();

        CertificateInfo[] trustedCertificates = assertion.getTrustedCertificateInfo();
        if ( trustedCertificates == null ) {
            trustedCertificates = new CertificateInfo[0];
        }

        CertificateInfo[] trustedCertificateIssuers = assertion.getTrustedIssuerCertificateInfo();
        if ( trustedCertificateIssuers == null ) {
            trustedCertificateIssuers = new CertificateInfo[0];    
        }

        // Ensure trusted / unrevoked issuer
        String issuerDn = CertUtils.getIssuerDN( cert );
        try {
            // Determine if certificate or issuer is trusted
            X509Certificate trustedIssuerCert = null;
            boolean isTrustedSubject = false;
            if ( isTrusted( trustedCertificates, cert ) ) {
                isTrustedSubject = true;
            } else {
                Collection<TrustedCert> trustedCerts = trustedCertServices.getCertsBySubjectDnFiltered(issuerDn, true, null, null);
                for (TrustedCert trustedCert : trustedCerts) {
                    X509Certificate issuerCert = trustedCert.getCertificate();
                    if ( isTrusted( trustedCertificateIssuers, issuerCert ) ) {
                        trustedIssuerCert = issuerCert;
                        break;
                    }
                }
            }

            // Validate and optionally check revocation
            if ( !isTrustedSubject && trustedIssuerCert==null ) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_UNTRUSTED, what);
            } else {
                try {
                    CertificateValidationResult cvr = certValidationProcessor.check(
                            isTrustedSubject ?
                                    new X509Certificate[]{cert} :
                                    new X509Certificate[]{cert, trustedIssuerCert},
                            CertificateValidationType.PATH_VALIDATION,
                            validationType,
                            CertValidationProcessor.Facility.IDENTITY,
                            auditor);
                    if ( cvr == CertificateValidationResult.OK ) {
                        valid = true;
                    }
                } catch ( GeneralSecurityException gse ) {
                    auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_VAL_ERROR, new String[]{what}, gse);
                }
            }
        } catch ( FindException fe ) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_VAL_ERROR, new String[]{what}, fe);
        }

        return valid;
    }

    /**
     * Is the given trustedCert one of the given certificateInfos
     */
    private boolean isTrusted( final CertificateInfo[] certificateInfos, final X509Certificate certificate ) {
        boolean trusted = false;

        for ( CertificateInfo info : certificateInfos ) {
            if ( info != null && info.matches(certificate) ) {
                trusted = true;
                break;
            }
        }

        return trusted;
    }

}
