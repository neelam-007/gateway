package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.common.uddi.UDDIClient;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Wizard interface for the publication of a WSP document reference to a UDDI registry.
 * Panels are: UDDI target, Policy description, Service binding.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 12, 2006<br/>
 */
public class PublishPolicyToUDDIWizard extends Wizard {
    public static PublishPolicyToUDDIWizard getInstance(Frame parent, String policyURL, String policyConsumptionURL, String serviceName) {
        AssociateUDDIServiceToPolicyWizardStep astep = new AssociateUDDIServiceToPolicyWizardStep(null);
        UDDIPolicyDetailsWizardStep dstep = new UDDIPolicyDetailsWizardStep(astep);
        UDDITargetWizardStep tstep = new UDDITargetWizardStep(dstep);
        return new PublishPolicyToUDDIWizard(parent, tstep, policyURL, policyConsumptionURL, serviceName);
    }

    protected PublishPolicyToUDDIWizard(Frame parent, WizardStepPanel panel, String policyURL, String policyConsumptionURL, String serviceName) {
        super(parent, panel);
        setTitle("Publish Policy tModel to UDDI Registry Wizard");
        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishPolicyToUDDIWizard.this);
            }
        });
        Data data = new Data();
        data.setCapturedPolicyURL(policyURL);
        data.setPolicyConsumptionURL(policyConsumptionURL);
        data.setPolicyName("Policy for " + serviceName);
        data.setPolicyDescription("Associated Policy"); // Systinet uses this name to identify policy references
        
        wizardInput = data;
    }

    public class Data implements UDDITargetWizardStep.Data {
        private String policyName;
        private String policyDescription;
        private String capturedPolicyURL;
        private String policyConsumptionURL;
        private UDDIClient uddi;
        private String policytModelKey;
        private String policytModelName;

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public String getPolicyDescription() {
            return policyDescription;
        }

        public void setPolicyDescription(String policyDescription) {
            this.policyDescription = policyDescription;
        }

        public String getCapturedPolicyURL() {
            return capturedPolicyURL;
        }

        public void setCapturedPolicyURL(String capturedPolicyURL) {
            this.capturedPolicyURL = capturedPolicyURL;
        }

        public UDDIClient getUddi() {
            return uddi;
        }

        public void setUddi(UDDIClient uddi) {
            this.uddi = uddi;
        }

        public String getPolicytModelKey() {
            return policytModelKey;
        }

        public void setPolicytModelKey(String policytModelKey) {
            this.policytModelKey = policytModelKey;
        }

        public String getPolicytModelName() {
            return policytModelName;
        }

        public void setPolicytModelName(String policytModelName) {
            this.policytModelName = policytModelName;
        }

        public String getPolicyConsumptionURL() {
            return policyConsumptionURL;
        }

        public void setPolicyConsumptionURL(String policyConsumptionURL) {
            this.policyConsumptionURL = policyConsumptionURL;
        }
    }
}
