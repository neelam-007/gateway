/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 19, 2005<br/>
 */
package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * A wizard step panel to use with the {@link AddCertificateWizard}. It lets the administrator associate an
 * security header actor attribute value to a recipient cert. This is used be
 * the {@link XmlSecurityRecipientContextEditor}.
 *
 * @author flascelles@layer7-tech.com
 */
public class RecipientSecurityHeaderWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField actorAttributeValueField;

    public RecipientSecurityHeaderWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    public boolean canFinish() {
       return true;
    }

    public String getDescription() {
        return "The actor attribute is located in the 'Security' soap header and is used to identify the " +
               "intended recipient of the xml security decorations.";
    }

    public String getStepLabel() {
        return "Associate Actor Attribute";
    }

    public String getCapturedValue() {
        return actorAttributeValueField.getText();
    }

    public boolean onNextButton() {
        String currentval = getCapturedValue();
        if (currentval != null && currentval.length() > 0) {
            return true;
        }
        return false;
    }
}
