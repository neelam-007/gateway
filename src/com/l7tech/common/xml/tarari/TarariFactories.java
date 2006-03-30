/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariLoader;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tarari hardware-accelerated SoapInfoFactory.
 */
public class TarariFactories implements TarariMessageContextFactory {
    public TarariFactories() {}

    public TarariMessageContext makeMessageContext(InputStream messageBody)
            throws IOException, SAXException, SoftwareFallbackException
    {
        final GlobalTarariContextImpl globalContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        if (globalContext == null)
            throw new SoftwareFallbackException("No GlobalTarariContext");
        try {
            return new TarariMessageContextImpl(RaxDocument.createDocument(new XmlSource(messageBody)));
        } catch (XmlParseException e) { // more-specific
            TarariUtil.translateException(e);
        } catch (XmlConfigException e) { // less-specific
            TarariUtil.translateException(e);
        }
        throw new RuntimeException(); // unreachable
    }
}
