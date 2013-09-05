/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 * $Id: SubjectConfirmationNameIdentifierWizardStepPanel.java 21045 2008-11-05 01:46:35Z vchan $
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.XmlElementEncryptionConfigPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.XmlElementEncryptionConfig;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 * The SAML Subject Confirmation selections <code>WizardStepPanel</code>
 *
 * Main difference between this class and com.l7tech.console.panels.saml.SubjectConfirmationNameIdentifierWizardStepPanel
 * is issue mode versus request mode.
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class SubjectConfirmationNameIdentifierWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JTextField nameQualifierTextField;
    private JTextField valueTextField;
    private JPanel formatsButtonPanel;
    private JCheckBox includeNameCheckBox;
    private JPanel valuePanel;
    private JRadioButton valueFromCredsRadioButton;
    private JRadioButton valueSpecifiedRadio;
    private JRadioButton valueFromUserRadioButton;
    private JLabel nameQualifierLabel;
    private JCheckBox encryptNameIdentifierCheckBox;
    private JButton configureEncryptionButton;
    private JRadioButton customIdentifierButton;
    private JRadioButton  automaticButton;
    private JRadioButton  x509SubjectNameButton;
    private JRadioButton emailAddressButton;
    private JRadioButton windowsDomainQualifiedNameButton;
    private JRadioButton unspecifiedButton;
    private SquigglyTextField customFormatTextField;

    private XmlElementEncryptionConfig xmlElementEncryptionConfig = new XmlElementEncryptionConfig();

    private Map<String, JToggleButton> nameFormatsMap;
    private boolean showTitleLabel;

    private int version;
    private SamlProtocolAssertion assertion;
    private final ActionListener enableDisableListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableDisable();
        }
    };
    private static final String FAKE_URI_AUTOMATIC = "urn:l7tech.com:automatic";
    private static final String FAKE_URI_CUSTOM = "urn:l7tech.com:custom";

    /**
     * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, Assertion prevAssertion) {
        super(next, prevAssertion);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
      * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
      */
     public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, Assertion prevAssertion) {
         this(next, true, prevAssertion);
     }

    /**
     * There is no support for any Modes. The usages of this class in SAMLP is for creating a request only.
     */
    @Override
    protected AssertionMode getMode() {
        throw new IllegalStateException("Mode is not supported");
    }

    /**
     * There is no support for any Modes. The usages of this class in SAMLP is for creating a request only.
     */
    @Override
    protected boolean isRequestMode() {
        throw new IllegalStateException("isRequest mode is not supported");
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        assertion = SamlProtocolAssertion.class.cast(settings);
        version = assertion.getVersion() == null ? 1 : assertion.getVersion();
        enableForVersion();
        nameQualifierTextField.setText(assertion.getNameQualifier());

        SamlpRequestBuilderAssertion sia = (SamlpRequestBuilderAssertion) settings;
        NameIdentifierInclusionType type = sia.getNameIdentifierType();
        if (type == null) {
            if (sia.getNameIdentifierValue() != null) {
                type = NameIdentifierInclusionType.SPECIFIED;
            } else {
                type = NameIdentifierInclusionType.FROM_CREDS;
            }
        }
        switch(type) {
            case NONE:
                includeNameCheckBox.setSelected(false);
                break;
            case FROM_CREDS:
                includeNameCheckBox.setSelected(true);
                valueFromCredsRadioButton.setSelected(true);
                break;
            case FROM_USER:
                includeNameCheckBox.setSelected(true);
                valueFromUserRadioButton.setSelected(true);
                break;
            case SPECIFIED:
                includeNameCheckBox.setSelected(true);
                valueSpecifiedRadio.setSelected(true);
                valueTextField.setText(sia.getNameIdentifierValue());
                break;
        }

        boolean chose = false;
        for (String formatUri : nameFormatsMap.keySet()) {
            if (formatUri.equals(sia.getNameIdentifierFormat())) {
                nameFormatsMap.get(formatUri).setSelected(true);
                chose = true;
                break;
            }
        }

        if (!chose) {
            if(sia.getCustomNameIdentifierFormat() != null) {
                nameFormatsMap.get(FAKE_URI_CUSTOM).setSelected(true);
                customFormatTextField.setText(sia.getCustomNameIdentifierFormat());
                chose = true;
            }
        }

        if (!chose) nameFormatsMap.get(FAKE_URI_AUTOMATIC).setSelected(true);

        encryptNameIdentifierCheckBox.setSelected(sia.isEncryptNameIdentifier());
        xmlElementEncryptionConfig = sia.getXmlEncryptConfig();

        enableDisable();
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);
        assertion.setNameQualifier(nameQualifierTextField.getText());

        SamlpRequestBuilderAssertion sia = (SamlpRequestBuilderAssertion) settings;
        if (includeNameCheckBox.isSelected()) {
            for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isSelected() && jc.isEnabled()) {
                    final String uri = entry.getKey();

                    if (FAKE_URI_CUSTOM.equals(uri)) {
                        final String customValue = customFormatTextField.getText().trim();
                        sia.setCustomNameIdentifierFormat(customValue);
                        sia.setNameIdentifierFormat(null);
                    } else {
                        sia.setNameIdentifierFormat((FAKE_URI_AUTOMATIC.equals(uri)) ? null : uri);
                        sia.setCustomNameIdentifierFormat(null);
                    }

                    break;
                }
            }
            if (valueFromCredsRadioButton.isSelected()) {
                sia.setNameIdentifierType(NameIdentifierInclusionType.FROM_CREDS);
                sia.setNameIdentifierValue(null);
            } else if (valueFromUserRadioButton.isSelected()) {
                sia.setNameIdentifierType(NameIdentifierInclusionType.FROM_USER);
                sia.setNameIdentifierValue(null);
            } else if (valueSpecifiedRadio.isSelected()) {
                sia.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
                sia.setNameIdentifierValue(valueTextField.getText());
            } else {
                throw new RuntimeException("No NameIdentifier value radio button selected"); // Can't happen
            }
        } else {
            sia.setNameIdentifierType(NameIdentifierInclusionType.NONE);
            sia.setNameIdentifierFormat(null);
            sia.setCustomNameIdentifierFormat(null);
            sia.setNameIdentifierValue(null);
        }
        final boolean configuredToEncrypt = includeNameCheckBox.isSelected() && encryptNameIdentifierCheckBox.isSelected();
        sia.setEncryptNameIdentifier(configuredToEncrypt);
        if(configuredToEncrypt){
            sia.setXmlEncryptConfig(xmlElementEncryptionConfig);
        } else {
            // If not configured to encrypt, then remove any previously encryption configuration so it is not persisted.
            final XmlElementEncryptionConfig xmlEncryptConfig = new XmlElementEncryptionConfig();
            //preserve the use OAEP setting (SSG-7583)
            xmlEncryptConfig.setUseOaep(xmlElementEncryptionConfig.isUseOaep());
            sia.setXmlEncryptConfig(xmlEncryptConfig);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        includeNameCheckBox.addActionListener(enableDisableListener);
        valueFromCredsRadioButton.addActionListener(enableDisableListener);
        valueFromUserRadioButton.addActionListener(enableDisableListener);
        valueSpecifiedRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableListener.actionPerformed(e);
                valueTextField.requestFocus();
            }
        });

        encryptNameIdentifierCheckBox.addActionListener(enableDisableListener);

        configureEncryptionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final XmlElementEncryptionConfigPanel encryptionConfigPanel = new XmlElementEncryptionConfigPanel(new XmlElementEncryptionConfig(), true, false);
                encryptionConfigPanel.setData(xmlElementEncryptionConfig);
                encryptionConfigPanel.setPolicyPosition(assertion, getPreviousAssertion());
                final OkCancelDialog dlg = new OkCancelDialog<XmlElementEncryptionConfig>(
                        SubjectConfirmationNameIdentifierWizardStepPanel.this.owner,
                        "EncryptedID Encryption Properties", true, encryptionConfigPanel);
                dlg.pack();
                com.l7tech.gui.util.Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.wasOKed()) {
                            try {
                                xmlElementEncryptionConfig = encryptionConfigPanel.getData();
                            } catch (AssertionPropertiesOkCancelSupport.ValidationException e1) {
                                // OkCancelDialog can only dismiss when doUpdateModel has successfully been called.
                                // If getData also throws it means the contract was broken.
                                throw new IllegalStateException("getData() should not throw after dialog is dismissed.");
                            }
                        }
                    }
                });
            }
        });

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        nameFormatsMap = new HashMap<String, JToggleButton>();
        nameFormatsMap.put(FAKE_URI_AUTOMATIC, automaticButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT, x509SubjectNameButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_EMAIL, emailAddressButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_WINDOWS, windowsDomainQualifiedNameButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, unspecifiedButton);
        nameFormatsMap.put(FAKE_URI_CUSTOM, customIdentifierButton);

        customIdentifierButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableDisable();
            }
        });

        enableDisable();

        for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(customFormatTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                if (component instanceof SquigglyTextField) {
                    final SquigglyTextField sqigglyField = (SquigglyTextField) component;
                    SquigglyFieldUtils.validateSquigglyFieldForUris(sqigglyField, false);
                    validateCustomFormatTextFieldNotEmpty();
                    notifyListeners();
                }
            }
        }, 300);
    }

    private boolean validateCustomFormatTextFieldNotEmpty() {
        boolean isValid = true;
        final String customText = customFormatTextField.getText();
        if (customText.isEmpty()) {
            customFormatTextField.setSquiggly();
            customFormatTextField.setModelessFeedback("A URI must be supplied");
            isValid =false;
        }
        return isValid;
    }

    private void enableDisable() {
        final boolean enable = includeNameCheckBox.isSelected();
        formatsButtonPanel.setEnabled(enable);
        if (enable) {
            enableForVersion(); // Only enable buttons appropriate for the selected version
        } else {
            for (JToggleButton tb : nameFormatsMap.values()) {
                tb.setEnabled(false); // Disable all buttons
            }
        }

        customFormatTextField.setEnabled(customIdentifierButton.isSelected());
        encryptNameIdentifierCheckBox.setEnabled(enable);
        configureEncryptionButton.setEnabled(enable && encryptNameIdentifierCheckBox.isSelected());

        nameQualifierTextField.setEnabled(enable);
        nameQualifierLabel.setEnabled(enable);

        valuePanel.setEnabled(enable);
        valueTextField.setEnabled(enable && valueSpecifiedRadio.isSelected());
        valueSpecifiedRadio.setEnabled(enable);
        valueFromCredsRadioButton.setEnabled(enable);
        valueFromUserRadioButton.setEnabled(enable);
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Name Identifier";
    }

    @Override
    public String getDescription() {
        return
        "<html>Specify one or more name formats that will be accepted by the gateway " +
          "and the optional subject name qualifier</html>";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one. At
     * least one name format must be selected
     *
     * @return true if the panel is valid, false otherwise
     */
    @Override
    public boolean canAdvance() {
        if (!includeNameCheckBox.isSelected()) return true;

        if (customIdentifierButton.isSelected()) {
            final String errorString = SquigglyFieldUtils.validateSquigglyFieldForUris(customFormatTextField, false);
            if (errorString != null) {
                return false;
            }

            final boolean isValid = validateCustomFormatTextFieldNotEmpty();
            if (!isValid) {
                return false;
            }
        }

        for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            if (jc.isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enable only the name identifiers that are applicable for a given saml version(s)
     */
    private void enableForVersion() {
        JToggleButton[] allFormats = nameFormatsMap.values().toArray(new JToggleButton[] {});
        if (version == 0 ||
            version == 2) {
            // enable all
            for (JToggleButton method : allFormats) {
                method.setEnabled(true);
            }
        } else if (version == 1) {
            HashMap<String, JToggleButton> v1Map = new HashMap<String, JToggleButton>(nameFormatsMap);
            v1Map.keySet().retainAll(Arrays.asList(SamlConstants.ALL_NAMEIDENTIFIERS));
            for (JToggleButton method : allFormats) {
                method.setEnabled(v1Map.containsValue(method));
            }
            JToggleButton autobutton = nameFormatsMap.get(FAKE_URI_AUTOMATIC);
            autobutton.setEnabled(true);
            JToggleButton customButton = nameFormatsMap.get(FAKE_URI_CUSTOM);
            customButton.setEnabled(true);
        }

        encryptNameIdentifierCheckBox.setVisible(version == 2);
        configureEncryptionButton.setVisible(version == 2);
    }
}
