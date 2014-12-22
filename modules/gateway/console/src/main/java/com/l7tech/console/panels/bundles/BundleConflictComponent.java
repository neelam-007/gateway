package com.l7tech.console.panels.bundles;

import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.bundle.MigrationDryRunResult;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.ErrorType.InvalidResource;
import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.ErrorType.TargetNotFound;
import static com.l7tech.objectmodel.EntityType.JDBC_CONNECTION;

/**
 * Displays all conflicts for a specific bundle component
 */
public class BundleConflictComponent extends JPanel {
    private static final Logger logger = Logger.getLogger(BundleConflictComponent.class.getName());

    private JList<String> serviceConflictList;
    private JList<String> policyConflictList;
    private JList<String> certificateConflictList;
    private JList<String> encapsulatedAssertionConflictList;
    private JList<String> missingJdbcConnList;
    private JList<String> missingAssertionList;
    private JPanel mainPanel;
    private JPanel serviceConflictsPanel;
    private JPanel policyConflictsPanel;
    private JPanel certificateConflictsPanel;
    private JPanel encapsulatedAssertionConflictsPanel;
    private JPanel jdbcPanel;
    private JPanel assertionPanel;
    private JPanel existingEntityPanel;
    private JPanel entityNotFoundPanel;
    private JPanel infoOnlyConflictPanel;
    private JScrollPane actionableConflictScrollPane;
    private JPanel resolutionErrorPanel;
    private JPanel existingEntityTitlePanel;
    private JPanel entityNotFoundTitlePanel;
    private JPanel resolutionErrorTitlePanel;
    private JPanel deletedEntityTitlePanel;
    private JPanel deletedEntityPanel;
    private JButton selectAllExistingButton;
    private JButton selectAllUpdateButton;
    private JButton selectAllCreateButton;

    final private Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions;

    public BundleConflictComponent(final JDialog parent, final String bundleId, final PolicyBundleDryRunResult dryRunResult, final boolean versionModified,
                                   final Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        this.selectedMigrationResolutions = selectedMigrationResolutions;

        // actionable conflicts
        final List<MigrationDryRunResult> migrationErrorMappings = dryRunResult.getMigrationDryRunResults(bundleId);
        actionableConflictScrollPane.setVisible(!migrationErrorMappings.isEmpty());
        buildMigrationConflicts(parent, versionModified, migrationErrorMappings);

        // hide actionable title panels if there's no actionable content
        existingEntityTitlePanel.setVisible(existingEntityPanel.getComponentCount() > 0);
        entityNotFoundTitlePanel.setVisible(entityNotFoundPanel.getComponentCount() > 0);
        deletedEntityTitlePanel.setVisible(deletedEntityPanel.getComponentCount() > 0);
        resolutionErrorTitlePanel.setVisible(resolutionErrorPanel.getComponentCount() > 0);

        if (existingEntityTitlePanel.isVisible()) {
            Utilities.buttonToLink(selectAllExistingButton);
            Utilities.buttonToLink(selectAllUpdateButton);
            Utilities.buttonToLink(selectAllCreateButton);
        }

        // initialize to not show infoOnlyConflictPanel (until one of the info-only list below is not empty)
        infoOnlyConflictPanel.setVisible(false);

        // info-only conflicts
        final List<String> serviceConflicts = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.SERVICES);
        buildJList(serviceConflictList, serviceConflictsPanel, serviceConflicts);

        final List<String> conflictPolicies = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.POLICIES);
        buildJList(policyConflictList, policyConflictsPanel, conflictPolicies);

        final List<String> conflictCertificates = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.CERTIFICATES);
        buildJList(certificateConflictList, certificateConflictsPanel, conflictCertificates);

        final List<String> conflictEncapsulatedAssertions = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.ENCAPSULATED_ASSERTION);
        buildJList(encapsulatedAssertionConflictList, encapsulatedAssertionConflictsPanel, conflictEncapsulatedAssertions);

        final List<String> missingJdbcConns = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.JDBC_CONNECTIONS);
        buildJList(missingJdbcConnList, jdbcPanel, missingJdbcConns);

        final List<String> missingAssertions = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.ASSERTIONS);
        buildJList(missingAssertionList, assertionPanel, missingAssertions);
    }

    @NotNull
    public JPanel getMainPanel() {
        return mainPanel;
    }

    boolean hasInfoOnlyConflict() {
        return infoOnlyConflictPanel.isVisible();
    }

    boolean hasUnresolvableEntityConflict() {
        return resolutionErrorPanel.getComponentCount() > 0;
    }

    private void createUIComponents() {
        existingEntityPanel = new JPanel();
        existingEntityPanel.setLayout(new BoxLayout(existingEntityPanel, BoxLayout.Y_AXIS));

        entityNotFoundPanel = new JPanel();
        entityNotFoundPanel.setLayout(new BoxLayout(entityNotFoundPanel, BoxLayout.Y_AXIS));

        deletedEntityPanel = new JPanel();
        deletedEntityPanel.setLayout(new BoxLayout(deletedEntityPanel, BoxLayout.Y_AXIS));

        resolutionErrorPanel = new JPanel();
        resolutionErrorPanel.setLayout(new BoxLayout(resolutionErrorPanel, BoxLayout.Y_AXIS));
    }

    private void buildJList(JList<String> listToBuild, JPanel containingPanel, List<String> items) {
        if (!items.isEmpty()) {
            // display if any list is not empty
            infoOnlyConflictPanel.setVisible(true);

            final SortedListModel<String> model = new SortedListModel<>(new Comparator<String>() {
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

    private void buildMigrationConflicts(final JDialog parent, final boolean versionModified, List<MigrationDryRunResult> migrationErrorMappings) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        final SortedMap<String, SortedMap<String, MigrationErrorTypePanel>> errorTargetMapping = new TreeMap<>();

        for (MigrationDryRunResult migrationErrorMapping : migrationErrorMappings) {
            try {
                final ConflictDisplayerDialog.ErrorType errorType = ConflictDisplayerDialog.ErrorType.valueOf(migrationErrorMapping.getErrorTypeStr());
                final EntityType entityType = EntityType.valueOf(migrationErrorMapping.getEntityTypeStr());

                // treat this error and entity type combo like TargetNotFound
                if (errorType == InvalidResource && entityType == JDBC_CONNECTION) {
                    migrationErrorMapping.setErrorTypeStr(TargetNotFound.toString());
                }

                SortedMap<String, MigrationErrorTypePanel> typeTargetMapping = errorTargetMapping.get(migrationErrorMapping.getErrorTypeStr());
                if (typeTargetMapping == null) {
                    typeTargetMapping = new TreeMap<>();
                    errorTargetMapping.put(migrationErrorMapping.getErrorTypeStr(), typeTargetMapping);
                }

                MigrationErrorTypePanel targetDetail = typeTargetMapping.get(migrationErrorMapping.getEntityTypeStr());
                if (targetDetail == null) {
                    targetDetail = new MigrationErrorTypePanel(parent, errorType, EntityType.valueOf(migrationErrorMapping.getEntityTypeStr()), versionModified, selectedMigrationResolutions);
                    typeTargetMapping.put(migrationErrorMapping.getEntityTypeStr(), targetDetail);

                    switch (errorType) {
                        case TargetExists:
                            existingEntityPanel.add(targetDetail.getContentPane());
                            break;
                        case TargetNotFound:
                            entityNotFoundPanel.add(targetDetail.getContentPane());
                            break;
                        case EntityDeleted:
                            deletedEntityPanel.add(targetDetail.getContentPane());
                            break;
                        default:
                            resolutionErrorPanel.add(new JLabel(migrationErrorMapping.getErrorTypeStr() + ": type=" + migrationErrorMapping.getEntityTypeStr() +
                                    ", srcId=" + migrationErrorMapping.getSrcId() + ", " + migrationErrorMapping.getErrorMessage()));
                            break;
                    }
                }

                targetDetail.addMigrationError(migrationErrorMapping.getName(), migrationErrorMapping.getSrcId(), migrationErrorMapping.getPolicyResourceXml(), new JButton[]{selectAllExistingButton, selectAllUpdateButton, selectAllCreateButton});
            } catch (Exception e) {
                logger.warning(ExceptionUtils.getMessage(e));
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        ExceptionUtils.getMessage(e), "Resolving Migration Issues Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }
}