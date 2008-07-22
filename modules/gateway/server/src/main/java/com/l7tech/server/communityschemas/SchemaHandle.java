/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.server.util.Handle;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Public handle to a schema obtained from SchemaManager.compile().
 */
public final class SchemaHandle extends Handle<CompiledSchema> {
    private final String systemId;
    private final String tns;

    SchemaHandle(CompiledSchema cs) {
        super(cs);
        this.systemId = cs.getSystemId();
        this.tns = cs.getTargetNamespace();
    }

    /** Validate the entire message against this schema. */
    public void validateMessage(Message msg, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        getCompiledSchema().validateMessage(msg, errorHandler);
    }

    /** Validate just these elements against this schema.  This will not be hardware accelerated. */
    public void validateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        getCompiledSchema().validateElements(elementsToValidate, errorHandler);
    }

    /** Overridden in order to open up acces to this to the current package. */
    protected CompiledSchema getTarget() {
        return super.getTarget();
    }

    /**
     * Get the CompiledSchema this handle points to.  Unlike {@link #getTarget()}, this can never return null.
     *
     * @return the CompiledSchema instance.  Never null.
     * @throws IllegalStateException if this SchemaHandle has already been closed.
     */
    CompiledSchema getCompiledSchema() {
        CompiledSchema target = getTarget();
        if (target == null) throw new IllegalStateException("Unable to validate schema -- SchemaHandle has already been closed (systemId=" + systemId + "   tns=" + tns + ")");
        return target;
    }
}
