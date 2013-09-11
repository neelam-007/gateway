package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
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

        useSSOTokenFromSmContextRadioButton.addActionListener(buttonSwitchListener);
        useSSOTokenFromContextVariableRadioButton.addActionListener(buttonSwitchListener);
        setSiteMinderCookieCheckBox.addActionListener(buttonSwitchListener);

        inputValidator.addRule(createTargetVariablePanelValidationRule("SiteMinder Variable Prefix",
                siteminderPrefixVariablePanel));

        inputValidator.addRule(createTargetVariablePanelValidationRule("SSO Token Context Variable",
                ssoTokenVariablePanel));

        inputValidator.constrainTextField(cookieNameTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (cookieNameTextField.isEnabled()) {
                    String val = cookieNameTextField.getText().trim();

                    if (val.isEmpty()) {
                        return "Cookie Name is required.";
                    }

                    Matcher m = SPACE_CHARS.matcher(val);

                    if (m.find()) {
                        return "Cookie Name cannot contain spaces.";
                    }
                }

                return null;
            }
        });

        inputValidator.constrainTextField(cookieVersionTextField,
                createIntegerOrContextVariableRule("Version", cookieVersionTextField));

        inputValidator.constrainTextField(cookieMaxAgeTextField,
                createIntegerOrContextVariableRule("Max Age", cookieMaxAgeTextField));

        inputValidator.constrainTextField(cookieSecureTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (cookieSecureTextField.isEnabled()) {
                    String val = cookieSecureTextField.getText().trim();

                    if (!val.isEmpty() && Syntax.getReferencedNames(val).length == 0) {
                        if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
                           return "Is Secure value must be either \"true\" or \"false\" or a context variable.";
                        }
                    }
                }

                return null;
            }
        });

        enableDisableComponents();
    }

    private InputValidator.ValidationRule createTargetVariablePanelValidationRule(final String name,
                                                                                  final TargetVariablePanel component) {
        return new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (component.isEnabled()) {
                    if (StringUtils.isBlank(component.getVariable())) {
                        return name + " must not be empty!";
                    } else if(!VariableMetadata.isNameValid(component.getVariable())) {
                        return name + " must have valid name";
                    }
                }

                return null;
            }
        };
    }

    private InputValidator.ValidationRule createIntegerOrContextVariableRule(final String name,
                                                                             final JTextComponent component) {
        return new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (component.isEnabled()) {
                    String val = component.getText().trim();

                    if (!val.isEmpty() && Syntax.getReferencedNames(val).length == 0) {
                        try {
                            Integer.parseInt(val);
                        } catch (Exception e) {
                            return name + " must be a valid integer or a context variable.";
                        }
                    }
                }

                return null;
            }
        };
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
        String validationErrorMessage = inputValidator.validate();

        if (null != validationErrorMessage) {
            throw new ValidationException(validationErrorMessage);
        }

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
        return propertyPanel;
    }
}
