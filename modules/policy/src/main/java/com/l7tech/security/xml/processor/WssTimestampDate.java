/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.ParsedElement;

/**
 * @author mike
 */
public interface WssTimestampDate extends ParsedElement {
    long asTime();
    String asIsoString();
}
