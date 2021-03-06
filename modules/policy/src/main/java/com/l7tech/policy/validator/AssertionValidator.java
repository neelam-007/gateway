/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;

/**
 * <code>AssertionValidator</code> implementations validate the assertion.
 * The <code>AssertionValidator</code> implementations are associated with
 * the <code>Assertion</code> in the {@link ValidatorFactory }. The validator
 * implementation is required to implement the constructor with the assertion
 * they validate as the argument.
 *
 * @author mike
 * @version 1.0
 * @see ValidatorFactory
 */
public interface AssertionValidator {
    /**
     * Validate the assertion in the given path, service and store the result in the
     * validator result.
     *
     * @param path    the assertion path where the assertion is located
     * @param pvc     information about the context in which the assertion appears
     * @param result  the result where the validation warnings or errors are collected
     */
    void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result);
}
