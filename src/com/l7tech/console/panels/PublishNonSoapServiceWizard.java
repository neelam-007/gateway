package com.l7tech.console.panels;

import java.awt.*;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class PublishNonSoapServiceWizard extends Wizard {
    public static PublishNonSoapServiceWizard getInstance(Frame parent) {
        WizardStepPanel panel2 = new IdentityProviderWizardPanel();
        WizardStepPanel panel1 = new NonSoapServicePanel(panel2);
        return new PublishNonSoapServiceWizard(parent, panel1);
    }

    public PublishNonSoapServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
    }
}
