package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitCustomization;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
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
    private Map<SolutionKit, Integer> instanceModifierMaxLengthMap = new HashMap<>();

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
        solutionKitsModel.deselectAll();
        settings.setSelectedSolutionKits(new TreeSet<SolutionKit>());
        solutionKitsModel.setRows(new ArrayList<>(settings.getLoadedSolutionKits().keySet()));
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
        boolean success = false;
        String errorMessage = "";

        // For installation, check if instance modifier is unique for a selected solution kit.
        // However, this checking will be ignored for any solution kit upgrade bundle.
        final String bundle = settings.getBundleAsString(solutionKit);

        if (// Case 1: User runs Install
            settings.getSolutionKitToUpgrade() == null ||
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
            Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Testing Solution Kit",
                    "The gateway is testing selected solution kit(s)",
                    solutionKitAdmin.testInstall(solutionKit, bundle),
                    false);

            if (result.isLeft()) {
                errorMessage = result.left();
                logger.log(Level.WARNING, errorMessage);
            } else if (result.isRight()) {
                Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(result.right())));
                Mappings mappings = (Mappings)item.getContent();
                testMappings.put(solutionKit, mappings);
                success = true;
            }
        } catch (InvocationTargetException | IOException e) {
            testMappings.clear();
            errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
        } catch (InterruptedException e) {
            testMappings.clear();
            return false;
        }

        if (!success) {
            DialogDisplayer.showMessageDialog(this, errorMessage, "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
        }

        return success;
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
            }
        });

        Utilities.buttonToLink(clearAllButton);
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solutionKitsModel.deselectAll();
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
                final SolutionKit selectedSolutionKit = solutionKitsModel.getRowObject(solutionKitsTable.getSelectedRow());
                addCustomUis(customizableButtonPanel, settings, selectedSolutionKit);
                enableDisableInstanceModifierButton();
            }
        });
        solutionKitsModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
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
                if(comp instanceof JComponent) {
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

    private void enableDisableInstanceModifierButton() {
        final boolean enabled = solutionKitsTable.getSelectedRow() != -1;
        instanceModifierButton.setEnabled(enabled);
    }

    private void initializeInstanceModifierButton() {
        ActionListener[] actionListeners = instanceModifierButton.getActionListeners();
        for (ActionListener actionListener: actionListeners) instanceModifierButton.removeActionListener(actionListener);

        instanceModifierButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int selectedIdx = solutionKitsTable.getSelectedRow();
                if (selectedIdx == -1) return;

                final SolutionKit solutionKit = solutionKitsModel.getRowObject(solutionKitsTable.convertRowIndexToModel(selectedIdx));
                final SolutionKitInstanceModifierDialog instanceModifierDialog = new SolutionKitInstanceModifierDialog(
                    TopComponents.getInstance().getTopParent(),
                    solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), // Old instance modifier
                    getMaxLengthForInstanceModifier(solutionKit)
                );
                instanceModifierDialog.pack();
                Utilities.centerOnParentWindow(instanceModifierDialog);
                DialogDisplayer.display(instanceModifierDialog);

                if (instanceModifierDialog.isOK()) {
                    final String newInstanceModifier = instanceModifierDialog.getInstanceModifier();
                    if (StringUtils.isEmpty(newInstanceModifier)) return;
                    solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, newInstanceModifier);

                    solutionKitsModel.fireTableDataChanged();
                }
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
     * Calculate what the max length of instance modifier could be.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    private int getMaxLengthForInstanceModifier(@NotNull final SolutionKit solutionKit) {
        Integer maxLength = instanceModifierMaxLengthMap.get(solutionKit);

        if (maxLength == null) {
            Map<SolutionKit, Bundle> kitBundleMap = settings.getLoadedSolutionKits();
            Bundle bundle = kitBundleMap.get(solutionKit);

            // Compute a new max length
            maxLength = getMaxLengthForInstanceModifier(bundle.getReferences());

            // Save the max value for this solution kit in the map
            instanceModifierMaxLengthMap.put(solutionKit, maxLength);
        }

        return maxLength;
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

    /**
     * Calculate what the max length of instance modifier could be.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * TODO make this validation available to headless interface (i.e. SolutionKitManagerResource)
     *
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    public static int getMaxLengthForInstanceModifier(@NotNull final List<Item> bundleReferenceItems) {
        int maxAllowedLengthAllow = Integer.MAX_VALUE;
        int allowedLength;
        String entityName;
        EntityType entityType;

        for (Item item: bundleReferenceItems) {
            entityName = item.getName();
            entityType = EntityType.valueOf(item.getType());

            if (entityType == EntityType.FOLDER || entityType == EntityType.ENCAPSULATED_ASSERTION) {
                // The format of a folder name is "<folder_name> <instance_modifier>".
                // The format of a encapsulated assertion name is "<instance_modifier> <encapsulated_assertion_name>".
                // The max length of a folder name or an encapsulated assertion name is 128.
                allowedLength = 128 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.POLICY) {
                // The format of a policy name is "<instance_modifier> <policy_name>".
                // The max length of a policy name is 255.
                allowedLength = 255 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.SERVICE) {
                // The max length of a service routing uri is 128
                // The format of a service routing uri is "/<instance_modifier>/<service_name>".
                allowedLength = 128 - entityName.length() - 2; // 2 represents two chars of '/' in the routing uri.
            }  else {
                continue;
            }

            if (maxAllowedLengthAllow > allowedLength) {
                maxAllowedLengthAllow = allowedLength;
            }
        }

        if (maxAllowedLengthAllow < 0) maxAllowedLengthAllow = 0;

        return maxAllowedLengthAllow;
    }
}