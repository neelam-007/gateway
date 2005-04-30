/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author mike
 */
public interface UsernameToken extends SecurityToken {
    LoginCredentials asLoginCredentials();

    /** @return XML serialized version of this SecurityToken using the specified Security namespace and owner document. */
    Element asElement(Document factory, String securityNs, String securityPrefix);

    /**
     * @return XML serialized version of this SecurityToken.  This will return an existing element, if there is one.
     *         Otherwise, a new element will be created as the root of a new document and returned.
     *         This will use the default security namespace and prefix.
     */
    Element asElement();
}
