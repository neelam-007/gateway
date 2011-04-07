package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminUserAccountPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AdminUserAccountPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.AdminUserAccountPropertiesDialog");

    private static final String DIALOG_TITLE = resources.getString("title");

    private static final String PARAM_LOGIN_ATTEMPTS= "logon.maxAllowableAttempts";
    private static final String PARAM_LOCKOUT = "logon.lockoutTime";
    private static final String PARAM_EXPIRY = "logon.sessionExpiry";  
    private static final String PARAM_INACTIVITY = "logon.inactivityPeriod";

    private ClusterProperty invalidAttemptProperty;
    private ClusterProperty minLockoutProperty;
    private ClusterProperty expiryProperty;
    private ClusterProperty inactivityProperty;


    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JSpinner invalidAttemptsSpinner;
    private JSpinner minLockoutSpinner;
    private JSpinner expirySpinner;
    private JSpinner inactivitySpinner;
    private JLabel warningLabel;

    private InputValidator inputValidator;
    private boolean confirmed = false;
    private boolean isReadOnly = false;
    private boolean pciEnabled;

    // STIG minimum values
    private static int STIG_ATTEMPTS = 5;
    private static int STIG_LOCKOUT = 20;
    private static int STIG_EXPIRY = 15;
    private static int STIG_INACTIVITY = 35;

    // PCI-DSS minimum values
    private static int PCI_ATTEMPTS = 6;
    private static int PCI_LOCKOUT = 30;
    private static int PCI_EXPIRY = 15;
    private static int PCI_INACTIVITY = 90;

    public AdminUserAccountPropertiesDialog(Window owner,  boolean isReadOnly) {
        super(owner, DIALOG_TITLE, AdminUserAccountPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.isReadOnly = isReadOnly;
        initialize();
    }

    public AdminUserAccountPropertiesDialog(Frame owner,  boolean isReadOnly) {
        super(owner, DIALOG_TITLE, AdminUserAccountPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.isReadOnly = isReadOnly;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        pciEnabled = Registry.getDefault().getAdminLogin().getPropertyPCIDSSEnabled();
        inputValidator = new InputValidator(this, DIALOG_TITLE);

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });


        ((SpinnerNumberModel)invalidAttemptsSpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel)invalidAttemptsSpinner.getModel()).setMaximum(20);
        ((SpinnerNumberModel)minLockoutSpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel)minLockoutSpinner.getModel()).setMaximum(1440); // 1 day
        ((SpinnerNumberModel)expirySpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)expirySpinner.getModel()).setMaximum(60);
        ((SpinnerNumberModel)inactivitySpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)inactivitySpinner.getModel()).setMaximum(365);

         RunOnChangeListener requirementsListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run(){
                updateRequirementWarning();
            }
        });

        invalidAttemptsSpinner.addChangeListener(requirementsListener);
        minLockoutSpinner.addChangeListener(requirementsListener);
        expirySpinner.addChangeListener(requirementsListener);
        inactivitySpinner.addChangeListener(requirementsListener);


        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
        okButton.setEnabled(!isReadOnly);           
    }

    private String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

    private void updateRequirementWarning() {
        boolean STIG = !pciEnabled;
        boolean noWarning;
        noWarning = ((Integer)invalidAttemptsSpinner.getValue() <= (STIG?STIG_ATTEMPTS : PCI_ATTEMPTS));
        noWarning = noWarning && ((Integer)minLockoutSpinner.getValue() <= (STIG?STIG_LOCKOUT : PCI_LOCKOUT));
        noWarning = noWarning && ((Integer)expirySpinner.getValue() <= (STIG?STIG_EXPIRY: PCI_EXPIRY));
        noWarning = noWarning && ((Integer)invalidAttemptsSpinner.getValue() <= (STIG?STIG_INACTIVITY : PCI_INACTIVITY));


        warningLabel.setText(noWarning? null: (STIG? getResourceString("below.stig.warning"):getResourceString("below.pcidss.warning")));
    }

    /**
     * Configure the GUI control states with information gathered from the userAccountPolicy instance.
     */
    private void modelToView() {
        invalidAttemptProperty = getClusterProp(PARAM_LOGIN_ATTEMPTS);
        invalidAttemptsSpinner.setValue(Integer.parseInt(invalidAttemptProperty.getValue()));

        minLockoutProperty = getClusterProp(PARAM_LOCKOUT);
        minLockoutSpinner.setValue(Integer.parseInt(minLockoutProperty.getValue())/60);

        expiryProperty = getClusterProp(PARAM_EXPIRY);
        expirySpinner.setValue(Integer.parseInt(expiryProperty.getValue()));

        inactivityProperty = getClusterProp(PARAM_INACTIVITY);
        inactivitySpinner.setValue(Integer.parseInt(inactivityProperty.getValue()));
    }

    /**
     * Configure the cluster properties to the GUI control values
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {         
        setClusterProp(invalidAttemptProperty,(Integer)invalidAttemptsSpinner.getValue());
        setClusterProp(minLockoutProperty,(Integer)minLockoutSpinner.getValue()*60);
        setClusterProp(expiryProperty,(Integer)expirySpinner.getValue());
        setClusterProp(inactivityProperty,(Integer)inactivitySpinner.getValue());
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    private void setClusterProp(ClusterProperty prop, Integer value) {
        if(prop.getValue().equals(value.toString()))
            return;
        try {
            prop.setValue(value.toString());
            getClusterAdmin().saveProperty(prop);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "exception setting property", e);
        } catch (UpdateException e) {
            logger.log(Level.SEVERE, "exception setting property", e);
        } catch (DeleteException e) {
            logger.log(Level.SEVERE, "exception setting property", e);
        }
    }

    private ClusterProperty getClusterProp(String name){
        try {
            ClusterProperty prop = getClusterAdmin().findPropertyByName(name);
            if(prop==null){
                String value = getClusterAdmin().getClusterProperty(name);
                prop = new ClusterProperty(name,value);
            }
            return prop;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "exception getting properties", e);
            return null;
        }
    }

    private ClusterStatusAdmin getClusterAdmin(){
        return Registry.getDefault().getClusterStatusAdmin();
    }

}
