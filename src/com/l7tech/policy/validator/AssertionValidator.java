/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;

/**
 *
 * @author mike
 * @version 1.0
 */
public interface AssertionValidator {
    void validate( Assertion assertion, PolicyValidatorResult result );
}
