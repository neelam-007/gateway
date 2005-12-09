/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.common.message;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides access to a {@link SecurityKnob} from a {@link Message}.
 */
public class SecurityFacet extends MessageFacet implements SecurityKnob {
    private ProcessorResult processorResult = null;
    private final List tokens = new ArrayList();

    /**
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    SecurityFacet(Message message, MessageFacet delegate) {
        super(message, delegate);
    }

    public List getAllSecurityTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public void addSecurityToken(SecurityToken token) {
        if (token == null) throw new NullPointerException();
        tokens.add(token);
    }

    public ProcessorResult getProcessorResult() {
        return processorResult;
    }

    public void setProcessorResult(ProcessorResult pr) {
        processorResult = pr;
    }

    public MessageKnob getKnob(Class c) {
        if (c == SecurityKnob.class) {
            return this;
        } else {
            return super.getKnob(c);
        }
    }
}
