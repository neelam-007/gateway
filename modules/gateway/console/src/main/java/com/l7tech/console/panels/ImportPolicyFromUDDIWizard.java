package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.uddi.UDDIClient;

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
        UDDITargetWizardStep tstep = new UDDITargetWizardStep(istep, false);
        tstep.setPanelDescription("Provide the UDDI registry URL and account information to retrieve the policy");
        return new ImportPolicyFromUDDIWizard(parent, tstep);
    }

    protected ImportPolicyFromUDDIWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Import WS-Policy from URL in UDDI Registry Wizard");
        getButtonHelp().addActionListener(new ActionListener() {
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

    public class Data implements UDDITargetWizardStep.Data {
        private UDDIClient uddi;
        private String policyXML;
        private boolean confirmed = false;
        private String capturedPolicyURL;
        private String policytModelKey;
        private String policyName;

        public UDDIClient getUddi() {
            return uddi;
        }

        public void setUddi(UDDIClient uddi) {
            this.uddi = uddi;
        }

        public String getPolicyXML() {
            return policyXML;
        }

        public void setPolicyXML(String policyXML) {
            this.policyXML = policyXML;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public String getCapturedPolicyURL() {
            return capturedPolicyURL;
        }

        public void setCapturedPolicyURL(String capturedPolicyURL) {
            this.capturedPolicyURL = capturedPolicyURL;
        }

        public void setConfirmed(boolean confirmed) {
            this.confirmed = confirmed;
        }

        public String getPolicytModelKey() {
            return policytModelKey;
        }

        public void setPolicytModelKey(String policytModelKey) {
            this.policytModelKey = policytModelKey;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policytModelName) {
            this.policyName = policytModelName;
        }
    }
}
