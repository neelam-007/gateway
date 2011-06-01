package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
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
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminUserAccountPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AdminUserAccountPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.AdminUserAccountPropertiesDialog");

    private static final String DIALOG_TITLE = resources.getString("title");

    private static final String PARAM_LOGIN_ATTEMPTS = "logon.maxAllowableAttempts";
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

    private boolean confirmed = false;
    private boolean isReadOnly = false;
    private IdentityAdmin.AccountMinimums accountMinimums;

    public AdminUserAccountPropertiesDialog(Window owner, boolean isReadOnly) {
        super(owner, DIALOG_TITLE, AdminUserAccountPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.isReadOnly = isReadOnly;
        initialize();
    }

    public AdminUserAccountPropertiesDialog(Frame owner, boolean isReadOnly) {
        super(owner, DIALOG_TITLE, AdminUserAccountPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.isReadOnly = isReadOnly;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        accountMinimums = Registry.getDefault().getIdentityAdmin().getAccountMinimums();
        final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);

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


        ((SpinnerNumberModel) invalidAttemptsSpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel) invalidAttemptsSpinner.getModel()).setMaximum(20);
        ((SpinnerNumberModel) minLockoutSpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel) minLockoutSpinner.getModel()).setMaximum(1440); // 1 day
        ((SpinnerNumberModel) expirySpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel) expirySpinner.getModel()).setMaximum(1440); // 1 day
        ((SpinnerNumberModel) inactivitySpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel) inactivitySpinner.getModel()).setMaximum(365);

        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(invalidAttemptsSpinner, getResourceString("maxattempts.label")));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(minLockoutSpinner, getResourceString("lockout.label")));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(expirySpinner, getResourceString("expiry.label")));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(inactivitySpinner, getResourceString("inactivity.label")));

        RunOnChangeListener requirementsListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
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

    private String getResourceString(String key) {
        final String value = resources.getString(key);
        if (value.endsWith(":")) {
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

    private void updateRequirementWarning() {
        boolean noWarning = true;

        if (accountMinimums != null) {
            noWarning = ((Integer) invalidAttemptsSpinner.getValue() <= accountMinimums.getAttempts());
            noWarning = noWarning && ((Integer) minLockoutSpinner.getValue() >= accountMinimums.getLockout());
            noWarning = noWarning && (accountMinimums.getExpiry() < 0 || ((Integer) expirySpinner.getValue() <= accountMinimums.getExpiry()));
            noWarning = noWarning && ((Integer) inactivitySpinner.getValue() <= accountMinimums.getInactivity());
        }

        warningLabel.setText(noWarning ? null : MessageFormat.format(getResourceString("below.warning"), accountMinimums.getName()));
    }

    /**
     * Configure the GUI control states with information gathered from the userAccountPolicy instance.
     */
    private void modelToView() {
        Collection<ClusterPropertyDescriptor> descriptors = getClusterAdmin().getAllPropertyDescriptors();

        invalidAttemptProperty = getClusterProp(PARAM_LOGIN_ATTEMPTS, descriptors);
        setSpinnerValue(invalidAttemptsSpinner,invalidAttemptProperty,1,descriptors);

        minLockoutProperty = getClusterProp(PARAM_LOCKOUT, descriptors);
        setSpinnerValue(minLockoutSpinner,minLockoutProperty,60,descriptors);

        expiryProperty = getClusterProp(PARAM_EXPIRY, descriptors);
        setSpinnerValue(expirySpinner,expiryProperty,1,descriptors);

        inactivityProperty = getClusterProp(PARAM_INACTIVITY, descriptors);
        setSpinnerValue(inactivitySpinner,inactivityProperty,1,descriptors);
    }

    /**
     * Configure the cluster properties to the GUI control values
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {
        setClusterProp(invalidAttemptProperty, (Integer) invalidAttemptsSpinner.getValue());
        setClusterProp(minLockoutProperty, (Integer) minLockoutSpinner.getValue() * 60);
        setClusterProp(expiryProperty, (Integer) expirySpinner.getValue());
        setClusterProp(inactivityProperty, (Integer) inactivitySpinner.getValue());
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

    /**
     * @return true if the dialog has been dismissed with the ok button
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    private void setClusterProp(ClusterProperty prop, Integer value) {
        if (prop.getValue().equals(value.toString()))
            return;
        try {
            prop.setValue(value.toString());
            getClusterAdmin().saveProperty(prop);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Exception setting property", e);
        } catch (UpdateException e) {
            logger.log(Level.SEVERE, "Exception setting property", e);
        } catch (DeleteException e) {
            logger.log(Level.SEVERE, "Exception setting property", e);
        }
    }

    private ClusterProperty getClusterProp(String name, Collection<ClusterPropertyDescriptor> descriptors) {
        try {
            ClusterProperty prop = getClusterAdmin().findPropertyByName(name);
            if (prop == null) {
                String value = findDefaultValue(descriptors, name);
                prop = new ClusterProperty(name, value);
            }
            return prop;
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting properties", e);
            return null;
        }
    }

    private void setSpinnerValue(JSpinner boundedNumberSpinner, ClusterProperty prop, int divisor, Collection<ClusterPropertyDescriptor> descriptors){
        try{
            int value = Integer.parseInt(prop.getValue())/ divisor;
            SpinnerNumberModel model = (SpinnerNumberModel)boundedNumberSpinner.getModel();

            if ((Integer)model.getMinimum() <= value&&  value <= (Integer)model.getMaximum()) {
                boundedNumberSpinner.setValue(value);
                return ;
            }
        }catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Invalid property value: "+prop.getValue(), ExceptionUtils.getDebugException(e));
        }
        String value = findDefaultValue(descriptors, prop.getName());
        boundedNumberSpinner.setValue(Integer.parseInt(value)/ divisor);
    }

    private String findDefaultValue(Collection<ClusterPropertyDescriptor> descriptors, String name) {
        for (ClusterPropertyDescriptor desc : descriptors) {
            if (desc.getName().equals(name))
                return desc.getDefaultValue();
        }
        logger.log(Level.SEVERE, "Exception getting default value for property :" + name);
        return null;
    }

    private ClusterStatusAdmin getClusterAdmin() {
        return Registry.getDefault().getClusterStatusAdmin();
    }

}
