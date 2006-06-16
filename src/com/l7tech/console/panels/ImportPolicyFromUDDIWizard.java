package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A SSM Wizard allowing an administrator to pull a policy out of a Systinet UDDI registry
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizard extends Wizard {
    public static ImportPolicyFromUDDIWizard getInstance(Frame parent) {
        // todo, confirm panel showing policy imported
        ImportPolicyFromUDDIWizardStep istep = new ImportPolicyFromUDDIWizardStep(null);
        UDDITargetWizardStep tstep = new UDDITargetWizardStep(istep);
        return new ImportPolicyFromUDDIWizard(parent, tstep);
    }

    protected ImportPolicyFromUDDIWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Publish WS-Policy Document Reference to UDDI Registry");
        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ImportPolicyFromUDDIWizard.this);
            }
        });
        wizardInput = new Data();
    }

    public class Data implements UDDITargetWizardStep.Data {
        private String uddiurl;
        private String accountName;
        private String accountPasswd;

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
    }
}
