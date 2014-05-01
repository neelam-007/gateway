package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
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
    private JCheckBox authenticateViaSiteMinderCookieCheckBox;
    private TargetVariablePanel siteminderPrefixVariablePanel;
    private TargetVariablePanel cookieVariablePanel;
    private JCheckBox usernameAndPasswordCheckBox;
    private JCheckBox x509CertificateCheckBox;
    private JTextField namedUser;
    private JTextField namedCertificate;
    private JLabel usernameLabel;
    private JLabel certificateNameLabel;
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
        useLastCredentialsRadioButton.setSelected(true);

        ActionListener buttonSwitchListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        };

        useLastCredentialsRadioButton.addActionListener(buttonSwitchListener);
        specifyCredentialsRadioButton.addActionListener(buttonSwitchListener);
        authenticateViaSiteMinderCookieCheckBox.addActionListener(buttonSwitchListener);
        usernameAndPasswordCheckBox.addActionListener(buttonSwitchListener);
        x509CertificateCheckBox.addActionListener(buttonSwitchListener);
        Utilities.enableGrayOnDisabled(usernameLabel,namedUser,certificateNameLabel,namedCertificate);

        inputValidator.constrainTextFieldToBeNonEmpty("Username", namedUser, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Certificate CN or DN", namedCertificate, null);

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (StringUtils.isBlank(siteminderPrefixVariablePanel.getVariable())) {
                    return "SiteMinder Variable Prefix must not be empty!";
                } else if(!VariableMetadata.isNameValid(siteminderPrefixVariablePanel.getVariable())) {
                    return "SiteMinder Variable Prefix must have valid name";
                }

                return null;
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (cookieVariablePanel.isEnabled()) {
                    if (StringUtils.isBlank(cookieVariablePanel.getVariable())) {
                        return "SSO Token Context Variable must not be empty!";
                    } else if(!VariableMetadata.isNameValid(cookieVariablePanel.getVariable())) {
                        return "SSO Token Context Variable must have valid name";
                    }
                }

                return null;
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if ( specifyCredentialsRadioButton.isSelected()
                       && ! usernameAndPasswordCheckBox.isSelected()
                       && ! x509CertificateCheckBox.isSelected() ) {
                    return "At least one of Username or X509 Certificate must be selected when using Specified Credentials.";
                }

                return null;
            }
        });

        enableDisableComponents();

        pack();
    }

    private void enableDisableComponents() {
        cookieVariablePanel.setEnabled(authenticateViaSiteMinderCookieCheckBox.isSelected());
        usernameLabel.setEnabled(specifyCredentialsRadioButton.isSelected() && usernameAndPasswordCheckBox.isSelected());
        namedUser.setEnabled(specifyCredentialsRadioButton.isSelected() && usernameAndPasswordCheckBox.isSelected());
        certificateNameLabel.setEnabled(specifyCredentialsRadioButton.isSelected() && x509CertificateCheckBox.isSelected());
        namedCertificate.setEnabled(specifyCredentialsRadioButton.isSelected() && x509CertificateCheckBox.isSelected());
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
        usernameAndPasswordCheckBox.setSelected(assertion.isSendUsernamePasswordCredential());
        x509CertificateCheckBox.setSelected(assertion.isSendX509CertificateCredential());
        namedUser.setText(assertion.getNamedUser());
        namedCertificate.setText(assertion.getNamedCertificate());

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
    public SiteMinderAuthenticateAssertion getData(SiteMinderAuthenticateAssertion assertion) throws ValidationException {
        String validationErrorMessage = inputValidator.validate();

        if (null != validationErrorMessage) {
            throw new ValidationException(validationErrorMessage);
        }

        assertion.setUseSMCookie(authenticateViaSiteMinderCookieCheckBox.isSelected());
        assertion.setCookieSourceVar(cookieVariablePanel.getVariable());
        assertion.setPrefix(siteminderPrefixVariablePanel.getVariable());
        assertion.setLastCredential(useLastCredentialsRadioButton.isSelected());
        assertion.setSendUsernamePasswordCredential(usernameAndPasswordCheckBox.isSelected());
        assertion.setSendX509CertificateCredential(x509CertificateCheckBox.isSelected());
        assertion.setNamedUser(namedUser.getText().trim());
        assertion.setNamedCertificate(namedCertificate.getText().trim());
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
}
