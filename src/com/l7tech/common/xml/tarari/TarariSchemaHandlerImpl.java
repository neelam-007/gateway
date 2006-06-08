/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ConcurrentModificationException;

/**
 * Tarari-specific functionality for schema validation
 */
public class TarariSchemaHandlerImpl implements TarariSchemaHandler {
    private int loadCount = 0;

    public void setTarariSchemaResolver(final TarariSchemaResolver resolver) throws ConcurrentModificationException {
        synchronized (this) {
            if (loadCount != 0)
                throw new ConcurrentModificationException("A call to loadHardware is already pending");
        }
        if (resolver == null) throw new NullPointerException();
        SchemaLoader.setSchemaResolver(new SchemaResolver() {
            public byte[] resolveSchema(String string, String string1, String string2) {
                return resolver.resolveSchema(string, string1, string2);
            }
        });
    }

    public void loadHardware(String systemId, String schemadoc)
            throws SoftwareFallbackException, SAXException
    {
        synchronized (this) { loadCount++; }
        try {
            byte[] bytes = schemadoc.getBytes("UTF-8");
            SchemaLoader.loadSchema(bytes, bytes.length, systemId == null ? "" : systemId);
        } catch (XmlConfigException e) {
            TarariUtil.translateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen (it's a BAIS)
        } finally {
            synchronized (this) { --loadCount; }
        }
    }

    public void unloadHardware(String tns) {
        SchemaLoader.unloadSchema(tns);
    }

    public boolean validate(TarariMessageContext tmc) {
        return ((TarariMessageContextImpl)tmc).getRaxDocument().validate();
    }
}
