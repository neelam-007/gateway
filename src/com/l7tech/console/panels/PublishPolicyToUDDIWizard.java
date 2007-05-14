package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

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
        UDDIPolicyDetailsWizardStep dstep = new UDDIPolicyDetailsWizardStep(astep, policyURL, serviceName);
        UDDITargetWizardStep tstep = new UDDITargetWizardStep(dstep);
        return new PublishPolicyToUDDIWizard(parent, tstep, policyConsumptionURL);
    }

    protected PublishPolicyToUDDIWizard(Frame parent, WizardStepPanel panel, String policyConsumptionURL) {
        super(parent, panel);
        setTitle("Publish Policy tModel to UDDI Registry Wizard");
        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishPolicyToUDDIWizard.this);
            }
        });
        wizardInput = new Data();
        ((Data)wizardInput).setPolicyConsumptionURL(policyConsumptionURL);
    }

    public class Data implements UDDITargetWizardStep.Data {
        private String policyName;
        private String policyDescription;
        private String capturedPolicyURL;
        private String uddiurl;
        private String accountName;
        private String accountPasswd;
        private String policytModelKey;
        private String authInfo;
        private String policyConsumptionURL;

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

        public String getUddiurl() {
            return uddiurl;
        }

        public void setUddiurl(String uddiurl) {
            if (uddiurl.indexOf("/uddi") < 1) {
                if (uddiurl.endsWith("/")) {
                    uddiurl = uddiurl + "uddi/";
                } else {
                    uddiurl = uddiurl + "/uddi/";
                }
            }
            if (!uddiurl.endsWith("/")) {
                uddiurl = uddiurl + "/";
            }
            this.uddiurl = uddiurl;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getAccountPasswd() {
            return accountPasswd;
        }

        public void setAccountPasswd(String accountPasswd) {
            this.accountPasswd = accountPasswd;
        }

        public String getPolicytModelKey() {
            return policytModelKey;
        }

        public void setPolicytModelKey(String policytModelKey) {
            this.policytModelKey = policytModelKey;
        }

        public String getAuthInfo() {
            return authInfo;
        }

        public void setAuthInfo(String authInfo) {
            this.authInfo = authInfo;
        }

        public String getPolicyConsumptionURL() {
            return policyConsumptionURL;
        }

        public void setPolicyConsumptionURL(String policyConsumptionURL) {
            this.policyConsumptionURL = policyConsumptionURL;
        }
    }
}
