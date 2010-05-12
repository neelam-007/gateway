package com.l7tech.external.assertions.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.*;

/**
 *
 */
public class NonSoapEncryptElementValidator extends ElementSelectingXpathBasedAssertionValidator {
    private final AssertionValidatorSupport vs;
    private final AssertionValidator elementSelectingXpathValidator;

    public NonSoapEncryptElementValidator(final NonSoapEncryptElementAssertion assertion) {
        super(assertion);
        vs = new AssertionValidatorSupport<NonSoapEncryptElementAssertion>(assertion);
        elementSelectingXpathValidator = new ElementSelectingXpathValidator(assertion);
        requireValidCertificate(vs, assertion);
    }

    private static void requireValidCertificate(AssertionValidatorSupport vs, NonSoapEncryptElementAssertion assertion) {
        String certb64 = assertion.getRecipientCertificateBase64();
        if (certb64 == null || certb64.trim().length() < 1) {
            vs.addMessage("No recipient certificate is configured.  The assertion will always fail.");
            return;
        }

        try {
            CertUtils.decodeFromPEM(assertion.getRecipientCertificateBase64(), false);
        } catch (Exception e) {
            vs.addMessageAndException("Invalid certificate configured.  The assertion will always fail.", e);
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        vs.validate(path, pvc, result);
        elementSelectingXpathValidator.validate(path, pvc, result);
    }
}
