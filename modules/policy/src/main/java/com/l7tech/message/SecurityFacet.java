/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

import java.security.cert.X509Certificate;
import java.util.*;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Provides access to a {@link SecurityKnob} from a {@link Message}.
 */
public class SecurityFacet extends MessageFacet implements SecurityKnob {
    private ProcessorResult processorResult = null;
    private final List<SecurityToken> tokens = new ArrayList<SecurityToken>();
    private DecorationRequirements decorationRequirements = null;
    private Map<String,DecorationRequirements> decorationRequirementsForAlternateRecipients = new HashMap<String,DecorationRequirements>();
    private ProcessorResultFactory lazyProcessor = null;
    private WsSecurityVersion wsSecurityVersion;
    private WssDecorator.DecorationResult decorationResult;
    private boolean signatureConfirmationValidated = false;
    private boolean needsSignatureConfirmations = false;

    /**
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    SecurityFacet(Message message, MessageFacet delegate) {
        super(message, delegate);
    }

    @Override
    public List<SecurityToken> getAllSecurityTokens() {
        return Collections.unmodifiableList(tokens);
    }

    @Override
    public void addSecurityToken(SecurityToken token) {
        if (token == null) throw new NullPointerException();
        tokens.add(token);
    }

    /**
     * @return the factory for lazily populating missing processor results, or null if not set.
     */
    public ProcessorResultFactory getLazyProcessor() {
        return lazyProcessor;
    }

    /**
     * Configure a factory for lazily populating processor results.
     *
     * @param lazyProcessor a factory for lazily producing a missing processor result, or null.
     */
    public void setProcessorResultFactory(ProcessorResultFactory lazyProcessor) {
        this.lazyProcessor = lazyProcessor;
    }

    @Override
    public ProcessorResult getProcessorResult() {
        return processorResult;
    }

    @Override
    public ProcessorResult getOrCreateProcessorResult() throws ProcessorException, SAXException, IOException {
        if (processorResult != null || lazyProcessor == null)
            return processorResult;

        return processorResult = lazyProcessor.createProcessorResult();
    }

    @Override
    public void setProcessorResult(ProcessorResult pr) {
        processorResult = pr;
    }

    /**
     * Get the decorations that should be applied to this Message some time in the future. One DecorationRequirements
     * per recipient, the default recipient having its requirements at the end of the array. Can return an empty array
     * but never null.
     */
    @Override
    public DecorationRequirements[] getDecorationRequirements() {
        Set<String> keys = decorationRequirementsForAlternateRecipients.keySet();
        int arraysize = keys.size();
        if (decorationRequirements != null) {
            arraysize += 1;
        }
        DecorationRequirements[] output = new DecorationRequirements[arraysize];
        int i = 0;
        for (String key : keys) {
            output[i] = decorationRequirementsForAlternateRecipients.get(key);
            i++;
        }
        if (decorationRequirements != null) {
            output[arraysize-1] = decorationRequirements;
        }
        return output;
    }

    @Override
    public DecorationRequirements getAlternateDecorationRequirements(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            return getOrMakeDecorationRequirements();
        }
        String actor = recipient.getActor();
        DecorationRequirements output = decorationRequirementsForAlternateRecipients.get(actor);
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

    @Override
    public DecorationRequirements getOrMakeDecorationRequirements() {
        if (decorationRequirements == null) {
            decorationRequirements = new DecorationRequirements();
        }
        return decorationRequirements;
    }

    @Override
    public void removeAllDecorationRequirements() {
        decorationRequirements = null;
        decorationRequirementsForAlternateRecipients = new HashMap<String,DecorationRequirements>();
    }

    @Override
    public WsSecurityVersion getPolicyWssVersion() {
        return wsSecurityVersion;
    }

    @Override
    public void setPolicyWssVersion(WsSecurityVersion wsSecurityVersion) {
        this.wsSecurityVersion = wsSecurityVersion;
    }

    @Override
    public void setDecorationResult(WssDecorator.DecorationResult dr) {
        decorationResult = dr;
    }

    @Override
    public WssDecorator.DecorationResult getDecorationResult() {
        return decorationResult;
    }

    @Override
    public void setSignatureConfirmationValidated(boolean validated) {
        this.signatureConfirmationValidated = validated;
    }

    @Override
    public boolean isSignatureConfirmationValidated() {
        return signatureConfirmationValidated;
    }

    @Override
    public boolean isNeedsSignatureConfirmations() {
        return needsSignatureConfirmations;
    }

    @Override
    public void setNeedsSignatureConfirmations(boolean needsConfirmations) {
        needsSignatureConfirmations = needsConfirmations;
    }

    @Override
    public boolean hasWss11Decorations() {
        for (DecorationRequirements decoration : getDecorationRequirements()) {
            if (decoration.isWss11()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MessageKnob getKnob(Class c) {
        if (c == SecurityKnob.class) {
            return this;
        } else {
            return super.getKnob(c);
        }
    }
}
