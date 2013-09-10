package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.ContextVariableTextComponentValidationRule;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
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
public class SiteMinderAuthorizationPropertiesDialog extends AssertionPropertiesOkCancelSupport<SiteMinderAuthorizeAssertion> {
    private static final Pattern SPACE_CHARS = Pattern.compile("(\\s)+");

    private JRadioButton useSSOTokenFromSmContextRadioButton;
    private JRadioButton useSSOTokenFromContextVariableRadioButton;
    private TargetVariablePanel ssoTokenVariablePanel;
    private TargetVariablePanel siteminderPrefixVariablePanel;
    private JPanel propertyPanel;
    private JTextField cookieDomainTextField;
    private JTextField cookiePathTextField;
    private JTextField cookieVersionTextField;
    private JTextField cookieSecureTextField;
    private JTextField cookieMaxAgeTextField;
    private JTextField cookieCommentTextField;
    private JCheckBox setSiteMinderCookieCheckBox;
    private JTextField cookieNameTextField;
    private final InputValidator inputValidator;

    public SiteMinderAuthorizationPropertiesDialog(final Frame owner, final SiteMinderAuthorizeAssertion assertion) {
        super(SiteMinderAuthorizeAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator(this, getTitle());
        initComponents();

    }

    @Override
    protected void initComponents() {
        super.initComponents();

        siteminderPrefixVariablePanel.setVariable(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        siteminderPrefixVariablePanel.setDefaultVariableOrPrefix(SiteMinderAuthenticateAssertion.DEFAULT_PREFIX);
        useSSOTokenFromSmContextRadioButton.setSelected(true);

        ActionListener buttonSwitchListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        };

        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableDisableComponents();
            }
        });

        useSSOTokenFromSmContextRadioButton.addActionListener(buttonSwitchListener);
        useSSOTokenFromContextVariableRadioButton.addActionListener(buttonSwitchListener);
        setSiteMinderCookieCheckBox.addActionListener(buttonSwitchListener);

        ssoTokenVariablePanel.addChangeListener(changeListener);

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                    return siteminderPrefixVariablePanel.getErrorMessage();
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return ssoTokenVariablePanel.getErrorMessage();
            }
        });

        inputValidator.constrainTextField(cookieNameTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!cookieNameTextField.isEnabled()) return null;

                Matcher m = SPACE_CHARS.matcher(cookieNameTextField.getText().trim());
                if (m.find()) {
                    return "Cookie Name cannot contain spaces!";
                } else if (cookieNameTextField.getText().trim().isEmpty()) {
                    return "Cookie Name is required!";
                }
                return null;
            }
        });

        inputValidator.constrainTextField(cookieMaxAgeTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!cookieMaxAgeTextField.isEnabled()) return null;
                String val = cookieMaxAgeTextField.getText().trim();
                if (Syntax.getReferencedNames(val).length > 0) return null;
                if (!val.isEmpty()) {
                    try {
                        if (Integer.parseInt(val) >= -1) return null;
                    } catch (Exception e) {
                        return "Version value must be a valid integer or a context variable";
                    }
                }
                return null;
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
                        if (Integer.parseInt(val) >= 0) return null;
                    } catch (Exception e) {
                        return "Version value must be a valid integer or a context variable";
                    }
                }

                return null;
            }
        });

        inputValidator.constrainTextField(cookieSecureTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!cookieSecureTextField.isEnabled()) return null;

                String val = cookieSecureTextField.getText().trim();
                if (Syntax.getReferencedNames(val).length > 0) return null;

                if (!val.isEmpty() && !(val.trim().equalsIgnoreCase("true") || val.trim().equalsIgnoreCase("false"))) {
                   return "Is Secure value must be either \"true\" or \"false\" or a context variable";
                }

                return null;
            }
        });

        inputValidator.attachToButton(getOkButton(), super.createOkAction());
    }

    private void enableDisableComponents() {
        ssoTokenVariablePanel.setEnabled(useSSOTokenFromContextVariableRadioButton.isSelected());
        cookieNameTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookieDomainTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookiePathTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookieMaxAgeTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookieSecureTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookieVersionTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());
        cookieCommentTextField.setEnabled(setSiteMinderCookieCheckBox.isSelected());

        getOkButton().setEnabled(siteminderPrefixVariablePanel.isEntryValid() && (!ssoTokenVariablePanel.isEnabled() || ssoTokenVariablePanel.isEntryValid()));
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(SiteMinderAuthorizeAssertion assertion) {
        useSSOTokenFromSmContextRadioButton.setSelected(assertion.isUseCustomCookieName());
        useSSOTokenFromContextVariableRadioButton.setSelected(assertion.isUseVarAsCookieSource());
        ssoTokenVariablePanel.setVariable(assertion.getCookieSourceVar());

        if (assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            siteminderPrefixVariablePanel.setVariable(assertion.getPrefix());
        } else {
            siteminderPrefixVariablePanel.setVariable(SiteMinderAuthorizeAssertion.DEFAULT_PREFIX);
        }

        if (assertion.getCookieName() != null && !assertion.getCookieName().isEmpty()) {
            cookieNameTextField.setText(assertion.getCookieName());
        } else {
            cookieNameTextField.setText(SiteMinderAuthorizeAssertion.DEFAULT_SMSESSION_NAME);
        }

        setSiteMinderCookieCheckBox.setSelected(assertion.isSetSMCookie());
        cookieDomainTextField.setText(assertion.getCookieDomain());
        cookiePathTextField.setText(assertion.getCookiePath());
        cookieMaxAgeTextField.setText(assertion.getCookieMaxAge());
        cookieSecureTextField.setText(assertion.getCookieSecure());
        cookieVersionTextField.setText(assertion.getCookieVersion());
        cookieCommentTextField.setText(assertion.getCookieComment());

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
    public SiteMinderAuthorizeAssertion getData(SiteMinderAuthorizeAssertion assertion) throws ValidationException {
        assertion.setUseCustomCookieName(useSSOTokenFromSmContextRadioButton.isSelected());
        assertion.setUseVarAsCookieSource(useSSOTokenFromContextVariableRadioButton.isSelected());
        assertion.setCookieSourceVar(ssoTokenVariablePanel.getVariable());
        assertion.setPrefix(siteminderPrefixVariablePanel.getVariable());
        assertion.setSetSMCookie(setSiteMinderCookieCheckBox.isSelected());
        assertion.setCookieName(cookieNameTextField.getText().trim());
        assertion.setCookieDomain(cookieDomainTextField.getText().trim());
        assertion.setCookiePath(cookiePathTextField.getText().trim());
        assertion.setCookieMaxAge(cookieMaxAgeTextField.getText().trim());
        assertion.setCookieSecure(cookieSecureTextField.getText().trim());
        assertion.setCookieVersion(cookieVersionTextField.getText().trim());
        assertion.setCookieComment(cookieCommentTextField.getText().trim());

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
        return propertyPanel;
    }

    @Override
    protected ActionListener createOkAction() {
        // returns a no-op action so we can add our own Ok listener
        return new RunOnChangeListener();
    }
}
