/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.l7tech.security.xml.decorator.DecorationRequirements.WsaHeaderSigningStrategy.*;

/**
 * Provides access to a {@link SecurityKnob} from a {@link Message}.
 */
public class SecurityFacet extends MessageFacet implements SecurityKnob {
    private ProcessorResult processorResult = null;
    private final List<SecurityToken> tokens = new ArrayList<SecurityToken>();
    private DecorationRequirements decorationRequirements = null;
    private Map<String,RecipientContext> recipientContextForAlternateRecipients = new HashMap<String,RecipientContext>();
    private ProcessorResultFactory lazyProcessor = null;
    private WsSecurityVersion wsSecurityVersion;
    private Map<String,List<WssDecorator.DecorationResult>> decorationResults = new HashMap<String, List<WssDecorator.DecorationResult>>();
    private boolean signatureConfirmationValidated = false;
    private boolean needsSignatureConfirmations = false;
    private DecorationRequirements.WsaHeaderSigningStrategy wsaHeaderSignStrategy = DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR;

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
        final Set<String> keys = recipientContextForAlternateRecipients.keySet();
        final List<DecorationRequirements> allReqs = new ArrayList<DecorationRequirements>(keys.size());//max size

        for (String key : keys) {
            final RecipientContext recipientCtx = recipientContextForAlternateRecipients.get(key);
            if(recipientCtx.decorationRequirements != null){
                allReqs.add(recipientCtx.decorationRequirements);
            }
        }
        if (decorationRequirements != null) {
            allReqs.add(decorationRequirements);
        }
        return allReqs.toArray(new DecorationRequirements[allReqs.size()]);
    }

    @Override
    public boolean hasAlternateDecorationRequirements(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            return hasDecorationRequirements();
        }

        boolean hasDecReqs = false;
        String actor = recipient.getActor();
        if(recipientContextForAlternateRecipients.containsKey(actor)){
            final RecipientContext recipientCtx = recipientContextForAlternateRecipients.get(actor);
            hasDecReqs = recipientCtx != null;
        }
        
        return hasDecReqs;
    }

    @Override
    public DecorationRequirements getAlternateDecorationRequirements(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            return getOrMakeDecorationRequirements();
        }

        String actor = recipient.getActor();
        RecipientContext recipientCtx = recipientContextForAlternateRecipients.get(actor);
        if(recipientCtx == null){
            recipientCtx = new RecipientContext();
            recipientContextForAlternateRecipients.put(actor, recipientCtx);
        }

        DecorationRequirements output;
        if(recipientCtx.decorationRequirements == null){
            output = new DecorationRequirements(recipientCtx.signWsaHeaderStrategy);
            X509Certificate clientCert = recipient.getX509Certificate();
            output.setRecipientCertificate(clientCert);
            output.setSecurityHeaderActor(actor);
            recipientCtx.decorationRequirements = output;
        } else {
            output = recipientCtx.decorationRequirements;
        }

        return output;
    }

    @Override
    public boolean hasDecorationRequirements() {
        return decorationRequirements != null;
    }

    @Override
    public DecorationRequirements getOrMakeDecorationRequirements() {
        if (decorationRequirements == null) {
            decorationRequirements = new DecorationRequirements(wsaHeaderSignStrategy);
        }
        return decorationRequirements;
    }

    @Override
    public void removeAllDecorationRequirements() {
        decorationRequirements = null;
        recipientContextForAlternateRecipients = new HashMap<String,RecipientContext>();
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
    public void addDecorationResult(WssDecorator.DecorationResult dr) {
        if (! decorationResults.containsKey(dr.getSecurityHeaderActor())) {
            decorationResults.put(dr.getSecurityHeaderActor(), new ArrayList<WssDecorator.DecorationResult>());
        }
        decorationResults.get(dr.getSecurityHeaderActor()).add(dr);
    }

    @Override
    public List<WssDecorator.DecorationResult> getDecorationResults(String actor) {
        return decorationResults.get(actor);
    }

    @Override
    public List<WssDecorator.DecorationResult> getAllDecorationResults() {
        List<WssDecorator.DecorationResult> results = new ArrayList<WssDecorator.DecorationResult>();
        for (String actor : decorationResults.keySet()) {
            results.addAll(decorationResults.get(actor));
        }
        return results;
    }

    @Override
    public void removeDecorationResults(String actor) {
        decorationResults.remove(actor);
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

    @Override
    public void flagDoNotSignWsaAddressing(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            wsaHeaderSignStrategy = NEVER_SIGN_WSA_HEADERS;
        } else {
            String actor = recipient.getActor();
            final RecipientContext recipientCtx = recipientContextForAlternateRecipients.get(actor);
            if(recipientCtx != null){
                recipientCtx.signWsaHeaderStrategy = NEVER_SIGN_WSA_HEADERS;
            } else {
                recipientContextForAlternateRecipients.put(actor,
                        new RecipientContext(NEVER_SIGN_WSA_HEADERS));
            }
        }
    }

    /**
     * Hold information relating to an alternate Recipient.
     * Stores DecorationRequirements, if they exist.
     * Stores a WsaHeaderSigningStrategy which specifies the behaviour required for WS-Addressing header signing, should a
     * DecorationRequirement be created for the recipient.
     */
    private static class RecipientContext{
        // - PRIVATE

        private RecipientContext() {
        }

        private RecipientContext(DecorationRequirements.WsaHeaderSigningStrategy signWsaHeaderStrategy) {
            this.signWsaHeaderStrategy = signWsaHeaderStrategy;
        }

        private DecorationRequirements decorationRequirements;
        /**
         * This is simply a flag. Once DecorationRequirements have been created this value should never be used again.
         * After decoration requirements have been created, any changes to the wsa header signing strategy should be
         * made directly within the decoration requirements.
         */
        private DecorationRequirements.WsaHeaderSigningStrategy signWsaHeaderStrategy = DEFAULT_WSA_HEADER_SIGNING_BEHAVIOUR;
    }

}
