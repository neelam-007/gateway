package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A SSM Wizard allowing an administrator to pull a policy out of a UDDI registry
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizard extends Wizard {
    public static ImportPolicyFromUDDIWizard getInstance(Frame parent) {
        ImportPolicyFromUDDIFinalStep fstep = new ImportPolicyFromUDDIFinalStep(null);
        ImportPolicyFromUDDIWizardStep istep = new ImportPolicyFromUDDIWizardStep(fstep);
        return new ImportPolicyFromUDDIWizard(parent, istep);
    }

    protected ImportPolicyFromUDDIWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Import WS-Policy from URL in UDDI Registry Wizard");
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ImportPolicyFromUDDIWizard.this);
            }
        });
        wizardInput = new Data();
    }

    /**
     * @return policy xml imported or null if cancelled
     */
    public String importedPolicy() {
        if (((Data)wizardInput).isConfirmed()) {
            return ((Data)wizardInput).getPolicyXML();
        }
        return null;
    }

    public class Data {
        private String policyXML;
        private boolean confirmed;

        public String getPolicyXML() {
            return policyXML;
        }

        public void setPolicyXML(String policyXML) {
            this.policyXML = policyXML;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public void setConfirmed(boolean confirmed) {
            this.confirmed = confirmed;
        }
    }
}
