package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

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
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/12/13
 */
public class SiteMinderCheckProtectedPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderCheckProtectedAssertion> {

    private static final Pattern AGENTID_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9]+).name\\s*=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private JPanel propertyPanel;
    private JComboBox agentComboBox;
    private JTextField resourceTextField;
    private JComboBox actionComboBox;
    private TargetVariablePanel prefixTargetVariablePanel;
    private final InputValidator inputValidator;

    public SiteMinderCheckProtectedPropertiesDialog(final Frame owner, final SiteMinderCheckProtectedAssertion assertion) {
        super(SiteMinderCheckProtectedAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();

    }

    @Override
    protected void initComponents() {
        super.initComponents();
        prefixTargetVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        prefixTargetVariablePanel.setDefaultVariableOrPrefix(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        agentComboBox.setModel(agentComboBoxModel);

        DefaultComboBoxModel<String> actionComboBoxModel = new DefaultComboBoxModel<>(new String[]{"GET", "POST", "PUT"});
        actionComboBox.setModel(actionComboBoxModel);

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
        inputValidator.attachToButton(getOkButton(), super.createOkAction());
        enableDisableComponents();
    }

    /**
     * populates agent combo box with agent IDs
     *
     * @param agentComboBoxModel
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
        String action = (String) actionComboBox.getEditor().getItem();

        getOkButton().setEnabled(prefixTargetVariablePanel.isEntryValid() &&
                (action.length() > 0) &&
                !resourceTextField.getText().isEmpty() &&
                agentComboBox.getSelectedIndex() > -1
        );
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
                }
            }
        } catch (FindException e) {
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
        assertion.setAgentGoid(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getGoid());
        assertion.setAgentId(((SiteMinderConfigurationKey) agentComboBox.getSelectedItem()).getAgentId());
        assertion.setProtectedResource(resourceTextField.getText());
        assertion.setAction((String) actionComboBox.getSelectedItem());
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

    @Override
    protected ActionListener createOkAction() {
        // returns a no-op action so we can add our own Ok listener
        return new RunOnChangeListener();
    }


    private SiteMinderAdmin getSiteMinderAdmin() {
        return Registry.getDefault().getSiteMinderConfigurationAdmin();
    }

    private static class SiteMinderConfigurationKey {
        private String agentId;
        private Goid goid;

        private SiteMinderConfigurationKey(SiteMinderConfiguration config) {
            agentId = config.getName();
            goid = config.getGoid();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SiteMinderConfigurationKey)) return false;

            SiteMinderConfigurationKey that = (SiteMinderConfigurationKey) o;

            if (agentId != null ? !agentId.equals(that.agentId) : that.agentId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return agentId != null ? agentId.hashCode() : 0;
        }

        @Override
        public String toString() {
            return agentId;
        }

        public Goid getGoid() {
            return goid;
        }

        public String getAgentId() {
            return agentId;
        }
    }


}
