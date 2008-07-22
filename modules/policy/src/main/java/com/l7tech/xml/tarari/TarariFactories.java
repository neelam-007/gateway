/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.xml.SoftwareFallbackException;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.xml.tarari.TarariUtil;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlSource;
import com.tarari.xml.rax.RaxDocument;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

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
            final XmlSource xmlSource;

            // Eat any recognized byte order mark and, if we do, sniff the encoding while we are at it
            ByteOrderMarkInputStream bomSniffingDog = new ByteOrderMarkInputStream(messageBody);
            String enc = bomSniffingDog.getEncoding();
            if (enc == null || ByteOrderMarkInputStream.UTF8.equals(enc)) {
                // We'll hope it's either ASCII or UTF-8, which Tarari can support natively
                xmlSource = new XmlSource(bomSniffingDog);
            } else {
                // It's an encoding that Tarari doesn't support, so we'll have to convert it on the fly
                Reader reader = new InputStreamReader(bomSniffingDog, enc);
                xmlSource = new XmlSource("", reader, true, enc);
            }

            return new TarariMessageContextImpl(RaxDocument.createDocument(xmlSource));

        } catch (XmlParseException e) { // more-specific
            TarariUtil.translateException(e);
        } catch (XmlConfigException e) { // less-specific
            TarariUtil.translateException(e);
        }
        throw new RuntimeException(); // unreachable
    }
}
