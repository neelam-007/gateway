package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

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
        boolean bad = false;
        String xml = data.getPolicyXML();
        if (xml == null || xml.length() == 0) {
            bad = true;
        } else {
            Assertion ass = null;
            try {
                ass = WspReader.getDefault().parsePermissively(xml);
                if (ass == null) {
                    bad = true;
                }
            } catch (IOException e) {
                bad = true;
            }
        }
        if (bad) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          "The policy being imported is not a valid policy, or is empty.",
                                          "Invalid/Empty Policy",
                                          JOptionPane.WARNING_MESSAGE, null);
        }
        data.setConfirmed(!bad);

        return !bad;
    }
}
