/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.service.PublishedService;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates the SWA assertion in the context of the policy and the
 * service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class SwaRequestAssertionValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(SwaRequestAssertionValidator.class.getName());
    private final RequestSwAAssertion assertion;

    public SwaRequestAssertionValidator(RequestSwAAssertion ra) {
        if (ra == null) {
            throw new IllegalArgumentException();
        }
        assertion = ra;
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        if (path == null || service == null || result == null) {
            throw new IllegalArgumentException();
        }
        if (!service.isSoap()) {
            return;
        }
        try {
            Wsdl wsdl = service.parsedWsdl();
            // check whether any binding info operation
            for (Iterator iterator = assertion.getBindings().values().iterator(); iterator.hasNext();) {
                BindingInfo bi = (BindingInfo)iterator.next();
                if (hasMimeParts(bi, wsdl)) {
                    return;
                }
            }
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
              path,
              "This service does not declare any MIME input parameters (\"multipart/related\")\n" +
              "This assertion only works with soap services with attachments.",
              null));
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean hasMimeParts(BindingInfo bi, Wsdl wsdl) {
        final String bindingName = bi.getBindingName();
        Binding binding = wsdl.getBinding(bindingName);
        if (binding == null) {
            logger.warning("Could not resolve binding '" + bindingName + "'");
        }

        List bop = binding.getBindingOperations();
        for (Iterator iterator = bop.iterator(); iterator.hasNext();) {
            BindingOperation bo = (BindingOperation)iterator.next();
            MIMEMultipartRelated mmr = wsdl.getMimeMultipartRelatedInput(bo);
            if (mmr != null) {   // todo: not sure if need to check if it is empty too (mmr.getMIMEParts().isEmpty())? - em
                return true;
            }
        }
        return false;
    }
}
