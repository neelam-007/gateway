package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitCustomization;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gui.ErrorMessageDialog;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Wizard panel which allows the user to select component(s) within a solution kit to install.
 */
public class SolutionKitSelectionPanel extends WizardStepPanel<SolutionKitsConfig>  {
    private static final Logger logger = Logger.getLogger(SolutionKitSelectionPanel.class.getName());
    private static final String STEP_LABEL = "Select Solution Kit";
    private static final String STEP_DESC = "Select solution kit(s) to install.";

    private JPanel mainPanel;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JTable solutionKitsTable;
    private JButton manageLicensesButton;
    private JPanel customizableButtonPanel;
    private JButton instanceModifierButton;

    private SelectableTableModel<SolutionKit> solutionKitsModel;

    private final SolutionKitAdmin solutionKitAdmin;
    private SolutionKitsConfig settings = null;
    private Map<SolutionKit, Mappings> testMappings = new HashMap<>();
    private List<SolutionKit> solutionKitsToUpgrade = new ArrayList<>();

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
        return !solutionKitsModel.getSelected().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        testMappings.clear();

        if  (settings.getSelectedSolutionKits().isEmpty()) {
            solutionKitsModel.setRows(new ArrayList<>(settings.getLoadedSolutionKits().keySet()));
        }
        solutionKitsToUpgrade = settings.getSolutionKitsToUpgrade();
        this.settings = settings;

        initializeInstanceModifierButton();
        addCustomUis(customizableButtonPanel, settings, null);
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setSelectedSolutionKits(new TreeSet<>(solutionKitsModel.getSelected()));
        settings.setTestMappings(testMappings);
    }

    @Override
    public boolean onNextButton() {
        for (SolutionKit solutionKit: solutionKitsModel.getSelected()) {
            boolean success = testInstall(solutionKit);
            if (! success) return false;
        }

        return true;
    }

    private boolean testInstall(final SolutionKit solutionKit) {
        String errorMessage = "";

        // For installation, check if instance modifier is unique for a selected solution kit.
        // However, this checking will be ignored for any solution kit upgrade bundle.
        final String bundle = settings.getBundleAsString(solutionKit);

        //final List<SolutionKit> solutionKitsToUpgrade = settings.getSolutionKitsToUpgrade();
        final SolutionKit solutionKitToUpgrade = SolutionKitUtils.searchSolutionKitByGuidToUpgrade(solutionKitsToUpgrade, solutionKit.getSolutionKitGuid());

        if (// Case 1: User runs Install
            solutionKitToUpgrade == null ||
            // Case 2: User runs Upgrade but the skar file does not contain UpgradeBundle.xml.  We treat this case same as Install.
            !settings.isUpgradeInfoProvided(solutionKit)) {

            final boolean duplicateInstanceModifierFound = ! checkInstanceModifierUniqueness(solutionKit);
            if (duplicateInstanceModifierFound) return false;
        }

        // invoke custom callback
        try {
            new SkarProcessor(settings).invokeCustomCallback(solutionKit);
        } catch (SolutionKitException e) {
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        }

        if (bundle == null) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error: unable to get Solution Kit bundle.", "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }
        try {
            final String result = AdminGuiUtils.doAsyncAdminWithException(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Testing Solution Kit",
                    "The gateway is testing selected solution kit(s)",
                    solutionKitAdmin.testInstall(solutionKit, bundle),
                    false
            );

            Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(result)));
            Mappings mappings = (Mappings) item.getContent();
            testMappings.put(solutionKit, mappings);

        } catch (InvocationTargetException | IOException e) {
            testMappings.clear();
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        } catch (InterruptedException e) {
            testMappings.clear();
        } catch (SolutionKitException t) {  //for expected and foreseeable errors, display to the user for correction
            errorMessage = ExceptionUtils.getMessage(t);
            logger.log(Level.WARNING, errorMessage);
            DialogDisplayer.showMessageDialog(this, errorMessage + ".  See Policy Manager log for more details.", "Solution Kit Install Error", JOptionPane.ERROR_MESSAGE, null);
        } catch (Throwable t) { //for unexpected, unhandled exceptions, show the standard BIG ERROR dialog
            ErrorMessageDialog errorMessageDialog = new ErrorMessageDialog(SolutionKitSelectionPanel.this.getOwner(), "Solution Kit Manager has encountered an unexpected error", t);
            Utilities.centerOnParentWindow(errorMessageDialog);
            DialogDisplayer.pack(errorMessageDialog);
            DialogDisplayer.display(errorMessageDialog);
        }

        return true; //No errors, we're good to go
    }

    @Override
    public void notifyActive() {
        // auto next step the wizard for solution kit with single item
        if (solutionKitsModel.getRowCount() == 1) {
            solutionKitsModel.select(0);
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
                solutionKitsModel.selectAll();
                enableDisableInstanceModifierButton();
            }
        });

        Utilities.buttonToLink(clearAllButton);
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solutionKitsModel.deselectAll();
                enableDisableInstanceModifierButton();
            }
        });

        manageLicensesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onManageLicenses();
            }
        });

        //noinspection unchecked
        solutionKitsModel = TableUtil.configureSelectableTable(solutionKitsTable, true, 0,
            column("", 50, 50, 100, new Functions.Unary<Boolean, SolutionKit>() {
                @Override
                public Boolean call(SolutionKit solutionKit) {
                    return solutionKitsModel.isSelected(solutionKit);
                }
            }),
            column("Name", 50, 400, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
                    if (StringUtils.isEmpty(featureSet) || ConsoleLicenseManager.getInstance().isFeatureEnabled(featureSet)) {
                        return solutionKit.getName();
                    } else {
                        return "(Unlicensed) " + solutionKit.getName();
                    }
                }
            }),
            column("Version", 50, 60, 500, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getSolutionKitVersion();
                }
            }),
            column("Instance Modifier", 50, 240, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
                }
            }),
            column("Description", 50, 500, 5000, new Functions.Unary<String, SolutionKit>() {
                @Override
                public String call(SolutionKit solutionKit) {
                    return solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY);
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        solutionKitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                addCustomUis(customizableButtonPanel, settings, getSelectedSolutionKit());
            }
        });
        solutionKitsModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                enableDisableInstanceModifierButton();
                notifyListeners();
            }
        });
        Utilities.setRowSorter(solutionKitsTable, solutionKitsModel,
            new int[]{1, 2, 3, 4}, new boolean[]{true, true, true, true},
            new Comparator[]{String.CASE_INSENSITIVE_ORDER, String.CASE_INSENSITIVE_ORDER, String.CASE_INSENSITIVE_ORDER, String.CASE_INSENSITIVE_ORDER}
        );

        //Set up tool tips for the table cells.
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // If user chooses to upgrade, make sure that all rows not for upgrade should be disabled.
                final SolutionKit solutionKit = getSolutionKitAtRow(row);
                final SolutionKit solutionKitToUpgrade = SolutionKitUtils.searchSolutionKitByGuidToUpgrade(solutionKitsToUpgrade, solutionKit.getSolutionKitGuid());
                final boolean isEnabled = solutionKitsToUpgrade.isEmpty() || solutionKitToUpgrade != null;
                comp.setEnabled(isEnabled);

                if (column != 0 && comp instanceof JComponent) {
                    ((JComponent)comp).setToolTipText(value == null? "N/A": String.valueOf(value));
                }

                return comp;
            }
        };
        solutionKitsTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(2).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(3).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(4).setCellRenderer(renderer);

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private SolutionKit getSelectedSolutionKit() {
        final int selectedIdx = solutionKitsTable.getSelectedRow();
        return selectedIdx == -1?
            null :
            solutionKitsModel.getRowObject(solutionKitsTable.convertRowIndexToModel(selectedIdx));
    }

    private SolutionKit getSolutionKitAtRow(final int row) {
        return row == -1?
            null :
            solutionKitsModel.getRowObject(solutionKitsTable.convertRowIndexToModel(row));
    }

    private void enableDisableInstanceModifierButton() {
        final boolean enabled = solutionKitsModel.getSelected().size() > 0;
        instanceModifierButton.setEnabled(enabled);
    }

    private void initializeInstanceModifierButton() {
        ActionListener[] actionListeners = instanceModifierButton.getActionListeners();
        for (ActionListener actionListener: actionListeners) instanceModifierButton.removeActionListener(actionListener);

        instanceModifierButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<SolutionKit> solutionKitsSelected = solutionKitsModel.getSelected();
                if (solutionKitsSelected.isEmpty()) return;

                final SolutionKitInstanceModifierDialog instanceModifierDialog = new SolutionKitInstanceModifierDialog(
                    TopComponents.getInstance().getTopParent(), solutionKitsSelected, settings
                );
                instanceModifierDialog.pack();
                Utilities.centerOnParentWindow(instanceModifierDialog);
                DialogDisplayer.display(instanceModifierDialog, new Runnable() {
                    @Override
                    public void run() {
                        solutionKitsModel.fireTableDataChanged();
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
                solutionKitsModel.fireTableDataChanged();
            }
        });
    }

    private void addCustomUis(final JPanel customizableButtonPanel, final SolutionKitsConfig settings, final SolutionKit selectedSolutionKit) {
        // Initially remove any button from customizableButtonPanel
        customizableButtonPanel.removeAll();

        // If the selected solution kit has customization with a custom UI, then create a button via the custom UI.
        // Otherwise, there is not any button created.
        if (selectedSolutionKit != null) {
            final Map<SolutionKit, SolutionKitCustomization> customizations = settings.getCustomizations();
            final SolutionKitCustomization customization = customizations.get(selectedSolutionKit);
            if (customization != null) {
                final SolutionKitManagerUi customUi = customization.getCustomUi();
                if (customUi != null) {

                    // make metadata and bundle read-only to the custom UI; test for null b/c implementer can optionally null the context
                    SolutionKitManagerContext skContext = customUi.getContext();
                    if (skContext != null) {
                        try {
                            skContext.setSolutionKitMetadata(SolutionKitUtils.createDocument(selectedSolutionKit));
                            skContext.setMigrationBundle(settings.getBundleAsDocument(selectedSolutionKit));
                        } catch (TooManyChildElementsException | MissingRequiredElementException e) {
                            final String errorMessage = "Solution kit metadata and/or bundle not available to custom UI class due to an unexpected error.";
                            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
                            DialogDisplayer.showMessageDialog(this, errorMessage + "  See Policy Manager log for more details.", "Custom UI Error", JOptionPane.ERROR_MESSAGE, null);
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

    /**
     * Check if instance modifier is unique for a selected solution kit.
     *
     * @param solutionKit: a solution kit whose instance modifier will be checked.
     * @return true if the instance modifier is valid.  That is, it does not violate the instance modifier uniqueness for a given solution kit.
     */
    private boolean checkInstanceModifierUniqueness(@NotNull final SolutionKit solutionKit) {
        final Map<String, List<String>> usedInstanceModifiersMap = settings.getInstanceModifiers();

        final String solutionKitGuid = solutionKit.getSolutionKitGuid();
        if (usedInstanceModifiersMap.keySet().contains(solutionKitGuid)) {
            final List<String> usedInstanceModifiers = usedInstanceModifiersMap.get(solutionKitGuid);
            final String newInstanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if (usedInstanceModifiers != null && usedInstanceModifiers.contains(newInstanceModifier)) {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "The solution kit '" + solutionKit.getName() + "' has already used " +
                        (newInstanceModifier == null? "an empty instance modifier" : "the instance modifier, '" + newInstanceModifier + "'") +
                        ".\nPlease specify a different instance modifier to continue installation.",
                    "Duplicate Instance Modifier Warning", JOptionPane.WARNING_MESSAGE, null);

                return false;
            }
        }

        return true;
    }
}