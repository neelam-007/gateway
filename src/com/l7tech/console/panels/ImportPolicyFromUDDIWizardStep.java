package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Second step in the ImportPolicyFromUDDIWizard.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField nameField;
    private JButton searchButton;
    private JList policyList;
    private JButton importButton;

    public ImportPolicyFromUDDIWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Search UDDI Directory for Policy and Import it";
    }

    public String getStepLabel() {
        return "Select Policy From UDDI";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }
}
