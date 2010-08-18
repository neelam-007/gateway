package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 8:21:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryAssertionPropertiesEditor implements AssertionPropertiesEditor<Saml2AttributeQueryAssertion> {
    private boolean confirmed = false;
    protected boolean readOnly;
    private Saml2AttributeQueryAssertion assertion;
    private Saml2AttributeQueryWizard wizard;

    public Saml2AttributeQueryAssertionPropertiesEditor(Saml2AttributeQueryAssertion assertion) {
        this.assertion = assertion;
        createWizard();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Saml2AttributeQueryAssertion getData(Saml2AttributeQueryAssertion ass) {
        ass.setLdapProviderOid(assertion.getLdapProviderOid());
        ass.setWhiteList(assertion.isWhiteList());
        ass.setRestrictedAttributeList(assertion.getRestrictedAttributeList());

        return ass;
    }

    public void setData(Saml2AttributeQueryAssertion ass) {
        assertion = ass;

        createWizard();
    }

    public JDialog getDialog() {
        return wizard;
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

    private void createWizard() {
        Frame mw = TopComponents.getInstance().getTopParent();
        wizard = new Saml2AttributeQueryWizard(mw, assertion);

        wizard.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) { confirmed = true; }
        });
    }
}
