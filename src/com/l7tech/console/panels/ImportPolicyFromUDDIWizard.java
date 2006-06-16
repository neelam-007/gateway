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
        // todo, other panels
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

        public void setUddiurl(String in) {
        }

        public void setAccountName(String in) {
        }

        public void setAccountPasswd(String in) {
        }
    }
}
