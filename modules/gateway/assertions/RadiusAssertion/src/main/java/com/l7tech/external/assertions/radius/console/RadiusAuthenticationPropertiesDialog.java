package com.l7tech.external.assertions.radius.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordManagerWindow;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.radius.RadiusAdmin;
import com.l7tech.external.assertions.radius.RadiusAuthenticateAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.MutablePair;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RadiusAuthenticationPropertiesDialog extends AssertionPropertiesOkCancelSupport<RadiusAuthenticateAssertion> {

    public static final String DIALOG_TITLE = "Radius Configuration";
    private JPanel propertyPanel;
    private JTextField hostTextField;
    private JTextField authPortTextField;
    private JTextField acctPortTextField;
    private JTextField timeoutTextField;
    private SecurePasswordComboBox securePasswordComboBox;
    private JTable advancedPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JComboBox authenticatorComboBox;
    private JLabel acctPortLabel;
    private JButton managePasswordsButton;
    private TargetVariablePanel prefixTargetVariablePanel;
    private JLabel hostLabel;
    private JLabel authPortLabel;
    private JLabel timeoutSecLabel;
    private JLabel authenticatorLabel;
    private JLabel radiusVariablePrefixLabel;
    private JLabel secretLabel;
    private AdvancedPropertiesTableModel advancedPropertiesTableModel = new AdvancedPropertiesTableModel();
    private static final String DEFAULT_AUTH_PORT = "1812";
    private static final String DEFAULT_ACCT_PORT = "1813";
    private static final String DEFAULT_TIMEOUT = "5";

    public RadiusAuthenticationPropertiesDialog(final Frame owner, final RadiusAuthenticateAssertion assertion) {
        super(RadiusAuthenticateAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    protected ActionListener createOkAction() {
        return new RunOnChangeListener();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        };
        prefixTargetVariablePanel.setVariable(RadiusAuthenticateAssertion.DEFAULT_PREFIX);

        acctPortLabel.setVisible(false);
        acctPortTextField.setVisible(false);

        advancedPropertiesTable.setModel( advancedPropertiesTableModel );
        advancedPropertiesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        advancedPropertiesTable.getTableHeader().setReorderingAllowed( false );

        authenticatorComboBox.setModel(new DefaultComboBoxModel(getRadiusAdmin().getAuthenticators()));
        securePasswordComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        managePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SecurePasswordManagerWindow dialog =
                        new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();

                Utilities.centerOnScreen(dialog);

                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        securePasswordComboBox.reloadPasswordList();
                        DialogDisplayer.pack(RadiusAuthenticationPropertiesDialog.this);
                    }
                });
            }
        });

        InputValidator okValidator =
                new InputValidator(this, DIALOG_TITLE);

        okValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if (StringUtils.isBlank(prefixTargetVariablePanel.getVariable())) {
                    return Utilities.removeColonFromLabel(radiusVariablePrefixLabel) + " must not be empty!";
                } else if(!VariableMetadata.isNameValid(prefixTargetVariablePanel.getVariable())) {
                    return Utilities.removeColonFromLabel(radiusVariablePrefixLabel) + " must have valid name";
                }
                return null;
            }
        });
        okValidator.constrainTextFieldToBeNonEmpty(Utilities.removeColonFromLabel(hostLabel), hostTextField, null);
        okValidator.constrainTextFieldToBeNonEmpty(Utilities.removeColonFromLabel(authPortLabel), authPortTextField, new InputValidator.ComponentValidationRule(authPortTextField) {
            @Override
            public String getValidationError() {

                int port = -1;
                final String text = authPortTextField.getText();
                if (!text.isEmpty() && Syntax.getReferencedNames(text).length == 0) {
                    try {
                        port = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        //Nothing here
                    }

                    if(port < 0 || port > 65535)
                        return  "Invalid " + Utilities.removeColonFromLabel(authPortLabel) + " number " + text + "\nValue must be in the range between 0 and 65535";

                }

                return null;
            }
        });
        okValidator.constrainTextFieldToBeNonEmpty(Utilities.removeColonFromLabel(acctPortLabel), acctPortTextField, null);
        okValidator.constrainTextFieldToBeNonEmpty(Utilities.removeColonFromLabel(timeoutSecLabel), timeoutTextField, new InputValidator.ComponentValidationRule(timeoutTextField) {
            @Override
            public String getValidationError() {

                int timeout = -1;
                final String text = timeoutTextField.getText();
                if (!text.isEmpty() && Syntax.getReferencedNames(text).length == 0) {
                    try {
                        timeout = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        //Nothing here
                    }

                    if(timeout < 0 )
                        return  "Invalid timeout " + text + "\nValue must be positive Integer";
                }

                return null;
            }
        });
        okValidator.ensureComboBoxSelection(Utilities.removeColonFromLabel(secretLabel), securePasswordComboBox);

        InputValidator.ValidationRule authenticatorRule =
                new InputValidator.ComponentValidationRule(authenticatorComboBox) {
                    @Override
                    public String getValidationError() {
                        if (authenticatorComboBox.getSelectedItem() != null ) {
                                if (!getRadiusAdmin().isAuthenticatorSupport((String) authenticatorComboBox.getSelectedItem())) {
                                    return "The " + Utilities.removeColonFromLabel(authenticatorLabel) + " " + authenticatorComboBox.getSelectedItem() + " is not supported.";
                                }
                        } else {
                            return "The " + Utilities.removeColonFromLabel(authenticatorLabel) + " " + " must not be empty.";
                        }
                        return null;
                    }
                };

        okValidator.addRule(authenticatorRule);

        okValidator.attachToButton(getOkButton(), super.createOkAction());


        advancedPropertiesTable.setModel(advancedPropertiesTableModel);
        advancedPropertiesTable.getTableHeader().setReorderingAllowed(false);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        editButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        removeButton.setEnabled(false);

        Utilities.setDoubleClickAction(advancedPropertiesTable, editButton);

    }

    private void addAdvancedProperty(final JTable advancedPropertiesTable, final AdvancedPropertiesTableModel advancedTableModel) {
        final RadiusAttributePropertiesDialog dialog = new RadiusAttributePropertiesDialog(this, null, advancedTableModel.toMap());
        dialog.setTitle("Customize");
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (!dialog.isCanceled()) {
                    updatePropertiesList(advancedPropertiesTable, advancedTableModel, dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        });
    }

    private void editAdvancedProperty(final JTable advancedPropertiesTable, final AdvancedPropertiesTableModel advancedTableModel) {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        final String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        final String value = (String) advancedTableModel.getValueAt(viewRow, 1);

        final RadiusAttributePropertiesDialog dialog = new RadiusAttributePropertiesDialog(this, new MutablePair<String,
                String>(name, value), advancedTableModel.toMap());
        dialog.setTitle("Customize");
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (!dialog.isCanceled()) {
                    updatePropertiesList(advancedPropertiesTable, advancedTableModel, dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        });
    }

    private void removeAdvancedProperty(JTable advancedPropertiesTable, AdvancedPropertiesTableModel advancedTableModel) {
        int[] viewRow = advancedPropertiesTable.getSelectedRows();

        if (viewRow.length < 1) return;

        Pair<String, String>[] rows = new Pair[viewRow.length];

        for (int i = 0; i < viewRow.length; i++) {
            String name = (String) advancedTableModel.getValueAt(viewRow[i], 0);
            String value = (String) advancedTableModel.getValueAt(viewRow[i], 1);
            rows[i] = new Pair<String, String>(name, value);
        }

        for (int i = 0; i < rows.length; i++) {
            updatePropertiesList(advancedPropertiesTable, advancedTableModel, rows[i], true);
        }

    }

    private void updatePropertiesList(JTable advancedPropertiesTable, AdvancedPropertiesTableModel advancedTableModel,
                                      final Pair<String, String> selectedProperty, boolean deleted) {
        ArrayList<String> keyset = new ArrayList<String>();
        int currentRow;

        if (deleted) {
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
            advancedTableModel.advancedPropertiesMap.remove(selectedProperty.left);
        } else {
            advancedTableModel.advancedPropertiesMap.put(selectedProperty.left, selectedProperty.right);
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
        }

        // Refresh the table
        advancedTableModel.fireTableDataChanged();

        // Refresh the selection highlight
        if (currentRow == advancedTableModel.advancedPropertiesMap.size())
            currentRow--; // If the previous deleted row was the last row
        if (currentRow >= 0) advancedPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
    }


    private void enableOrDisableComponents() {
        final boolean enableAdvancedPropertyContextualControls = advancedPropertiesTable.getSelectedRow() >= 0;
        editButton.setEnabled(enableAdvancedPropertyContextualControls);
        removeButton.setEnabled(enableAdvancedPropertyContextualControls);

    }

    @Override
    public void setData(RadiusAuthenticateAssertion assertion) {

        authenticatorComboBox.setSelectedItem(assertion.getAuthenticator());

        advancedPropertiesTableModel.fromMap(assertion.getAttributes());
        hostTextField.setText(assertion.getHost());
        if (assertion.getAcctPort() != null) {
            authPortTextField.setText(assertion.getAuthPort());
        } else {
            authPortTextField.setText(DEFAULT_AUTH_PORT);
        }
        if (assertion.getAcctPort() != null) {
            acctPortTextField.setText(assertion.getAcctPort());
        } else {
            acctPortTextField.setText(DEFAULT_ACCT_PORT);
        }
        if (assertion.getTimeout() != null) {
            timeoutTextField.setText(assertion.getTimeout());
        } else {
            timeoutTextField.setText(DEFAULT_TIMEOUT);
        }

        if (assertion.getSecretGoid() != null) {
            securePasswordComboBox.setSelectedSecurePassword(assertion.getSecretGoid());
        }


        if (assertion.getPrefix() != null && !assertion.getPrefix().isEmpty()) {
            prefixTargetVariablePanel.setVariable(assertion.getPrefix());
        } else {
            prefixTargetVariablePanel.setVariable(RadiusAuthenticateAssertion.DEFAULT_PREFIX);
        }
    }

    @Override
    public RadiusAuthenticateAssertion getData(RadiusAuthenticateAssertion assertion) throws ValidationException {
        assertion.setPrefix(prefixTargetVariablePanel.getVariable());
        assertion.setAttributes(advancedPropertiesTableModel.toMap());
        assertion.setHost(hostTextField.getText());
        assertion.setAuthPort(authPortTextField.getText());
        assertion.setAcctPort(acctPortTextField.getText());
        assertion.setTimeout(timeoutTextField.getText());
        assertion.setSecretGoid(securePasswordComboBox.getSelectedSecurePassword().getGoid());
        assertion.setAuthenticator((String) authenticatorComboBox.getSelectedItem());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private static RadiusAdmin getRadiusAdmin() {
        return Registry.getDefault().getExtensionInterface(RadiusAdmin.class, null);
    }

    /**
     * Inner class AdvancedPropertiesTableModel
     */
    private static class AdvancedPropertiesTableModel extends AbstractTableModel {
        private static final int MAX_TABLE_COLUMN_NUM = 2;
        private Map<String, String> advancedPropertiesMap = new LinkedHashMap<String, String>();

        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public int getRowCount() {
            return advancedPropertiesMap.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Name";
                case 1:
                    return "Value";
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            String name = (String) advancedPropertiesMap.keySet().toArray()[row];

            switch (col) {
                case 0:
                    return name;
                case 1:
                    return advancedPropertiesMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @NotNull
        private Map<String, String> toMap() {
            return new HashMap<String, String>(advancedPropertiesMap);
        }

        private void fromMap(@Nullable final Map<String, String> properties) {
            advancedPropertiesMap = new LinkedHashMap<String, String>();

            if (properties != null) {
                advancedPropertiesMap.putAll(properties);
            }
        }
    }

}
