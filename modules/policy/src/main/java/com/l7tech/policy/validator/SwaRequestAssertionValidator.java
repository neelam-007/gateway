/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.validator;

import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates the SWA assertion in the context of the policy and the
 * service.
 */
public class SwaRequestAssertionValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(SwaRequestAssertionValidator.class.getName());
    private final RequestSwAAssertion assertion;
    private String wsdlError = null;

    public SwaRequestAssertionValidator(RequestSwAAssertion ra) {
        if (ra == null) {
            throw new IllegalArgumentException();
        }
        assertion = ra;
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (path == null || wsdl == null || result == null) throw new IllegalArgumentException();
        if (!soap) return;

        if (wsdlError != null) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path, wsdlError, null));
            return;
        }

        // check whether any binding info operation
        for (BindingInfo bi : assertion.getBindings().values()) {
            if (hasMimeParts(bi, wsdl)) return;
        }
        wsdlError = "This service does not declare any MIME input parameters (\"multipart/related\")\n" +
                                 "This assertion only works with soap services with attachments.";
        result.addWarning(new PolicyValidatorResult.Warning(assertion, path, wsdlError, null));
    }


    private boolean hasMimeParts(BindingInfo bi, Wsdl wsdl) {
        final String bindingName = bi.getBindingName();
        Binding binding = wsdl.getBinding(bindingName);
        if (binding == null) {
            logger.warning("Could not resolve binding '" + bindingName + "'");
        }
        else {
            //noinspection unchecked
            List<BindingOperation> bops = binding.getBindingOperations();
            for (BindingOperation bop : bops) {
                MIMEMultipartRelated mmr = wsdl.getMimeMultipartRelatedInput(bop);
                if (mmr != null) {   // todo: not sure if need to check if it is empty too (mmr.getMIMEParts().isEmpty())? - em
                    return true;
                }
            }
        }
        return false;
    }
}
