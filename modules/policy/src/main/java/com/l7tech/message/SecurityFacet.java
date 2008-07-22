/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Provides access to a {@link SecurityKnob} from a {@link Message}.
 */
public class SecurityFacet extends MessageFacet implements SecurityKnob {
    private ProcessorResult processorResult = null;
    private final List tokens = new ArrayList();
    private DecorationRequirements decorationRequirements = null;
    private Map decorationRequirementsForAlternateRecipients = new HashMap();

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

    /**
     * Get the decorations that should be applied to this Message some time in the future. One DecorationRequirements
     * per recipient, the default recipient having its requirements at the end of the array. Can return an empty array
     * but never null.
     */
    public DecorationRequirements[] getDecorationRequirements() {
        Set keys = decorationRequirementsForAlternateRecipients.keySet();
        int arraysize = keys.size();
        if (decorationRequirements != null) {
            arraysize += 1;
        }
        DecorationRequirements[] output = new DecorationRequirements[arraysize];
        int i = 0;
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            output[i] = (DecorationRequirements)decorationRequirementsForAlternateRecipients.get(iterator.next());
            i++;
        }
        if (decorationRequirements != null) {
            output[arraysize-1] = decorationRequirements;
        }
        return output;
    }

    public DecorationRequirements getAlternateDecorationRequirements(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            return getOrMakeDecorationRequirements();
        }
        String actor = recipient.getActor();
        DecorationRequirements output = (DecorationRequirements)decorationRequirementsForAlternateRecipients.get(actor);
        if (output == null) {
            output = new DecorationRequirements();
            X509Certificate clientCert;
            clientCert = recipient.getX509Certificate();
            output.setRecipientCertificate(clientCert);
            output.setSecurityHeaderActor(actor);
            decorationRequirementsForAlternateRecipients.put(actor, output);
        }
        return output;
    }

    public DecorationRequirements getOrMakeDecorationRequirements() {
        if (decorationRequirements == null) {
            decorationRequirements = new DecorationRequirements();
        }
        return decorationRequirements;
    }

    public MessageKnob getKnob(Class c) {
        if (c == SecurityKnob.class) {
            return this;
        } else {
            return super.getKnob(c);
        }
    }
}
