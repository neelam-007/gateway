/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
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
public class SubjectConfirmationWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JCheckBox checkBoxSenderVouches;
    private JCheckBox checkBoxHolderOfKey;
    private JCheckBox checkBoxBearer;
    private JCheckBox checkBoxRequireProofOfPosession;
    private Map confirmationsMap;
    private JCheckBox checkBoxNoSubjectConfirmation;
    private boolean proofOfPossesionManuallyDisabled = false;

    /**
     * Creates new form WizardPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();
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
        SamlStatementAssertion statement = (SamlStatementAssertion)settings;
        statement.getSubjectConfirmations();
        for (Iterator iterator = confirmationsMap.values().iterator(); iterator.hasNext();) {
            JCheckBox jCheckBox = (JCheckBox)iterator.next();
            jCheckBox.setSelected(false);
        }
        String[] confirmations = statement.getSubjectConfirmations();
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
            JCheckBox jc = (JCheckBox)confirmationsMap.get(confirmation);
            if (jc == null) {
                throw new IllegalArgumentException("No widget for confirmation " + confirmation);
            }
            jc.setSelected(true);
        }
        checkBoxRequireProofOfPosession.setSelected(statement.isRequireProofOfPosession());
        checkBoxNoSubjectConfirmation.setSelected(statement.isNoSubjectConfirmation());
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
        SamlStatementAssertion statement = (SamlStatementAssertion)settings;
        Collection confirmations = new ArrayList();
        for (Iterator iterator = confirmationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                confirmations.add(entry.getKey().toString());
            }
        }
        statement.setSubjectConfirmations((String[])confirmations.toArray(new String[]{}));
        statement.setRequireProofOfPosession(checkBoxRequireProofOfPosession.isSelected());
        statement.setNoSubjectConfirmation(checkBoxNoSubjectConfirmation.isSelected());
    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        checkBoxHolderOfKey.setToolTipText("<html>Key Info for the Subject, that the Assertion describes<br>" +
                                             " MUST be present within the Subject Confirmation.</html>");

        checkBoxHolderOfKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!proofOfPossesionManuallyDisabled) { //if the uses did not diable explicitely
                    checkBoxRequireProofOfPosession.setSelected(true);
                }
            }
        });
        checkBoxSenderVouches.setToolTipText("<html>The attesting entity, different form the subject,<br>" +
                                               " vouches for the verification of the subject.</html>");
        checkBoxBearer.setToolTipText("<html>Browser/POST Profile of SAML</html>");

        checkBoxNoSubjectConfirmation.setToolTipText("<html>No Subject Confirmation MUST be present</html>");
        confirmationsMap = new HashMap();
        confirmationsMap.put(SamlConstants.CONFIRMATION_HOLDER_OF_KEY, checkBoxHolderOfKey);
        confirmationsMap.put(SamlConstants.CONFIRMATION_SENDER_VOUCHES, checkBoxSenderVouches);
        confirmationsMap.put(SamlConstants.CONFIRMATION_BEARER, checkBoxBearer);

        checkBoxRequireProofOfPosession.setToolTipText("<html>Require the message signature that provides the proof material</html>");

        checkBoxRequireProofOfPosession.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!checkBoxRequireProofOfPosession.isSelected()) {
                    proofOfPossesionManuallyDisabled = true;
                }
            }
        });

        for (Iterator iterator = confirmationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Subject Confirmation";
    }

    public String getDescription() {
        return
        "<html>Wpecify one or more subject confirmations that will be accepted by the gateway<br>" +
          "and whether the message signature is required as the proof material</html>";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        if (checkBoxNoSubjectConfirmation.isSelected()) {
            return true;
        }

        for (Iterator iterator = confirmationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                return true;
            }
        }
        return false;
    }
}