package com.l7tech.console.panels.bundles;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ConflictDisplayerDialog extends JDialog {
    public static enum MappingAction {
        // com.l7tech.server.bundling.EntityMappingInstructions.MappingAction not accessible in console packages
        NewOrUpdate, NewOrExisting, AlwaysCreateNew, Delete, Ignore
    }

    public static enum ErrorType {
        // com.l7tech.gateway.api.Mapping.ErrorType not accessible in console packages
        TargetExists, TargetNotFound, EntityDeleted, UniqueKeyConflict, CannotReplaceDependency, ImproperMapping, InvalidResource, Unknown
    }

    public static final String MAPPING_TARGET_ID_ATTRIBUTE = "targetId";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane;
    private boolean wasoked;
    private boolean hasInfoOnlyConflict;
    private List<String> bundleIds = new ArrayList<>();

    // holds the aggregate of selected resolutions across all bundles
    private Map<String, Map<String, Pair<MappingAction, Properties>>> migrationBundlesActionOverrides = new Hashtable<>();

    public ConflictDisplayerDialog(final Window owner,
                                   final List<BundleInfo> bundleInfos,
                                   final PolicyBundleDryRunResult dryRunResult,
                                   final boolean versionModified) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        super(owner, "Conflicts detected");
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        getRootPane().setDefaultButton(buttonCancel);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tabbedPane.removeAll();
        for (BundleInfo bundleInfo : bundleInfos) {
            final String bundleId = bundleInfo.getId();

            // Store each bundle id (i.e., component id), which will be used in the method getMigrationBundlesActionOverrides()
            bundleIds.add(bundleId);

            if (dryRunResult.anyConflictsForBundle(bundleId)) {
                Map<String, Pair<MappingAction, Properties>> selectedMigrationResolutions = migrationBundlesActionOverrides.get(bundleId);
                if (selectedMigrationResolutions == null) {
                    selectedMigrationResolutions = new Hashtable<>();
                    migrationBundlesActionOverrides.put(bundleId, selectedMigrationResolutions);
                }

                BundleConflictComponent bundleConflictComponent = new BundleConflictComponent(this, bundleId, dryRunResult, versionModified, selectedMigrationResolutions);
                JPanel mainPanel = bundleConflictComponent.getMainPanel();

                if (bundleConflictComponent.hasInfoOnlyConflict()) {
                    hasInfoOnlyConflict = true;
                }

                if (bundleConflictComponent.hasUnresolvableEntityConflict()) {
                    buttonOK.setEnabled(false);
                }

                tabbedPane.add(bundleInfo.getName(), mainPanel);
            }
        }
    }

    public boolean wasOKed() {
        return wasoked;
    }

    /**
     * Due to limited visibility of com.l7tech.server.bundling.EntityMappingInstructions.MappingAction, we transport the enum as String
     * @return map of selected migration resolutions
     */
    public Map<String, Map<String, Pair<String, Properties>>> getMigrationBundlesActionOverrides() {
        Map<String, Map<String, Pair<String, Properties>>> returnedMap = new Hashtable<>(migrationBundlesActionOverrides.size());

        for (String bundleId: bundleIds) {
            Map<String, Pair<MappingAction, Properties>> selectedMigrationResolutions = migrationBundlesActionOverrides.get(bundleId);
            // If the element map does not exist or empty, then ignore the bundle-override map.
            if (selectedMigrationResolutions == null || selectedMigrationResolutions.isEmpty()) continue;

            Map<String, Pair<String, Properties>> convertedSelectedMigrationResolutions = new Hashtable<>(selectedMigrationResolutions.size());
            for (String srcId: selectedMigrationResolutions.keySet()) {
                Pair<MappingAction, Properties> actionAndProperties = selectedMigrationResolutions.get(srcId);
                convertedSelectedMigrationResolutions.put(srcId, new Pair<>(actionAndProperties.left.toString(), actionAndProperties.right));
            }

            // Each bundle component has its own override action map.
            returnedMap.put(bundleId, convertedSelectedMigrationResolutions);
        }

        return returnedMap;
    }

    private void onOK() {
        if (hasInfoOnlyConflict) {
            DialogDisplayer.showSafeConfirmDialog(
                    this,
                    "<html><left><p>Be aware that continuing installation with detected conflicts may result in partial installation of components: (1) Components with</p>" +
                            "<p>detected conflicts will NOT be installed. (2) References to a conflicted component will be replaced with references to a version of</p>" +
                            "<p>the same component on the target Gateway, if one exists. These references may occur in service policies or policy fragments.</p></left></html>",
                    "Confirm Installation with Conflicts",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.CANCEL_OPTION) {
                                return;
                            }
                            wasoked = true;
                            dispose();
                        }
                    }
            );
        } else {
            wasoked = true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }
}