package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.siteminder.SiteMinderAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/25/13
 */
public class SiteMinderPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderAssertion> {

    private static final Pattern AGENTID_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9]+).name\\s*=",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);

    private JComboBox agentComboBox;
    private JTextField resourceTextField;
    private JComboBox actionComboBox;
    private JCheckBox authenticateViaSiteMinderCookieCheckBox;
    private JRadioButton useCookieFromRequestRadioButton;
    private JRadioButton useCookieFromContextRadioButton;
    private JTextField smCookieNameTextField;
    private TargetVariablePanel cookieVariablePanel;
    private TargetVariablePanel siteminderPrefixVariablePanel;
    private JPanel propertyPanel;
    private JRadioButton useLastCredentialRadioButton;
    private JRadioButton findCredentialRadioButton;
    private JComboBox credentialTypeComboBox;
    private JTextField cookieDomainTextField;
    private JTextField cookiePathTextField;
    private JTextField cookieVersionTextField;
    private JTextField cookieSecureTextField;
    private JTextField cookieMaxAgeTextField;
    private JTextField cookieCommentTextField;
    private final InputValidator inputValidator;
    private ClusterStatusAdmin clusterStatusAdmin;

    public SiteMinderPropertiesDialog(final Frame owner, final SiteMinderAssertion assertion) {
        super(SiteMinderAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();

    }

    @Override
    protected void initComponents() {
        super.initComponents();
        initAdminConnection();
        siteminderPrefixVariablePanel.setVariable(SiteMinderAssertion.DEFAULT_PREFIX);
        siteminderPrefixVariablePanel.setDefaultVariableOrPrefix(SiteMinderAssertion.DEFAULT_PREFIX);
        DefaultComboBoxModel<String> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        agentComboBox.setModel(agentComboBoxModel);
        agentComboBox.setSelectedIndex(0);

        DefaultComboBoxModel<String> actionComboBoxModel = new DefaultComboBoxModel<>(new String[]{"GET","POST","PUT"});
        actionComboBox.setModel(actionComboBoxModel);
        smCookieNameTextField.setText(SiteMinderAssertion.DEFAULT_SMSESSION_NAME);
        siteminderPrefixVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });
        authenticateViaSiteMinderCookieCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });
        actionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
        useCookieFromRequestRadioButton.setSelected(true);
        useCookieFromRequestRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
        useCookieFromContextRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
        useLastCredentialRadioButton.setSelected(true);
        useLastCredentialRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
        findCredentialRadioButton.setSelected(false);
        findCredentialRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });
        inputValidator.constrainTextFieldToBeNonEmpty("Protected Resource", resourceTextField, null);
        inputValidator.ensureComboBoxSelection("Action", actionComboBox);
        inputValidator.constrainTextField(cookieMaxAgeTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!cookieMaxAgeTextField.isEnabled()) return null;
                String val = cookieMaxAgeTextField.getText().trim();
                if (Syntax.getReferencedNames(val).length > 0) return null;
                if (!val.isEmpty()) {
                    try {
                        int ival = Integer.parseInt(val);
                        if (ival >= -1 && ival <= Integer.MAX_VALUE) return null;
                    } catch (Exception e) {
                        return "Version value must be a valid integer or a context variable";
                    }
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        inputValidator.constrainTextField(cookieVersionTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!cookieVersionTextField.isEnabled()) return null;
                String val = cookieVersionTextField.getText().trim();
                if (Syntax.getReferencedNames(val).length > 0) return null;
                if (!val.isEmpty()) {
                    try {
                        int ival = Integer.parseInt(val);
                        if (ival >= 0 && ival <= Integer.MAX_VALUE) return null;
                    } catch (Exception e) {
                        return "Version value must be a valid integer or a context variable";
                    }
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        inputValidator.constrainTextField(cookieSecureTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(!cookieSecureTextField.isEnabled()) return null;
                String val = cookieSecureTextField.getText().trim();
                if(Syntax.getReferencedNames(val).length > 0) return null;
                if(!val.isEmpty() && !(val.trim().equalsIgnoreCase("true") || val.trim().equalsIgnoreCase("false"))) {
                   return "Is Secure value must be either \"true\" or \"false\" or a context variable";
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        inputValidator.attachToButton(getOkButton(), super.createOkAction());

    }

    /**
     * populates agent combo box with agent IDs
     * TODO: move to generic entity model instead of cluster properties
     * @param agentComboBoxModel
     */
    private void populateAgentComboBoxModel(DefaultComboBoxModel<String> agentComboBoxModel) {
        try {
            ClusterProperty smConfigProperty = clusterStatusAdmin.findPropertyByName("siteminder12.agent.configuration");
            String smConfig = smConfigProperty.getValue();
            //search for agent name in the cluster property file
            String agentId = null;
            Matcher m = AGENTID_PATTERN.matcher(smConfig);
            while (m.find()) {
                agentId = m.group(1);
                agentComboBoxModel.addElement(agentId);
            }

        } catch (FindException e) {
            //do not throw any exceptions at this point. leave agent combo box empty
        }
    }

    private void enableDisableComponents() {
        useCookieFromRequestRadioButton.setEnabled(authenticateViaSiteMinderCookieCheckBox.isSelected());
        useCookieFromContextRadioButton.setEnabled(authenticateViaSiteMinderCookieCheckBox.isSelected());
        smCookieNameTextField.setEnabled(useCookieFromRequestRadioButton.isSelected());
        cookieVariablePanel.setEnabled(useCookieFromContextRadioButton.isSelected());
        credentialTypeComboBox.setEnabled(findCredentialRadioButton.isSelected());
        getOkButton().setEnabled(siteminderPrefixVariablePanel.isEntryValid() && actionComboBox.getSelectedIndex() > -1 && !resourceTextField.getText().isEmpty());
    }
    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(SiteMinderAssertion assertion) {
        agentComboBox.setSelectedItem(assertion.getAgentID());
        resourceTextField.setText(assertion.getProtectedResource());
        actionComboBox.getModel().setSelectedItem(assertion.getAction());
        authenticateViaSiteMinderCookieCheckBox.setSelected(assertion.isUseSMCookie());
        useCookieFromRequestRadioButton.setSelected(assertion.isUseCustomCookieName());
        if(assertion.getCookieNameVariable() != null && !assertion.getCookieNameVariable().isEmpty()) {
            smCookieNameTextField.setText(assertion.getCookieNameVariable());
        }
        else {
            smCookieNameTextField.setText(SiteMinderAssertion.DEFAULT_SMSESSION_NAME);
        }
        useCookieFromContextRadioButton.setSelected(assertion.isUseVarAsCookieSource());
        cookieVariablePanel.setVariable(assertion.getCookieSourceVar());
        if(assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            siteminderPrefixVariablePanel.setVariable(assertion.getPrefix());
        }
        else {
            siteminderPrefixVariablePanel.setVariable(SiteMinderAssertion.DEFAULT_PREFIX);
        }
        useLastCredentialRadioButton.setSelected(assertion.isLastCredential());
        findCredentialRadioButton.setSelected(!assertion.isLastCredential());
        cookieDomainTextField.setText(assertion.getCookieDomain());
        cookiePathTextField.setText(assertion.getCookiePath());
        cookieMaxAgeTextField.setText(assertion.getCookieMaxAge());
        cookieSecureTextField.setText(assertion.isCookieSecure());
        cookieVersionTextField.setText(assertion.getCookieVersion());
        cookieCommentTextField.setText(assertion.getCookieComment());
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
    public SiteMinderAssertion getData(SiteMinderAssertion assertion) throws ValidationException {
        assertion.setAgentID((String)agentComboBox.getSelectedItem());
        assertion.setProtectedResource(resourceTextField.getText());
        assertion.setAction((String) actionComboBox.getSelectedItem());
        assertion.setUseSMCookie(authenticateViaSiteMinderCookieCheckBox.isSelected());
        assertion.setUseCustomCookieName(useCookieFromRequestRadioButton.isSelected());
        assertion.setCookieNameVariable(smCookieNameTextField.getText());
        assertion.setUseVarAsCookieSource(useCookieFromContextRadioButton.isSelected());
        assertion.setCookieSourceVar(cookieVariablePanel.getVariable());
        assertion.setPrefix(siteminderPrefixVariablePanel.getVariable());
        assertion.setLastCredential(useLastCredentialRadioButton.isSelected());
        assertion.setCookieDomain(cookieDomainTextField.getText());
        assertion.setCookiePath(cookiePathTextField.getText());
        assertion.setCookieMaxAge(cookieMaxAgeTextField.getText());
        assertion.setCookieSecure(cookieSecureTextField.getText());
        assertion.setCookieVersion(cookieVersionTextField.getText());
        assertion.setCookieComment(cookieCommentTextField.getText());
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

    private void initAdminConnection() {
        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
    }

}
