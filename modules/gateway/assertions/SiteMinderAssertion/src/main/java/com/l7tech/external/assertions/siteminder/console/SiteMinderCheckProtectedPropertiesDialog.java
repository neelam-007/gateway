package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/12/13
 */
public class SiteMinderCheckProtectedPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderCheckProtectedAssertion> {
    private static final Logger logger = Logger.getLogger(SiteMinderCheckProtectedPropertiesDialog.class.getName());

    private static final String[] ACTIONS = new String[] {"GET", "POST", "PUT"};

    private JPanel propertyPanel;
    private JTextField resourceTextField;
    private JComboBox<SiteMinderConfigurationKey> agentComboBox;
    private JComboBox<String> actionComboBox;
    private TargetVariablePanel prefixTargetVariablePanel;
    private InputValidator inputValidator;

    public SiteMinderCheckProtectedPropertiesDialog(final Frame owner, final SiteMinderCheckProtectedAssertion assertion) {
        super(SiteMinderCheckProtectedAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        inputValidator = new InputValidator(this, getTitle());

        prefixTargetVariablePanel.setVariable(SiteMinderCheckProtectedAssertion.DEFAULT_PREFIX);
        prefixTargetVariablePanel.setDefaultVariableOrPrefix(SiteMinderCheckProtectedAssertion.DEFAULT_PREFIX);

        DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        agentComboBox.setModel(agentComboBoxModel);

        actionComboBox.setModel(new DefaultComboBoxModel<>(ACTIONS));

        prefixTargetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        agentComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        actionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        actionComboBox.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                enableDisableComponents();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                enableDisableComponents();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                enableDisableComponents();
            }
        });

        resourceTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableComponents();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableComponents();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableComponents();
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Protected Resource", resourceTextField, null);
        inputValidator.ensureComboBoxSelection("Action", actionComboBox);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (0 == getSelectedAction().trim().length()) {
                    return "The Action field must not be empty.";
                }

                return null;
            }
        });

        enableDisableComponents();
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

    private void enableDisableComponents() {
        String action = getSelectedAction();

        getOkButton().setEnabled(!isReadOnly() &&
                prefixTargetVariablePanel.isEntryValid() &&
                action != null &&
                action.length() > 0 &&
                !resourceTextField.getText().isEmpty() &&
                agentComboBox.getSelectedIndex() > -1 &&
                agentComboBox.getSelectedItem() != null
        );
    }

    private String getSelectedAction() {
        String selectedAction = null;

        if (actionComboBox.getSelectedIndex() > -1) {
            selectedAction = (String) actionComboBox.getSelectedItem();
        } else if (actionComboBox.getEditor().getItem() != null) {
            selectedAction = ((JTextField) actionComboBox.getEditor().getEditorComponent()).getText();
        }

        return selectedAction;
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(SiteMinderCheckProtectedAssertion assertion) {
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

        resourceTextField.setText(assertion.getProtectedResource());
        actionComboBox.getModel().setSelectedItem(assertion.getAction());

        if (assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            prefixTargetVariablePanel.setVariable(assertion.getPrefix());
        } else {
            prefixTargetVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        }
    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but implementors may create and return
     * a new bean instead, if they must.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in.
     *         Never null.
     * @throws com.l7tech.console.panels.AssertionPropertiesOkCancelSupport.ValidationException
     *          if the data cannot be collected because of a validation error.
     */
    @Override
    public SiteMinderCheckProtectedAssertion getData(SiteMinderCheckProtectedAssertion assertion) throws ValidationException {
        String validationErrorMessage = inputValidator.validate();

        if (null != validationErrorMessage) {
            throw new ValidationException(validationErrorMessage);
        }

        assertion.setAgentGoid(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getGoid());
        assertion.setAgentId(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getAgentId());
        assertion.setProtectedResource(resourceTextField.getText().trim());
        assertion.setAction(((String) actionComboBox.getSelectedItem()).trim());
        assertion.setPrefix(prefixTargetVariablePanel.getVariable());

        return assertion;
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     *
     * @return a panel that can be used to edit the assertion properties.  Never null.
     */
    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        return Registry.getDefault().getSiteMinderConfigurationAdmin();
    }
}
