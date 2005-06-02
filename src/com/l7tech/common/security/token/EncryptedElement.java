/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

/**
 * @author emil
 */
public interface EncryptedElement extends ParsedElement {
    /**
     * @return the xml encryption algorithm such as http://www.w3.org/2001/04/xmlenc#tripledes-cbc
     */
    String getAlgorithm();
}
