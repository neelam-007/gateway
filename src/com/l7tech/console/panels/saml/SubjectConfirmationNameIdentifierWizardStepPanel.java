/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.common.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
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
    private JTextField textFieldNameQualifier;
    private JCheckBox checkBoxFormatX509SubjectName;
    private JCheckBox checkBoxEmailAddress;
    private JCheckBox checkBoxWindowsDomainQualifiedName;
    private JCheckBox checkBoxUnspecified;
    private HashMap nameFormatsMap;
    private boolean showTitleLabel;

    /**
     * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
      * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
      */
     public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next) {
         this(next, true);
     }

    /**
     * Creates new form SubjectConfirmationNameIdentifierWizardStepPanel
     */
    public SubjectConfirmationNameIdentifierWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
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
        requestWssSaml.setNameQualifier(textFieldNameQualifier.getText());
        for (Iterator iterator = nameFormatsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            jc.setSelected(false);
        }
        String[] formats = requestWssSaml.getNameFormats();
        for (int i = 0; i < formats.length; i++) {
            String format = formats[i];
            JCheckBox jc = (JCheckBox)nameFormatsMap.get(format);
            if (jc == null) {
                throw new IllegalArgumentException("No widget corresponds to format " + format);
            }
            jc.setSelected(true);
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
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;
        Collection formats = new ArrayList();
        for (Iterator iterator = nameFormatsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                formats.add(entry.getKey().toString());
            }
        }
        requestWssSaml.setNameFormats((String[])formats.toArray(new String[]{}));
        requestWssSaml.setNameQualifier(textFieldNameQualifier.getText());
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
        nameFormatsMap = new HashMap();
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT, checkBoxFormatX509SubjectName);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_EMAIL, checkBoxEmailAddress);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_WINDOWS, checkBoxWindowsDomainQualifiedName);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, checkBoxUnspecified);
        for (Iterator iterator = nameFormatsMap.entrySet().iterator(); iterator.hasNext();) {
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
        for (Iterator iterator = nameFormatsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                return true;
            }
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