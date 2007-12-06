/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.common.security.saml.KeyInfoInclusionType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The SAML Subject Confirmatioin selections <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class SubjectConfirmationWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JToggleButton confirmationSenderVouchesButton;
    private JToggleButton confirmationHolderOfKeyButton;
    private JToggleButton confirmationBearerButton;
    private JToggleButton confirmationNoneButton;
    private JCheckBox checkBoxSVMessageSignature;
    private JCheckBox checkBoxHoKMessageSignature;
    private JPanel confirmationMethodsPanel;
    private JLabel extraTextLabel;
    private JPanel subjectCertPanel;
    private JRadioButton subjectCertThumbprintRadioButton;
    private JRadioButton subjectCertLiteralRadioButton;
    private JCheckBox subjectCertIncludeCheckbox;
    private JRadioButton subjectCertSkiRadioButton;

    private Map<String, JToggleButton> confirmationsMap;
    private boolean showTitleLabel;

    private final boolean issueMode;
    private final ActionListener enableDisableListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableDisable();
        }
    };

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        this(next, true, issueMode);
    }


    /**
     * Creates new form Subject confirmation WizardPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public SubjectConfirmationWizardStepPanel(WizardStepPanel next) {
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
        if (issueMode) {
            SamlIssuerAssertion sia = (SamlIssuerAssertion) settings;
            final SubjectStatement.Confirmation confirmation = SubjectStatement.Confirmation.forUri(sia.getSubjectConfirmationMethodUri());
            if (confirmation == SubjectStatement.HOLDER_OF_KEY) {
                confirmationHolderOfKeyButton.setSelected(true);
            } else if (confirmation == SubjectStatement.SENDER_VOUCHES) {
                confirmationSenderVouchesButton.setSelected(true);
            } else if (confirmation == SubjectStatement.BEARER) {
                confirmationBearerButton.setSelected(true);
            } else {
                confirmationNoneButton.setSelected(true);
            }

            KeyInfoInclusionType ckitype = sia.getSubjectConfirmationKeyInfoType();
            if (ckitype == null) ckitype = KeyInfoInclusionType.CERT;
            switch(ckitype) {
                case NONE:
                    subjectCertIncludeCheckbox.setSelected(false);
                    break;
                case CERT:
                    subjectCertIncludeCheckbox.setSelected(true);
                    subjectCertLiteralRadioButton.setSelected(true);
                    break;
                case STR_SKI:
                    subjectCertIncludeCheckbox.setSelected(true);
                    subjectCertSkiRadioButton.setSelected(true);
                    break;
                case STR_THUMBPRINT:
                    subjectCertIncludeCheckbox.setSelected(true);
                    subjectCertThumbprintRadioButton.setSelected(true);
                    break;
                default:
                    throw new RuntimeException("Unsupported Subject KeyInfoInclusionType: " + ckitype); // Can't happen
            }
        } else {
            RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
            requestWssSaml.getSubjectConfirmations();
            for (JToggleButton jToggleButton : confirmationsMap.values()) {
                jToggleButton.setSelected(false);
            }
            String[] confirmations = requestWssSaml.getSubjectConfirmations();
            for (String confirmation : confirmations) {
                JToggleButton jc = confirmationsMap.get(confirmation);
                if (jc == null) {
                    throw new IllegalArgumentException("No widget for confirmation " + confirmation);
                }
                jc.setSelected(true);
            }
            checkBoxSVMessageSignature.setSelected(requestWssSaml.isRequireSenderVouchesWithMessageSignature());
            checkBoxSVMessageSignature.setEnabled(confirmationSenderVouchesButton.isSelected());
            checkBoxHoKMessageSignature.setSelected(requestWssSaml.isRequireHolderOfKeyWithMessageSignature());
            checkBoxHoKMessageSignature.setEnabled(confirmationHolderOfKeyButton.isSelected());
            confirmationNoneButton.setSelected(requestWssSaml.isNoSubjectConfirmation());
        }
        enableDisable();
    }

    private void enableDisable() {
        boolean subjectCertEnabled = issueMode && subjectCertIncludeCheckbox.isSelected() && confirmationHolderOfKeyButton.isSelected();
        boolean subjectCertVisible = issueMode;

        subjectCertIncludeCheckbox.setEnabled(confirmationHolderOfKeyButton.isSelected());

        subjectCertLiteralRadioButton.setEnabled(subjectCertEnabled);
        subjectCertSkiRadioButton.setEnabled(subjectCertEnabled);
        subjectCertThumbprintRadioButton.setEnabled(subjectCertEnabled);

        if (subjectCertEnabled &&
                !(subjectCertLiteralRadioButton.isSelected() ||
                  subjectCertSkiRadioButton.isSelected() ||
                  subjectCertThumbprintRadioButton.isSelected())
                )
        {
            // Bug 4304 -- ensure at least one radio buttion is selected
            subjectCertLiteralRadioButton.setSelected(true);
        }

        subjectCertPanel.setVisible(subjectCertVisible);
        subjectCertIncludeCheckbox.setVisible(subjectCertVisible);
        subjectCertLiteralRadioButton.setVisible(subjectCertVisible);
        subjectCertSkiRadioButton.setVisible(subjectCertVisible);
        subjectCertThumbprintRadioButton.setVisible(subjectCertVisible);
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
        if (issueMode) {
            SamlIssuerAssertion assertion = (SamlIssuerAssertion) settings;
            if (confirmationHolderOfKeyButton.isSelected()) {
                assertion.setSubjectConfirmationMethodUri(SubjectStatement.HOLDER_OF_KEY.getUri());
            } else if (confirmationSenderVouchesButton.isSelected()) {
                assertion.setSubjectConfirmationMethodUri(SubjectStatement.SENDER_VOUCHES.getUri());
            } else if (confirmationBearerButton.isSelected()) {
                assertion.setSubjectConfirmationMethodUri(SubjectStatement.BEARER.getUri());
            } else if (confirmationNoneButton.isSelected()) {
                assertion.setSubjectConfirmationMethodUri(null);
            }

            if (subjectCertIncludeCheckbox.isSelected()) {
                if (subjectCertLiteralRadioButton.isSelected()) {
                    assertion.setSubjectConfirmationKeyInfoType(KeyInfoInclusionType.CERT);
                } else if (subjectCertSkiRadioButton.isSelected()) {
                    assertion.setSubjectConfirmationKeyInfoType(KeyInfoInclusionType.STR_SKI);
                } else if (subjectCertThumbprintRadioButton.isSelected()) {
                    assertion.setSubjectConfirmationKeyInfoType(KeyInfoInclusionType.STR_THUMBPRINT);
                } else {
                    throw new RuntimeException("No Subject Cert radio button selected"); //Can't happen
                }
            } else {
                assertion.setSubjectConfirmationKeyInfoType(KeyInfoInclusionType.NONE);
            }
        } else {
            RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
            Collection<String> confirmations = new ArrayList<String>();
            for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isSelected()) {
                    confirmations.add(entry.getKey());
                }
            }
            requestWssSaml.setSubjectConfirmations(confirmations.toArray(new String[0]));
            requestWssSaml.setRequireHolderOfKeyWithMessageSignature(checkBoxHoKMessageSignature.isSelected());
            requestWssSaml.setRequireSenderVouchesWithMessageSignature(checkBoxSVMessageSignature.isSelected());
            requestWssSaml.setNoSubjectConfirmation(confirmationNoneButton.isSelected());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (issueMode) {
            confirmationHolderOfKeyButton = new JRadioButton("Holder of Key");
            confirmationSenderVouchesButton = new JRadioButton("Sender Vouches");
            confirmationBearerButton = new JRadioButton("Bearer");
            confirmationNoneButton = new JRadioButton("None");

            ButtonGroup radios = new ButtonGroup();
            radios.add(confirmationHolderOfKeyButton);
            radios.add(confirmationSenderVouchesButton);
            radios.add(confirmationBearerButton);
            radios.add(confirmationNoneButton);

            checkBoxHoKMessageSignature = new JCheckBox("not shown");
            checkBoxSVMessageSignature = new JCheckBox("not shown");
            extraTextLabel.setText("Issue an assertion with the following Subject Confirmation Method");

            subjectCertIncludeCheckbox.addActionListener(enableDisableListener);
            confirmationHolderOfKeyButton.addActionListener(enableDisableListener);
            confirmationSenderVouchesButton.addActionListener(enableDisableListener);
            confirmationBearerButton.addActionListener(enableDisableListener);
            confirmationNoneButton.addActionListener(enableDisableListener);
        } else {
            confirmationHolderOfKeyButton = new JCheckBox("Holder of Key");
            confirmationSenderVouchesButton = new JCheckBox("Sender Vouches");
            confirmationBearerButton = new JCheckBox("Bearer");
            confirmationNoneButton = new JCheckBox("None");

            checkBoxHoKMessageSignature = new JCheckBox("Require Message Signature");
            checkBoxSVMessageSignature = new JCheckBox("Require Message Signature");
        }

        confirmationMethodsPanel.add(confirmationHolderOfKeyButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        confirmationMethodsPanel.add(confirmationSenderVouchesButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        confirmationMethodsPanel.add(confirmationBearerButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        confirmationMethodsPanel.add(confirmationNoneButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        if (!issueMode) {
            confirmationMethodsPanel.add(checkBoxHoKMessageSignature, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
            confirmationMethodsPanel.add(checkBoxSVMessageSignature, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        String toolTipPRoofOfPosession = "<html>Require the Proof Of Possession -  Signed Message Body" +
          "<br>Alternatively, Proof of Possession can be secured with SSL Client Certificate by using SSL transport.</html>";

        confirmationHolderOfKeyButton.setToolTipText("<html>Key Info for the Subject, that the Assertion describes<br>" +
          " MUST be present within the Subject Confirmation.</html>");

        confirmationHolderOfKeyButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                checkBoxHoKMessageSignature.setEnabled(confirmationHolderOfKeyButton.isSelected());
            }
        });

        checkBoxHoKMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        confirmationSenderVouchesButton.setToolTipText("<html>The attesting entity, different form the subject,<br>" +
          " vouches for the verification of the subject.</html>");
        confirmationSenderVouchesButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                checkBoxSVMessageSignature.setEnabled(confirmationSenderVouchesButton.isSelected());
            }
        });
        confirmationBearerButton.setToolTipText("<html>Browser/POST Profile of SAML</html>");

        confirmationNoneButton.setToolTipText("<html>No Subject Confirmation MUST be present</html>");
        confirmationsMap = new HashMap<String, JToggleButton>();
        confirmationsMap.put(SamlConstants.CONFIRMATION_HOLDER_OF_KEY, confirmationHolderOfKeyButton);
        confirmationsMap.put(SamlConstants.CONFIRMATION_SENDER_VOUCHES, confirmationSenderVouchesButton);
        confirmationsMap.put(SamlConstants.CONFIRMATION_BEARER, confirmationBearerButton);

        checkBoxSVMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
        confirmationNoneButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                notifyListeners();
            }
        });
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Subject Confirmation";
    }

    public String getDescription() {
        if (!issueMode) {
            return "<html>Specify one or more subject confirmations that will be accepted by the gateway" +
                   " and whether the message signature is required as the proof material</html>";
        } else {
            return "<html>Specify the subject confirmation method that will be included in the assertion.</html>";
        }
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        if (confirmationNoneButton.isSelected()) {
            return true;
        }

        for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            if (jc.isSelected()) {
                return true;
            }
        }

        return confirmationNoneButton.isSelected();
    }
}