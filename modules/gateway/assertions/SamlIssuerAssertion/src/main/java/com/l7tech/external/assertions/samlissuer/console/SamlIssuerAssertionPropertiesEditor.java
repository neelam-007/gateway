package com.l7tech.external.assertions.samlissuer.console;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.panels.saml.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.samlissuer.SamlIssuerAssertion;

import javax.swing.*;

/**
 * @author alex
 */
public class SamlIssuerAssertionPropertiesEditor implements AssertionPropertiesEditor<SamlIssuerAssertion> {
    private SamlPolicyAssertionWizard wizard;
    private boolean readOnly;
    private boolean confirmed;
    private SamlIssuerAssertion assertion;

    public SamlIssuerAssertionPropertiesEditor() {
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
    public void setData(SamlIssuerAssertion assertion) {
        this.assertion = assertion;

        IntroductionWizardStepPanel p =
          new IntroductionWizardStepPanel(
            new VersionWizardStepPanel(
              new IssuerWizardStepPanel(
                new SelectStatementWizardStepPanel(
                  new AuthorizationStatementWizardStepPanel(
                    new AttributeStatementWizardStepPanel(
                        new SubjectConfirmationNameIdentifierWizardStepPanel(
                            new SubjectConfirmationWizardStepPanel(
                                new ConditionsWizardStepPanel(
                                    new SamlSignatureStepPanel(null, true), true, true), true, true), true), true), true), true), false), true), true);

        SamlPolicyAssertionWizard wiz = new SamlPolicyAssertionWizard(assertion, TopComponents.getInstance().getTopParent(), p, readOnly);
        wiz.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) { confirmed = true; }
        });
        wiz.addValidationRulesDefinedInWizardStepPanel(ConditionsWizardStepPanel.class, p);
        wiz.addValidationRulesDefinedInWizardStepPanel(SubjectConfirmationWizardStepPanel.class, p);
        wizard = wiz;
    }

    @Override
    public SamlIssuerAssertion getData(SamlIssuerAssertion assertion) {
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

}
