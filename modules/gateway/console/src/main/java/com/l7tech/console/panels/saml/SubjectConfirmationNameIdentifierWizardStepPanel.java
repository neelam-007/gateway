/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.SamlElementGenericConfig;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;

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

    private static final String AUTOMATIC = "Automatic";
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
        @Override
        public void actionPerformed(ActionEvent e) {
            enableDisable();
        }
    };
    private static final String FAKE_URI_AUTOMATIC = "urn:l7tech.com:automatic";

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
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        final SamlPolicyAssertion assertion = (SamlPolicyAssertion) settings;
        version = assertion.getVersion() == null ? 1 : assertion.getVersion();
        enableForVersion();
        nameQualifierTextField.setText(assertion.getNameQualifier());

        if (issueMode) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) settings;
            NameIdentifierInclusionType type = issuerConfiguration.getNameIdentifierType();
            if (type == null) {
                if (issuerConfiguration.getNameIdentifierValue() != null) {
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
                    valueTextField.setText(issuerConfiguration.getNameIdentifierValue());
                    break;
            }

            boolean chose = false;
            for (String formatUri : nameFormatsMap.keySet()) {
                if (formatUri.equals(issuerConfiguration.getNameIdentifierFormat())) {
                    nameFormatsMap.get(formatUri).setSelected(true);
                    chose = true;
                    break;
                }
            }

            if (!chose) nameFormatsMap.get(FAKE_URI_AUTOMATIC).setSelected(true);

            enableDisable();
        } else {
            RequireSaml requestSaml = (RequireSaml)settings;
            for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isEnabled())
                    jc.setSelected(false);
            }
            String[] formats = requestSaml.getNameFormats();
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
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        ((SamlPolicyAssertion) settings).setNameQualifier(nameQualifierTextField.getText());

        if (issueMode) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) settings;
            if (includeNameCheckBox.isSelected()) {
                for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                    JToggleButton jc = entry.getValue();
                    if (jc.isSelected() && jc.isEnabled()) {
                        final String uri = entry.getKey();
                        issuerConfiguration.setNameIdentifierFormat(FAKE_URI_AUTOMATIC.equals(uri) ? null : uri);
                        break;
                    }
                }
                if (valueFromCredsRadioButton.isSelected()) {
                    issuerConfiguration.setNameIdentifierType(NameIdentifierInclusionType.FROM_CREDS);
                } else if (valueFromUserRadioButton.isSelected()) {
                    issuerConfiguration.setNameIdentifierType(NameIdentifierInclusionType.FROM_USER);
                } else if (valueSpecifiedRadio.isSelected()) {
                    issuerConfiguration.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
                    issuerConfiguration.setNameIdentifierValue(valueTextField.getText());
                } else {
                    throw new RuntimeException("No NameIdentifier value radio button selected"); // Can't happen
                }
            } else {
                issuerConfiguration.setNameIdentifierType(NameIdentifierInclusionType.NONE);
                issuerConfiguration.setNameIdentifierFormat(null);
                issuerConfiguration.setNameIdentifierValue(null);
            }
        } else {
            RequireSaml requestSaml = (RequireSaml)settings;
            Collection<String> formats = new ArrayList<String>();
            for (Map.Entry<String, JToggleButton> entry : nameFormatsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isSelected() && jc.isEnabled()) {
                    formats.add(entry.getKey());
                }
            }
            requestSaml.setNameFormats(formats.toArray(new String[]{}));
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToggleButton automaticButton = null;
        JToggleButton x509SubjectNameButton;
        JToggleButton emailAddressButton;
        JToggleButton windowsDomainQualifiedNameButton;
        JToggleButton unspecifiedButton;
        JToggleButton kerberosButton;
        JToggleButton entityIdentifierButton;
        JToggleButton persistentIdentifierButton;
        JToggleButton transientIdentifierButton;

        if (issueMode) {
            automaticButton = new JRadioButton(AUTOMATIC);
            x509SubjectNameButton = new JRadioButton(X_509_SUBJECT_NAME);
            emailAddressButton = new JRadioButton(EMAIL_ADDRESS);
            windowsDomainQualifiedNameButton = new JRadioButton(WINDOWS_DOMAIN_QUALIFIED_NAME);
            unspecifiedButton = new JRadioButton(UNSPECIFIED);
            kerberosButton = null;
            entityIdentifierButton = null;
            persistentIdentifierButton = null;
            transientIdentifierButton = null;

            ButtonGroup bg = new ButtonGroup();
            bg.add(automaticButton);
            bg.add(x509SubjectNameButton);
            bg.add(emailAddressButton);
            bg.add(windowsDomainQualifiedNameButton);
            bg.add(unspecifiedButton);

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

        RC rc = new RC(0,0);
        addButton(automaticButton, rc);
        addButton(x509SubjectNameButton, rc);
        addButton(emailAddressButton, rc);
        addButton(windowsDomainQualifiedNameButton, rc);
        addButton(kerberosButton, rc);
        addButton(entityIdentifierButton, rc);
        addButton(persistentIdentifierButton, rc);
        addButton(transientIdentifierButton, rc);
        addButton(unspecifiedButton, rc);

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        nameFormatsMap = new HashMap<String, JToggleButton>();
        addNameFormat(FAKE_URI_AUTOMATIC, automaticButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT, x509SubjectNameButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_EMAIL, emailAddressButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_WINDOWS, windowsDomainQualifiedNameButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, unspecifiedButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_KERBEROS, kerberosButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_ENTITY, entityIdentifierButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_PERSISTENT, persistentIdentifierButton);
        addNameFormat(SamlConstants.NAMEIDENTIFIER_TRANSIENT, transientIdentifierButton);

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
    }

    private void addNameFormat(String uri, JToggleButton automaticButton) {
        if (automaticButton != null) nameFormatsMap.put(uri, automaticButton);
    }

    private static class RC {
        int row, col;

        public RC(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private void addButton(JToggleButton tb, RC rc) {
        if (tb == null) return;

        formatsButtonPanel.add(tb, new GridBagConstraints(rc.col, rc.row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        if (rc.row == 4) {
            rc.row = 0;
            rc.col++;
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
     * @return true if the panel is valid, false otherwis
     */
    @Override
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
            JToggleButton autobutton = nameFormatsMap.get(FAKE_URI_AUTOMATIC);
            if (autobutton != null) autobutton.setEnabled(true);
        }
    }
}
