/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class AttributeStatementWizardStepPanel extends WizardStepPanel {
    private static final String DEFAULT_NAME_FORMAT = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
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
    private SquigglyTextField filterExpressionTextField;
    private JLabel filterSamlAttributesLabel;
    private JPanel filterPanel;
    private DefaultTableModelWithAssociatedBean<SamlAttributeStatement.Attribute> attributesTableModel;
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
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        SamlAttributeStatement statement = assertion.getAttributeStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }

        int nrows = attributesTableModel.getRowCount();
        Collection attributes = new ArrayList();
        for (int i = 0; i < nrows; i++) {
            final SamlAttributeStatement.Attribute attribute = attributesTableModel.getBeanForRow(i);
            attributes.add(attribute);
        }
        statement.setAttributes((SamlAttributeStatement.Attribute[])attributes.toArray(new SamlAttributeStatement.Attribute[]{}));
        statement.setFilterExpression(filterExpressionTextField.getText());
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
            attributesTableModel.addRow(att, new Object[]{
                    att.getName(),
                    att.getNamespace(),
                    toDisplayNameFormat(att.getNameFormat(), samlVersion),
                    att.isAnyValue() ? ANY : att.getValue(),
                    att.isRepeatIfMulti()});
        }

        filterExpressionTextField.setText(statement.getFilterExpression());
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

        filterPanel.setVisible(issueMode);

        attributesTableModel = new DefaultTableModelWithAssociatedBean<SamlAttributeStatement.Attribute>(new String[]{"Name", "Namespace", "Name Format", "Value", "Repeat?"}, 0);
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
                final SamlAttributeStatement.Attribute attribute = attributesTableModel.getBeanForRow(row);

                EditAttributeDialog editAttributeDialog = new EditAttributeDialog(owner, attribute, samlVersion, issueMode);
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
                        attributesTableModel.setValueAt(attribute.isAnyValue() ? ANY : attribute.getValue(), row, 3);
                        attributesTableModel.setValueAt(attribute.isRepeatIfMulti(), row, 4);
                        attributesTableModel.updateBeanForRow(row, (SamlAttributeStatement.Attribute) bean);
                        notifyListeners();
                    }
                });
                editAttributeDialog.setVisible(true);
            }
        });
        addAttributeButton.addActionListener(new ActionListener() {
            @Override
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
                    @Override
                    public void onEditAccepted(Object source, Object bean) {
                        attributesTableModel.addRow((SamlAttributeStatement.Attribute) bean, new Object[] {
                                attribute.getName(),
                                attribute.getNamespace(),
                                toDisplayNameFormat(attribute.getNameFormat(), samlVersion),
                                attribute.isAnyValue() ? ANY : attribute.getValue(),
                                attribute.isRepeatIfMulti()});
                        notifyListeners();
                    }
                });
                editAttributeDialog.setVisible(true);
            }
        });

        //Do not use TextComponentPauseListenerManager.registerPauseListener. See it's java doc for warning.
        filterExpressionTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        }));

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
    @Override
    public boolean canAdvance() {

        final String filterExp = filterExpressionTextField.getText();
        boolean invalid = false;
        if (!"".equals(filterExp)) {
            try {
                if (!Syntax.validateStringOnlyReferencesVariables(filterExp)) {
                    filterExpressionTextField.setSquiggly();
                    filterExpressionTextField.setModelessFeedback("Only variables may be referenced");
                    invalid = true;
                }
            } catch (VariableNameSyntaxException e) {
                filterExpressionTextField.setSquiggly();
                filterExpressionTextField.setModelessFeedback("Invalid variable reference");
                invalid = true;
            }
        }

        if (!invalid) {
            filterExpressionTextField.setNone();
            filterExpressionTextField.setModelessFeedback(null);
        }

        return attributesTableModel.getRowCount() > 0 && !invalid;
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

    /**
     * Convert to display name format (display default)
     */
    private static String toDisplayNameFormat( final String nameFormat, final int samlVersion ) {
        String defaultNameFormat = DEFAULT_NAME_FORMAT;

        if ( nameFormat != null ) {
            defaultNameFormat = nameFormat;
        } else if ( samlVersion == 1 ) {
            defaultNameFormat = null;        
        }

        return defaultNameFormat;
    }

    /**
     * Convert from display name format (use null for default)
     */
    private static String fromDisplayNameFormat( final Object displayNameFormat ) {
        String nameFormat = null;

        if ( displayNameFormat != null &&
             !DEFAULT_NAME_FORMAT.equals( displayNameFormat ) ) {
            nameFormat = displayNameFormat.toString();
        }

        return nameFormat;
    }

    /**
     * Quick hack of a table model which will track a bean for each row. Ideally the columns and getValuesAt would
     * be pulled from the bean, perhaps based on bean property annotations. For now this is the same as default table
     * model except that a bean is tracked for each row.
     *
     * This allows for data not shown in the table columns to be stored.
     * @param <T>
     */
    private class DefaultTableModelWithAssociatedBean<T extends Cloneable> extends DefaultTableModel {

        private DefaultTableModelWithAssociatedBean(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void addRow(T bean, Object[] columnDataForRow) {
            super.addRow(columnDataForRow);
            beanList.add(bean);
        }

        public void updateBeanForRow(int row, T bean) {
            if (beanList.size() < row) {
                throw new IllegalArgumentException("Unknown row.");
            }
            beanList.remove(row);
            beanList.add(row , bean);
        }

        @Override
        public void removeRow(int row) {
            super.removeRow(row);
            beanList.remove(row);
        }

        public T getBeanForRow(int row) {
            return beanList.get(row);
        }

        @Override
        public Object getValueAt(int row, int column) {
            final T t = beanList.get(row);
            if (t == null) {
                throw new IllegalStateException("Unknown value at row " + row);
            }
            // proceed
            return super.getValueAt(row, column);
        }

        // - PRIVATE

        private final List<T> beanList = new ArrayList<T>();
    }
}