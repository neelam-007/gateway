/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.xml.tarari.TarariMessageContext;
import org.xml.sax.SAXException;

/**
 * Represents something that knows how to get some SoapInfo.
 */
public interface SoapInfoFactory {
    SoapInfo getSoapInfo(TarariMessageContext context) throws SAXException;
}
