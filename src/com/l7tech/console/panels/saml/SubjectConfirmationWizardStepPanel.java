/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JCheckBox checkBoxSVMessageSignature;
    private Map confirmationsMap;
    private JCheckBox checkBoxNoSubjectConfirmation;
    private boolean showTitleLabel;
    private JCheckBox checkBoxHoKMessageSignature;

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }


    /**
     * Creates new form Subject confirmation WizardPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
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
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
        requestWssSaml.getSubjectConfirmations();
        for (Iterator iterator = confirmationsMap.values().iterator(); iterator.hasNext();) {
            JCheckBox jCheckBox = (JCheckBox)iterator.next();
            jCheckBox.setSelected(false);
        }
        String[] confirmations = requestWssSaml.getSubjectConfirmations();
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
            JCheckBox jc = (JCheckBox)confirmationsMap.get(confirmation);
            if (jc == null) {
                throw new IllegalArgumentException("No widget for confirmation " + confirmation);
            }
            jc.setSelected(true);
        }
        checkBoxSVMessageSignature.setSelected(requestWssSaml.isRequireSenderVouchesWithMessageSignature());
        checkBoxSVMessageSignature.setEnabled(checkBoxSenderVouches.isSelected());
        checkBoxHoKMessageSignature.setSelected(requestWssSaml.isRequireHolderOfKeyWithMessageSignature());
        checkBoxHoKMessageSignature.setEnabled(checkBoxHolderOfKey.isSelected());
        checkBoxNoSubjectConfirmation.setSelected(requestWssSaml.isNoSubjectConfirmation());
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
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
        Collection confirmations = new ArrayList();
        for (Iterator iterator = confirmationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                confirmations.add(entry.getKey().toString());
            }
        }
        requestWssSaml.setSubjectConfirmations((String[])confirmations.toArray(new String[]{}));
        requestWssSaml.setRequireHolderOfKeyWithMessageSignature(checkBoxHoKMessageSignature.isSelected());
        requestWssSaml.setRequireSenderVouchesWithMessageSignature(checkBoxSVMessageSignature.isSelected());
        requestWssSaml.setNoSubjectConfirmation(checkBoxNoSubjectConfirmation.isSelected());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        String toolTipPRoofOfPosession = "<html>Require the Proof Of Posession -  Signed Message Body" +
          "<br>Alternatively, Proof of Possession can be secured with SSL Client Certificate by using SSL transport.</html>";

        checkBoxHolderOfKey.setToolTipText("<html>Key Info for the Subject, that the Assertion describes<br>" +
          " MUST be present within the Subject Confirmation.</html>");

        checkBoxHolderOfKey.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                checkBoxHoKMessageSignature.setEnabled(checkBoxHolderOfKey.isSelected());
            }
        });
        checkBoxHoKMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        checkBoxSenderVouches.setToolTipText("<html>The attesting entity, different form the subject,<br>" +
          " vouches for the verification of the subject.</html>");
        checkBoxSenderVouches.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                checkBoxSVMessageSignature.setEnabled(checkBoxSenderVouches.isSelected());
            }
        });
        checkBoxBearer.setToolTipText("<html>Browser/POST Profile of SAML</html>");

        checkBoxNoSubjectConfirmation.setToolTipText("<html>No Subject Confirmation MUST be present</html>");
        confirmationsMap = new HashMap();
        confirmationsMap.put(SamlConstants.CONFIRMATION_HOLDER_OF_KEY, checkBoxHolderOfKey);
        confirmationsMap.put(SamlConstants.CONFIRMATION_SENDER_VOUCHES, checkBoxSenderVouches);
        confirmationsMap.put(SamlConstants.CONFIRMATION_BEARER, checkBoxBearer);

        checkBoxSVMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        for (Iterator iterator = confirmationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
        checkBoxNoSubjectConfirmation.addChangeListener(new ChangeListener() {
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
        return
        "<html>Specify one or more subject confirmations that will be accepted by the gateway" +
          " and whether the message signature is required as the proof material</html>";
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
        if (checkBoxNoSubjectConfirmation.isSelected()) {
            return true;
        }
        return false;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }
}