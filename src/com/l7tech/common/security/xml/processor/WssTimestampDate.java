/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import java.util.Date;

/**
 * @author mike
 */
public interface WssTimestampDate extends ParsedElement {
    Date asDate();
}
