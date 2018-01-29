package com.l7tech.external.assertions.csrsigner.server;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.csrsigner.CsrSignerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.RsaSignerEngine;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;

/**
 * Server side implementation of the CsrSignerAssertion.
 *
 * @see com.l7tech.external.assertions.csrsigner.CsrSignerAssertion
 */
public class ServerCsrSignerAssertion extends AbstractServerAssertion<CsrSignerAssertion> {

    private static final int MAX_CSR_SIZE = SyspropUtil.getInteger(ServerCsrSignerAssertion.class.getName() + ".maxCsrBytes", 200 * 1024);
    private static final int MAX_CSR_AGE = 10 * 365; // 10 years = 5 years (industry max) X 2.
    final String[] variablesUsed;

    @Inject
    SsgKeyStoreManager ssgKeyStoreManager;

    @Inject
    DefaultKey defaultKey;


    public ServerCsrSignerAssertion( final CsrSignerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        variablesUsed = assertion.getVariablesUsed();
    }

    public ServerCsrSignerAssertion( final CsrSignerAssertion assertion, final AuditFactory auditFactory ) throws PolicyAssertionException {
        super(assertion, auditFactory);
        variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final SignerInfo signerInfo;
        try {
            signerInfo = assertion.isUsesDefaultKeyStore()
                    ? defaultKey.getSslInfo()
                    : ssgKeyStoreManager.lookupKeyByKeyAlias(assertion.getKeyAlias(), assertion.getNonDefaultKeystoreId());
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to access CA private key: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }

        final String csrVar = assertion.getCsrVariableName();
        if (csrVar == null || csrVar.trim().length() < 1) {
            logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "no CSR variable name configured");
            return AssertionStatus.SERVER_ERROR;
        }

        final byte[] csrBytes = findCsrBytes(context, csrVar);
        if (csrBytes.length < 1) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "CSR variable was empty");
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            final PrivateKey caKey = signerInfo.getPrivate();
            final X509Certificate[] caChain = signerInfo.getCertificateChain();

            RsaSignerEngine signerEngine = JceProvider.getInstance().createRsaSignerEngine(caKey, caChain);

            // Set the days until expiry of the certificate.
            CertGenParams params = new CertGenParams();
            final Map<String, Object> varMap = context.getVariableMap(variablesUsed, getAudit());
            String daysUntilExpiry = assertion.getExpiryAgeDays();
            int resolvedExpiryAge;

            // Check if expiryAgeDays has been previously set.
            if (StringUtils.isEmpty(daysUntilExpiry)){
                // daysUntilExpiry is a newly introduced field and will be null for policies saved with
                // older version of this assertion.  For previously saved policies that have null values,
                // populate the expiry age based on the original default logic.
                if (StringUtils.isEmpty(assertion.getCertDNVariableName())){
                    resolvedExpiryAge = CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_NO_DN_OVERRIDE;
                } else {
                    resolvedExpiryAge = CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_DN_OVERRIDE;
                }
                params.setDaysUntilExpiry(resolvedExpiryAge);

            } else {
                // The expiry age was previously populated.  If context variable is present, resolve.
                String expiryAgeFromContextVar = ExpandVariables.process(assertion.getExpiryAgeDays(), varMap, getAudit());
                if (StringUtils.isBlank(expiryAgeFromContextVar)) {
                    // Could not resolve the context variable.
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Could not resolve the context variable in the Expiry Age field");
                    return AssertionStatus.FAILED;
                } else {
                    resolvedExpiryAge = Integer.parseInt(expiryAgeFromContextVar);
                    if (resolvedExpiryAge <= 0 || resolvedExpiryAge > MAX_CSR_AGE ) {
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The Expiry Age field must a number between 1 and " + String.valueOf(MAX_CSR_AGE));
                        return AssertionStatus.FAILED;
                    }
                    params.setDaysUntilExpiry(resolvedExpiryAge);
                }
            }

            // TODO allow certificate generation parameters to be customized, particularly things like DN, expiry date, and digest algorithm
            String certDNVariableName = assertion.getCertDNVariableName();
            if (certDNVariableName != null && certDNVariableName.trim().length() > 0) {
                final String certDN =  (String)context.getVariable(certDNVariableName);
                if (certDN != null && certDN.trim().length() > 0) {
                    params = new CertGenParams(new X500Principal(certDN), resolvedExpiryAge , false, null); // input expiry age calculated in previous section.
                    // In regular flow where the Override DN is not specified, the CertGetparam's notAfter was never set and
                    // daysUntilExpiry was used to calculate the expiry date.
                    // When the Overide Dn is specified, the CertGetparam's notAfter is set by the CertGenParams() constructor
                    // and is used to calculate the certificate expiry date.   Therefore, in this flow,
                    // params.setDaysUntilExpiry(resolvedExpiryAge) is not required.
                }
            }

            X509Certificate cert = (X509Certificate)signerEngine.createCertificate(csrBytes, params);

            X509Certificate[] fullChain = new X509Certificate[caChain.length + 1];
            fullChain[0] = cert;
            System.arraycopy(caChain, 0, fullChain, 1, caChain.length);

            context.setVariable(assertion.prefix(CsrSignerAssertion.VAR_CERT), cert);
            context.setVariable(assertion.prefix(CsrSignerAssertion.VAR_CHAIN), fullChain);

            return AssertionStatus.NONE;

        } catch (UnrecoverableKeyException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to access private key material: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Failed to generate certificate: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }
    }

    private byte[] findCsrBytes(PolicyEnforcementContext context, String csrVar) {
        final Object csrObj;
        try {
            csrObj = context.getVariable(csrVar);
            if (csrObj == null) {
                logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, csrVar);
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            }
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, csrVar);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        if (csrObj instanceof byte[]) {
            logger.log(Level.FINE, "CSR value is a byte array, assuming it is already in DER format");
            // It is already a raw byte array; assume it is in DER format already
            return (byte[]) csrObj;
        } else if (csrObj instanceof Message) {
            Message message = (Message) csrObj;
            try {
                return IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream(true), MAX_CSR_SIZE);
            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to read CSR: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.NO_SUCH_PART, new String[]{ csrVar, e.getWhatWasMissing() }, ExceptionUtils.getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            }
        }

        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "unsupported CSR variable type of " + csrVar.getClass().getName());
        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);

    }
}