/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

/**
 * Provided by the caller TO the WssProcessor.  WssProcessor will use this to look up sessions.
 * WssProcessor will never create an instance of this.
 */
public interface SecurityContextFinder {
    SecurityContext getSecurityContext(String securityContextIdentifier);
}
