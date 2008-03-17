package com.l7tech.external.assertions.ncesval.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.SigningSecurityToken;
import com.l7tech.common.security.token.XmlSecurityToken;
import com.l7tech.common.security.token.X509SecurityToken;
import com.l7tech.common.security.token.X509SigningSecurityToken;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.WssProcessorImpl;
import com.l7tech.common.security.CertificateValidationResult;
import com.l7tech.common.security.CertificateValidationType;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.CertificateInfo;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

/**
 * Server side implementation of the NcesValidatorAssertion.
 *
 * @see com.l7tech.external.assertions.ncesval.NcesValidatorAssertion
 */
public class ServerNcesValidatorAssertion extends AbstractServerAssertion<NcesValidatorAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNcesValidatorAssertion.class.getName());

    private static final int MAX_CACHE_AGE = 5 * 1000;

    private final Auditor auditor;
    private final CertValidationProcessor certValidationProcessor;
    private final SecurityTokenResolver securityTokenResolver;
    private final TrustedCertManager trustedCertManager;

    public ServerNcesValidatorAssertion(NcesValidatorAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.auditor = new Auditor(this, context, logger);
        this.certValidationProcessor = (CertValidationProcessor)context.getBean("certValidationProcessor");
        this.securityTokenResolver = (SecurityTokenResolver)context.getBean("securityTokenResolver");
        this.trustedCertManager = (TrustedCertManager)context.getBean("trustedCertManager");
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message msg;
        final String what;
        switch(assertion.getTarget()) {
            case REQUEST:
                what = "request";
                msg = context.getRequest();
                break;
            case RESPONSE:
                what = "response";
                msg = context.getResponse();
                break;
            case OTHER:
                what = assertion.getOtherMessageVariableName();
                if (what == null) throw new PolicyAssertionException(assertion, "Target is OTHER but OtherMessageVariableName is null");
                msg = context.getRequestMessage(what);
                if (msg == null) {
                    auditor.logAndAudit(AssertionMessages.NCESVALID_NO_MSG, what);
                    return AssertionStatus.FAILED;
                }
                break;
            default:
                throw new IllegalStateException("Unsupported TargetMessageType: " + assertion.getTarget());
        }

        try {
            if (!msg.isSoap(true)) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_NOT_SOAP, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_BAD_XML, new String[] { what, ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.BAD_REQUEST;
        }

        SecurityKnob sk = (SecurityKnob)msg.getKnob(SecurityKnob.class);

        final ProcessorResult wssResult;
        if (sk != null) {
            wssResult = sk.getProcessorResult();
        } else {
            // Need to Trogdor this message before we can validate it
            try {
                WssProcessorImpl trogdor = new WssProcessorImpl(msg);
                trogdor.setSecurityTokenResolver(securityTokenResolver);
                wssResult = trogdor.processMessage();
            } catch (InvalidDocumentFormatException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (ProcessorException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (BadSecurityContextException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (SAXException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }

        SamlAssertion saml = null;
        X509Certificate cert = null;
        for (XmlSecurityToken token : wssResult.getXmlSecurityTokens()) {
            if (token instanceof SamlAssertion) {
                if (assertion.isSamlRequired()) {
                    saml = (SamlAssertion)token;
                }
            } else  if (token instanceof X509SecurityToken) {
                X509SecurityToken x509Token = (X509SecurityToken)token;
                if ( x509Token.isPossessionProved() ) {
                    cert = x509Token.getCertificate();
                }
            }
        }

        SigningSecurityToken samlSigner = null;
        SigningSecurityToken timestampSigner = null;
        SigningSecurityToken messageIdSigner = null;
        SigningSecurityToken bodySigner = null;

        for (SignedElement signedElement : wssResult.getElementsThatWereSigned()) {
            SigningSecurityToken sst = signedElement.getSigningSecurityToken();
            Element el = signedElement.asElement();
            if (saml != null && saml.asElement().isEqualNode(el)) samlSigner = sst;
            if (XmlUtil.elementInNamespace(el, SoapUtil.WSA_NAMESPACE_ARRAY) && SoapUtil.MESSAGEID_EL_NAME.equals(el.getLocalName())) messageIdSigner = sst;
            if (XmlUtil.elementInNamespace(el, SoapUtil.WSU_URIS_ARRAY) && SoapUtil.TIMESTAMP_EL_NAME.equals(el.getLocalName())) timestampSigner = sst;
            try {
                if (SoapUtil.isBody(el)) bodySigner = sst;
            } catch (InvalidDocumentFormatException e) {
                throw new PolicyAssertionException(assertion, "Can't find SOAP Body element", e);
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
            ok = samlSigner == timestampSigner && timestampSigner == messageIdSigner && messageIdSigner == bodySigner;
        } else {
            ok = timestampSigner == messageIdSigner && messageIdSigner == bodySigner;
        }

        if (!ok) {
            auditor.logAndAudit(AssertionMessages.NCESVALID_DIFF_SIGNATURES, what);
            return AssertionStatus.BAD_REQUEST;
        }

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
            } if (!certificateValid( cert, what )) {
                return AssertionStatus.FALSIFIED;
            }
        }

        return AssertionStatus.NONE;
    }

    /**
     * Ensure signing certificate is valid / trusted 
     */
    private boolean certificateValid( final X509Certificate cert, final String what ) {
        boolean valid = false;

        // This can be null, in which case the facilities value is used
        CertificateValidationType validationType = assertion.getCertificateValidationType();

        // Find trusted issuer
        String issuerDn = cert.getIssuerDN().getName();
        if ( assertion.getTrustedCertificateInfo() != null && assertion.getTrustedCertificateInfo().length > 0 ) {
            try {
                TrustedCert trustedCert = trustedCertManager.getCachedCertBySubjectDn( issuerDn, MAX_CACHE_AGE );

                if ( trustedCert == null ||
                     !isTrusted( assertion.getTrustedCertificateInfo(), trustedCert ) ) {
                    auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_UNTRUSTED, what);
                } else {
                    X509Certificate signerParent = trustedCert.getCertificate();

                    try {
                        CertificateValidationResult cvr = certValidationProcessor.check(
                                new X509Certificate[]{cert, signerParent},
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
            } catch ( CertificateException ce ) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_VAL_ERROR, new String[]{what}, ce);
            } catch ( FindException fe ) {
                auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_VAL_ERROR, new String[]{what}, fe);
            }
        } else {
            auditor.logAndAudit(AssertionMessages.NCESVALID_CERT_UNTRUSTED, what);
        }

        return valid;
    }

    /**
     * Is the given trustedCert one of the given certificateInfos
     */
    private boolean isTrusted( final CertificateInfo[] certificateInfos, final TrustedCert trustedCert ) {
        boolean trusted = false;

        for ( CertificateInfo info : certificateInfos ) {
            try {
                X509Certificate certificate = trustedCert.getCertificate();
                if ( info != null && info.getSubjectDn() != null &&
                     info.getIssuerDn() != null && info.getSerialNumber() !=null &&
                     info.getSubjectDn().equals( certificate.getSubjectDN().getName() ) &&
                     info.getIssuerDn().equals( certificate.getIssuerDN().getName() ) &&
                     info.getSerialNumber().equals( certificate.getSerialNumber() )) {
                    trusted = true;
                    break;
                }
            } catch ( CertificateException ce ) {
                // so not trusted
            }
        }

        return trusted;
    }

}
