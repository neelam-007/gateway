package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard that
 * allows a saved tModel policy to be associated to a UDDI
 * business service in the registry.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 14, 2006<br/>
 */
public class AssociateUDDIServiceToPolicyWizardPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField serviceNameField;
    private JButton addRefButton;
    private JList serviceList;
    private JButton button2;

    /**
     * Creates new form WizardPanel
     */
    public AssociateUDDIServiceToPolicyWizardPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Associate Policy tModel to Business Service";
    }

    public String getStepLabel() {
        return "Associate Policy tModel to Business Service";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }
}
