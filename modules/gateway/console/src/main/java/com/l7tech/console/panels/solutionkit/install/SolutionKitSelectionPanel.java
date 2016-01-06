package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gui.ErrorMessageDialog;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which allows the user to select component(s) within a solution kit to install.
 */
public class SolutionKitSelectionPanel extends WizardStepPanel<SolutionKitsConfig>  {
    private static final Logger logger = Logger.getLogger(SolutionKitSelectionPanel.class.getName());
    private static final String STEP_LABEL = "Select Solution Kit";
    private static final String STEP_DESC = "Use the checkbox(es) to select the solution kit(s) to install.  Optionally highlight the solution kit row(s) to perform a button action.  For example to enable the Set Instance Modifier button.";
    private final String[] tableColumnNames = {"", "Name", "Version", "Instance Modifier", "Description"};

    private JPanel mainPanel;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JTable solutionKitsTable;
    private JButton manageLicensesButton;
    private JPanel customizableButtonPanel;
    private JButton instanceModifierButton;

    private final SolutionKitAdmin solutionKitAdmin;
    private SolutionKitsConfig settings = null;
    private final Map<SolutionKit, Mappings> testMappings = new HashMap<>();
    private Set<SolutionKit> solutionKitsLoaded = new TreeSet<>();
    private Set<SolutionKit> solutionKitsSelected = new TreeSet<>();
    private Map<String, Set<String>> guidAndInstanceModifierMapFromUpgrade = new HashMap<>();

    public SolutionKitSelectionPanel() {
        super(null);
        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return !solutionKitsSelected.isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        testMappings.clear();
        this.settings = settings;

        if  (settings.getSelectedSolutionKits().isEmpty()) {
            initializeSolutionKitsLoaded();
            initializeSolutionKitsTable();
        }

        initializeInstanceModifierButton();
        addCustomUis(customizableButtonPanel, settings, null);
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setTestMappings(testMappings);
    }

    @Override
    public boolean onNextButton() {
        // Check whether any two selected solution kits have same GUID and instance modifier. If so, display warning
        // and stop installation.  This checking is applied to install and upgrade.
        final String errorReport = SolutionKitUtils.haveDuplicateSelectedSolutionKits(solutionKitsSelected);
        if (StringUtils.isNotBlank(errorReport)) {
            DialogDisplayer.showMessageDialog(
                this,
                "There are more than two selected solution kits having same GUID and Instance Modifier.\n" + errorReport,
                "Solution Kit Installation Warning",
                JOptionPane.WARNING_MESSAGE,
                null
            );

            return false;
        }

        return testInstall();
    }

    private boolean testInstall() {
        final AtomicBoolean success = new AtomicBoolean(false);
        String errorMessage;
        try {
            settings.setSelectedSolutionKits(solutionKitsSelected);
            final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(settings, solutionKitAdmin, new SkarProcessor(settings));
            solutionKitProcessor.testInstallOrUpgrade(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {

                @Override
                public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                    final String  result = AdminGuiUtils.doAsyncAdminWithException(
                            solutionKitAdmin,
                            SolutionKitSelectionPanel.this.getOwner(),
                            "Testing Solution Kit",
                            "The gateway is testing selected solution kit(s)",
                            solutionKitAdmin.testInstallAsync(loaded.left, loaded.middle, loaded.right),
                            false);

                    final Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(result)));
                    Mappings mappings = (Mappings) item.getContent();
                    if (null == mappings || null == mappings.getMappings()) {
                        DialogDisplayer.showMessageDialog(SolutionKitSelectionPanel.this, "Unexpected error: unable to get Solution Kit mappings.", "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
                        success.set(false);
                    }
                    testMappings.put(loaded.left, mappings);
                }

            });
            success.set(true);
        } catch (InvocationTargetException | IOException e) {
            testMappings.clear();
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        } catch (InterruptedException e) {
            testMappings.clear();
        } catch (SolutionKitException t) {  //for expected and foreseeable errors, display to the user for correction
            errorMessage = ExceptionUtils.getMessage(t);
            logger.log(Level.WARNING, errorMessage);
            DialogDisplayer.showMessageDialog(this, errorMessage + ".", "Solution Kit Install Error", JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable t) { //for unexpected, unhandled exceptions, show the standard BIG ERROR dialog
            ErrorMessageDialog errorMessageDialog = new ErrorMessageDialog(SolutionKitSelectionPanel.this.getOwner(), "Solution Kit Manager has encountered an unexpected error", t);
            Utilities.centerOnParentWindow(errorMessageDialog);
            DialogDisplayer.pack(errorMessageDialog);
            DialogDisplayer.display(errorMessageDialog);
        }

        return success.get();
    }

    @Override
    public void notifyActive() {
        // auto next step the wizard for solution kit with single item
        if (solutionKitsLoaded.size() == 1) {
            addAllExceptUnavailable();
            initializeSolutionKitsTable();
            enableDisableInstanceModifierButton();
            notifyListeners();
// todo auto next step bug: left step menu shows incorrect last step; comment out for now
//            if (!disableAutoNext && owner instanceof InstallSolutionKitWizard) {
//                ((InstallSolutionKitWizard) owner).clickButtonNext();
//            }
        }
//        disableAutoNext = true;
    }

    private void initialize() {
        Utilities.buttonToLink(selectAllButton);
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addAllExceptUnavailable();
                initializeSolutionKitsTable();
                enableDisableInstanceModifierButton();
                notifyListeners();
            }
        });

        Utilities.buttonToLink(clearAllButton);
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solutionKitsSelected.clear();
                initializeSolutionKitsTable();
                enableDisableInstanceModifierButton();
                notifyListeners();
            }
        });

        manageLicensesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onManageLicenses();
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void addAllExceptUnavailable() {
        solutionKitsSelected.clear();

        int index = 0;
        for (SolutionKit solutionKit: solutionKitsLoaded) {
            if (isEditableOrEnabledAt(index++)) {
                solutionKitsSelected.add(solutionKit);
            }
        }
    }

    private void initializeSolutionKitsLoaded() {
        solutionKitsLoaded = new TreeSet<>(settings.getLoadedSolutionKits().keySet());

        // The next update is only applied to Upgrade, not for Install
        if (! settings.isUpgrade()) return;

        guidAndInstanceModifierMapFromUpgrade = SolutionKitUtils.getGuidAndInstanceModifierMapFromUpgrade(settings.getSolutionKitsToUpgrade());

        final Set<String> guids = guidAndInstanceModifierMapFromUpgrade.keySet();
        final StringBuilder errorSB = new StringBuilder();
        String guid;
        Set<String> instanceModifierSet;

        for (SolutionKit loadedSK: solutionKitsLoaded) {
            guid = loadedSK.getSolutionKitGuid();
            if (guids.contains(guid)) {
                instanceModifierSet = guidAndInstanceModifierMapFromUpgrade.get(guid);
                if (instanceModifierSet.size() > 1) {
                    errorSB.append("Selecting '").append(loadedSK.getName()).append("'").append(" for upgrade will be disabled, since there are two or more solution kit instances using it to upgrade.\n");
                } else if (instanceModifierSet.size() == 1) {
                    loadedSK.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, instanceModifierSet.toArray(new String[]{})[0]);
                }
            }
        }

        if (errorSB.length() > 0) {
            DialogDisplayer.showMessageDialog(this,errorSB.toString(), "Solution Kit Upgrade Warning", JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void initializeSolutionKitsTable() {
        DefaultTableModel solutionKitsModel = new DefaultTableModel(populateData(), tableColumnNames) {
            @Override
            public Class getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return (column == 0) ? isEditableOrEnabledAt(row) : false;
            }
        };
        solutionKitsTable.setModel(solutionKitsModel);

        // Set the column widths
        solutionKitsTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Selected or not
        solutionKitsTable.getColumnModel().getColumn(1).setPreferredWidth(410); // Name
        solutionKitsTable.getColumnModel().getColumn(2).setPreferredWidth(80); // Version
        solutionKitsTable.getColumnModel().getColumn(3).setPreferredWidth(160); // Instance Modifier
        solutionKitsTable.getColumnModel().getColumn(4).setPreferredWidth(410); // Description

        solutionKitsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        solutionKitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableInstanceModifierButton();

                // If multiple row selected or the selected solution kit is not available for upgrade, then do not add custom ui
                int[] selectedRows = solutionKitsTable.getSelectedRows();
                if (selectedRows.length == 1 && isEditableOrEnabledAt(selectedRows[0])) {
                    addCustomUis(customizableButtonPanel, settings, getSolutionKitAt(solutionKitsTable.getSelectedRow()));
                } else {
                    addCustomUis(customizableButtonPanel, settings, null);
                }
                notifyListeners();
            }
        });
        solutionKitsModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                final int row = solutionKitsTable.getSelectedRow();
                final int col = solutionKitsTable.getSelectedColumn();
                if (col == 0) {
                    boolean selected = (Boolean) solutionKitsTable.getValueAt(row, col);
                    SolutionKit solutionKit = getSolutionKitAt(row);

                    if (selected) {
                        solutionKitsSelected.add(solutionKit);
                    } else {
                        solutionKitsSelected.remove(solutionKit);
                    }
                }

                notifyListeners();
            }
        });

        //Set up tool tips for the table cells.
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column != 0 && comp instanceof JComponent) {
                    ((JComponent)comp).setToolTipText(value == null? "N/A": String.valueOf(value));

                    comp.setEnabled(isEditableOrEnabledAt(row));
                }
                return comp;
            }
        };
        solutionKitsTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(2).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(3).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(4).setCellRenderer(renderer);
    }

    private Object[][] populateData() {
        final int row = solutionKitsLoaded.size();
        final int col = tableColumnNames.length;
        final Object[][] data = new Object[row][col];
        int rowIdx = 0;

        for (SolutionKit solutionKit: solutionKitsLoaded) {
            data[rowIdx++] = new Object[] {
                solutionKitsSelected.contains(solutionKit)? Boolean.TRUE : Boolean.FALSE,
                getSolutionKitDisplayName(solutionKit),
                solutionKit.getSolutionKitVersion(),
                solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY),
                solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY)
            };
        }

        return data;
    }

    private String getSolutionKitDisplayName(final SolutionKit solutionKit) {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (StringUtils.isEmpty(featureSet) || ConsoleLicenseManager.getInstance().isFeatureEnabled(featureSet)) {
            return solutionKit.getName();
        } else {
            return "(Unlicensed) " + solutionKit.getName();
        }
    }

    private boolean isEditableOrEnabledAt(final int index) {
        final SolutionKit loadedSolutionKit = getSolutionKitAt(index);
        final Set<String> instanceModifierSet = guidAndInstanceModifierMapFromUpgrade.get(loadedSolutionKit.getSolutionKitGuid());

        return
            // Case 1: Always true for Install
            (!settings.isUpgrade()) ||
            // Case 2: Upgrade
            (instanceModifierSet != null && instanceModifierSet.size() == 1);
    }

    private SolutionKit getSolutionKitAt(final int index) {
        return index == -1? null : solutionKitsLoaded.toArray(new SolutionKit[]{})[index];
    }

    private void enableDisableInstanceModifierButton() {
        boolean enabled = false;
        final int selectedRows[] = solutionKitsTable.getSelectedRows();
        int row;

        for (int i = 0; i < selectedRows.length; i++) {
            row = selectedRows[i];
            if (isEditableOrEnabledAt(row)) {
                enabled = true;
            } else {
                enabled = false;
                break;
            }
        }

        // The button is enabled if at least one solution kit is selected and it must be available for install or upgrade.
        instanceModifierButton.setEnabled(enabled);
    }

    private void initializeInstanceModifierButton() {
        ActionListener[] actionListeners = instanceModifierButton.getActionListeners();
        for (ActionListener actionListener: actionListeners) instanceModifierButton.removeActionListener(actionListener);

        // Instance modifier button only modifies the solution kits in highlighted rows
        instanceModifierButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] selectedRows = solutionKitsTable.getSelectedRows();
                if (selectedRows.length == 0) return;

                final List<SolutionKit> toBeModified = new ArrayList<>(selectedRows.length);
                final SolutionKit[] solutionKitsLoadedArray = solutionKitsLoaded.toArray(new SolutionKit[]{});
                for (int row : selectedRows) {
                    toBeModified.add(solutionKitsLoadedArray[row]);
                }

                final SolutionKitInstanceModifierDialog instanceModifierDialog = new SolutionKitInstanceModifierDialog(
                    TopComponents.getInstance().getTopParent(), toBeModified, settings
                );
                instanceModifierDialog.pack();
                Utilities.centerOnParentWindow(instanceModifierDialog);
                DialogDisplayer.display(instanceModifierDialog, new Runnable() {
                    @Override
                    public void run() {
                        initializeSolutionKitsTable();
                    }
                });
            }
        });

        enableDisableInstanceModifierButton();
    }

    private void onManageLicenses() {
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        ManageLicensesDialog dlg = new ManageLicensesDialog(mainWindow);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setModal(true);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                initializeSolutionKitsTable();
            }
        });
    }

    private void addCustomUis(final JPanel customizableButtonPanel, final SolutionKitsConfig settings, final SolutionKit selectedSolutionKit) {
        // Initially remove any button from customizableButtonPanel
        customizableButtonPanel.removeAll();

        // If the selected solution kit has customization with a custom UI, then create a button via the custom UI.
        // Otherwise, there is not any button created.
        if (selectedSolutionKit != null) {
            final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = settings.getCustomizations();
            final Pair<SolutionKit, SolutionKitCustomization> customization = customizations.get(selectedSolutionKit.getSolutionKitGuid());
            if (customization != null && customization.right != null) {
                final SolutionKitManagerUi customUi = customization.right.getCustomUi();
                if (customUi != null) {

                    // make metadata and bundle read-only to the custom UI; test for null b/c implementer can optionally null the context
                    SolutionKitManagerContext skContext = customUi.getContext();
                    if (skContext != null) {
                        try {
                            skContext.setSolutionKitMetadata(SolutionKitUtils.createDocument(selectedSolutionKit));
                            skContext.setMigrationBundle(settings.getBundleAsDocument(selectedSolutionKit));
                            final String uninstallBundle = selectedSolutionKit.getUninstallBundle();
                            if (StringUtils.isNotBlank(uninstallBundle)) {
                                skContext.setUninstallBundle(XmlUtil.stringToDocument(uninstallBundle));
                            }
                            skContext.setUpgrade(settings.isUpgrade());
                        } catch (TooManyChildElementsException | MissingRequiredElementException | SAXException e) {
                            final String errorMessage = "Solution kit metadata and/or bundle not available to custom UI class due to an unexpected error.";
                            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
                            DialogDisplayer.showMessageDialog(this, errorMessage + ".", "Custom UI Error", JOptionPane.ERROR_MESSAGE, null);
                        }
                    }

                    // call button create logic
                    customizableButtonPanel.add(customUi.createButton(customizableButtonPanel));
                }
            }
        }
    }

    /**
     * Called by IDEA's UI initialization when "Custom Create" is checked for any UI component.
     */
    private void createUIComponents() {
        customizableButtonPanel = new JPanel();
        customizableButtonPanel.setLayout(new BorderLayout());
    }
}