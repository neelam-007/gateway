/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.tarari.xml.XmlConfigException;
import com.tarari.xml.rax.schema.SchemaLoader;

/**
 * Tarari-specific functionality for schema validation
 */
public class TarariSchemaHandlerImpl implements TarariSchemaHandler {
    public void loadHardware(String tns, String schemadoc) throws InvalidDocumentFormatException {
        try {
            SchemaLoader.loadSchema(schemadoc);
        } catch (XmlConfigException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    public void unloadHardware(String tns) {
        SchemaLoader.unloadSchema(tns);
    }

    public boolean validate(TarariMessageContext tmc) {
        return ((TarariMessageContextImpl)tmc).getRaxDocument().validate();
    }
}
