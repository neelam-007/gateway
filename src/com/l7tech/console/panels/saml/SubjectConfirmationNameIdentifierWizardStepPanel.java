/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.common.security.saml.NameIdentifierInclusionType;
import com.l7tech.common.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * The SAML Subject Confirmatioin selections <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class SubjectConfirmationNameIdentifierWizardStepPanel extends WizardStepPanel {
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

    private Map<String, JToggleButton> nameFormatsMap;
    private boolean showTitleLabel;
    private final boolean issueMode;

    private static final String X_509_SUBJECT_NAME = "X.509 Subject Name";
    private static final String EMAIL_ADDRESS = "Email Address";
    private static final String WINDOWS_DOMAIN_QUALIFIED_NAME = "Windows Domain Qualified Name";
    private static final String KERBEROS_PRINCIPAL_NAME = "Kerberos Principal Name";
    private static final String ENTITY_IDENTIFIER = "Entity Identifier";
    private static final String PERSISTENT_IDENTIFIER = "Persistent Identifier";
    private static final String TRANSIENT_IDENTIFIER = "Transient Identifier";
    private static final String UNSPECIFIED = "Unspecified";
    private int version;
    private final ActionListener enableDisableListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableDisable();
        }
    };

    /**
     * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
      * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
      */
     public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean issueMode) {
         this(next, true, issueMode);
     }

    /**
     * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next) {
        this(next, true, false);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        final SamlPolicyAssertion assertion = (SamlPolicyAssertion) settings;
        version = assertion.getVersion() == null ? 1 : assertion.getVersion();
        enableForVersion();
        String nq = assertion.getNameQualifier();
        if (nq != null) nameQualifierTextField.setText(nq);

        if (issueMode) {
            SamlIssuerAssertion sia = (SamlIssuerAssertion) settings;
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

            for (String formatUri : nameFormatsMap.keySet()) {
                if (formatUri.equals(sia.getNameIdentifierFormat())) {
                    nameFormatsMap.get(formatUri).setSelected(true);
                    break;
                }
            }

            enableDisable();
        } else {
            RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
            for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isEnabled())
                    jc.setSelected(false);
            }
            String[] formats = requestWssSaml.getNameFormats();
            for (String format : formats) {
                JToggleButton jc = nameFormatsMap.get(format);
                if (jc == null) {
                    throw new IllegalArgumentException("No widget corresponds to format " + format);
                }
                jc.setSelected(true);
            }
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        String nq = nameQualifierTextField.getText();
        if (nq != null && nq.length() > 0) ((SamlPolicyAssertion) settings).setNameQualifier(nq);

        if (issueMode) {
            SamlIssuerAssertion sia = (SamlIssuerAssertion) settings;
            if (includeNameCheckBox.isSelected()) {
                for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                    JToggleButton jc = entry.getValue();
                    if (jc.isSelected() && jc.isEnabled()) {
                        sia.setNameIdentifierFormat(entry.getKey());
                        break;
                    }
                }
                if (valueFromCredsRadioButton.isSelected()) {
                    sia.setNameIdentifierType(NameIdentifierInclusionType.FROM_CREDS);
                } else if (valueFromUserRadioButton.isSelected()) {
                    sia.setNameIdentifierType(NameIdentifierInclusionType.FROM_USER);
                } else if (valueSpecifiedRadio.isSelected()) {
                    sia.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
                    sia.setNameIdentifierValue(valueTextField.getText());
                } else {
                    throw new RuntimeException("No NameIdentifier value radio button selected"); // Can't happen
                }
            } else {
                sia.setNameIdentifierType(NameIdentifierInclusionType.NONE);
                sia.setNameIdentifierFormat(null);
                sia.setNameIdentifierValue(null);
            }
        } else {
            RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
            Collection<String> formats = new ArrayList<String>();
            for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isSelected() && jc.isEnabled()) {
                    formats.add(entry.getKey());
                }
            }
            requestWssSaml.setNameFormats(formats.toArray(new String[]{}));
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToggleButton x509SubjectNameButton;
        JToggleButton emailAddressButton;
        JToggleButton windowsDomainQualifiedNameButton;
        JToggleButton unspecifiedButton;
        JToggleButton kerberosButton;
        JToggleButton entityIdentifierButton;
        JToggleButton persistentIdentifierButton;
        JToggleButton transientIdentifierButton;

        if (issueMode) {
            x509SubjectNameButton = new JRadioButton(X_509_SUBJECT_NAME);
            emailAddressButton = new JRadioButton(EMAIL_ADDRESS);
            windowsDomainQualifiedNameButton = new JRadioButton(WINDOWS_DOMAIN_QUALIFIED_NAME);
            kerberosButton = new JRadioButton(KERBEROS_PRINCIPAL_NAME);
            entityIdentifierButton = new JRadioButton(ENTITY_IDENTIFIER);
            persistentIdentifierButton = new JRadioButton(PERSISTENT_IDENTIFIER);
            transientIdentifierButton = new JRadioButton(TRANSIENT_IDENTIFIER);
            unspecifiedButton = new JRadioButton(UNSPECIFIED);

            ButtonGroup bg = new ButtonGroup();
            bg.add(x509SubjectNameButton);
            bg.add(emailAddressButton);
            bg.add(windowsDomainQualifiedNameButton);
            bg.add(kerberosButton);
            bg.add(entityIdentifierButton);
            bg.add(persistentIdentifierButton);
            bg.add(transientIdentifierButton);
            bg.add(unspecifiedButton);

            includeNameCheckBox.addActionListener(enableDisableListener);
            valueFromCredsRadioButton.addActionListener(enableDisableListener);
            valueFromUserRadioButton.addActionListener(enableDisableListener);
            valueSpecifiedRadio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    enableDisableListener.actionPerformed(e);
                    valueTextField.requestFocus();
                }
            });
        } else {
            x509SubjectNameButton = new JCheckBox(X_509_SUBJECT_NAME);
            emailAddressButton = new JCheckBox(EMAIL_ADDRESS);
            windowsDomainQualifiedNameButton = new JCheckBox(WINDOWS_DOMAIN_QUALIFIED_NAME);
            kerberosButton = new JCheckBox(KERBEROS_PRINCIPAL_NAME);
            entityIdentifierButton = new JCheckBox(ENTITY_IDENTIFIER);
            persistentIdentifierButton = new JCheckBox(PERSISTENT_IDENTIFIER);
            transientIdentifierButton = new JCheckBox(TRANSIENT_IDENTIFIER);
            unspecifiedButton = new JCheckBox(UNSPECIFIED);

            includeNameCheckBox.setVisible(false);
            valuePanel.setVisible(false);
            valueTextField.setVisible(false);
            valueSpecifiedRadio.setVisible(false);
            valueFromCredsRadioButton.setVisible(false);
            valueFromUserRadioButton.setVisible(false);
        }

        formatsButtonPanel.add(x509SubjectNameButton, constraints(0, 0));
        formatsButtonPanel.add(emailAddressButton, constraints(0, 1));
        formatsButtonPanel.add(windowsDomainQualifiedNameButton, constraints(0, 2));
        formatsButtonPanel.add(kerberosButton, constraints(0, 3));
        formatsButtonPanel.add(entityIdentifierButton, constraints(1, 0));
        formatsButtonPanel.add(persistentIdentifierButton, constraints(1, 1));
        formatsButtonPanel.add(transientIdentifierButton, constraints(1, 2));
        formatsButtonPanel.add(unspecifiedButton, constraints(1, 3));

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        nameFormatsMap = new HashMap<String, JToggleButton>();
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT, x509SubjectNameButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_EMAIL, emailAddressButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_WINDOWS, windowsDomainQualifiedNameButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, unspecifiedButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_KERBEROS, kerberosButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_ENTITY, entityIdentifierButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_PERSISTENT, persistentIdentifierButton);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_TRANSIENT, transientIdentifierButton);

        enableDisable();

        for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
    }

    private void enableDisable() {
        if (!issueMode) return;
        final boolean enable = includeNameCheckBox.isSelected();
        formatsButtonPanel.setEnabled(enable);
        if (enable) {
            enableForVersion(); // Only enable buttons appropriate for the selected version
        } else {
            for (JToggleButton tb : nameFormatsMap.values()) {
                tb.setEnabled(false); // Disable all buttons
            }
        }

        nameQualifierTextField.setEnabled(enable);
        nameQualifierLabel.setEnabled(enable);

        valuePanel.setEnabled(enable);
        valueTextField.setEnabled(enable && valueSpecifiedRadio.isSelected());
        valueSpecifiedRadio.setEnabled(enable);
        valueFromCredsRadioButton.setEnabled(enable);
        valueFromUserRadioButton.setEnabled(enable);
    }

    private GridBagConstraints constraints(int gridx, int gridy) {
        return new GridBagConstraints(gridx, gridy, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Name Identifier";
    }

    public String getDescription() {
        return
        "<html>Specify one or more name formats that will be accepted by the gateway " +
          "and the optional subject name qualifier</html>";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one. At
     * least one name format must be selected
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        if (issueMode && !includeNameCheckBox.isSelected()) return true;
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
        }
    }
}