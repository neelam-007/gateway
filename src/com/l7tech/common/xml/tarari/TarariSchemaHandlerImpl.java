/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.util.ExceptionUtils;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

/**
 * Tarari-specific functionality for schema validation
 */
public class TarariSchemaHandlerImpl implements TarariSchemaHandler {
    private SchemaResolver tarariResolver;

    public void setTarariSchemaResolver(final TarariSchemaResolver resolver) {
        if (resolver == null) throw new NullPointerException();
        this.tarariResolver = new SchemaResolver() {
            public byte[] resolveSchema(String namespaceUri, String locationHint, String baseUri) {
                return resolver.resolveSchema(namespaceUri, locationHint, baseUri);
            }
        };
    }

    public void loadHardware(String systemId, String schemadoc)
            throws SoftwareFallbackException, SAXException
    {
        try {
            byte[] bytes = schemadoc.getBytes("UTF-8");
            SchemaLoader.setSchemaResolver(tarariResolver);
            SchemaLoader.loadSchema(new ByteArrayInputStream(bytes), systemId);
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

    public boolean validate(TarariMessageContext tmc) throws SAXException {
        try {
            return ((TarariMessageContextImpl)tmc).getRaxDocument().validate();
        } catch (com.tarari.xml.XmlException e) {
            throw new SAXException("Unable to validate document: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
