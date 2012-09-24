package com.l7tech.external.assertions.kerberos.authentication.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.external.assertions.kerberos.authentication.KerberosAuthenticationAssertion;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;

/**
 * Copyright: Layer 7 Technologies, 2012
 * Date: 7/9/12
 */
public class KerberosAuthenticationDialog extends AssertionPropertiesOkCancelSupport<KerberosAuthenticationAssertion> {
    private JRadioButton useGatewayKeytabRadioButton;
    private JRadioButton useConfiguredCredentialsRadioButton;
    private JTextField gatewayAccountName;
    private SecurePasswordComboBox securePasswordComboBox;
    private JButton manageStoredPasswordsButton;
    private JPanel propertyPanel;
    private JLabel gatewayAccountNameLabel;
    private JTextField realmTextField;
    private JTextField servicePrincipalTextField;
    private JRadioButton lastAuthenticatedUserRadioButton;
    private JRadioButton specifyUserRadioButton;
    private JTextField authenticatedUserTextField;
    private JRadioButton protocolTransitionRadioButton;
    private JRadioButton constrainedProxyRadioButton;
    private JPanel authenticatedUserPanel;
    private JLabel servicePrincipalLabel;
    private JLabel passwordLabel;
    private JLabel realmLabel;
    private final InputValidator inputValidator;

    public KerberosAuthenticationDialog(final Frame owner, final KerberosAuthenticationAssertion assertion){
        super(KerberosAuthenticationAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();

    }


    @Override
    protected void initComponents() {
        super.initComponents();
        useGatewayKeytabRadioButton.setSelected(true);
        useConfiguredCredentialsRadioButton.setSelected(false);

        securePasswordComboBox.setRenderer(TextListCellRenderer.<SecurePasswordComboBox>basicComboBoxRenderer());
        manageStoredPasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });

        ChangeListener enableDisableComponentsListener = new ChangeListener() {
            /**
             * Invoked when the target of the listener has changed its state.
             *
             * @param e a ChangeEvent object
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        };

        useConfiguredCredentialsRadioButton.addChangeListener(enableDisableComponentsListener);
        inputValidator.constrainTextFieldToBeNonEmpty(realmLabel.getText(), realmTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty(gatewayAccountNameLabel.getText(), gatewayAccountName, null);
        inputValidator.constrainTextFieldToBeNonEmpty(specifyUserRadioButton.getText(), authenticatedUserTextField, null );
        inputValidator.constrainTextFieldToBeNonEmpty(servicePrincipalLabel.getText(), servicePrincipalTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                Matcher m = KerberosAuthenticationAssertion.spnPattern.matcher(servicePrincipalTextField.getText());
                if(!m.matches() && !Syntax.isAnyVariableReferenced(servicePrincipalTextField.getText())){
                    return "Invalid Service Principal name " + servicePrincipalTextField.getText();
                }
                return null;
            }
        });
        inputValidator.ensureComboBoxSelection(passwordLabel.getText(), securePasswordComboBox);
        inputValidator.attachToButton(getOkButton(), super.createOkAction());

        RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        gatewayAccountName.getDocument().addDocumentListener(enableDisableListener);
        specifyUserRadioButton.addChangeListener(enableDisableListener);
        protocolTransitionRadioButton.addChangeListener(enableDisableListener);

    }

    private void enableDisableComponents() {
        gatewayAccountName.setEnabled(useConfiguredCredentialsRadioButton.isSelected());
        boolean isValid = gatewayAccountName.isEnabled() && gatewayAccountName.getText() != null && !"".equals(gatewayAccountName.getText().trim());
        securePasswordComboBox.setEnabled(isValid);
        manageStoredPasswordsButton.setEnabled(isValid);
        constrainedProxyRadioButton.setEnabled(true);
        protocolTransitionRadioButton.setEnabled(true);
        lastAuthenticatedUserRadioButton.setEnabled(protocolTransitionRadioButton.isSelected());
        specifyUserRadioButton.setEnabled(protocolTransitionRadioButton.isSelected());
        authenticatedUserTextField.setEnabled(specifyUserRadioButton.isSelected() && protocolTransitionRadioButton.isSelected());
        authenticatedUserPanel.setEnabled(protocolTransitionRadioButton.isSelected());

    }

    private void doManagePasswords() {
        final SecurePassword password = securePasswordComboBox.getSelectedSecurePassword();
        final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(getOwner());

        securePasswordManagerWindow.pack();
        Utilities.centerOnParentWindow(securePasswordManagerWindow);
        DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
            @Override
            public void run() {
                securePasswordComboBox.reloadPasswordList();

                if ( password != null ) {
                    securePasswordComboBox.setSelectedSecurePassword( password.getOid() );
                    enableDisableComponents();
                    DialogDisplayer.pack(KerberosAuthenticationDialog.this);
                }
                else {
                    securePasswordComboBox.setSelectedItem(null);
                }
            }
        });
    }



    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(KerberosAuthenticationAssertion assertion) {
        realmTextField.setText(assertion.getRealm());
        servicePrincipalTextField.setText(assertion.getServicePrincipalName());
        useGatewayKeytabRadioButton.setSelected(assertion.isKrbUseGatewayKeytab());
        useConfiguredCredentialsRadioButton.setSelected(!assertion.isKrbUseGatewayKeytab());
        gatewayAccountName.setText(assertion.getKrbConfiguredAccount());
        if(securePasswordComboBox.containsItem(assertion.getKrbSecurePasswordReference())) {
            securePasswordComboBox.setSelectedSecurePassword(assertion.getKrbSecurePasswordReference());
        }
        else {
            securePasswordComboBox.setSelectedItem(null);
        }
        protocolTransitionRadioButton.setSelected(assertion.isS4U2Self());
        constrainedProxyRadioButton.setSelected(assertion.isS4U2Proxy());
        lastAuthenticatedUserRadioButton.setSelected(assertion.isLastAuthenticatedUser());
        specifyUserRadioButton.setSelected(!assertion.isLastAuthenticatedUser());
        authenticatedUserTextField.setText(assertion.isLastAuthenticatedUser()? "":assertion.getAuthenticatedUser());

        enableDisableComponents();

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
    public KerberosAuthenticationAssertion getData(KerberosAuthenticationAssertion assertion) throws ValidationException {
        assertion.setRealm(realmTextField.getText());
        assertion.setServicePrincipalName(servicePrincipalTextField.getText());
        assertion.setKrbUseGatewayKeytab(useGatewayKeytabRadioButton.isSelected());
        assertion.setKrbConfiguredAccount(gatewayAccountName.getText());
        if(securePasswordComboBox.getSelectedSecurePassword() != null) {
            assertion.setKrbSecurePasswordReference(securePasswordComboBox.getSelectedSecurePassword().getOid());
        }
        assertion.setS4U2Self(protocolTransitionRadioButton.isSelected());
        assertion.setS4U2Proxy(constrainedProxyRadioButton.isSelected());
        assertion.setLastAuthenticatedUser(lastAuthenticatedUserRadioButton.isSelected());
        String authenticatedUser = !lastAuthenticatedUserRadioButton.isSelected()? authenticatedUserTextField.getText():null;
        assertion.setAuthenticatedUser(authenticatedUser);

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
        return new RunOnChangeListener();
    }
}
