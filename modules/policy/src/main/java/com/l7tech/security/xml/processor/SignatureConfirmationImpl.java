package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SignatureConfirmation;
import com.l7tech.security.token.SigningSecurityToken;
import org.w3c.dom.Element;

import java.util.*;

/**
 * WSS Processor's result implementation for SignatureConfirmation.
 */
class SignatureConfirmationImpl implements SignatureConfirmation {

    private static final String VALUE_ATTRIBUTE_NAME = "Value";

    // signature confirmation values -> ConfirmedSignature element
    private final Map<String,Element> confirmationElements = new HashMap<String, Element>();

    // validated signature confirmations -> signing tokens
    private final Map<String,List<SigningSecurityToken>> signingTokens = new HashMap<String, List<SigningSecurityToken>>();

    // "strict" SignatureConfirmation processing errors; will fail signature confirmation verification when done in strict mode
    private final List<String> errors = new ArrayList<String>();

    private boolean hasNullValue = false;

    
    @Override
    public void addConfirmationElement(Element e, boolean strict) {
        if (! e.hasAttribute(VALUE_ATTRIBUTE_NAME)) {
            if (strict &&  ! confirmationElements.isEmpty() ) {
                errors.add("Value-less SignatureConfirmation must be the only SignatureConfirmation, but there are already others found.");
            }
            hasNullValue = true;
        }
        confirmationElements.put(e.getAttribute(VALUE_ATTRIBUTE_NAME), e);
    }

    @Override
    public Map<String, Element> getConfirmationElements() {
        return confirmationElements;
    }

    @Override
    public Element getElement(String signatureConfirmation) {
        return confirmationElements.get(signatureConfirmation);
    }

    @Override
    public void addSigningToken(String signatureConfirmation, Set<SigningSecurityToken> newTokens) {
        if (! confirmationElements.containsKey(signatureConfirmation))
            throw new IllegalArgumentException("Unknown signature confirmation: " + signatureConfirmation);

        List<SigningSecurityToken> tokens = signingTokens.get(signatureConfirmation);
        if (tokens == null) {
            tokens = new ArrayList<SigningSecurityToken>();
            signingTokens.put(signatureConfirmation, tokens);
        }
        tokens.addAll(newTokens);
    }

    @Override
    public boolean hasNullValue() {
        return hasNullValue;
    }

    @Override
    public Map<String, List<SigningSecurityToken>> getConfirmedValues() {
        if (getStatus() != Status.CONFIRMED)
            throw new IllegalStateException("No signature confirmation values can be retrieved if their validation failed.");
        else
            return signingTokens;
    }

    @Override
    public void addError(String error) {
        errors.add(error);
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Status getStatus() {
        return ! errors.isEmpty() ? Status.INVALID :
               confirmationElements.isEmpty() ? Status.NO_CONFIRMATION :
               Status.CONFIRMED;
    }
}
