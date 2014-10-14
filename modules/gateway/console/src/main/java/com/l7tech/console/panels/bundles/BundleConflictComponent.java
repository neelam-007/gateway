package com.l7tech.console.panels.bundles;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Displays all conflicts for a specific bundle component
 */
public class BundleConflictComponent extends JPanel {
    // todo is there a way to directly reference the enum constants in the following?
    //    - com.l7tech.server.bundling.EntityMappingInstructions.MappingAction
    //    - com.l7tech.gateway.api.Mapping.ErrorType
    public static final String ERROR_TYPE_TARGET_EXISTS = "TargetExists";
    public static final String ERROR_TYPE_TARGET_NOT_FOUND = "TargetNotFound";
    public static final String ERROR_TYPE_INVALID_RESOURCE = "InvalidResource";
    public static final String ERROR_ACTION_NEW_OR_UPDATE = "NewOrUpdate";

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

    final private Map<String, String> selectedMigrationResolutions;

    public BundleConflictComponent(final JDialog parent, final String bundleId, final PolicyBundleDryRunResult dryRunResult, final Map<String, String> selectedMigrationResolutions) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        this.selectedMigrationResolutions = selectedMigrationResolutions;

        // actionable conflicts
        final List<String> migrationErrorMappings = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.MIGRATION);
        actionableConflictScrollPane.setVisible(!migrationErrorMappings.isEmpty());
        buildMigrationConflicts(parent, migrationErrorMappings);

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

    private void createUIComponents() {
        existingEntityPanel = new JPanel();
        existingEntityPanel.setLayout(new BoxLayout(existingEntityPanel, BoxLayout.Y_AXIS));

        entityNotFoundPanel = new JPanel();
        entityNotFoundPanel.setLayout(new BoxLayout(entityNotFoundPanel, BoxLayout.Y_AXIS));
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

    private void buildMigrationConflicts(final JDialog parent, List<String> migrationErrorMappings) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        final SortedMap<String, SortedMap<String, List<Pair<String,String>>>> errorTargetMapping = new TreeMap<>();

        for (String migrationErrorMapping: migrationErrorMappings) {
            try {
                Document issueDoc = XmlUtil.stringToDocument(migrationErrorMapping);
                Element mappingElmt = (Element) issueDoc.getFirstChild();
                String errorType = mappingElmt.getAttribute("errorType");
                String entityType = mappingElmt.getAttribute("type");
                String srcId = mappingElmt.getAttribute("srcId");

                Element stringValueElmt = XmlUtil.findFirstDescendantElement(mappingElmt, "http://ns.l7tech.com/2010/04/gateway-management", "StringValue");
                String stringValue = stringValueElmt.getFirstChild().getNodeValue();
                int nameStartIdx = stringValue.indexOf("Name=");
                String name = null;
                if (nameStartIdx >= 0){
                    int nameEndIdx = stringValue.indexOf(", ", nameStartIdx);
                    name = stringValue.substring(nameStartIdx + 5, nameEndIdx);
                }
                if (name == null || name.equals("null")) name = "N/A";

                if (errorType == null || errorType.trim().isEmpty()) {
                    throw new Exception("Error Type not specified.");
                }

                // The below case will be treated as errorType equal to ERROR_TYPE_TARGET_NOT_FOUND.
                if (errorType.equals(ERROR_TYPE_INVALID_RESOURCE) && EntityType.JDBC_CONNECTION.toString().equals(entityType)) {
                    errorType = ERROR_TYPE_TARGET_NOT_FOUND;
                }

                if (errorType.equals(ERROR_TYPE_TARGET_EXISTS) || errorType.equals(ERROR_TYPE_TARGET_NOT_FOUND)) {
                    SortedMap<String, List<Pair<String,String>>> typeTargetMapping = errorTargetMapping.get(errorType);
                    if (typeTargetMapping == null) {
                        typeTargetMapping = new TreeMap<>();
                        errorTargetMapping.put(errorType, typeTargetMapping);
                    }

                    List<Pair<String,String>> targetDetailList = typeTargetMapping.get(entityType);
                    if (targetDetailList == null) {
                        targetDetailList = new ArrayList<>();
                        typeTargetMapping.put(entityType, targetDetailList);
                    }

                    targetDetailList.add(new Pair<>(name, srcId));
                } else {
                    throw new Exception("Error Type: " + errorType + " not supported.");
                }
            } catch (Exception e) {
                logger.warning(ExceptionUtils.getMessage(e));
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        ExceptionUtils.getMessage(e), "Resolving Migration Issues Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }

        if (! errorTargetMapping.isEmpty()) {
            for (String errorType: errorTargetMapping.keySet()) {
                for (String targetType: errorTargetMapping.get(errorType).keySet()) {
                    if (errorType.equals(ERROR_TYPE_TARGET_EXISTS)) {
                        existingEntityPanel.add(
                                new MigrationErrorTypePanel(parent, errorType, targetType, errorTargetMapping.get(errorType).get(targetType), selectedMigrationResolutions).getContentPane()
                        );
                    } else if (errorType.equals(ERROR_TYPE_TARGET_NOT_FOUND)) {
                        entityNotFoundPanel.add(
                                new MigrationErrorTypePanel(parent, errorType, targetType, errorTargetMapping.get(errorType).get(targetType), selectedMigrationResolutions).getContentPane()
                        );
                    }
                }
            }
        }
    }
}