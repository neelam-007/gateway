package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SignatureConfirmation;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.token.SignedElement;
import org.w3c.dom.Element;

import java.util.*;

/**
 * WSS Processor's result implementation for SignatureConfirmation.
 */
class SignatureConfirmationImpl implements SignatureConfirmation {

    private static final String VALUE_ATTRIBUTE_NAME = "Value";

    // signature confirmation values -> ConfirmedSignature element
    private final Map<String, ParsedElement> confirmationElements = new HashMap<String, ParsedElement>();

    // validated signature confirmations -> signing tokens
    private final Map<String,List<SignedElement>> signedElements = new HashMap<String, List<SignedElement>>();

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
        confirmationElements.put(e.hasAttribute(VALUE_ATTRIBUTE_NAME) ? e.getAttribute(VALUE_ATTRIBUTE_NAME) : null, new ParsedElementImpl(e));
    }

    @Override
    public Map<String, ParsedElement> getConfirmationElements() {
        return confirmationElements;
    }

    @Override
    public Element getElement(String signatureConfirmation) {
        ParsedElement parsedElement = confirmationElements.get(signatureConfirmation);
        return parsedElement == null ? null : parsedElement.asElement();
    }

    @Override
    public void addSignedElement(String signatureConfirmation, Set<SignedElement> newSigned) {
        if (! confirmationElements.containsKey(signatureConfirmation))
            throw new IllegalArgumentException("Unknown signature confirmation: " + signatureConfirmation);

        List<SignedElement> signed = signedElements.get(signatureConfirmation);
        if (signed == null) {
            signed = new ArrayList<SignedElement>();
            signedElements.put(signatureConfirmation, signed);
        }
        signed.addAll(newSigned);
    }

    @Override
    public boolean hasNullValue() {
        return hasNullValue;
    }

    @Override
    public Map<String, List<SignedElement>> getConfirmedValues() {
        if (getStatus() != Status.CONFIRMED)
            throw new IllegalStateException("No signature confirmation values can be retrieved if their validation failed.");
        else
            return signedElements;
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
