/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.ValidationTarget;
import com.l7tech.server.util.Handle;
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

    /**
     * Validate the message against this schema, following the options given with the validationTarget parameter.
     *
     * @param msg the message to be validated
     * @param validationTarget specifies which parts of the message to validate, whether hardware validation should be attempted, etc.
     *                         @see com.l7tech.message.ValidationTarget
     * @param errorHandler collects schema validation errors
     */
    public void validateMessage(Message msg, ValidationTarget validationTarget, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        getCompiledSchema().validateMessage(msg, validationTarget, errorHandler);
    }

    /** Overridden in order to open up access to this to the current package. */
    @Override
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
