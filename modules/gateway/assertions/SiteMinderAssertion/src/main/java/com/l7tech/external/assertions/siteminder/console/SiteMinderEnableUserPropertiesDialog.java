package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.siteminder.SiteMinderEnableUserAssertion;
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
 * Date: 2017-02-09
 * Time: 3:40 PM
 */
public class SiteMinderEnableUserPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderEnableUserAssertion> {
    private static final Logger logger = Logger.getLogger(SiteMinderEnableUserPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JComboBox<SiteMinderConfigurationKey> agentComboBox;
    private JTextField domOidTextField;
    private JTextField usernameTextField;

    private InputValidator inputValidator;

    public SiteMinderEnableUserPropertiesDialog(final Frame owner, final SiteMinderEnableUserAssertion assertion) {
        super(SiteMinderEnableUserAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(SiteMinderEnableUserAssertion assertion) {
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
    }

    @Override
    public SiteMinderEnableUserAssertion getData(SiteMinderEnableUserAssertion assertion) throws ValidationException {
        String validationErrorMessage = inputValidator.validate();

        if (null != validationErrorMessage) {
            throw new ValidationException(validationErrorMessage);
        }

        assertion.setAgentGoid(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getGoid());
        assertion.setAgentId(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getAgentId());
        assertion.setDomOid(domOidTextField.getText().trim());
        assertion.setUsername(usernameTextField.getText().trim());

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

        inputValidator = new InputValidator(this, getTitle());
        inputValidator.ensureComboBoxSelection("Configuration Name", agentComboBox);
        inputValidator.constrainTextFieldToBeNonEmpty("Domain Object ID", domOidTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Username", usernameTextField, null);
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

