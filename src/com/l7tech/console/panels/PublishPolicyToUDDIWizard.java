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
    public static PublishPolicyToUDDIWizard getInstance(Frame parent, String policyURL, String serviceName) {
        AssociateUDDIServiceToPolicyWizardPanel astep = new AssociateUDDIServiceToPolicyWizardPanel(null);
        UDDIRegisterPolicyWizardPanel fstep = new UDDIRegisterPolicyWizardPanel(astep);
        UDDIPolicyDetailsWizardStep dstep = new UDDIPolicyDetailsWizardStep(fstep, policyURL, serviceName);
        UDDITargetWizardStep tstep = new UDDITargetWizardStep(dstep);
        // todo, other panel(s)
        return new PublishPolicyToUDDIWizard(parent, tstep);
    }

    protected PublishPolicyToUDDIWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Publish WS-Policy Document Reference to UDDI Registry");
        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishPolicyToUDDIWizard.this);
            }
        });
        wizardInput = new Data();
    }

    public class Data {
        private String policyName;
        private String policyDescription;
        private String capturedPolicyURL;
        private String uddiurl;
        private String accountName;
        private String accountPasswd;
        private String policytModelKey;
        private String authInfo;

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
    }
}
