/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.X509SigningSecurityToken;
import org.w3c.dom.Element;

/**
 * @author mike
 */
public abstract class MutableX509SigningSecurityToken extends MutableSigningSecurityToken implements X509SigningSecurityToken {
    public MutableX509SigningSecurityToken(Element element) {
        super(element);
    }
}
