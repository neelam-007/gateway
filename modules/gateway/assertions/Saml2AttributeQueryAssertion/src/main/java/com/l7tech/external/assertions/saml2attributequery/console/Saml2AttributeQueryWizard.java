package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.Wizard;
import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;
import com.l7tech.external.assertions.saml2attributequery.console.Saml2AttributeQueryAttributeListPanel;
import com.l7tech.objectmodel.Goid;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 9:08:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryWizard extends Wizard {
    public Saml2AttributeQueryWizard(Frame owner, Saml2AttributeQueryAssertion assertion) {
        super(owner, new Saml2AttributeQueryIntroductionPanel(
                new Saml2AttributeQueryLdapServerPanel(
                    new Saml2AttributeQueryAttributeListPanel(
                            new Saml2AttributeQueryIssuerPanel(
                                    new Saml2AttributeQueryAudiencePanel(null, false),
                                    false),
                            false),
                    false),
                false)
        );

        setTitle("SAML 2 Attribute Query Processor Wizard");
        wizardInput = assertion;
        pack();
    }

    public static void main(String[] args) {
        Frame f = new Frame();
        Saml2AttributeQueryAssertion assertion = new Saml2AttributeQueryAssertion();
        assertion.setLdapProviderOid(new Goid(0,2));
        Saml2AttributeQueryWizard wizard = new Saml2AttributeQueryWizard(f, assertion);

        wizard.setVisible(true);
    }
}
