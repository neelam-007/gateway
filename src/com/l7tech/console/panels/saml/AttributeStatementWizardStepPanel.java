/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class AttributeStatementWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JScrollPane attributeTableScrollPane;
    private JTable attributeTable;
    private JButton addAttributeButton;
    private JButton removeButton;
    private JButton editButton;
    private DefaultTableModel attributesTableModel;
    /**
     * Creates new form WizardPanel
     */
    public AttributeStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form WizardPanel
     */
    public AttributeStatementWizardStepPanel(WizardStepPanel next) {
        this(next, true);
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
        SamlAttributeStatement statement = (SamlAttributeStatement)settings;
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlAttributeStatement statement = (SamlAttributeStatement)settings;
        // put in table
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
        attributesTableModel = new DefaultTableModel( new String[] {"Name", "Namespace", "Value"}, 0);
        attributeTable.setModel(attributesTableModel);

    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Attribute Statement";
    }


    public String getDescription() {
        return
        "<html>Specify the SAML attributes that the SAML statement MUST describe; the " +
          "Attribue Name [required] The Attribute Namespace [optional] and the Attribute" +
          "Value [required]</html>";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * The resource must be specified
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        return true;
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