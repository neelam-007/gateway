/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.token.SecurityTokenType;

/**
 * The abstract superclass of all TokenStrategies. Holds the expected {@link SecurityTokenType}.
 */
public abstract class AbstractTokenStrategy implements TokenStrategy {
    private final SecurityTokenType tokenType;

    protected AbstractTokenStrategy(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public SecurityTokenType getType() {
        return tokenType;
    }
}
