package com.l7tech.console.panels.bundles;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    private static final String MGMT_VERSION_NAMESPACE = "http://ns.l7tech.com/2010/04/gateway-management";

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

    final private Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions;

    public BundleConflictComponent(final JDialog parent, final String bundleId, final PolicyBundleDryRunResult dryRunResult, final boolean versionModified,
                                   final Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        this.selectedMigrationResolutions = selectedMigrationResolutions;

        // actionable conflicts
        final List<String> migrationErrorMappings = dryRunResult.getConflictsForItem(bundleId, PolicyBundleDryRunResult.DryRunItem.MIGRATION);
        actionableConflictScrollPane.setVisible(!migrationErrorMappings.isEmpty());
        buildMigrationConflicts(parent, versionModified, migrationErrorMappings);

        // hide actionable title panels if there's no actionable content
        existingEntityTitlePanel.setVisible(existingEntityPanel.getComponentCount() > 0);
        entityNotFoundTitlePanel.setVisible(entityNotFoundPanel.getComponentCount() > 0);
        resolutionErrorTitlePanel.setVisible(resolutionErrorPanel.getComponentCount() > 0);

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

    private void buildMigrationConflicts(final JDialog parent, final boolean versionModified, List<String> migrationErrorMappings) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        final SortedMap<String, SortedMap<String, MigrationErrorTypePanel>> errorTargetMapping = new TreeMap<>();

        for (String migrationErrorMapping: migrationErrorMappings) {
            try {
                final Dto dto = parseMappingXml(migrationErrorMapping);

                final ConflictDisplayerDialog.ErrorType errorType = ConflictDisplayerDialog.ErrorType.valueOf(dto.errorTypeStr);
                final EntityType entityType = EntityType.valueOf(dto.entityTypeStr);

                // treat this error and entity type combo like TargetNotFound
                if (errorType == InvalidResource && entityType == JDBC_CONNECTION) {
                    dto.errorTypeStr = TargetNotFound.toString();
                }

                SortedMap<String, MigrationErrorTypePanel> typeTargetMapping = errorTargetMapping.get(dto.errorTypeStr);
                if (typeTargetMapping == null) {
                    typeTargetMapping = new TreeMap<>();
                    errorTargetMapping.put(dto.errorTypeStr, typeTargetMapping);
                }

                MigrationErrorTypePanel targetDetail = typeTargetMapping.get(dto.entityTypeStr);
                if (targetDetail == null) {
                    targetDetail = new MigrationErrorTypePanel(parent, errorType, dto.entityTypeStr, versionModified, selectedMigrationResolutions);
                    typeTargetMapping.put(dto.entityTypeStr, targetDetail);

                    switch (errorType) {
                        case TargetExists:
                            existingEntityPanel.add(targetDetail.getContentPane());
                            break;
                        case TargetNotFound:
                            entityNotFoundPanel.add(targetDetail.getContentPane());
                            break;
                        default:
                            resolutionErrorPanel.add(new JLabel(dto.errorTypeStr + ": type=" + dto.entityTypeStr + ", srcId=" + dto.srcId + ", " + dto.errorMessage));
                            break;
                    }
                }

                targetDetail.addMigrationError(dto.name, dto.srcId, dto.extraInfo);
            } catch (Exception e) {
                logger.warning(ExceptionUtils.getMessage(e));
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        ExceptionUtils.getMessage(e), "Resolving Migration Issues Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private class Dto {
        public String errorTypeStr, entityTypeStr, srcId, errorMessage, name, extraInfo;

        public Dto(String errorTypeStr, String entityTypeStr, String srcId, String errorMessage, String name, String extraInfo) {
            this.errorTypeStr = errorTypeStr;
            this.entityTypeStr = entityTypeStr;
            this.srcId = srcId;
            this.errorMessage = errorMessage;
            this.name = name;
            this.extraInfo = extraInfo;
        }
    }

    /**
     * Parse mapping xml for error type, entity type, source id
     *      TODO we should be able to move this XML parsing logic to the server?
     *          i.e. MigrationBundleInstaller via DryRunInstallPolicyBundleEvent (server only package visibility) to
     *               PolicyBundleInstallerAdminAbstractImpl via PolicyBundleDryRunResult (visible to both server and console)
     */
    private Dto parseMappingXml(final String migrationErrorMapping) throws Exception {
        final Document issueDoc = XmlUtil.stringToDocument(migrationErrorMapping);
        final Element mappingElement = (Element) issueDoc.getFirstChild();

        final String errorTypeStr = mappingElement.getAttribute("errorType");
        if (StringUtils.isEmpty(errorTypeStr)) {
            throw new Exception("Unexpected mapping format: errorType attribute missing.");
        }

        final String entityTypeStr = mappingElement.getAttribute("type");
        if (StringUtils.isEmpty(entityTypeStr)) {
            throw new Exception("Unexpected mapping format: type attribute missing.");
        }

        final String srcId = mappingElement.getAttribute("srcId");
        if (StringUtils.isEmpty(srcId)) {
            throw new Exception("Unexpected mapping format: srcId attribute missing.");
        }

        final Element errorMessageElement = XmlUtil.findFirstDescendantElement(mappingElement, MGMT_VERSION_NAMESPACE, "StringValue");
        String errorMessage = "";
        String name = null;
        if (errorMessageElement != null) {
            errorMessage = errorMessageElement.getFirstChild().getNodeValue();
            int nameStartIdx = errorMessage.indexOf("Name=");

            if (nameStartIdx >= 0){
                int nameEndIdx = errorMessage.indexOf(", ", nameStartIdx);
                name = errorMessage.substring(nameStartIdx + 5, nameEndIdx);
                if (name == null || "null".equals(name)) {
                    name = "N/A";
                }
            }
        }

        // Save policy xml into Dto.extraInfo
        final Element resourceEl = XmlUtil.findFirstDescendantElement(mappingElement, MGMT_VERSION_NAMESPACE, "Resource");
        String policyXml = null;
        if (resourceEl != null) {
            policyXml = resourceEl.getTextContent();
        }

        return new Dto(errorTypeStr, entityTypeStr, srcId, errorMessage, name, policyXml);
    }
}