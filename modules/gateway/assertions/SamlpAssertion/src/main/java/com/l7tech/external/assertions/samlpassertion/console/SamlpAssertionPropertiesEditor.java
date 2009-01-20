/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;

/**
 * This is the base properties editor class for the SAMLP assertion wizards.
 *
 * @author vchan
 */
public abstract class SamlpAssertionPropertiesEditor<ASN extends Assertion> implements AssertionPropertiesEditor<ASN> {
    private SamlpAssertionWizard wizard;
    protected boolean readOnly;
    protected boolean confirmed;
    private ASN assertion;

    public SamlpAssertionPropertiesEditor() {
    }

    public JDialog getDialog() {
        return wizard;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(ASN assertion) {
        this.assertion = assertion;

        wizard = createAssertionWizard(assertion);
    }

    public ASN getData(ASN assertion) {
        return this.assertion;
    }

    public Object getParameter( final String name ) {
        Object value = null;

        if ( PARAM_READONLY.equals( name )) {
            value = readOnly;
        }

        return value;
    }

    public void setParameter( final String name, Object value ) {
        if ( PARAM_READONLY.equals( name ) && value instanceof Boolean ) {
            readOnly = (Boolean) value;
        }
    }

    protected abstract AssertionMode getMode();

    protected abstract SamlpAssertionWizard createAssertionWizard(ASN assertion);

}