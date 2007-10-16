/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;

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
public class AttributeStatementWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private final boolean issueMode;
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
     */
    public AttributeStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
      * Creates new form AttributeStatementWizardStepPanel
      */
     public AttributeStatementWizardStepPanel(WizardStepPanel next, boolean issueMode) {
         this(next, true, issueMode);
     }

    /**
     * Creates new form AttributeStatementWizardStepPanel
     */
    public AttributeStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public AttributeStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    public AttributeStatementWizardStepPanel(WizardStepPanel next) {
        this(next, true, false);
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
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        SamlAttributeStatement statement = assertion.getAttributeStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }

        int nrows = attributesTableModel.getRowCount();
        Collection attributes = new ArrayList();
        for (int i = 0; i < nrows; i++) {
            String value = attributesTableModel.getValueAt(i, 3).toString();
            boolean isAny = ANY.equals(value);
            SamlAttributeStatement.Attribute att = new SamlAttributeStatement.Attribute(
                    toString(attributesTableModel.getValueAt(i, 0)),
                    toString(attributesTableModel.getValueAt(i, 1)),
                    toString(attributesTableModel.getValueAt(i, 2)),
                    isAny ? null : value,
                    isAny, Boolean.TRUE.equals(attributesTableModel.getValueAt(i,4)));
            attributes.add(att);
        }
        statement.setAttributes((SamlAttributeStatement.Attribute[])attributes.toArray(new SamlAttributeStatement.Attribute[]{}));
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
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        samlVersion = assertion.getVersion();

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
                    att.getNameFormat(),
                    att.isAnyValue() ? ANY : att.getValue(),
                    att.isRepeatIfMulti()});
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (issueMode) {
            attributeTableLabel.setText("Include the following attributes and values");
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }
        attributesTableModel = new DefaultTableModel(new String[]{"Name", "Namespace", "Name Format", "Value", "Repeat?"}, 0){
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
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(attributeTable.getSelectedRow() !=-1);
                editButton.setEnabled(attributeTable.getSelectedRow() !=-1);
            }
        });

        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                attributesTableModel.removeRow(attributeTable.getSelectedRow());
                notifyListeners();
            }
        });

        editButton.setEnabled(false);
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int row = attributeTable.getSelectedRow();
                final SamlAttributeStatement.Attribute attribute = new SamlAttributeStatement.Attribute();
                attribute.setName(AttributeStatementWizardStepPanel.toString(attributesTableModel.getValueAt(row, 0)));
                attribute.setNamespace(AttributeStatementWizardStepPanel.toString(attributesTableModel.getValueAt(row, 1)));
                attribute.setNameFormat(AttributeStatementWizardStepPanel.toString(attributesTableModel.getValueAt(row, 2)));
                attribute.setRepeatIfMulti(Boolean.TRUE.equals(attributesTableModel.getValueAt(row, 4)));
                String value = AttributeStatementWizardStepPanel.toString(attributesTableModel.getValueAt(row, 3));
                if (ANY.equals(value)) {
                    attribute.setAnyValue(true);
                    attribute.setValue(null);
                } else {
                    attribute.setAnyValue(false);
                    attribute.setValue(value);
                }
                EditAttributeDialog editAttributeDialog = new EditAttributeDialog(owner, attribute, samlVersion, issueMode);
                editAttributeDialog.addBeanListener(new BeanAdapter() {
                    /**
                     * Fired when the bean edit is accepted.
                     *
                     * @param source the event source
                     * @param bean   the bean being edited
                     */
                    public void onEditAccepted(Object source, Object bean) {
                        attributesTableModel.setValueAt(attribute.getName(), row, 0);
                        attributesTableModel.setValueAt(attribute.getNamespace(), row, 1);
                        attributesTableModel.setValueAt(attribute.getNameFormat(), row, 2);
                        attributesTableModel.setValueAt(attribute.isAnyValue() ? ANY : attribute.getValue(), row, 3);
                        attributesTableModel.setValueAt(attribute.isRepeatIfMulti(), row, 4);
                        notifyListeners();
                    }
                });
                editAttributeDialog.setVisible(true);
            }
        });
        addAttributeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final SamlAttributeStatement.Attribute attribute = new SamlAttributeStatement.Attribute();
                EditAttributeDialog editAttributeDialog = new EditAttributeDialog(owner, attribute, samlVersion, issueMode);
                editAttributeDialog.addBeanListener(new BeanAdapter() {
                    /**
                     * Fired when the bean edit is accepted.
                     *
                     * @param source the event source
                     * @param bean   the bean being edited
                     */
                    public void onEditAccepted(Object source, Object bean) {
                        attributesTableModel.addRow(new Object[] {
                                attribute.getName(),
                                attribute.getNamespace(),
                                attribute.getNameFormat(),
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
    public String getStepLabel() {
        return "Attribute Statement";
    }


    public String getDescription() {
        if (issueMode) {
            return
        "<html>Specify the SAML attributes that the SAML statement will include; the " +
              "Attribute Name [required] The Attribute Namespace [optional] and the Attribute" +
              "Value [required]</html>";
        } else {
            return
        "<html>Specify the SAML attributes that the SAML statement MUST describe; the " +
              "Attribute Name [required] The Attribute Namespace [optional] and the Attribute" +
              "Value [required]</html>";
        }
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * The resource must be specified
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        return attributesTableModel.getRowCount() > 0;
    }

    /**
     * Null safe toString
     */
    private static String toString(Object object) {
        String stringValue = null;

        if (object != null) {
            stringValue = object.toString();
        }

        return stringValue;
    }
}