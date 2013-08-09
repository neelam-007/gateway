package com.l7tech.external.assertions.oauth.server;

import com.l7tech.external.assertions.oauth.OAuthValidationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Map;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.*;
import java.net.URLDecoder;

/**
 * Server side implementation of the OAuthValidationAssertion.
 *
 * @see com.l7tech.external.assertions.oauth.OAuthValidationAssertion
 */
public class ServerOAuthValidationAssertion extends AbstractServerAssertion<OAuthValidationAssertion> {
    private final String[] variablesUsed;

    private final TrustedCertCache trustedCertCache;

    public ServerOAuthValidationAssertion(OAuthValidationAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.trustedCertCache = context.getBean( "trustedCertCache", TrustedCertCache.class );
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            final String tokenSig = ExpandVariables.process(assertion.getOAuthTokenSignature(), vars, getAudit(), true);
            final String textBlock = ExpandVariables.process(assertion.getOAuthTokenText(), vars, getAudit(), true);

            if (tokenSig == null || tokenSig.length() == 0) {
                logger.log(Level.WARNING, "OAuth token signature not set");
                return AssertionStatus.FAILED;
            }
            if (textBlock == null || textBlock.length() == 0) {
                logger.log(Level.WARNING, "OAuth signed data not set");
                return AssertionStatus.FAILED;
            }

            boolean valid = validateTokenSignature(tokenSig, textBlock);
            context.setVariable(OAuthValidationAssertion.VARIABLE_TOKEN_SIGNATURE_VALID, valid);

            if (!valid && assertion.isFailOnMismatch())
                return AssertionStatus.FALSIFIED;

        } catch (Exception ex) {
            logger.log(Level.INFO, "Error while processing OAuth token", ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }


    private boolean validateTokenSignature(final String signature, final String dataToVerify) throws Exception {

        // decode base64 signature value (unescape signature value first if necessary)
        final byte[] signatureBytes;
        if (assertion.isSignatureEncoded()) {
            final String unescapeSig = URLDecoder.decode(signature, "UTF-8");
            signatureBytes = HexUtils.decodeBase64(unescapeSig);
        } else {
            signatureBytes = HexUtils.decodeBase64(signature);
        }

        // derive the bytes used to verify the signature with
        final byte[] dataBytes = dataToVerify.getBytes();

        // get the Cert used to verify the sig
        try {
            Signature verifier = Signature.getInstance(assertion.getSignatureAlgorithm());
            verifier.initVerify(getSelectedCertificate());
            verifier.update(dataBytes);
            boolean result = verifier.verify(signatureBytes);
            logger.log(Level.INFO, "OAuth token valid: {0}\nSignature Value: {1}\nVerified Text: {2}", new Object[] {result, signature, dataToVerify});
            return result;
        } catch (NoSuchAlgorithmException nsa) {
            logger.log(Level.WARNING, "Missing signature algorithm: " + assertion.getSignatureAlgorithm(), ExceptionUtils.getDebugException(nsa));
            throw nsa;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error validating signature", ExceptionUtils.getDebugException(ex));
            throw ex;
        }
    }


    /**
     * Returns the specified signature verification certificate in the assertion using the alias configured in
     * the assertion.
     *
     * @return the X509Certificate that was selected in assertion and null if a pre-configured cert was not specified
     * @throws java.security.cert.CertificateException when an error is encountered while retrieving a certificate
     */
    private X509Certificate getSelectedCertificate() throws CertificateException {

        X509Certificate selectedCert = null;
        String description = "";
        try {
            final String certName = assertion.getVerifyCertificateName();

            if ( certName != null ) {
                description = "name " + certName;
                Collection<TrustedCert> trustedCertificates = trustedCertCache.findByName( certName );
                X509Certificate certificate = null;
                X509Certificate expiredCertificate = null;
                for ( TrustedCert trustedCert : trustedCertificates ) {
                    if ( !isExpiredCert(trustedCert) ) {
                        certificate = trustedCert.getCertificate();
                        break;
                    } else if ( expiredCertificate == null ) {
                        expiredCertificate = trustedCert.getCertificate();
                    }
                }

                if ( certificate != null || expiredCertificate != null ) {
                    selectedCert = certificate!=null ? certificate : expiredCertificate;
                } else {
                    logAndAudit(AssertionMessages.WSSECURITY_RECIP_NO_CERT, description);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            }
        } catch ( FindException e ) {
            logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_ERROR, description);
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        return selectedCert;
    }

    /**
     * Checks whether the certificate in the argument is expired.
     *
     * @param trustedCert the TrustedCert to check
     * @return true if the certificate is expired, false otherwise
     */
    private boolean isExpiredCert( final TrustedCert trustedCert ) {
        boolean expired = true;

        try {
            expired = trustedCert.isExpiredCert();
        } catch (CertificateException e) {
            logAndAudit(AssertionMessages.WSSECURITY_RECIP_CERT_EXP, new String[]{ trustedCert.getName() + " (#"+trustedCert.getGoid()+")"}, e);
        }

        return expired;
    }
}