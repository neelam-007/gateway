package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SignatureConfirmation;
import org.w3c.dom.Element;

/**
 * Implementation of SignatureConfirmation used by WssProcessor.
 */
class SignatureConfirmationImpl extends ParsedElementImpl implements SignatureConfirmation {
    private final String confirmationValue;

    SignatureConfirmationImpl(Element element, String confirmationValue) {
        super(element);
        this.confirmationValue = confirmationValue;
    }

    public String getConfirmationValue() {
        return confirmationValue;
    }
}
