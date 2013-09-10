package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;


import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/15/13
 */
public class SiteMinderAuthenticationPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderAuthenticateAssertion> {
    
    private JPanel propertyPanel;
    private JRadioButton useLastCredentialsRadioButton;
    private JRadioButton specifyCredentialsRadioButton;
    private JTextField credentialsTextField;
    private JCheckBox authenticateViaSiteMinderCookieCheckBox;
    private TargetVariablePanel siteminderPrefixVariablePanel;
    private TargetVariablePanel cookieVariablePanel;
    private final InputValidator inputValidator;

    public SiteMinderAuthenticationPropertiesDialog(final Frame owner, final SiteMinderAuthenticateAssertion assertion) {
        super(SiteMinderAuthenticateAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        siteminderPrefixVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        siteminderPrefixVariablePanel.setDefaultVariableOrPrefix(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);

        siteminderPrefixVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        useLastCredentialsRadioButton.setSelected(true);
        useLastCredentialsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        specifyCredentialsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        authenticateViaSiteMinderCookieCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        cookieVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        inputValidator.constrainTextField(credentialsTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (specifyCredentialsRadioButton.isSelected()) {
                    if (credentialsTextField.getText().isEmpty()) {
                        return "User credentials must not be empty";
                    }
                }
                return null;
            }
        });

        inputValidator.attachToButton(getOkButton(), super.createOkAction());

        enableDisableComponents();
        pack();
    }

    private void enableDisableComponents() {
        cookieVariablePanel.setEnabled(authenticateViaSiteMinderCookieCheckBox.isSelected());
        credentialsTextField.setEnabled(specifyCredentialsRadioButton.isSelected());
        getOkButton().setEnabled(siteminderPrefixVariablePanel.isEntryValid() &&
                (!cookieVariablePanel.isEnabled() || cookieVariablePanel.isEntryValid()));
    }
    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(SiteMinderAuthenticateAssertion assertion) {
        authenticateViaSiteMinderCookieCheckBox.setSelected(assertion.isUseSMCookie());
        cookieVariablePanel.setVariable(assertion.getCookieSourceVar());

        if(assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            siteminderPrefixVariablePanel.setVariable(assertion.getPrefix());
        } else {
            siteminderPrefixVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        }

        useLastCredentialsRadioButton.setSelected(assertion.isLastCredential());
        specifyCredentialsRadioButton.setSelected(!assertion.isLastCredential());
        credentialsTextField.setText(assertion.getLogin());
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
    public SiteMinderAuthenticateAssertion getData(SiteMinderAuthenticateAssertion assertion) throws ValidationException {
        assertion.setUseSMCookie(authenticateViaSiteMinderCookieCheckBox.isSelected());
        assertion.setCookieSourceVar(cookieVariablePanel.getVariable());
        assertion.setPrefix(siteminderPrefixVariablePanel.getVariable());
        assertion.setLastCredential(useLastCredentialsRadioButton.isSelected());
        assertion.setLogin(credentialsTextField.getText());
        //set user credentials

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
}
