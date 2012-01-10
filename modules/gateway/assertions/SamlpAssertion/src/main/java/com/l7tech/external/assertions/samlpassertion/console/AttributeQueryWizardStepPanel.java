/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class AttributeQueryWizardStepPanel extends SamlpWizardStepPanel {
    private static final String DEFAULT_NAME_FORMAT = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JScrollPane attributeTableScrollPane;
    private JTable attributeTable;
    private JButton addAttributeButton;
    private JButton removeButton;
    private JButton editButton;
    private JLabel attributeTableLabel;
    private DefaultTableModel attributesTableModel;
    private int samlVersion;
    private static final String ANY = "<any>";

    /**
     * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     * @param showTitleLabel specify whether to show the title label or not
     * @param mode AssertionMode for this instance of the wizard.
     */
    public AttributeQueryWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode, Assertion prevAssertion) {
        super(next, mode, prevAssertion);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
      * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     * @param mode AssertionMode for this instance of the wizard.
      */
     public AttributeQueryWizardStepPanel(WizardStepPanel next, AssertionMode mode, Assertion prevAssertion) {
         this(next, true, mode, prevAssertion);
     }

    /**
     * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     * @param showTitleLabel specify whether to show the title label or not
     * @param mode AssertionMode for this instance of the wizard.
     * @param owner the JDialog that is the parent of this panel.
     */
    public AttributeQueryWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode, JDialog owner, Assertion prevAssertion) {
        super(next, mode, prevAssertion);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
        initialize();
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

        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);
        SamlAttributeStatement statement = assertion.getAttributeStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }

        int nrows = attributesTableModel.getRowCount();
        Collection<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();
        boolean isV2 = (assertion.getSamlVersion() == 2);
        for (int i = 0; i < nrows; i++) {
            String value = attributesTableModel.getValueAt(i, 4).toString();
            boolean isAny = ANY.equals(value);
            SamlAttributeStatement.Attribute att = new SamlAttributeStatement.Attribute(
                    toString(attributesTableModel.getValueAt(i, 0)),
                    toString(attributesTableModel.getValueAt(i, 1)),
                    fromDisplayNameFormat(attributesTableModel.getValueAt(i, 2)),
                    isAny ? null : value,
                    isAny, Boolean.TRUE.equals(attributesTableModel.getValueAt(i,5)));

            // SAMLP v2.0 friendlyName
            if (isV2) {
                att.setFriendlyName(toString(attributesTableModel.getValueAt(i, 3)));
            }

            attributes.add(att);
        }
        statement.setAttributes( attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]) );
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
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);
        samlVersion = assertion.getSamlVersion();

        SamlAttributeStatement statement = assertion.getAttributeStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        // put in table
        attributesTableModel.setRowCount(0);
        attributesTableModel.fireTableDataChanged();
        SamlAttributeStatement.Attribute[] attributes = statement.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            SamlAttributeStatement.Attribute att = attributes[i];
            attributesTableModel.addRow(new Object[]{
                    att.getName(),
                    att.getNamespace(),
                    toDisplayNameFormat(att.getNameFormat(), samlVersion),
                    att.getFriendlyName(),
                    att.isAnyValue() ? ANY : att.getValue(),
                    att.isRepeatIfMulti()});
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (isRequestMode()) {
            attributeTableLabel.setText("Include the following attributes and values");
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }
        attributesTableModel = new DefaultTableModel(new String[]{"Name", "Namespace", "Name Format", "Friendly", "Value", "Repeat?"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attributeTableScrollPane.getViewport().setBackground(attributeTable.getBackground());
        attributeTable.setModel(attributesTableModel);
        attributeTable.getTableHeader().setReorderingAllowed(false);
        ListSelectionModel selectionModel = attributeTable.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(attributeTable.getSelectedRow() !=-1);
                editButton.setEnabled(attributeTable.getSelectedRow() !=-1);
            }
        });

        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attributesTableModel.removeRow(attributeTable.getSelectedRow());
                notifyListeners();
            }
        });

        editButton.setEnabled(false);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int row = attributeTable.getSelectedRow();
                final SamlAttributeStatement.Attribute attribute = new SamlAttributeStatement.Attribute();
                attribute.setName(AttributeQueryWizardStepPanel.toString(attributesTableModel.getValueAt(row, 0)));
                attribute.setNamespace(AttributeQueryWizardStepPanel.toString(attributesTableModel.getValueAt(row, 1)));
                attribute.setNameFormat(AttributeQueryWizardStepPanel.fromDisplayNameFormat(attributesTableModel.getValueAt(row, 2)));
                attribute.setFriendlyName(AttributeQueryWizardStepPanel.toString(attributesTableModel.getValueAt(row, 3)));
                attribute.setRepeatIfMulti(Boolean.TRUE.equals(attributesTableModel.getValueAt(row, 5)));
                String value = AttributeQueryWizardStepPanel.toString(attributesTableModel.getValueAt(row, 4));
                if (ANY.equals(value)) {
                    attribute.setAnyValue(true);
                    attribute.setValue(null);
                } else {
                    attribute.setAnyValue(false);
                    attribute.setValue(value);
                }
                SamlpEditAttributeDialog editAttributeDialog = new SamlpEditAttributeDialog(owner, attribute, samlVersion, isRequestMode());
                editAttributeDialog.addBeanListener(new BeanAdapter() {
                    /**
                     * Fired when the bean edit is accepted.
                     *
                     * @param source the event source
                     * @param bean   the bean being edited
                     */
                    @Override
                    public void onEditAccepted(Object source, Object bean) {
                        attributesTableModel.setValueAt(attribute.getName(), row, 0);
                        attributesTableModel.setValueAt(attribute.getNamespace(), row, 1);
                        attributesTableModel.setValueAt(toDisplayNameFormat(attribute.getNameFormat(), samlVersion), row, 2);
                        attributesTableModel.setValueAt(attribute.getFriendlyName(), row, 3);
                        attributesTableModel.setValueAt(attribute.isAnyValue() ? ANY : attribute.getValue(), row, 4);
                        attributesTableModel.setValueAt(attribute.isRepeatIfMulti(), row, 5);
                        notifyListeners();
                    }
                });
                editAttributeDialog.setVisible(true);
            }
        });
        Utilities.setDoubleClickAction(attributeTable, editButton);

        addAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SamlAttributeStatement.Attribute attribute = new SamlAttributeStatement.Attribute();
                SamlpEditAttributeDialog editAttributeDialog = new SamlpEditAttributeDialog(owner, attribute, samlVersion, isRequestMode());
                editAttributeDialog.addBeanListener(new BeanAdapter() {
                    /**
                     * Fired when the bean edit is accepted.
                     *
                     * @param source the event source
                     * @param bean   the bean being edited
                     */
                    @Override
                    public void onEditAccepted(Object source, Object bean) {
                        attributesTableModel.addRow(new Object[] {
                                attribute.getName(),
                                attribute.getNamespace(),
                                toDisplayNameFormat(attribute.getNameFormat(), samlVersion),
                                attribute.getFriendlyName(),
                                attribute.isAnyValue() ? ANY : attribute.getValue(),
                                attribute.isRepeatIfMulti()
                        });
                        notifyListeners();
                    }
                });
                editAttributeDialog.setVisible(true);
            }
        });
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Attribute Statement";
    }


    @Override
    public String getDescription() {
        if (isRequestMode()) {
            return
        "<html>Specify the SAML attributes that the SAML statement will include; the " +
              "Attribute Name [required] The Attribute Namespace [optional] and the Attribute" +
              "Value</html>";
        } else {
            return
        "<html>Specify the SAML attributes that the SAML statement MUST describe; the " +
              "Attribute Name [required] The Attribute Namespace [optional] and the Attribute" +
              "Value</html>";
        }
    }

    /**
     * Null safe toString
     * @param object the object that needs to be stringified
     * @return the stringified form of object
     */
    private static String toString(Object object) {
        String stringValue = null;

        if (object != null) {
            stringValue = object.toString();
        }

        return stringValue;
    }

    /**
     * Convert to display name format (display default)
     * @param nameFormat the format of the attribute to be represented
     * @param samlVersion which saml version should be rendered
     * @return the formatted string corresponding to the specified nameFormat and samlVersion
     */
    private static String toDisplayNameFormat( final String nameFormat, final int samlVersion ) {
        String defaultNameFormat = DEFAULT_NAME_FORMAT;

        if ( nameFormat != null ) {
            defaultNameFormat = nameFormat;
        } else if ( samlVersion < 2 ) {
            defaultNameFormat = null;
        }

        return defaultNameFormat;
    }

    /**
     * Convert from display name format (use null for default)
     * @param displayNameFormat the source string, of the formate obtained from toDisplayNameFormat
     */
    private static String fromDisplayNameFormat( final Object displayNameFormat ) {
        String nameFormat = null;

        if ( displayNameFormat != null &&
             !DEFAULT_NAME_FORMAT.equals( displayNameFormat ) ) {
            nameFormat = displayNameFormat.toString();
        }

        return nameFormat;
    }
}