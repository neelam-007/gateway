package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Final step in the ImportPolicyFromUDDIWizard where the
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 19, 2006<br/>
 */
public class ImportPolicyFromUDDIFinalStep extends WizardStepPanel {
    private JTextPane policyText;
    private JPanel mainPanel;
    private ImportPolicyFromUDDIWizard.Data data;

    public ImportPolicyFromUDDIFinalStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Review policy pulled from UDDI registry and confirm import process.";
    }

    public String getStepLabel() {
        return "Complete Import Process";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (ImportPolicyFromUDDIWizard.Data)settings;
        policyText.setText(data.getPolicyXML());   
        policyText.setCaretPosition(0);
    }

    public boolean onNextButton() {
        data.setConfirmed(true);
        return true;
    }
}
