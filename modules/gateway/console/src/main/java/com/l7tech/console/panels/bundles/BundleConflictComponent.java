package com.l7tech.console.panels.bundles;

import com.l7tech.console.util.SortedListModel;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

/**
 * Displays all conflicts for a specific bundle component
 */
public class BundleConflictComponent extends JPanel {

    private JList<String> serviceConflictList;
    private JList<String> policyConflictList;
    private JList<String> missingJdbcConnList;
    private JList<String> missingModAssList;
    private JPanel mainPanel;
    private JPanel serviceConflictsPanel;
    private JPanel policyConflictsPanel;
    private JPanel jdbcPanel;
    private JPanel modAssPanel;

    public BundleConflictComponent(final String bundleId, final PolicyBundleDryRunResult dryRunResult) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        final List<String> serviceConflicts = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.SERVICES);
        buildJList(serviceConflictList, serviceConflictsPanel, serviceConflicts);

        final List<String> conflictPolicies = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.POLICIES);
        buildJList(policyConflictList, policyConflictsPanel, conflictPolicies);
        final List<String> missingJdbcConns = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.JDBC_CONNECTIONS);
        buildJList(missingJdbcConnList, jdbcPanel, missingJdbcConns);
        final List<String> missingModAsses = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.MODULAR_ASSERTION);
        buildJList(missingModAssList, modAssPanel, missingModAsses);

    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void buildJList(JList<String> listToBuild, JPanel containingPanel, List<String> items) {
        if (!items.isEmpty()) {
            final SortedListModel<String> model = new SortedListModel<String>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });

            for (String item : items) {
                model.add(item);
            }
            //noinspection unchecked
            listToBuild.setModel(model);
        } else {
            containingPanel.setEnabled(false);
        }
    }
}