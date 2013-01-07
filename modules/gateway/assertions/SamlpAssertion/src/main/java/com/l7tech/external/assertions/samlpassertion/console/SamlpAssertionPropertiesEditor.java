/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.policy.PolicyPositionAware;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;

/**
 * This is the base properties editor class for the SAMLP assertion wizards.
 *
 * @author vchan
 */
public abstract class SamlpAssertionPropertiesEditor<ASN extends Assertion> implements AssertionPropertiesEditor<ASN>, PolicyPositionAware {
    private SamlpAssertionWizard wizard;
    protected boolean readOnly;
    protected boolean confirmed;
    protected ASN assertion;
    private PolicyPositionAware.PolicyPosition policyPosition;

    public SamlpAssertionPropertiesEditor() {
    }

    @Override
    public JDialog getDialog() {
        return wizard;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(ASN assertion) {
        this.assertion = assertion;

        wizard = createAssertionWizard(assertion);
    }

    @Override
    public ASN getData(ASN assertion) {
        return this.assertion;
    }

    @Override
    public Object getParameter( final String name ) {
        Object value = null;

        if ( PARAM_READONLY.equals( name )) {
            value = readOnly;
        }

        return value;
    }

    @Override
    public void setParameter( final String name, Object value ) {
        if ( PARAM_READONLY.equals( name ) && value instanceof Boolean ) {
            readOnly = (Boolean) value;
        }
    }

    @Override
    public PolicyPosition getPolicyPosition() {
        return this.policyPosition;
    }

    public Assertion getPreviousAssertion() {
        return policyPosition==null ? null : policyPosition.getPreviousAssertion();
    }



    /**
     * The policy position is set when the assertion about to be added to a policy.
     *
     * <p>The policy position can be used to extract contextual information from a
     * policy (such as variables that are set before this assertion)</p>
     *
     * @param policyPosition The position to be used for this assertion.
     */
    @Override
    public void setPolicyPosition( final PolicyPositionAware.PolicyPosition policyPosition ) {
        this.policyPosition = policyPosition;
    }

    protected abstract AssertionMode getMode();

    protected abstract SamlpAssertionWizard createAssertionWizard(ASN assertion);

}