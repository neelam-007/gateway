/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;

/**
 * @author mike
 */
public interface XmlSecurityToken extends ParsedElement, SecurityToken {
    String getElementId();
}
