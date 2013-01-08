package com.l7tech.external.assertions.validatecertificate.server;

import com.l7tech.external.assertions.validatecertificate.ValidateCertificateAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

/**
 * Server side implementation of ValidateCertificateAssertion which validates an X509Certificate.
 */
public class ServerValidateCertificateAssertion extends AbstractServerAssertion<ValidateCertificateAssertion> {
    @Inject
    private CertValidationProcessor certValidator;

    public ServerValidateCertificateAssertion(@NotNull final ValidateCertificateAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        if(StringUtils.isBlank(assertion.getSourceVariable())){
            throw new PolicyAssertionException(assertion, "Source Variable cannot be blank");
        }
    }

    /**
     * Executes validation on an X509Certificate stored as a variable in the PolicyEnforcementContext.
     *
     * @param context the PolicyEnforcementContext which contains a X509Certificate variable.  Never null.
     * @return AssertionStatus.NONE if validation is successful or if validation fails but the assertion is configured to be logOnly.
     * Otherwise returns AssertionStatus.FALSIFIED.
     */
    public AssertionStatus checkRequest(@NotNull final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus assertionStatus = assertion.isLogOnly() ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
        try {
            final Object found = context.getVariable(assertion.getSourceVariable());
            if (found instanceof X509Certificate) {
                final X509Certificate cert = (X509Certificate) found;
                final CertificateValidationResult result = certValidator.check(new X509Certificate[]{cert},
                        assertion.getValidationType(), assertion.getValidationType(), CertValidationProcessor.Facility.OTHER, getAudit());
                if (result.equals(CertificateValidationResult.OK)) {
                    assertionStatus = AssertionStatus.NONE;
                } else {
                    getAudit().logAndAudit(AssertionMessages.CERT_VALIDATION_STATUS_FAILURE, assertion.getValidationType().toString(), result.toString());
                }
            } else {
                logger.log(Level.WARNING, "Context variable " + assertion.getSourceVariable() + " found but is not a certificate");
                getAudit().logAndAudit(AssertionMessages.CERT_NOT_FOUND, assertion.getSourceVariable());
            }
        } catch (final NoSuchVariableException e) {
            logger.log(Level.WARNING, "No certificate found for variable: " + assertion.getSourceVariable(), ExceptionUtils.getDebugException(e));
            getAudit().logAndAudit(AssertionMessages.CERT_NOT_FOUND, assertion.getSourceVariable());
        } catch (final CertificateException e) {
            logger.log(Level.WARNING, "Error validating certificate: " + e.getMessage(), ExceptionUtils.getDebugException(e));
            getAudit().logAndAudit(AssertionMessages.CERT_VALIDATION_FAILURE, assertion.getValidationType().toString(), e.getMessage());
        } catch (final SignatureException e) {
            logger.log(Level.WARNING, "Error validating certificate signature", ExceptionUtils.getDebugException(e));
            getAudit().logAndAudit(AssertionMessages.CERT_VALIDATION_FAILURE, assertion.getValidationType().toString(), "error validating signature");
        }
        return assertionStatus;
    }
}
