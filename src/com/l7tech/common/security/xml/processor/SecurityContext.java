/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import javax.crypto.SecretKey;

/**
 * Provided by SecurityContextFinder TO the WssProcessor.  Result of looking up a session. WssProcessor will
 * never create an instance of this.
 */
public interface SecurityContext {
    SecretKey getSharedSecret();
}
