/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidatorResult;

/**
 *
 * @author mike
 * @version 1.0
 */
public interface AssertionValidator {
    void validate(PolicyValidatorResult result);
}
