/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * @author mike
 */
public interface UsernameToken extends SecurityToken {
    LoginCredentials asLoginCredentials();
}
