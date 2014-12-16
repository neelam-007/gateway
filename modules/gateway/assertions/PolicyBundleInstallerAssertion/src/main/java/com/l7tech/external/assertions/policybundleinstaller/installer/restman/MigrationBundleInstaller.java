package com.l7tech.external.assertions.policybundleinstaller.installer.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.installer.BaseInstaller;
import com.l7tech.objectmodel.EntityType;
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

                // handle version modifier
                final String versionModifier = context.getInstallationPrefix();
                if (isValidVersionModifier(versionModifier)) {
                    new VersionModifier(requestMessage, versionModifier).apply();
                }

                // get mappings, set action, add Properties, add Property
                final List<String> entityIdsInBundleMapping = new ArrayList<>();
                for (Element mapping : requestMessage.getMappings()) {
                    entityIdsInBundleMapping.add(mapping.getAttribute("srcId"));

                    Element propertiesElement = DomUtils.findFirstChildElementByName(mapping, MGMT_VERSION_NAMESPACE, "Properties");
                    if (propertiesElement == null) {
                        propertiesElement = DomUtils.createAndAppendElement(mapping, "Properties");
                    }
                    final Element property = DomUtils.createAndAppendElement(propertiesElement, "Property");
                    switch (valueOf(mapping.getAttribute(MAPPING_TYPE_ATTRIBUTE))) {
                        case JDBC_CONNECTION: case SECURE_PASSWORD:
                            property.setAttribute("key", MAPPING_ACTION_PROP_KEY_FAIL_ON_NEW);
                            break;
                        default:
                            property.setAttribute("key", MAPPING_ACTION_PROP_KEY_FAIL_ON_EXISTING);
                            break;
                    }
                    final Element booleanValue = DomUtils.createAndAppendElement(property, "BooleanValue");
                    booleanValue.setTextContent(Boolean.toString(true));
                }

                final String requestXml = requestMessage.getAsString();

                final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
                final String revisionComment = getPolicyRevisionComment(bundleInfo);
                pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?test=true&versionComment=" + URLEncoder.encode(revisionComment, UTF_8));   // test=true will perform the import but not commit it

                // save bundle xml for headless command line
                dryRunEvent.addComponentIdToBundleXml(bundleInfo.getId(), requestXml);

                final Pair<AssertionStatus, RestmanMessage> dryRunResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
                final RestmanMessage dryRunMessage = dryRunResult.right;

                // parse for mapping errors
                if (dryRunMessage.hasMappingError()) {
                    final List<Element> mappingErrors = dryRunMessage.getMappingErrors();

                    for (Element mappingError : mappingErrors) {
                        // Add "l7" namespace into each mapping element
                        RestmanMessage.setL7XmlNs(mappingError);

                        // Save a representation of each mapping element
                        dryRunEvent.addMigrationErrorMapping(convertToDryRunResult(mappingError, requestMessage));
                    }
                }

                // Find entities deleted from the bundle, while they are still in the target gateway.
                findDeletedEntities(dryRunEvent, entityIdsInBundleMapping, requestMessage.hasRootNodeItem());
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception serializing bundle document", e);
            } catch (UnexpectedManagementResponse e) {
                throw new RuntimeException("Unexpected exception", e);
            }
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
     */
    private void findDeletedEntities(final DryRunInstallPolicyBundleEvent dryRunEvent, final List<String> entityIdsInBundleMapping, final boolean isRootNodeTargetFold)
        throws InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse, IOException {
        if (entityIdsInBundleMapping == null || entityIdsInBundleMapping.isEmpty()) return;

        final String targetFoldGoid = isRootNodeTargetFold? entityIdsInBundleMapping.get(0) : entityIdsInBundleMapping.get(1);
        if (targetFoldGoid == null || targetFoldGoid.trim().isEmpty()) return;

        final String requestXml = "";
        final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);
        pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + "?folder=" + targetFoldGoid);
        pec.setVariable(VAR_RESTMAN_ACTION, "GET");

        final Pair<AssertionStatus, RestmanMessage> restmanRetrievingEntitiesResult = restmanInvoker.callManagementCheckInterrupted(pec, requestXml);
        final RestmanMessage restmanMessage = restmanRetrievingEntitiesResult.right;
        if (restmanMessage == null) {
            throw new RuntimeException("Retrieving Entities failed: response from restman is null.");
        } else if (restmanMessage.hasMappingError()) {
            try {
                throw new RuntimeException("Retrieving Entities failed: " + restmanMessage.getMappingErrorsAsString());
            } catch (IOException e) {
                throw new RuntimeException("Retrieving Entities failed:", e);
            }
        }

        List<String> targetEntityIds = new ArrayList<>();
        for (Element mapping: restmanMessage.getMappings()) {
            targetEntityIds.add(mapping.getAttribute("srcId"));
        }

        // Make a copy of targetEntityIds
        List<String> deletedEntityIds = new ArrayList<>();
        deletedEntityIds.addAll(targetEntityIds);

        // Remove from targetEntityIds all of its elements that are not contained in entityIdsInBundleMapping
        targetEntityIds.retainAll(entityIdsInBundleMapping);

        // Get all elements not contained in entityIdsInBundleMapping
        deletedEntityIds.removeAll(targetEntityIds);

        for (String srcId: deletedEntityIds) {
            dryRunEvent.addMigrationErrorMapping(
                new MigrationDryRunResult("EntityDeleted", restmanMessage.getEntityType(srcId), srcId, null, restmanMessage.getEntityName(srcId), null)
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

                        EntityMappingInstructions.MappingAction mappingAction = EntityMappingInstructions.MappingAction.valueOf(mapping.left);
                        requestMessage.setMappingAction(id, mappingAction, mapping.right);

                        if (mappingAction == EntityMappingInstructions.MappingAction.Delete) {
                            deletedEntities.put(id, (String) mapping.right.get(id));
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
     * @param entityTypeName: the name of an entity type
     * @return a uri string representing entity type
     */
    private String getEntityTypeUriName(@NotNull String entityTypeName) {
        EntityType entityType = EntityType.valueOf(entityTypeName);

        if (entityType == EntityType.SSG_ACTIVE_CONNECTOR) {
            return "activeConnectors";
        } else if (entityType == EntityType.TRUSTED_CERT) {
            return "trustedCertificates";
        } else if (entityType == EntityType.CLUSTER_PROPERTY) {
            return "clusterProperties";
        } else if (entityType == EntityType.CUSTOM_KEY_VALUE_STORE) {
            return "customKeyValues";
        } else if (entityType == EntityType.EMAIL_LISTENER) {
            return "emailListeners";
        } else if (entityType == EntityType.ENCAPSULATED_ASSERTION) {
            return "encapsulatedAssertions";
        } else if (entityType == EntityType.FOLDER) {
            return "folders";
        } else if (entityType == EntityType.GENERIC) {
            return "genericEntities";
        } else if (entityType == EntityType.HTTP_CONFIGURATION) {
            return "httpConfigurations";
        } else if (entityType == EntityType.ID_PROVIDER_CONFIG) {
            return "identityProviders";
        } else if (entityType == EntityType.JDBC_CONNECTION) {
            return "jdbcConnections";
        } else if (entityType == EntityType.JMS_CONNECTION) {
            return "jmsDestinations";
        } else if (entityType == EntityType.SSG_CONNECTOR) {
            return "listenPorts";
        } else if (entityType == EntityType.POLICY) {
            return "policies";
        } else if (entityType == EntityType.POLICY_ALIAS) {
            return "policyAliases";
        } else if (entityType == EntityType.SSG_KEY_ENTRY) {
            return "privateKeys";
        } else if (entityType == EntityType.SERVICE) {
            return "services";
        } else if (entityType == EntityType.REVOCATION_CHECK_POLICY) {
            return "revocationCheckingPolicies";
        } else if (entityType == EntityType.RBAC_ROLE) {
            return "roles";
        } else if (entityType == EntityType.SECURE_PASSWORD) {
            return "passwords";
        } else if (entityType == EntityType.SECURITY_ZONE) {
            return "securityZones";
        } else if (entityType == EntityType.SERVICE_ALIAS) {
            return "serviceAliases";
        } else if (entityType == EntityType.SITEMINDER_CONFIGURATION) {
            return "siteMinderConfigurations";
        } else if (entityType == EntityType.USER) {
            return "users";
        }

        throw new RuntimeException("Not supported entity type: " + entityTypeName);
    }
}