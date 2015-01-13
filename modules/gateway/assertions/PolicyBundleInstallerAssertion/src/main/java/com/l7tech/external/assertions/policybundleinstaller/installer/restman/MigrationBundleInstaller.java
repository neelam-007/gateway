package com.l7tech.external.assertions.policybundleinstaller.installer.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.installer.BaseInstaller;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.MigrationDryRunResult;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.BaseGatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller.InstallationException;
import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.MIGRATION_BUNDLE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker.*;
import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage.*;

/**
 * Install migration bundle using the Gateway Management REST API.
 */
public class MigrationBundleInstaller extends BaseInstaller {
    private final RestmanInvoker restmanInvoker;

    @Nullable
    private PolicyBundleInstallerCallback policyBundleInstallerCallback;

    public MigrationBundleInstaller(@NotNull final PolicyBundleInstallerContext context,
                                    @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                    @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback);
        this.restmanInvoker = new RestmanInvoker(cancelledCallback, gatewayManagementInvoker);
    }

    public void setPolicyBundleInstallerCallback(@Nullable PolicyBundleInstallerCallback policyBundleInstallerCallback) {
        this.policyBundleInstallerCallback = policyBundleInstallerCallback;
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, AccessDeniedManagementResponse, PolicyBundleInstallerCallback.CallbackException {
        checkInterrupted();

        final BundleInfo bundleInfo = context.getBundleInfo();
        final BundleItem migrationBundle = MIGRATION_BUNDLE;
        migrationBundle.setVersion(bundleInfo.getVersion());
        final Document bundle = context.getBundleResolver().getBundleItem(bundleInfo.getId(), migrationBundle, true);
        if (bundle != null) {
            logger.finest("Dry run checking by performing a test bundle import without committing.");
            checkInterrupted();

            // pre migration import callback
            if (policyBundleInstallerCallback != null) {
                policyBundleInstallerCallback.preMigrationBundleImport(bundleInfo, bundle);
            }

            try {
                final RestmanMessage requestMessage = new RestmanMessage(bundle);
                setTargetIdInRootFolderMapping(requestMessage);

                // handle version modifier
                final String versionModifier = context.getInstallationPrefix();
                if (isValidVersionModifier(versionModifier)) {
                    new VersionModifier(requestMessage, versionModifier).apply();
                }

                // get mappings, set action, add Properties, add Property
                final List<String> entityIdsFromBundleRequestMessageMappings = new ArrayList<>();
                for (Element mapping : requestMessage.getMappings()) {
                    entityIdsFromBundleRequestMessageMappings.add(mapping.getAttribute("srcId"));

                    Element propertiesElement = DomUtils.findFirstChildElementByName(mapping, MGMT_VERSION_NAMESPACE, "Properties");
                    if (propertiesElement == null) {
                        propertiesElement = DomUtils.createAndAppendElement(mapping, "Properties");
                    }

                    // don't set mapping property if already set
                    Element propertyElement = DomUtils.findFirstChildElementByName(propertiesElement, MGMT_VERSION_NAMESPACE, "Property");
                    if (propertyElement == null ) {
                        propertyElement = DomUtils.createAndAppendElement(propertiesElement, "Property");
                        switch (valueOf(mapping.getAttribute(MAPPING_TYPE_ATTRIBUTE))) {
                            case JDBC_CONNECTION: case SECURE_PASSWORD:
                                propertyElement.setAttribute("key", MAPPING_ACTION_PROP_KEY_FAIL_ON_NEW);
                                break;
                            default:
                                propertyElement.setAttribute("key", MAPPING_ACTION_PROP_KEY_FAIL_ON_EXISTING);
                                break;
                        }
                        final Element booleanValue = DomUtils.createAndAppendElement(propertyElement, "BooleanValue");
                        booleanValue.setTextContent(Boolean.toString(true));
                    }
                }

                final String requestXml = requestMessage.getAsString();

                final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
                final String revisionComment = getPolicyRevisionComment(bundleInfo);
                pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?test=true&versionComment=" + URLEncoder.encode(revisionComment, UTF_8));   // test=true will perform the import but not commit it

                // save bundle xml for headless command line
                dryRunEvent.addComponentIdToBundleXml(bundleInfo.getId(), requestXml);

                final Pair<AssertionStatus, RestmanMessage> dryRunResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
                final RestmanMessage dryRunMessage = dryRunResult.right;
                final List<String> idListOfExistingTargetFolders = new ArrayList<>();

                // parse for mapping errors
                if (dryRunMessage.hasMappingError()) {
                    final List<Element> mappingErrors = dryRunMessage.getMappingErrors();

                    for (Element mappingError : mappingErrors) {
                        // Add "l7" namespace into each mapping element
                        RestmanMessage.setL7XmlNs(mappingError);

                        // Save a representation of each mapping element
                        MigrationDryRunResult convertedResult = convertToDryRunResult(mappingError, requestMessage);
                        dryRunEvent.addMigrationErrorMapping(convertedResult);

                        if (convertedResult.getEntityTypeStr().equals(EntityType.FOLDER.toString()) && convertedResult.getErrorTypeStr().equals("TargetExists")) {
                            idListOfExistingTargetFolders.add(convertedResult.getSrcId());
                        }
                    }
                }

                // Find entities deleted from the bundle, while they are still in the target gateway.
                findDeletedEntities(dryRunEvent, entityIdsFromBundleRequestMessageMappings, idListOfExistingTargetFolders, requestMessage.hasRootFolderItem());
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception serializing bundle document", e);
            } catch (UnexpectedManagementResponse e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        }
    }

    /**
     * If a target folder is selected, then set an attribute "targetId" as the target folder goid in a root folder mapping.
     * Note: if such root folder mapping does not exist, then create a new one first.
     *
     * @param requestMessage: a Restman Request Message, which will be modified
     */
    private void setTargetIdInRootFolderMapping(final RestmanMessage requestMessage) {
        final Goid targetFolderId = context.getFolderGoid();

        if (requestMessage.hasRootFolderMapping()) {
            // Add an attribute targetId into the root folder mapping
            requestMessage.setRootFolderMappingTargetId(targetFolderId.toString());
        } else {
            // Create a new mapping with the root folder as srcId and the chosen folder as targetId
            requestMessage.addFolderMapping(targetFolderId.toString());
        }
    }

    /**
     * Parse error mapping for error type, entity type, source id.  And get resource policy xml.
     */
    private MigrationDryRunResult convertToDryRunResult(final Element mappingError, RestmanMessage restmanMessage) throws IOException, UnexpectedManagementResponse {
        final String errorTypeStr = mappingError.getAttribute(MAPPING_ERROR_TYPE_ATTRIBUTE);
        if (StringUtils.isEmpty(errorTypeStr)) {
            throw new UnexpectedManagementResponse("Unexpected mapping format: errorType attribute missing.");
        }

        final String entityTypeStr = mappingError.getAttribute(MAPPING_TYPE_ATTRIBUTE);
        if (StringUtils.isEmpty(entityTypeStr)) {
            throw new UnexpectedManagementResponse("Unexpected mapping format: type attribute missing.");
        }

        final String srcId = mappingError.getAttribute(MAPPING_SRC_ID_ATTRIBUTE);
        if (StringUtils.isEmpty(srcId)) {
            throw new UnexpectedManagementResponse("Unexpected mapping format: srcId attribute missing.");
        }

        final Element errorMessageElement = XmlUtil.findFirstDescendantElement(mappingError, MGMT_VERSION_NAMESPACE, "StringValue");
        String errorMessage = "";
        String name = null;
        if (errorMessageElement != null) {
            errorMessage = errorMessageElement.getFirstChild().getNodeValue();
            int nameStartIdx = errorMessage.indexOf("Name=");

            if (nameStartIdx >= 0){
                int nameEndIdx = errorMessage.indexOf(", ", nameStartIdx);
                name = errorMessage.substring(nameStartIdx + 5, nameEndIdx);
                if (name != null && "null".equals(name)) {
                    name = "N/A";
                }
            }
        }

        final String policyResourceXml = getPolicyXmlForErrorMapping(errorTypeStr, EntityType.valueOf(entityTypeStr), srcId, restmanMessage);

        return new MigrationDryRunResult(errorTypeStr, entityTypeStr, srcId, errorMessage, name, policyResourceXml);
    }

    /**
     * Find entities deleted from the bundle, while they are still in the target gateway.
     *
     * @param dryRunEvent: used to add MigrationErrorMapping
     * @param entityIdsFromBundleRequestMessageMappings: all entity ids defined in the mappings from the bundle request message
     * @param idListOfExistingTargetFolders: ids of the folders, which exist in the target gateway
     * @param isRootNodeTargetFold: a flag indicates if the installed folder is a root folder or not.
     */
    private void findDeletedEntities(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent,
                                     @NotNull final List<String> entityIdsFromBundleRequestMessageMappings,
                                     @NotNull final List<String> idListOfExistingTargetFolders,
                                     final boolean isRootNodeTargetFold) throws InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse, IOException {
        if (entityIdsFromBundleRequestMessageMappings.isEmpty()) return;

        // Assume the 1st request mapping is ALWAYS the root folder and the 2nd request mapping is ALWAYS the top level bundle folder
        final String targetFolderGoid = isRootNodeTargetFold? entityIdsFromBundleRequestMessageMappings.get(0) : entityIdsFromBundleRequestMessageMappings.get(1);
        if (StringUtils.isEmpty(targetFolderGoid)) return;

        // If the target folder does not exist in the target gateway, ignore this method
        if (! idListOfExistingTargetFolders.contains(targetFolderGoid)) return;

        // get migration bundle for the target folder
        final String requestXml = "";
        final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
        pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?folder=" + targetFolderGoid);
        pec.setVariable(VAR_RESTMAN_ACTION, "GET");

        final Pair<AssertionStatus, RestmanMessage> targetFolderBundleResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
        final RestmanMessage targetFolderBundle = targetFolderBundleResult.right;
        if (targetFolderBundle == null) {
            throw new RuntimeException("Retrieving Entities failed: response from restman is null.");
        } else if (targetFolderBundle.hasMappingError()) {
            try {
                throw new RuntimeException("Retrieving Entities failed: " + targetFolderBundle.getMappingErrorsAsString());
            } catch (IOException e) {
                throw new RuntimeException("Retrieving Entities failed:", e);
            }
        }

        final List<Element> targetFolderBundleMappings = targetFolderBundle.getMappings();
        final List<String> entityIdsInTargetFolderBundle = new ArrayList<>(targetFolderBundleMappings.size());
        for (Element mapping : targetFolderBundleMappings) {
            String action = mapping.getAttribute("action");
            if (action == null || EntityMappingInstructions.MappingAction.valueOf(action) == EntityMappingInstructions.MappingAction.Ignore) {
                continue;
            }

            String srcId = mapping.getAttribute("srcId");
            if (StringUtils.isEmpty(srcId)) {
                continue;
            }

            entityIdsInTargetFolderBundle.add(srcId);
        }

        // Make a copy of entityIdsInTargetFolderBundle
        final List<String> deletedEntityIds = new ArrayList<>();
        deletedEntityIds.addAll(entityIdsInTargetFolderBundle);

        // Remove from entityIdsInTargetFolderBundle all of its elements that are not contained in entityIdsFromBundleRequestMessageMappings
        entityIdsInTargetFolderBundle.retainAll(entityIdsFromBundleRequestMessageMappings);

        // Get all elements not contained in entityIdsFromBundleRequestMessageMappings
        deletedEntityIds.removeAll(entityIdsInTargetFolderBundle);

        for (String srcId: deletedEntityIds) {
            dryRunEvent.addMigrationErrorMapping(
                new MigrationDryRunResult("EntityDeleted", targetFolderBundle.getEntityType(srcId), srcId, null, targetFolderBundle.getEntityName(srcId), null)
            );
        }
    }

    /**
     *  Get policy xml if errorType is "TargetExists" and entity type is either Service or Policy.
     *  This is can be used for comparing bundle policy with existing target policy.
     */
    private String getPolicyXmlForErrorMapping(final String errorTypeStr, final EntityType entityType,
                                               final String srcId, RestmanMessage restmanMessage) throws IOException {
        // com.l7tech.gateway.api.Mapping.ErrorType.TargetExists not accessible from this class
        if (!"TargetExists".equals(errorTypeStr) || (entityType != SERVICE && entityType != POLICY)) {
            return null;
        }

        return restmanMessage.getResourceSetPolicy(srcId);
    }

    public void install() throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        final BundleInfo bundleInfo = context.getBundleInfo();
        final BundleItem migrationBundle = MIGRATION_BUNDLE;
        migrationBundle.setVersion(bundleInfo.getVersion());
        final Document policyBundle = context.getBundleResolver().getBundleItem(bundleInfo.getId(), migrationBundle, true);
        install(policyBundle);
    }

    public void install(@NotNull final String subFolder) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        final BundleInfo bundleInfo = context.getBundleInfo();
        final BundleItem migrationBundle = MIGRATION_BUNDLE;
        migrationBundle.setVersion(bundleInfo.getVersion());
        final Document policyBundle = context.getBundleResolver().getBundleItem(bundleInfo.getId(), subFolder, migrationBundle, true);
        install(policyBundle);
    }

    @NotNull
    @Override
    public BaseGatewayManagementInvoker getManagementClient() {
        return restmanInvoker;
    }

    private void install(@Nullable final Document bundle) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        checkInterrupted();
        final BundleInfo bundleInfo = context.getBundleInfo();

        if (bundle == null) {
            logger.info("No policies to install for bundle " + bundleInfo);
        } else {
            logger.finest("Installing bundle.");

            // pre migration import callback
            if (policyBundleInstallerCallback != null) {
                try {
                    policyBundleInstallerCallback.preMigrationBundleImport(bundleInfo, bundle);
                } catch (PolicyBundleInstallerCallback.CallbackException e) {
                    throw new InstallationException(e);
                }
            }

            final Map<String, String> deletedEntities = new HashMap<>();
            String requestXml;
            final RestmanMessage requestMessage = new RestmanMessage(bundle);
            setTargetIdInRootFolderMapping(requestMessage);

            try {
                // handle version modifier
                final String versionModifier = context.getInstallationPrefix();
                if (isValidVersionModifier(versionModifier)) {
                    new VersionModifier(requestMessage, versionModifier).apply();
                }

                // handle migration action override, resolve the target issues based on the user's options
                final Map<String, Pair<String, Properties>> migrationOverrides = context.getMigrationBundleOverrides();
                if (migrationOverrides != null && !migrationOverrides.isEmpty()) {
                    for (String id : migrationOverrides.keySet()) {
                        Pair<String, Properties> mapping = migrationOverrides.get(id);

                        if (mapping.left.equals("Delete")) {
                            deletedEntities.put(id, (String) mapping.right.get(id));
                        } else {
                            requestMessage.setMappingAction(id, EntityMappingInstructions.MappingAction.valueOf(mapping.left), mapping.right);
                        }
                    }
                }

                requestXml = requestMessage.getAsString();
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception serializing bundle document", e);
            }

            // set policy revision comment
            PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
            final String policyRevisionComment = getPolicyRevisionComment(bundleInfo);
            try {
                pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?versionComment=" + URLEncoder.encode(policyRevisionComment, UTF_8));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unexpected exception encoding policy revision comment '" + policyRevisionComment + "' using " + UTF_8, e);
            }

            final Pair<AssertionStatus, RestmanMessage> installResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
            final RestmanMessage installMessage = installResult.right;

            // check for errors
            if (installMessage.hasMappingError()) {
                try {
                    throw new RuntimeException(" installation failed. " + System.getProperty("line.separator") + installMessage.getMappingErrorsAsString());
                }  catch (IOException e) {
                    throw new RuntimeException(" installation failed. ", e);
                }
            }

            // Delete entities selected by users from the migration conflicts window
            requestXml = "";
            pec = restmanInvoker.getContext(requestXml);
            for (String deletedEntityId: deletedEntities.keySet()) {
                String entityTypeName = deletedEntities.get(deletedEntityId);

                pec.setVariable(VAR_RESTMAN_URI, "1.0/" + getEntityTypeUriName(entityTypeName) + "/" + deletedEntityId);
                pec.setVariable(VAR_RESTMAN_ACTION, "DELETE");

                try {
                    restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
                } catch (Exception e) {
                    boolean throwException = true;
                    if (ExceptionUtils.causedBy(e, SAXParseException.class)) {
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("Premature end of file")) {
                            // For REST Management deletion, there is no response message back.
                            // So in this case, ignore a SAXParseException exception with a message "Premature end of file".
                            throwException = false;
                        }
                    }
                    if (throwException) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Get a uri name for an entity type
     * Note: "Assertion Security Zone", "Group", "Interface Tag", and "Resource Document" are not handled in this method.
     * In REST Management API, "Assertion Security Zone" and "Group" don't have "Delete" operation.
     * In EntityType, there are no entity types matched to "Interface Tag" and "Resource Document", even though they are defined in REST Management API.
     *
     * Restman URI constants are not accessible from this modular assertion.
     * URIs are defined in package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.
     * For example ActiveConnectorResource.activeConnectors_URI = "activeConnectors";
     *
     * @param entityTypeName: the name of an entity type
     * @return a uri string representing entity type
     */
    private String getEntityTypeUriName(@NotNull String entityTypeName) {
        switch (EntityType.valueOf(entityTypeName)) {
            case CLUSTER_PROPERTY:
                return "clusterProperties";
            case CUSTOM_KEY_VALUE_STORE:
                return "customKeyValues";
            case EMAIL_LISTENER:
                return "emailListeners";
            case ENCAPSULATED_ASSERTION:
                return "encapsulatedAssertions";
            case FOLDER:
                return "folders";
            case GENERIC:
                return "genericEntities";
            case HTTP_CONFIGURATION:
                return "httpConfigurations";
            case ID_PROVIDER_CONFIG:
                return "identityProviders";
            case JDBC_CONNECTION:
                return "jdbcConnections";
            case JMS_CONNECTION:
                return "jmsDestinations";
            case POLICY:
                return "policies";
            case POLICY_ALIAS:
                return "policyAliases";
            case REVOCATION_CHECK_POLICY:
                return "revocationCheckingPolicies";
            case RBAC_ROLE:
                return "roles";
            case SECURE_PASSWORD:
                return "passwords";
            case SECURITY_ZONE:
                return "securityZones";
            case SERVICE:
                return "services";
            case SERVICE_ALIAS:
                return "serviceAliases";
            case SITEMINDER_CONFIGURATION:
                return "siteMinderConfigurations";
            case SSG_ACTIVE_CONNECTOR:
                return "activeConnectors";
            case SSG_CONNECTOR:
                return "listenPorts";
            case SSG_KEY_ENTRY:
                return "privateKeys";
            case TRUSTED_CERT:
                return "trustedCertificates";
            case USER:
                return "users";
            default:
                throw new RuntimeException("Not supported entity type: " + entityTypeName);
        }
    }
}