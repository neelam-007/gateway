/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.xml.SoftwareFallbackException;
import org.xml.sax.SAXException;

import java.io.InputStream;

/**
 * Represents something that knows how to get some SoapInfo.
 */
public interface SoapInfoFactory {
    SoapInfo getSoapInfo(InputStream inputStream) throws SoftwareFallbackException, SAXException;
}
