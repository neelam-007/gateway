/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.rax.schema.SchemaLoader;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Tarari-specific functionality for schema validation
 */
public class TarariSchemaHandlerImpl implements TarariSchemaHandler {
    public void loadHardware(String systemId, String schemadoc) throws SoftwareFallbackException, SAXException {
        try {
            byte[] bytes = schemadoc.getBytes("UTF-8");
            SchemaLoader.loadSchema(bytes, bytes.length, systemId == null ? "" : systemId);
        } catch (XmlConfigException e) {
            TarariUtil.translateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen (it's a BAIS)
        }
    }

    public void unloadHardware(String tns) {
        SchemaLoader.unloadSchema(tns);
    }

    public boolean validate(TarariMessageContext tmc) {
        return ((TarariMessageContextImpl)tmc).getRaxDocument().validate();
    }
}
