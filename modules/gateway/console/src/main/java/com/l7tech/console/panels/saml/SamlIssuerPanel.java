package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.assertion.SamlIssuerConfig;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class SamlIssuerPanel extends JPanel{

    // - PUBLIC

    /**
     *
     * @param showTitle if true the label 'Issuer' will be shown.
     * @param showIssuerCheckBox if true the 'Add Issuer' check box will be shown. If false then it's assumed that the Issuer
     */
    public SamlIssuerPanel(final boolean showTitle, final boolean showIssuerCheckBox) {
        this.showTitle = showTitle;
        this.showIssuerCheckBox = showIssuerCheckBox;
        initialize();
    }

    /**
     * Supports usages where the version can change while the panel is displayed.
     * @param version SAML Version 1 or 2.
     */
    public void setVersion(int version) {
        this.version = version;
        enableDisable();
    }

    public void setData(SamlIssuerConfig issuerElmConfig) throws IllegalArgumentException {
        version = issuerElmConfig.getVersion() == null ? 1 : issuerElmConfig.getVersion();
        isProtocolUsage = issuerElmConfig.samlProtocolUsage();

        final String customIssuerValue = issuerElmConfig.getCustomIssuerValue();
        if (customIssuerValue != null) {
            issuerFromTemplateRadioButton.setSelected(true);
            issuerValueSquigglyTextField.setText(customIssuerValue);
        } else {
            issuerDefaultRadioButton.setSelected(true);
        }

        if (issuerElmConfig.includeIssuer()) {
            addIssuerCheckBox.setSelected(true);
        }

        if (version == 2) {
            final String customIssuerFormat = issuerElmConfig.getCustomIssuerFormat();
            if (customIssuerFormat != null) {
                includeFormatAttributeCheckBox.setSelected(true);

                if (SamlConstants.NAMEIDENTIFIER_UNSPECIFIED.equals(customIssuerFormat)) {
                    unspecifiedRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_EMAIL.equals(customIssuerFormat)) {
                    emailAddressRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(customIssuerFormat)) {
                    x509SubjectRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_WINDOWS.equals(customIssuerFormat)) {
                    windowsDomainRadioButton.setSelected(true);
                } else if (SamlConstants.NAMEIDENTIFIER_KERBEROS.equals(customIssuerFormat)) {
                    kerberosRadioButton.setSelected(true);
                } else {
                    entityFormatRadioButton.setSelected(true);
                }
            } else {
                //set selected so if Format check box is enabled there will be a default selection.
                entityFormatRadioButton.setSelected(true);
            }

            final String customNameQualifier = issuerElmConfig.getCustomIssuerNameQualifier();
            if (customNameQualifier != null) {
                nameQualifierSquigglyTextField.setText(customNameQualifier);
            }
        }

        enableDisable();
    }

    public void getData(SamlIssuerConfig issuerElmConfig) throws IllegalArgumentException {

        if (!issuerDefaultRadioButton.isSelected()) {
            issuerElmConfig.setCustomIssuerValue(issuerValueSquigglyTextField.getText().trim());
        } else {
            issuerElmConfig.setCustomIssuerValue(null);
        }

        final boolean addIssuer;
        if (isProtocolUsage && version == 2) {
            //if protocol and 2 then the user chooses whether it's added if check box is shown
            final boolean visible = addIssuerCheckBox.isVisible();
            //noinspection SimplifiableConditionalExpression
            addIssuer = (visible) ? addIssuerCheckBox.isSelected() : true;
        } else //noinspection RedundantIfStatement
            if (isProtocolUsage) {
            // otherwise if protocol it's version 1 so no issuer
            addIssuer = false;
        } else {
            // not protocol - always add
            addIssuer = true;
        }

        if (addIssuer) {
            if (version == 2) {
                issuerElmConfig.includeIssuer(addIssuer);
                final String nameFormatUri;
                if (includeFormatAttributeCheckBox.isSelected()) {
                    if (unspecifiedRadioButton.isSelected()) {
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;
                    } else if (emailAddressRadioButton.isSelected()) {
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_EMAIL;
                    } else if (x509SubjectRadioButton.isSelected()) {
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_X509_SUBJECT;
                    } else if (windowsDomainRadioButton.isSelected()) {
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_WINDOWS;
                    } else if (kerberosRadioButton.isSelected()) {
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_KERBEROS;
                    } else {
                        //default is entity as per SAML Core 2.0
                        nameFormatUri = SamlConstants.NAMEIDENTIFIER_ENTITY;
                    }
                } else {
                    nameFormatUri = null;
                }
                issuerElmConfig.setCustomIssuerFormat(nameFormatUri);

                final String customNameQualifier = nameQualifierSquigglyTextField.getText().trim();
                if (!customNameQualifier.isEmpty()) {
                    issuerElmConfig.setCustomIssuerNameQualifier(customNameQualifier);
                } else {
                    issuerElmConfig.setCustomIssuerNameQualifier(null);
                }
            } else {
                //set to null properties not related to SAML 1.1
                issuerElmConfig.setCustomIssuerFormat(null);
                issuerElmConfig.setCustomIssuerNameQualifier(null);
            }
        } else {
            issuerElmConfig.includeIssuer(false);
            issuerElmConfig.setCustomIssuerValue(null);
            issuerElmConfig.setCustomIssuerFormat(null);
            issuerElmConfig.setCustomIssuerNameQualifier(null);
        }
    }

    public void validateData() throws AssertionPropertiesOkCancelSupport.ValidationException{

        if (version == 2) {
            final String error = SquigglyFieldUtils.validateSquigglyFieldForVariableReference(nameQualifierSquigglyTextField);
            if (error != null) {
                throw new AssertionPropertiesOkCancelSupport.ValidationException(error);
            }
        }

        if (issuerFromTemplateRadioButton.isSelected()) {
            final String error = SquigglyFieldUtils.validateSquigglyFieldForVariableReference(issuerValueSquigglyTextField);
            if (error != null) {
                throw new AssertionPropertiesOkCancelSupport.ValidationException(error);
            }
        }
    }

    /**
     * Configure a call back listener which is notified when ever a UI item is modified which changes the panels
     * configuration.
     *
     * This can be used by callers to call {@link #validateData()} to determine whether the panels current configuration
     * is valid or not. Expected to be used in wizards where the next button is enabled based on the panels current state.
     *
     * @param configListener call back listener.
     */
    public void setConfigListener(@Nullable Functions.Nullary<Void> configListener) {
        this.configListener = configListener;
    }

    // - PRIVATE

    private JCheckBox includeFormatAttributeCheckBox;
    private JPanel formatsButtonPanel;
    private JRadioButton entityFormatRadioButton;
    private JRadioButton unspecifiedRadioButton;
    private JRadioButton emailAddressRadioButton;
    private JRadioButton x509SubjectRadioButton;
    private JRadioButton windowsDomainRadioButton;
    private JRadioButton kerberosRadioButton;
    private SquigglyTextField issuerValueSquigglyTextField;
    private SquigglyTextField nameQualifierSquigglyTextField;
    private JPanel mainPanel;
    private JPanel formatPanel;
    private JLabel nameQualifierLabel;
    private JLabel titleLabel;
    private JRadioButton issuerDefaultRadioButton;
    private JRadioButton issuerFromTemplateRadioButton;
    private JPanel nameQualifierPanel;
    private JPanel issuerPanel;
    private JCheckBox addIssuerCheckBox;
    private JLabel issuerValueLabel;

    private int version;
    private boolean isProtocolUsage;
    private Functions.Nullary<Void> configListener;
    final boolean showTitle;
    final boolean showIssuerCheckBox;

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        if (showTitle) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.setVisible(false);
        }

        if (!showIssuerCheckBox) {
            addIssuerCheckBox.setVisible(false);
        }

        final RunOnChangeListener onChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                if (configListener != null) {
                    configListener.call();
                }
            }
        });

        addIssuerCheckBox.addActionListener(onChangeListener);

        includeFormatAttributeCheckBox.addActionListener(onChangeListener);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(issuerValueSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyFieldForVariableReference(issuerValueSquigglyTextField);
                if (configListener != null) {
                    configListener.call();
                }
            }
        }, 300);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(nameQualifierSquigglyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyFieldForVariableReference(nameQualifierSquigglyTextField);
                if (configListener != null) {
                    configListener.call();
                }
            }
        }, 300);

        issuerDefaultRadioButton.addActionListener(onChangeListener);
        issuerFromTemplateRadioButton.addActionListener(onChangeListener);
    }

    private void enableDisable() {
        final boolean isIssuerApplicable = (addIssuerCheckBox.isVisible())?
                addIssuerCheckBox.isSelected(): version ==2 || !isProtocolUsage;

        //can't check version in initialize as at that point we don't know what version were configuring for
        final boolean isVersion2 = version == 2;
        includeFormatAttributeCheckBox.setEnabled(isVersion2 && isIssuerApplicable);

        nameQualifierLabel.setEnabled(isVersion2 && isIssuerApplicable);
        nameQualifierSquigglyTextField.setEnabled(isVersion2 && isIssuerApplicable);

        if (version == 2) {
            final boolean includeNameFormat = includeFormatAttributeCheckBox.isSelected() && isIssuerApplicable;

            formatsButtonPanel.setEnabled(includeNameFormat);
            entityFormatRadioButton.setEnabled(includeNameFormat);
            unspecifiedRadioButton.setEnabled(includeNameFormat);
            emailAddressRadioButton.setEnabled(includeNameFormat);
            x509SubjectRadioButton.setEnabled(includeNameFormat);
            windowsDomainRadioButton.setEnabled(includeNameFormat);
            kerberosRadioButton.setEnabled(includeNameFormat);
        }

        issuerDefaultRadioButton.setEnabled(isIssuerApplicable);
        issuerFromTemplateRadioButton.setEnabled(isIssuerApplicable);
        issuerValueSquigglyTextField.setEnabled(issuerFromTemplateRadioButton.isSelected() && isIssuerApplicable);

        showHideComponents();
    }

    private void showHideComponents() {
        final boolean isVersion2 = version == 2;

        if (isVersion2) {
            issuerPanel.setVisible(true);
            formatPanel.setVisible(true);
            nameQualifierPanel.setVisible(true);
        } else if (!isProtocolUsage) {
            issuerPanel.setVisible(true);
            formatPanel.setVisible(false);
            nameQualifierPanel.setVisible(false);
        } else {
            // hide everything as not applicable
            issuerPanel.setVisible(false);
            formatPanel.setVisible(false);
            nameQualifierPanel.setVisible(false);
        }
    }
}
