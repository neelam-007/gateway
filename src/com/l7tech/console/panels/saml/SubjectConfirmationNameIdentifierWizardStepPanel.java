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
    private JCheckBox checkBoxKerberos;
    private JCheckBox checkBoxEntityIdentifier;
    private JCheckBox checkBoxPersistentIdentifier;
    private JCheckBox checkBoxTransientIdentifier;

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
        final String nameQualifier = requestWssSaml.getNameQualifier();
        if (nameQualifier !=null) {
            textFieldNameQualifier.setText(nameQualifier);
        }
        enableForVersion(requestWssSaml.getVersion()==null ? 1 : requestWssSaml.getVersion().intValue());
        for (Iterator iterator = nameFormatsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isEnabled())
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
            if (jc.isSelected() && jc.isEnabled()) {
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
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_KERBEROS, checkBoxKerberos);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_ENTITY, checkBoxEntityIdentifier);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_PERSISTENT, checkBoxPersistentIdentifier);
        nameFormatsMap.put(SamlConstants.NAMEIDENTIFIER_TRANSIENT, checkBoxTransientIdentifier);

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
     * Enable only the name identifiers that are applicable for a given saml version(s)
     */
    private void enableForVersion(int samlVersion) {
        JCheckBox[] allFormats = (JCheckBox[]) nameFormatsMap.values().toArray(new JCheckBox[] {});
        if (samlVersion == 0 ||
            samlVersion == 2) {
            // enable all
            for (int i = 0; i < allFormats.length; i++) {
                JCheckBox method = allFormats[i];
                method.setEnabled(true);
            }
        }
        else if (samlVersion == 1) {
            HashMap v1Map = new HashMap(nameFormatsMap);
            v1Map.keySet().retainAll(Arrays.asList(SamlConstants.ALL_NAMEIDENTIFIERS));
            for (int i = 0; i < allFormats.length; i++) {
                JCheckBox method = allFormats[i];
                method.setEnabled(v1Map.containsValue(method));
            }
        }
    }
}