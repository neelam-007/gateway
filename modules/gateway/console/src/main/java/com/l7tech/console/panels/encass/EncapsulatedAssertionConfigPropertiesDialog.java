package com.l7tech.console.panels.encass;

import com.l7tech.console.panels.IconSelectorPanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.tree.PaletteFolderRegistry;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.encass.EncapsulatedAssertionConstants.MAX_CHARS_FOR_NAME;
import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig.*;
import static com.l7tech.util.Functions.propertyTransform;
import static com.l7tech.util.Option.optional;

/**
 * Properties dialog for the entity representing an  encapsulated assertion configuration.
 * <p/>
 * Not be confused with the properties dialog for a particular encapsulated assertion instance in policy -- for that, see {@link EncapsulatedAssertionPropertiesDialog}.
 */
public class EncapsulatedAssertionConfigPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionConfigPropertiesDialog.class.getName());
    private static final String SELECT_ICON = "Select Icon";

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JComboBox<String> paletteFolderComboBox;
    private JButton changePolicyButton;
    private JLabel policyNameLabel;
    private JButton selectIconButton;
    private JTable outputsTable;
    private JTable inputsTable;
    private JButton addInputButton;
    private JButton addOutputButton;
    private JButton editInputButton;
    private JButton editOutputButton;
    private JButton deleteInputButton;
    private JButton deleteOutputButton;
    private JSplitPane inputsOutputsSplitPane;
    private JButton moveInputUpButton;
    private JButton moveInputDownButton;
    private JTextArea descriptionTextArea;
    private WrappingLabel artifactVersionDisplayLabel;
    private JLabel artifactVersionLabel;
    private JScrollPane artifactVersionScrollPane;
    private SecurityZoneWidget zoneControl;
    private JCheckBox allowTracingCheckBox;
    private JComboBox<EncapsulatedAssertionConfig> templateComboBox;

    private SimpleTableModel<EncapsulatedAssertionArgumentDescriptor> inputsTableModel;
    private SimpleTableModel<EncapsulatedAssertionResultDescriptor> outputsTableModel;

    private List<EncapsulatedAssertionArgumentDescriptor> savedInputs;
    private List<EncapsulatedAssertionResultDescriptor> savedOutputs;

    private final EncapsulatedAssertionConfig config;
    private final boolean readOnly;
    private final InputValidator inputValidator = new InputValidator(this, getTitle());
    private Policy policy;
    private String iconResourceFilename;
    private String iconBase64;
    private boolean confirmed = false;
    private final Set<String> usedConfigNames;

    /**
     * Create a new properties dialog that will show info for the specified bean,
     * and which will edit the specified bean in-place if the Ok button is successfully activated.
     *
     * @param parent             the parent window.  Should be specified to avoid focus/visibility issues.
     * @param config             the bean to edit.  Required.
     * @param readOnly           if true, the Ok button will be disabled.
     * @param usedConfigNames    EncapsulatedAssertionConfig names that are already in use.
     *                           New EncapsulatedAssertionConfigs cannot use these names and existing EncapsulatedAssertionConfigs cannot be edited to have these names.
     * @param autoPopulateParams whether the input and output params should be auto-populated if this is a new EncapsulatedAssertionConfig.
     */
    public EncapsulatedAssertionConfigPropertiesDialog(@NotNull final Window parent, @NotNull final EncapsulatedAssertionConfig config,
                                                       boolean readOnly, @NotNull final Set<String> usedConfigNames, final boolean autoPopulateParams) {
        super(parent, "Encapsulated Assertion Configuration Properties", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.readOnly = readOnly;
        this.usedConfigNames = usedConfigNames;
        if (config.getGuid() != null && config.getName() != null) {
            // only exclude its name from the reserved names if it already exists in the database
            // a new config may initially have the same name as an existing config
            this.usedConfigNames.remove(config.getName());
        }
        init(autoPopulateParams);
    }

    private void init(final boolean autoPopulateParams) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));

        final AbstractButton[] buttons = {changePolicyButton,
                addInputButton, editInputButton, deleteInputButton,
                moveInputUpButton, moveInputDownButton,
                addOutputButton, editOutputButton, deleteOutputButton,
                okButton, cancelButton};
        Utilities.equalizeButtonSizes(buttons);
        Utilities.enableGrayOnDisabled(buttons);
        Utilities.enableGrayOnDisabled(selectIconButton);
        Utilities.deuglifySplitPane(inputsOutputsSplitPane);

        inputValidator.constrainTextFieldToBeNonEmpty("name", nameField, null);
        inputValidator.constrainTextFieldToMaxChars("name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(nameField) {
            @Override
            public String getValidationError() {
                if (usedConfigNames.contains(nameField.getText()))
                    return "The specified name is already in use.";
                return null;
            }
        });
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
        inputValidator.addRule( new InputValidator.ComponentValidationRule( templateComboBox ) {
            @Override
            public String getValidationError() {

                return null;
            }
        } );

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
                column("GUI", 30, 30, 50, argumentGuiPromptExtractor, String.class),
                column("Name", 30, 140, 99999, argumentNameExtractor),
                column("Type", 30, 140, 99999, argumentTypeExtractor, DataType.class),
                column("Label", 30, 140, 99999, argumentGuiLabelExtractor));
        inputsTable.getColumnModel().getColumn(2).setCellRenderer(dataTypePrettyPrintingTableCellRenderer());
        inputsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        outputsTableModel = TableUtil.configureTable(outputsTable,
                column("Name", 30, 140, 99999, resultNameExtractor),
                column("Type", 30, 140, 99999, resultTypeExtractor, DataType.class));
        outputsTable.getColumnModel().getColumn(1).setCellRenderer(dataTypePrettyPrintingTableCellRenderer());
        outputsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        RunOnChangeListener enabler = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableThings();
            }
        });
        inputsTable.getSelectionModel().addListSelectionListener(enabler);
        outputsTable.getSelectionModel().addListSelectionListener(enabler);
        templateComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                EncapsulatedAssertionConfig template = (EncapsulatedAssertionConfig) templateComboBox.getSelectedItem();
                if ( template != null ) {
                    if ( savedInputs == null ) {
                        // Save inputs and outputs so we have something to restore later, if template turned off
                        savedInputs = new ArrayList<>( inputsTableModel.getRows() );
                        savedOutputs = new ArrayList<>( outputsTableModel.getRows() );
                    }

                    inputsTableModel.setRows( template.sortedArguments() );
                    outputsTableModel.setRows( new ArrayList<>( template.getResultDescriptors() ) );
                    sortOutputsTableModel();
                } else {
                    if ( savedInputs != null ) {
                        // Restore saved inputs and outputs
                        inputsTableModel.setRows( savedInputs );
                        savedInputs = null;

                        outputsTableModel.setRows( savedOutputs );
                        savedOutputs = null;
                    }
                }

                enableOrDisableThings();
            }
        } );

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

        moveInputUpButton.addActionListener(TableUtil.createMoveUpAction(inputsTable, inputsTableModel));
        moveInputDownButton.addActionListener(TableUtil.createMoveDownAction(inputsTable, inputsTableModel));

        selectIconButton.setText("");
        selectIconButton.setMargin(new Insets(0, 0, 0, 0));
        selectIconButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        selectIconButton.setHorizontalTextPosition(SwingConstants.CENTER);

        zoneControl.configure(config.getGoid().equals(EncapsulatedAssertionConfig.DEFAULT_GOID) ? OperationType.CREATE : readOnly ? OperationType.READ : OperationType.UPDATE, config);

        Utilities.enableDefaultFocusTraversal(descriptionTextArea);

        Utilities.setDoubleClickAction(inputsTable, editInputButton);
        Utilities.setDoubleClickAction(outputsTable, editOutputButton);

        okButton.setEnabled(!readOnly);
        initView(autoPopulateParams);
        enableOrDisableThings();
    }

    private DefaultTableCellRenderer dataTypePrettyPrintingTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, prettyPrintDataType(value), isSelected, hasFocus, row, column);
            }
        };
    }

    private Object prettyPrintDataType(Object value) {
        if (value instanceof DataType) {
            DataType dataType = (DataType) value;
            return dataType.getName();
        } else if (value instanceof String) {
            String s = (String) value;
            DataType dataType = DataType.forName(s);
            if (dataType != null)
                return dataType.getName();
        }
        return value;
    }

    // Find names already used by objects in the specified table model, excluding the specified self object.
    private <RT> Set<String> findUsedNames(SimpleTableModel<RT> tableModel, Functions.Unary<String, RT> nameExtractor, @Nullable RT self) {
        Set<String> usedNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        usedNames.addAll(Functions.map(tableModel.getRows(), nameExtractor));
        if (self != null) {
            usedNames.remove(optional(nameExtractor.call(self)).orSome(""));
        }
        return usedNames;
    }

    private void doInputProperties(final EncapsulatedAssertionArgumentDescriptor input, final boolean needsInsert) {
        if (input == null)
            return;

        Set<String> usedNames = findUsedNames(inputsTableModel, argumentNameExtractor, needsInsert ? null : input);
        final EncapsulatedAssertionArgumentDescriptorPropertiesDialog dlg = new EncapsulatedAssertionArgumentDescriptorPropertiesDialog(this, input, usedNames);
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
        Set<String> usedNames = findUsedNames(outputsTableModel, resultNameExtractor, needsInsert ? null : output);
        final EncapsulatedAssertionResultDescriptorPropertiesDialog dlg = new EncapsulatedAssertionResultDescriptorPropertiesDialog(this, output, usedNames);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display( dlg, new Runnable() {
            @Override
            public void run() {
                if ( !dlg.isConfirmed() )
                    return;

                output.setEncapsulatedAssertionConfig( config );
                if ( needsInsert ) {
                    outputsTableModel.addRow( output );
                }
                sortOutputsTableModel();
                int index = outputsTableModel.getRowIndex( output );
                if ( index >= 0 ) {
                    outputsTableModel.fireTableRowsUpdated( index, index );
                    outputsTable.getSelectionModel().setSelectionInterval( index, index );
                }
            }
        } );
    }

    private void sortOutputsTableModel() {
        outputsTableModel.setRows( Functions.sort( outputsTableModel.getRows(), new Comparator<EncapsulatedAssertionResultDescriptor>() {
            @Override
            public int compare( EncapsulatedAssertionResultDescriptor a, EncapsulatedAssertionResultDescriptor b ) {
                return String.CASE_INSENSITIVE_ORDER.compare( a.getResultName(), b.getResultName() );
            }
        } ) );
    }

    private <RT> ActionListener makeDeleteRowListener(final JTable table, final SimpleTableModel<RT> tableModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int[] selectedIndices = table.getSelectedRows();
                final List<RT> rows = new ArrayList<>(selectedIndices.length);
                for (int selectedIndex : selectedIndices) {
                    rows.add(tableModel.getRowObject(selectedIndex));
                }
                for (final RT row : rows) {
                    tableModel.removeRow(row);
                }
            }
        };
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initView(final boolean autoPopulateParams) {
        nameField.setText(config.getName());
        descriptionTextArea.setText(config.getProperty(PROP_DESCRIPTION));

        final String paletteFolderName = config.getProperty(PROP_PALETTE_FOLDER);
        loadPaletteFolders(paletteFolderName);
        paletteFolderComboBox.setSelectedItem(paletteFolderName);

        setPolicyAndPolicyNameLabel(config.getPolicy());

        iconResourceFilename = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME);
        iconBase64 = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64);
        selectIconButton.setIcon(EncapsulatedAssertionConsoleUtil.findIcon(iconResourceFilename, iconBase64).right);

        inputsTableModel.setRows(config.sortedArguments());
        outputsTableModel.setRows(new ArrayList<EncapsulatedAssertionResultDescriptor>(config.getResultDescriptors()));
        sortOutputsTableModel();

        if (config.getGuid() == null && config.getPolicy() != null) {
            // this is a new config which hasn't been saved yet but has been assigned a policy
            setPolicyAndPolicyNameLabel(config.getPolicy());
            if (autoPopulateParams) {
                prePopulateInputsTable();
                prePopulateOutputsTable();
            }
        }

        final String artifactVersion = config.getProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION);
        if (artifactVersion != null) {
            artifactVersionDisplayLabel.setText(artifactVersion);
        }

        allowTracingCheckBox.setSelected(config.getBooleanProperty(PROP_ALLOW_TRACING));

        loadTemplates();

        final String interfaceName = config.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE );
        final String methodName = config.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD );

        setSelectedTemplate( interfaceName, methodName );

        if ( interfaceName != null && methodName != null && !config.isUnsaved() ) {
            // TODO should we prevent changing the backing policy of an encass once it has been saved?
            //changePolicyButton.setEnabled( false );
        }


        templateComboBox.setEnabled( config.isUnsaved() );
    }

    private void setSelectedTemplate( String interfaceName, String methodName ) {
        if ( interfaceName == null || methodName == null ) {
            templateComboBox.setSelectedItem( null );
            return;
        }

        int items = templateComboBox.getItemCount();
        for ( int i = 0; i < items; ++i ) {
            EncapsulatedAssertionConfig template = templateComboBox.getItemAt( i );
            if ( template != null && interfaceName.equals( template.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE ) ) &&
                 methodName.equals( template.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD ) ) ) {
                templateComboBox.setSelectedIndex( i );
                return;
            }
        }

        templateComboBox.setSelectedItem( null );
    }

    private void setPolicyAndPolicyNameLabel(Policy policy) {
        this.policy = policy;
        policyNameLabel.setText(EncapsulatedAssertionConsoleUtil.getPolicyDisplayName(policy, config));
    }

    private void updateBean() {
        config.setName(nameField.getText());
        config.putProperty(PROP_PALETTE_FOLDER, (String) paletteFolderComboBox.getSelectedItem());
        String desc = descriptionTextArea.getText();
        if (desc.trim().length() > 0) {
            config.putProperty(PROP_DESCRIPTION, desc);
        } else {
            config.removeProperty(PROP_DESCRIPTION);
        }
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

        EncapsulatedAssertionConfig templateConfig = (EncapsulatedAssertionConfig) templateComboBox.getSelectedItem();
        if ( templateConfig != null ) {
            final String interfaceName = templateConfig.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE );
            final String methodName = templateConfig.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD );
            if ( interfaceName != null && methodName != null ) {
                config.putProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE, interfaceName );
                config.putProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD, methodName );
            }
            config.setArgumentDescriptors( Collections.<EncapsulatedAssertionArgumentDescriptor>emptySet() );
            config.setResultDescriptors( Collections.<EncapsulatedAssertionResultDescriptor>emptySet() );
        } else {
            config.removeProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE );
            config.removeProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD );
            int ord = 1;
            final List<EncapsulatedAssertionArgumentDescriptor> inputs = inputsTableModel.getRows();
            for (EncapsulatedAssertionArgumentDescriptor input : inputs) {
                input.setOrdinal(ord++);
            }

            config.setArgumentDescriptors( new HashSet<EncapsulatedAssertionArgumentDescriptor>( inputs ) );
            config.setResultDescriptors( new HashSet<EncapsulatedAssertionResultDescriptor>( outputsTableModel.getRows() ) );
        }

        config.setSecurityZone(zoneControl.getSelectedZone());
        config.putBooleanProperty(PROP_ALLOW_TRACING, allowTracingCheckBox.isSelected());
    }

    private void enableOrDisableThings() {
        boolean argsReadOnly = readOnly || templateComboBox.getSelectedItem() != null;

        boolean haveInput = getSelectedInput() != null;
        editInputButton.setEnabled(!argsReadOnly && haveInput);
        deleteInputButton.setEnabled(!argsReadOnly && haveInput);
        moveInputUpButton.setEnabled( !argsReadOnly && haveInput && inputsTable.getSelectedRow() > 0 );
        moveInputDownButton.setEnabled(!argsReadOnly && haveInput && !inputsTable.getSelectionModel().isSelectedIndex(inputsTable.getRowCount() - 1));

        boolean haveOutput = getSelectedOutput() != null;
        editOutputButton.setEnabled(!argsReadOnly && haveOutput);
        deleteOutputButton.setEnabled(!argsReadOnly && haveOutput);

        final String artifactVersion = config.getProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION);
        final boolean arty = artifactVersion != null;
        artifactVersionLabel.setEnabled(arty);
        artifactVersionLabel.setVisible(arty);
        artifactVersionDisplayLabel.setEnabled(arty);
        artifactVersionDisplayLabel.setVisible(arty);
        artifactVersionScrollPane.setEnabled(arty);
        artifactVersionScrollPane.setVisible(arty);

        setEnabled( !readOnly, changePolicyButton, selectIconButton, paletteFolderComboBox, nameField );
        setEnabled( !argsReadOnly, addInputButton, addOutputButton, inputsTable, outputsTable );
    }

    private void setEnabled(boolean enabled, JComponent... components) {
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
    }

    private EncapsulatedAssertionArgumentDescriptor getSelectedInput() {
        return inputsTableModel.getRowObject(inputsTable.getSelectedRow());
    }

    private EncapsulatedAssertionResultDescriptor getSelectedOutput() {
        return outputsTableModel.getRowObject(outputsTable.getSelectedRow());
    }

    private void loadTemplates() {

        Collection<EncapsulatedAssertionConfig> templates = new ArrayList<>();
        try {
            // TODO do this in a more efficient way, maybe select interface first, then pick operation
            Collection<String> interfaceNames = Registry.getDefault().getPolicyBackedServiceAdmin().findAllTemplateInterfaceNames();
            for ( String interfaceName : interfaceNames ) {
                Collection<EncapsulatedAssertionConfig> ops = Registry.getDefault().getPolicyBackedServiceAdmin().getInterfaceDescription( interfaceName );
                templates.addAll( ops );
            }
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Unable to load encapsulated assertion config templates: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException( e ) );
        }

        ArrayList<EncapsulatedAssertionConfig> comboList = new ArrayList<>();
        comboList.add(null);
        comboList.addAll( templates );
        templateComboBox.setModel( new DefaultComboBoxModel<>( comboList.toArray( new EncapsulatedAssertionConfig[comboList.size()] ) ) );
        templateComboBox.setSelectedItem(null);
        templateComboBox.setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, asDisplayName(value), index, isSelected, cellHasFocus);
            }

            private Object asDisplayName(Object value) {
                return value instanceof EncapsulatedAssertionConfig
                        ? ((EncapsulatedAssertionConfig) value).getName()
                        : value;
            }
        });
    }

    private void loadPaletteFolders(String currentName) {
        final PaletteFolderRegistry paletteFolderRegistry = TopComponents.getInstance().getPaletteFolderRegistry();
        List<String> folderNames = new ArrayList<String>(paletteFolderRegistry.getAssertionPaletteFolderIds());

        if (currentName != null && currentName.trim().length() > 0 && !folderNames.contains(currentName))
            folderNames.add(currentName);

        paletteFolderComboBox.setModel(new DefaultComboBoxModel<>( folderNames.toArray( new String[folderNames.size()] ) ));
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

            policyHeaders = Functions.sort(policyHeaders, new Comparator<PolicyHeader>() {
                @Override
                public int compare(PolicyHeader a, PolicyHeader b) {
                    return String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a), String.valueOf(b));
                }
            });

            PolicyHeader initialValue = null;
            if (policy != null) {
                for (PolicyHeader policyHeader : policyHeaders) {
                    if (Goid.equals(policyHeader.getGoid(), policy.getGoid())) {
                        initialValue = policyHeader;
                        break;
                    }
                }
            }

            final PolicyHeader[] options = policyHeaders.toArray(new PolicyHeader[policyHeaders.size()]);
            final PolicySelectorDialog policySelector = new PolicySelectorDialog(this, options, initialValue);
            policySelector.pack();
            Utilities.centerOnParentWindow(policySelector);
            DialogDisplayer.display(policySelector, new Runnable() {
                @Override
                public void run() {
                    final PolicyHeader selected = policySelector.getSelected();
                    if (policySelector.isConfirmed() && selected != null) {
                        final boolean autoPopulate = policySelector.isAutoPopulate();
                        if (policySelector.isSelectionChanged() || autoPopulate) {
                            // reload the policy
                            try {
                                final Policy newPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(selected.getGoid());
                                if (newPolicy == null) {
                                    showError("Policy not found", null);
                                    return;
                                }

                                setPolicyAndPolicyNameLabel(newPolicy);
                                if (autoPopulate) {
                                    prePopulateInputsTable();
                                    prePopulateOutputsTable();
                                }
                            } catch (final FindException e) {
                                showError("Unable to load policy", e);
                            }
                        }
                    }
                }
            });
        } catch (final FindException e) {
            showError("Unable to load list of available policy include fragments", e);
        }
    }

    @Nullable
    private Assertion getFragmentRootAssertionWithEntitiesAttached() {
        Assertion root = null;
        if (null != policy) {
            try {
                root = policy.getAssertion();
                PolicyUtil.provideNeededEntities(root, Registry.getDefault().getEntityFinder(), null);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Bad policy XML: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                /* FALLTHROUGH and return null */
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to locate at least one design time entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                /* FALLTHROUGH and return policy with as many entities populated as we were able to manage */
            }
        }
        return root;
    }

    /**
     * Add rows to the inputs table corresponding to the variables used by the currently-selected policy fragment.
     * <p/>
     * Skips variables which already exist in the table.
     * <p/>
     * Does not delete variables which already exist but are not found in the currently-selected policy fragment.
     */
    private void prePopulateInputsTable() {
        Assertion root = getFragmentRootAssertionWithEntitiesAttached();
        if (null == root)
            return;

        final String[] vars = PolicyVariableUtils.getVariablesUsedByPolicyButNotPreviouslySet(root, SsmPolicyVariableUtils.getSsmAssertionTranslator());
        final Set<String> existingNames = findUsedNames(inputsTableModel, argumentNameExtractor, null);
        for (String var : vars) {
            if (!existingNames.contains(var) && !BuiltinVariables.isSupported(var)) {
                final EncapsulatedAssertionArgumentDescriptor arg = new EncapsulatedAssertionArgumentDescriptor();
                arg.setEncapsulatedAssertionConfig(config);
                arg.setArgumentType(DataType.STRING.getShortName());
                arg.setArgumentName(var);
                arg.setGuiPrompt(false);
                inputsTableModel.addRow(arg);
            }
        }
    }

    /**
     * Add rows to the outputs table corresponding to the variables set by the currently-selected policy fragment.
     * <p/>
     * Skips variables which already exist in the table.
     * <p/>
     * Does not delete variables which already exist but are not found in the currently-selected policy fragment.
     */
    private void prePopulateOutputsTable() {
        Assertion root = getFragmentRootAssertionWithEntitiesAttached();
        if (null == root)
            return;

        final Map<String, VariableMetadata> vars = PolicyVariableUtils.getVariablesSetByPolicyButNotSubsequentlyUsed(root, SsmPolicyVariableUtils.getSsmAssertionTranslator());
        final Set<String> existingNames = findUsedNames(outputsTableModel, resultNameExtractor, null);
        for (final VariableMetadata vm : vars.values()) {
            if (!existingNames.contains(vm.getName()) && !BuiltinVariables.isSupported(vm.getName())) {
                final EncapsulatedAssertionResultDescriptor ret = new EncapsulatedAssertionResultDescriptor();
                ret.setEncapsulatedAssertionConfig(config);
                ret.setResultName(vm.getName());
                final DataType type = vm.getType();
                ret.setResultType(type == null ? DataType.UNKNOWN.getShortName() : type.getShortName());
                outputsTableModel.addRow(ret);
            }
        }
    }

    private void showError(String message, @Nullable Throwable e) {
        final String suffix;
        if (e == null) suffix = "";
        else suffix = ": " + ExceptionUtils.getMessage(e);
        DialogDisplayer.showMessageDialog(this, message + suffix, "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private void createUIComponents() {
        artifactVersionDisplayLabel = new WrappingLabel("", 1);
        artifactVersionDisplayLabel.setContextMenuEnabled(true);
    }

    /**
     * Listener for the 'set icon' button.
     */
    private class IconActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final Pair<EncapsulatedAssertionConsoleUtil.IconType, ImageIcon> currentIcon = EncapsulatedAssertionConsoleUtil.findIcon(iconResourceFilename, iconBase64);
            final OkCancelDialog<Pair<EncapsulatedAssertionConsoleUtil.IconType, String>> okCancelDialog = new OkCancelDialog<>(EncapsulatedAssertionConfigPropertiesDialog.this,
                    SELECT_ICON, true, new IconSelectorPanel(currentIcon.left.equals(EncapsulatedAssertionConsoleUtil.IconType.CUSTOM_IMAGE) ? null : currentIcon.right));
            okCancelDialog.pack();
            Utilities.centerOnParentWindow(okCancelDialog);
            DialogDisplayer.display(okCancelDialog, new Runnable() {
                @Override
                public void run() {
                    if (okCancelDialog.wasOKed()) {
                        final Pair<EncapsulatedAssertionConsoleUtil.IconType, String> selected = okCancelDialog.getValue();
                        if (EncapsulatedAssertionConsoleUtil.IconType.CUSTOM_IMAGE.equals(selected.left)) {
                            try {
                                final byte[] fileBytes = IOUtils.slurpFile(new File(selected.right));
                                iconBase64 = HexUtils.encodeBase64(fileBytes);
                                config.putProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64, iconBase64);
                            } catch (final IOException e) {
                                logger.log(Level.WARNING, "Error reading icon file. Using default icon.", ExceptionUtils.getDebugException(e));
                                iconBase64 = null;
                                config.removeProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64);
                            }
                            iconResourceFilename = null;
                        } else {
                            iconResourceFilename = selected.right;
                            iconBase64 = null;
                        }
                        selectIconButton.setIcon(EncapsulatedAssertionConsoleUtil.findIcon(iconResourceFilename, iconBase64).right);
                    }
                }
            });
        }
    }

    private static final Functions.Unary<String, EncapsulatedAssertionArgumentDescriptor> argumentGuiPromptExtractor = new Functions.Unary<String, EncapsulatedAssertionArgumentDescriptor>() {
        @Override
        public String call(EncapsulatedAssertionArgumentDescriptor conf) {
            return conf.isGuiPrompt() ? "   \u2713" : ""; // U+2713 'CHECK MARK'
        }
    };
    private static final Functions.Unary<String, EncapsulatedAssertionArgumentDescriptor> argumentNameExtractor = propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "argumentName");
    private static final Functions.Unary<DataType, EncapsulatedAssertionArgumentDescriptor> argumentTypeExtractor = Functions.propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "argumentType");
    private static final Functions.Unary<Object, EncapsulatedAssertionArgumentDescriptor> argumentGuiLabelExtractor = propertyTransform(EncapsulatedAssertionArgumentDescriptor.class, "guiLabel");

    private static final Functions.Unary<String, EncapsulatedAssertionResultDescriptor> resultNameExtractor = propertyTransform(EncapsulatedAssertionResultDescriptor.class, "resultName");
    private static final Functions.Unary<DataType, EncapsulatedAssertionResultDescriptor> resultTypeExtractor = Functions.propertyTransform(EncapsulatedAssertionResultDescriptor.class, "resultType");
}
