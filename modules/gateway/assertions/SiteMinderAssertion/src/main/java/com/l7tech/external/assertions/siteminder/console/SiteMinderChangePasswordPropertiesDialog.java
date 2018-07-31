package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.siteminder.SiteMinderChangePasswordAssertion;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: pakhy01
 * Date: 2017-02-06
 * Time: 10:36 AM
 */
public class SiteMinderChangePasswordPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderChangePasswordAssertion> {
    private static final Logger logger = Logger.getLogger(SiteMinderChangePasswordPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JComboBox<SiteMinderConfigurationKey> agentComboBox;
    private JTextField domOidTextField;
    private JTextField usernameTextField;
    private JPasswordField oldPasswordField;
    private JCheckBox showOldPasswordCheckBox;
    private JLabel oldPasswordWarningLabel;
    private JPasswordField newPasswordField;
    private JCheckBox showNewPasswordCheckBox;
    private JLabel newPasswordWarningLabel;

    private InputValidator inputValidator;

    public SiteMinderChangePasswordPropertiesDialog(final Frame owner, final SiteMinderChangePasswordAssertion assertion) {
        super(SiteMinderChangePasswordAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(SiteMinderChangePasswordAssertion assertion) {
        SiteMinderConfiguration config;
        try {
            if (assertion.getAgentGoid() != null) {
                config = getSiteMinderAdmin().getSiteMinderConfiguration(assertion.getAgentGoid());

                if (config != null) {
                    agentComboBox.setSelectedItem(new SiteMinderConfigurationKey(config));
                } else {
                    agentComboBox.setSelectedItem(null);
                }
            }
        } catch (FindException e) {
            logger.log(Level.INFO,
                    "Unable to find SiteMinderConfiguration for GOID " + assertion.getAgentGoid().toString(),
                    ExceptionUtils.getDebugException(e));
            agentComboBox.setSelectedItem(null);
        }

        domOidTextField.setText(assertion.getDomOid());
        usernameTextField.setText(assertion.getUsername());
        oldPasswordField.setText(assertion.getOldPassword());
        newPasswordField.setText(assertion.getNewPassword());
    }

    @Override
    public SiteMinderChangePasswordAssertion getData(SiteMinderChangePasswordAssertion assertion) throws ValidationException {
        String validationErrorMessage = inputValidator.validate();

        if (null != validationErrorMessage) {
            throw new ValidationException(validationErrorMessage);
        }

        assertion.setAgentGoid(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getGoid());
        assertion.setAgentId(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getAgentId());
        assertion.setDomOid(domOidTextField.getText().trim());
        assertion.setUsername(usernameTextField.getText().trim());
        assertion.setOldPassword(String.valueOf(oldPasswordField.getPassword()));
        assertion.setNewPassword(String.valueOf(newPasswordField.getPassword()));

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }


    @Override
    protected void initComponents() {
        super.initComponents();

        DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        agentComboBox.setModel(agentComboBoxModel);

        PasswordGuiUtils.configureOptionalSecurePasswordField(oldPasswordField, showOldPasswordCheckBox, oldPasswordWarningLabel);
        PasswordGuiUtils.configureOptionalSecurePasswordField(newPasswordField, showNewPasswordCheckBox, newPasswordWarningLabel);

        inputValidator = new InputValidator(this, getTitle());
        inputValidator.ensureComboBoxSelection("Configuration Name", agentComboBox);
        inputValidator.constrainTextFieldToBeNonEmpty("Domain Object ID", domOidTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Username", usernameTextField, null);
        inputValidator.constrainPasswordFieldToBeNonEmpty("Old Password", oldPasswordField);
        inputValidator.constrainPasswordFieldToBeNonEmpty("New Password", newPasswordField);
    }

    /**
     * Populates agent combo box model with agent IDs
     *
     * @param agentComboBoxModel the model to populate
     */
    private void populateAgentComboBoxModel(DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel) {
        try {
            java.util.List<SiteMinderConfiguration> agents = getSiteMinderAdmin().getAllSiteMinderConfigurations();

            for (SiteMinderConfiguration agent : agents) {
                agentComboBoxModel.addElement(new SiteMinderConfigurationKey(agent));
            }
        } catch (FindException e) {
            //do not throw any exceptions at this point. leave agent combo box empty
        }
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        return Registry.getDefault().getSiteMinderConfigurationAdmin();
    }
}
