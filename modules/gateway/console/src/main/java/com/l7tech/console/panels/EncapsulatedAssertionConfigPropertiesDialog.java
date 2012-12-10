package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionDataType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig.*;
import static com.l7tech.util.Functions.propertyTransform;

public class EncapsulatedAssertionConfigPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionConfigPropertiesDialog.class.getName());
    private static final String SELECT_ICON = "Select Icon";

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JComboBox paletteFolderComboBox;
    private JButton changePolicyButton;
    private JLabel policyNameLabel;
    private JButton selectIconButton;
    private JLabel iconLabel;
    private JTable outputsTable;
    private JTable inputsTable;
    private JButton addInputButton;
    private JButton addOutputButton;
    private JButton editInputButton;
    private JButton editOutputButton;
    private JButton deleteInputButton;
    private JButton deleteOutputButton;

    private SimpleTableModel<EncapsulatedAssertionArgumentDescriptor> inputsTableModel;
    private SimpleTableModel<EncapsulatedAssertionResultDescriptor> outputsTableModel;

    private final EncapsulatedAssertionConfig config;
    private final boolean readOnly;
    private final InputValidator inputValidator = new InputValidator(this, getTitle());
    private Policy policy;
    private String iconResourceFilename;
    private String iconBase64;
    private boolean confirmed = false;

    /**
     * Create a new properties dialog that will show info for the specified bean,
     * and which will edit the specified bean in-place if the Ok button is successfully activated.
     *
     * @param parent the parent window.  Should be specified to avoid focus/visibility issues.
     * @param config the bean to edit.  Required.
     * @param readOnly if true, the Ok button will be disabled.
     */
    public EncapsulatedAssertionConfigPropertiesDialog(Window parent, EncapsulatedAssertionConfig config, boolean readOnly) {
        super(parent, "Encapsulated Assertion Configuration", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.readOnly = readOnly;
        init();
    }

    private void init() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));

        final AbstractButton[] buttons = {changePolicyButton, selectIconButton,
            addInputButton, editInputButton, deleteInputButton,
            addOutputButton, editOutputButton, deleteOutputButton,
            okButton, cancelButton};
        Utilities.equalizeButtonSizes(buttons);
        Utilities.enableGrayOnDisabled(buttons);

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(changePolicyButton) {
            @Override
            public String getValidationError() {
                return policy != null ? null
                    : "An implementation policy must be specified.";
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(paletteFolderComboBox) {
            @Override
            public String getValidationError() {
                String folderName = (String) paletteFolderComboBox.getSelectedItem();
                return folderName != null && folderName.trim().length() > 0 ? null
                    : "A palette folder must be specified.";
            }
        });

        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!readOnly) {
                    updateBean();
                    confirmed = true;
                }
                dispose();
            }
        });

        changePolicyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doChangePolicy();
            }
        });
        selectIconButton.addActionListener(new IconActionListener());

        inputsTableModel = TableUtil.configureTable(inputsTable,
            column("GUI", 30, 30, 50, Functions.<Boolean, EncapsulatedAssertionArgumentDescriptor>propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "guiPrompt"), Boolean.class),
            column("Name", 30, 140, 99999, propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "argumentName")),
            column("Type", 30, 140, 99999, Functions.<EncapsulatedAssertionDataType, EncapsulatedAssertionArgumentDescriptor>propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "argumentType"), EncapsulatedAssertionDataType.class),
            column("Default", 30, 140, 99999, propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "defaultValue")));

        outputsTableModel = TableUtil.configureTable(outputsTable,
            column("Name", 30, 140, 99999, propertyTransform(EncapsulatedAssertionResultDescriptor.class, "resultName")),
            column("Type", 30, 140, 99999, Functions.<EncapsulatedAssertionDataType, EncapsulatedAssertionResultDescriptor>propertyTransform(EncapsulatedAssertionResultDescriptor.class, "resultType"), EncapsulatedAssertionDataType.class));

        RunOnChangeListener enabler = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableThings();
            }
        });
        inputsTable.getSelectionModel().addListSelectionListener(enabler);
        outputsTable.getSelectionModel().addListSelectionListener(enabler);

        deleteInputButton.addActionListener(makeDeleteRowListener(inputsTable, inputsTableModel));
        addInputButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doInputProperties(new EncapsulatedAssertionArgumentDescriptor(), true);
            }
        });
        editInputButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doInputProperties(getSelectedInput(), false);
            }
        });

        deleteOutputButton.addActionListener(makeDeleteRowListener(outputsTable, outputsTableModel));
        addOutputButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOutputProperties(new EncapsulatedAssertionResultDescriptor(), true);
            }
        });
        editOutputButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOutputProperties(getSelectedOutput(), false);
            }
        });

        okButton.setEnabled(!readOnly);
        updateView();
        enableOrDisableThings();
    }

    private void doInputProperties(final EncapsulatedAssertionArgumentDescriptor input, final boolean needsInsert) {
        if (input == null)
            return;
        final EncapsulatedAssertionArgumentDescriptorPropertiesDialog dlg = new EncapsulatedAssertionArgumentDescriptorPropertiesDialog(this, input);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.isConfirmed())
                    return;

                input.setEncapsulatedAssertionConfig(config);
                if (needsInsert)
                    inputsTableModel.addRow(input);
                int index = inputsTableModel.getRowIndex(input);
                if (index >= 0) {
                    inputsTableModel.fireTableRowsUpdated(index, index);
                    inputsTable.getSelectionModel().setSelectionInterval(index, index);
                }
            }
        });
    }

    private void doOutputProperties(final EncapsulatedAssertionResultDescriptor output, final boolean needsInsert) {
        if (output == null)
            return;
        final EncapsulatedAssertionResultDescriptorPropertiesDialog dlg = new EncapsulatedAssertionResultDescriptorPropertiesDialog(this, output);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.isConfirmed())
                    return;

                output.setEncapsulatedAssertionConfig(config);
                if (needsInsert)
                    outputsTableModel.addRow(output);
                int index = outputsTableModel.getRowIndex(output);
                if (index >= 0) {
                    outputsTableModel.fireTableRowsUpdated(index, index);
                    outputsTable.getSelectionModel().setSelectionInterval(index, index);
                }
            }
        });
    }

    private <RT> ActionListener makeDeleteRowListener(final JTable table, final SimpleTableModel<RT> tableModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RT row = tableModel.getRowObject(table.getSelectedRow());
                if (row == null)
                    return;
                tableModel.removeRow(row);
            }
        };
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateView() {
        nameField.setText(config.getName());

        final String paletteFolderName = config.getProperty(PROP_PALETTE_FOLDER);
        loadPaletteFolders(paletteFolderName);
        paletteFolderComboBox.setSelectedItem(paletteFolderName);

        setPolicyAndPolicyNameLabel(config.getPolicy());

        iconResourceFilename = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME);
        iconBase64 = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64);
        iconLabel.setIcon(EncapsulatedAssertionConsoleUtil.findIcon(config).right);

        inputsTableModel.setRows(new ArrayList<EncapsulatedAssertionArgumentDescriptor>(config.getArgumentDescriptors()));
        outputsTableModel.setRows(new ArrayList<EncapsulatedAssertionResultDescriptor>(config.getResultDescriptors()));
    }

    private void setPolicyAndPolicyNameLabel(Policy policy) {
        this.policy = policy;
        policyNameLabel.setText(policy == null ? "<Not Configured>" : policy.getName());
    }

    private void updateBean() {
        config.setName(nameField.getText());
        config.putProperty(PROP_PALETTE_FOLDER, (String) paletteFolderComboBox.getSelectedItem());
        config.setPolicy(policy);

        if (iconResourceFilename != null) {
            config.putProperty(PROP_ICON_RESOURCE_FILENAME, iconResourceFilename);
        } else {
            config.removeProperty(PROP_ICON_RESOURCE_FILENAME);
        }

        if (iconBase64 != null) {
            config.putProperty(PROP_ICON_BASE64, iconBase64);
        } else {
            config.removeProperty(PROP_ICON_BASE64);
        }

        config.setArgumentDescriptors(new HashSet<EncapsulatedAssertionArgumentDescriptor>(inputsTableModel.getRows()));
        config.setResultDescriptors(new HashSet<EncapsulatedAssertionResultDescriptor>(outputsTableModel.getRows()));
    }

    private void enableOrDisableThings() {
        boolean haveInput = getSelectedInput() != null;
        editInputButton.setEnabled(haveInput);
        deleteInputButton.setEnabled(haveInput);

        boolean haveOutput = getSelectedOutput() != null;
        editOutputButton.setEnabled(haveOutput);
        deleteOutputButton.setEnabled(haveOutput);
    }

    private EncapsulatedAssertionArgumentDescriptor getSelectedInput() {
        return inputsTableModel.getRowObject(inputsTable.getSelectedRow());
    }

    private EncapsulatedAssertionResultDescriptor getSelectedOutput() {
        return outputsTableModel.getRowObject(outputsTable.getSelectedRow());
    }

    private void loadPaletteFolders(String currentName) {
        final PaletteFolderRegistry paletteFolderRegistry = TopComponents.getInstance().getPaletteFolderRegistry();
        List<String> folderNames = new ArrayList<String>(paletteFolderRegistry.getPaletteFolderIds());

        if (currentName != null && currentName.trim().length() > 0 && !folderNames.contains(currentName))
            folderNames.add(currentName);

        paletteFolderComboBox.setModel(new DefaultComboBoxModel(folderNames.toArray(new String[0])));
        paletteFolderComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, asDisplayName(value), index, isSelected, cellHasFocus);
            }

            private Object asDisplayName(Object value) {
                return value instanceof String
                    ? paletteFolderRegistry.getPaletteFolderName((String) value)
                    : value;
            }
        });
    }

    private void doChangePolicy() {
        try {
            Collection<PolicyHeader> policyHeaders = Registry.getDefault().getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT));

            if (policyHeaders == null || policyHeaders.size() < 1) {
                showError("<html><p>No policy fragments are available to provide an implementation for this encapsulated assertion. <br/>Please create a policy first.", null);
                return;
            }

            PolicyHeader initialValue = null;
            if (policy != null) {
                for (PolicyHeader policyHeader : policyHeaders) {
                    if (policyHeader.getOid() == policy.getOid()) {
                        initialValue = policyHeader;
                        break;
                    }
                }
            }

            final PolicyHeader[] options = policyHeaders.toArray(new PolicyHeader[policyHeaders.size()]);
            DialogDisplayer.showInputDialog(this, "Select implementation policy:", "Set Implementation Policy", JOptionPane.QUESTION_MESSAGE, null, options, initialValue, new DialogDisplayer.InputListener() {
                @Override
                public void reportResult(Object option) {
                    if (option == null)
                        return;
                    PolicyHeader policyHeader = (PolicyHeader) option;

                    // Load the selected policy
                    try {
                        Policy newPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(policyHeader.getOid());
                        if (newPolicy == null) {
                            showError("Policy not found", null);
                            return;
                        }

                        setPolicyAndPolicyNameLabel(newPolicy);
                        if (inputsTableModel.getRowCount() < 1)
                            prePopulateInputsTable();
                        if (outputsTableModel.getRowCount() < 1)
                            prePopulateOutputsTable();

                    } catch (FindException e) {
                        showError("Unable to load policy", e);
                    }
                }
            });

        } catch (FindException e) {
            showError("Unable to load list of available policy include fragments", e);
        }
    }

    @Nullable
    private Assertion getFragmentRootAssertion() {
        if (null == policy)
            return null;
        try {
            return policy.getAssertion();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Bad policy XML: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    /**
     * Add rows to the inputs table corresponding to the variables used by the currently-selected policy fragment.
     */
    private void prePopulateInputsTable() {
        Assertion root = getFragmentRootAssertion();
        if (null == root)
            return;

        String[] vars = PolicyVariableUtils.getVariablesUsedByDescendantsAndSelf(root, SsmPolicyVariableUtils.getSsmAssertionTranslator());
        for (String var : vars) {
            EncapsulatedAssertionArgumentDescriptor arg = new EncapsulatedAssertionArgumentDescriptor();
            arg.setEncapsulatedAssertionConfig(config);
            arg.setArgumentType(EncapsulatedAssertionDataType.STRING.name());
            arg.setArgumentName(var);
            arg.setGuiPrompt(false);
            arg.setDefaultValue(null);
            inputsTableModel.addRow(arg);
        }
    }

    /**
     * Add rows to the outputs table corresponding to the variables set by the currently-selected policy fragment.
     */
    private void prePopulateOutputsTable() {
        Assertion root = getFragmentRootAssertion();
        if (null == root)
            return;

        Map<String, VariableMetadata> vars = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf(root, SsmPolicyVariableUtils.getSsmAssertionTranslator());
        for (VariableMetadata vm : vars.values()) {
            EncapsulatedAssertionResultDescriptor ret = new EncapsulatedAssertionResultDescriptor();
            ret.setEncapsulatedAssertionConfig(config);
            ret.setResultName(vm.getName());
            ret.setResultType(toEncAssDataType(vm.getType()).name());
            outputsTableModel.addRow(ret);
        }
    }

    private EncapsulatedAssertionDataType toEncAssDataType(DataType type) {
        if (DataType.BOOLEAN.equals(type))
            return EncapsulatedAssertionDataType.BOOLEAN;
        if (DataType.MESSAGE.equals(type))
            return EncapsulatedAssertionDataType.MESSAGE;
        if (DataType.DECIMAL.equals(type) || DataType.FLOAT.equals(type) || DataType.INTEGER.equals(type))
            return EncapsulatedAssertionDataType.NUMBER;
        if (DataType.DATE_TIME.equals(type))
            return EncapsulatedAssertionDataType.DATE;

        // TODO support more types
        return EncapsulatedAssertionDataType.STRING;
    }

    private void showError(String message, @Nullable Throwable e) {
        final String suffix;
        if (e == null) suffix = "";
        else suffix = ": " + ExceptionUtils.getMessage(e);
        DialogDisplayer.showMessageDialog(this, message + suffix, "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private class IconActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final OkCancelDialog<String> okCancelDialog = new OkCancelDialog<String>(TopComponents.getInstance().getTopParent(),
                    SELECT_ICON, true, new IconSelectorDialog((ImageIcon)iconLabel.getIcon()));
            okCancelDialog.pack();
            Utilities.centerOnParentWindow(okCancelDialog);
            DialogDisplayer.display(okCancelDialog, new Runnable() {
                @Override
                public void run() {
                    if(okCancelDialog.wasOKed()){
                        iconResourceFilename = okCancelDialog.getValue();
                        config.putProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME, iconResourceFilename);
                        iconBase64 = null;
                        iconLabel.setIcon(EncapsulatedAssertionConsoleUtil.findIcon(config).right);
                    }
                }
            });
        }
    }
}
