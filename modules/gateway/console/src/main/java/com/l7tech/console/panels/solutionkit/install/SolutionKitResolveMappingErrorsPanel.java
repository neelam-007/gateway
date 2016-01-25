package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Pair;
import com.sun.istack.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.Mapping.ErrorType.TargetNotFound;

/**
 * Wizard panel which allows the user resolve entity mapping errors in a solution kit.
 */
public class SolutionKitResolveMappingErrorsPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final String STEP_LABEL = "Resolve Entity Conflict";
    private static final String STEP_DESC = "Resolve entity conflicts (or click Finish if none exist).";

    private JPanel mainPanel;
    private JTabbedPane solutionKitMappingsTabbedPane;
    private JButton resolveButton;

    private Map<String, Pair<SolutionKit, Map<String, String>>> resolvedEntityIdsMap = new HashMap<>();    // the map of a value: key = from id. value  = to id.

    private Map<String, Integer> guidAndInstanceToActiveErrorMap = new HashMap<>(); //key = solution kit guid, value = number of active errors
    private List<String> guidAndInstanceArray = new ArrayList<>(); //list of all the guidAndInstanceArray

    private SolutionKitsConfig settings = null;

    public SolutionKitResolveMappingErrorsPanel() {
        super(null);
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
        return super.canAdvance();
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        resolvedEntityIdsMap.clear();
        guidAndInstanceToActiveErrorMap.clear();
        guidAndInstanceArray.clear();
        solutionKitMappingsTabbedPane.removeAll();

        this.settings = settings;

        for (final SolutionKit solutionKit: settings.getSelectedSolutionKits()) {
            Bundle bundle = settings.getBundle(solutionKit);
            if (bundle == null) continue;

            Mappings mappings = settings.getTestMappings(solutionKit);
            if (mappings == null) continue;

            Map<String, String> resolvedEntityIds = new HashMap<>();
            resolvedEntityIdsMap.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, resolvedEntityIds));

            final SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel("Name");
            solutionKitMappingsPanel.setData(solutionKit, mappings, bundle, resolvedEntityIds);
            solutionKitMappingsPanel.setDoubleClickAction(resolveButton);
            solutionKitMappingsPanel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    refreshMappingsTableButtons();
                }
            });

            String solutionKitName = solutionKit.getName();
            solutionKitMappingsTabbedPane.add(solutionKitName, solutionKitMappingsPanel);

            String guidAndInstance = solutionKit.getSolutionKitGuid()+((solutionKit.getProperty("InstanceModifier")==null)? "" : solutionKit.getProperty("InstanceModifier"));
            guidAndInstanceArray.add(guidAndInstance);

            // Look through mappings for error type
            for (Mapping mapping : mappings.getMappings()) {
                if (mapping.getErrorType() != null) {
                    if (guidAndInstanceToActiveErrorMap.containsKey(guidAndInstance)) {
                        guidAndInstanceToActiveErrorMap.put(guidAndInstance, guidAndInstanceToActiveErrorMap.get(guidAndInstance) + 1);
                    } else {
                        guidAndInstanceToActiveErrorMap.put(guidAndInstance, 1);
                        //Set the tab containing errors to red
                        final int tabIndex = guidAndInstanceArray.indexOf(guidAndInstance);
                        solutionKitMappingsTabbedPane.setBackgroundAt(tabIndex, Color.red);
                        solutionKitMappingsTabbedPane.setForegroundAt(tabIndex, Color.red);
                        solutionKitMappingsTabbedPane.setSelectedIndex(tabIndex);
                    }
                }

            }
        }
        refreshMappingsTableButtons();
    }

    @Override
    public void storeSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        settings.setResolvedEntityIds(resolvedEntityIdsMap);
    }

    @Override
    public boolean canFinish() {
        return areAllConflictsResolved();
    }

    private void initialize() {
        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SolutionKitMappingsPanel selectedSKMappingsPanel =
                    (SolutionKitMappingsPanel) solutionKitMappingsTabbedPane.getSelectedComponent();
                onResolve(selectedSKMappingsPanel);
                notifyListeners();
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void refreshMappingsTableButtons() {
        boolean enabled = false;
        boolean resolved;
        final Component selectedComponent = solutionKitMappingsTabbedPane.getSelectedComponent();

        if (selectedComponent instanceof SolutionKitMappingsPanel) {
            final SolutionKitMappingsPanel solutionKitMappingsPanel = (SolutionKitMappingsPanel) selectedComponent;
            Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
            if (mapping != null) {
                //for enabling/disabling the resolveButton, make sure we also take into account any resolvedOther issues (for entities that
                //have been deleted from the original install.
                resolved = solutionKitMappingsPanel.getResolvedEntityIds().get(mapping.getSrcId()) == null && !Boolean.valueOf(solutionKitMappingsPanel.getResolvedOther().get(mapping.getSrcId()));
                if (mapping.getErrorType() != null && resolved) {
                    enabled = true;
                }
            }
        }
        resolveButton.setEnabled(enabled);
    }

    private boolean areAllConflictsResolved() {
        for (int i = 0; i < solutionKitMappingsTabbedPane.getTabCount(); i++) {
            if (guidAndInstanceToActiveErrorMap.containsKey(guidAndInstanceArray.get(i)) && guidAndInstanceToActiveErrorMap.get(guidAndInstanceArray.get(i)) > 0) {
                return false;
            }
        }

        return true;
    }

    private void onResolve(@NotNull final SolutionKitMappingsPanel solutionKitMappingsPanel) {
        Mapping mapping = solutionKitMappingsPanel.getSelectedMapping();
        if (mapping == null) {
            return;
        }

        Mapping.ErrorType errorType = mapping.getErrorType();
        if (errorType == null) {
            return;
        }

        final SolutionKit solutionKit = solutionKitMappingsPanel.getSolutionKit();
        //Check whether any entities are missing from the original installation during an upgrade, and if so
        //inform the user that they need to re-install the original install before they can upgrade
        if (isMissingOriginalInstall(mapping, settings.getInstallMappings(solutionKit.getSolutionKitGuid()))) {
            DialogDisplayer.showMessageDialog(SolutionKitResolveMappingErrorsPanel.this, "An entity from the original install may be missing.  Please re-install the original Solution Kit before attempting to upgrade.",
                    "Error During Upgrade - Missing Entity", JOptionPane.ERROR_MESSAGE, null);
        } else {
            if (!SolutionKitsConfig.allowOverride(mapping)) {
                DialogDisplayer.showMessageDialog(SolutionKitResolveMappingErrorsPanel.this, "<html>This Solution Kit does not allow overriding of this mapping." +
                                "<br>This mapping requires the property '" + SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' be set to true by the .skar file author.</html>",
                        "Error Resolving Entity Conflict", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            final SolutionKitResolveMappingDialog dlg = new SolutionKitResolveMappingDialog(this.getOwner(), mapping,
                    solutionKitMappingsPanel.getBundleItems().get(mapping.getSrcId())
            );
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            DialogDisplayer.display(dlg);

            if (dlg.isConfirmed()) {
                solutionKitMappingsPanel.getResolvedEntityIds().put(mapping.getSrcId(), dlg.getResolvedId());
                refreshPanel(solutionKitMappingsPanel);
            }
        }
    }

    private void refreshPanel(@NotNull final SolutionKitMappingsPanel solutionKitMappingsPanel) {
        solutionKitMappingsPanel.reload();

        String guidAndInstance = guidAndInstanceArray.get(solutionKitMappingsTabbedPane.getSelectedIndex());

        int count = guidAndInstanceToActiveErrorMap.get(guidAndInstance) -1;
        guidAndInstanceToActiveErrorMap.put(guidAndInstance, count);

        //set the tab back to white if conditions are good
        if (count < 1) {
            guidAndInstanceToActiveErrorMap.remove(guidAndInstance);
            solutionKitMappingsTabbedPane.setBackgroundAt(solutionKitMappingsTabbedPane.getSelectedIndex(),Color.WHITE);
            solutionKitMappingsTabbedPane.setForegroundAt(solutionKitMappingsTabbedPane.getSelectedIndex(), Color.BLACK);
        }
    }

    /**
     * Convenience method to check for any missing entities from the original install while performing an upgrade
     * We check not only for "TargetNotFound" but also if there is a "FailOnNew" property that is set to true - this
     * is what identifies a missing entity versus a standard RESTman error that could be caused by other reasons.
     *
     * @param responseMapping the response mapping
     * @param installMappings the install mappings
     * @return true if missing, false if not
     */
    private boolean isMissingOriginalInstall(@NotNull final Mapping responseMapping, @NotNull final Map<String, Mapping> installMappings) {
        if (responseMapping.getErrorType() == TargetNotFound) {
            Mapping installMapping = installMappings.get(responseMapping.getSrcId());
            if (installMapping != null) {
                Boolean isFailOnNew = installMapping.getProperty("FailOnNew");
                if (isFailOnNew == null || !isFailOnNew) {
                    return true;
                }
            }
        }
        return false;
    }
}