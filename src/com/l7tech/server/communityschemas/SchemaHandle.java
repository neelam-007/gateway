/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.http.cache.UserObject;
import com.l7tech.server.util.Handle;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Public handle to a schema obtained from SchemaManager.compile().
 */
public final class SchemaHandle extends Handle<CompiledSchema> implements UserObject {
    SchemaHandle(CompiledSchema cs) {
        super(cs);
    }

    /** Validate the entire message against this schema. */
    public void validateMessage(Message msg, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        getTarget().validateMessage(msg, errorHandler);
    }

    /** Validate just these elements against this schema.  This will not be hardware accelerated. */
    public void validateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        getTarget().validateElements(elementsToValidate, errorHandler);
    }

    CompiledSchema getCompiledSchema() {
        return getTarget();
    }
}
