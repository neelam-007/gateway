/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

/**
 * Mapping that knows how to turn a RequestWssSaml into a wsse:SecurityToken element, and vice versa.
 * <p/>
 * The generic wsse:SecurityToken parser, {@link SecurityTokenTypeMapping}, delegates population of newly-deserialized
 * RequestWssSaml object back to this class.
 */
class SamlSecurityTokenAssertionMapping extends SecurityTokenAssertionMapping {
    public SamlSecurityTokenAssertionMapping() {
        super(new RequestWssSaml(), "RequestWssSaml", SecurityTokenType.SAML_ASSERTION);
    }

    protected String getPropertiesElementName() {
        return "SamlParams";
    }
}
