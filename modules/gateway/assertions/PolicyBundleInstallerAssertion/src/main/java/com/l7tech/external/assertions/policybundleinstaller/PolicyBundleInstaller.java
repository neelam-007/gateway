package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.policybundleinstaller.InstallerUtils.*;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;

import static com.l7tech.server.policy.bundle.PolicyUtils.findJdbcReferences;
import static com.l7tech.server.policy.bundle.PolicyUtils.updatePolicyIncludes;
import static com.l7tech.util.DomUtils.findExactlyOneChildElementByName;
import static com.l7tech.util.Functions.Nullary;

public class PolicyBundleInstaller {

    public PolicyBundleInstaller(@NotNull final GatewayManagementInvoker gatewayManagementInvoker,
                                 @NotNull final PolicyBundleInstallerContext context,
                                 @NotNull final Nullary<Boolean> cancelledCallback) {
        this.gatewayManagementInvoker = gatewayManagementInvoker;
        this.cancelledCallback = cancelledCallback;
        this.context = context;
    }

    public void setSavePolicyCallback(@Nullable PreBundleSavePolicyCallback savePolicyCallback) {
        this.savePolicyCallback = savePolicyCallback;
    }

    /**
     * Dry run the installation looking for conflicts. Any conflicts found are updated in the dry run event.
     *
     * @param dryRunEvent event used to capture any conflicts.
     *
     * @throws BundleResolver.BundleResolverException
     * @throws BundleResolver.UnknownBundleException
     * @throws BundleResolver.InvalidBundleException
     * @throws InterruptedException
     * @throws AccessDeniedManagementResponse
     */
    public void dryRunInstallBundle(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws BundleResolver.BundleResolverException,
            BundleResolver.UnknownBundleException,
            BundleResolver.InvalidBundleException,
            InterruptedException,
            AccessDeniedManagementResponse {

        checkInterrupted();

        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document serviceEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), SERVICE, true);
        final List<Element> allUrlPatternElms = GatewayManagementDocumentUtilities.findAllUrlPatternsFromEnumeration(serviceEnumDoc);
        for (Element allUrlPatternElm : allUrlPatternElms) {
            checkInterrupted();
            final String urlPattern = getPrefixedUrl(DomUtils.getTextValue(allUrlPatternElm));
            try {
                final List<Long> matchingServices = findMatchingService(urlPattern);
                if (!matchingServices.isEmpty()) {
                    dryRunEvent.addUrlPatternWithConflict(urlPattern);
                }
            } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for url pattern '" + urlPattern + "'", e);
            }
        }

        checkInterrupted();

        final Document policyEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), POLICY, true);
        final List<Element> policyNamesElms = GatewayManagementDocumentUtilities.findAllPolicyNamesFromEnumeration(policyEnumDoc);
        for (Element policyNamesElm : policyNamesElms) {
            checkInterrupted();
            final String policyName = getPrefixedPolicyName(DomUtils.getTextValue(policyNamesElm));
            try {
                final List<Long> matchingPolicies = findMatchingPolicy(policyName);
                if (!matchingPolicies.isEmpty()) {
                    dryRunEvent.addPolicyNameWithConflict(policyName);
                }
            } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for policy name  '" + policyName + "'", e);
            }
        }

        checkInterrupted();

        final Set<String> jdbcConnRefs = bundleInfo.getJdbcConnectionReferences();
        final BundleMapping bundleMapping = context.getBundleMapping();
        if (!jdbcConnRefs.isEmpty()) {
            final Map<String, String> jdbcMappings =
                    (bundleMapping != null) ? bundleMapping.getJdbcMappings() : new HashMap<String, String>();

            // validate each, consider any mapping that may be present.
            for (String jdbcConnRef : jdbcConnRefs) {
                checkInterrupted();
                final String jdbcConnToVerify = (jdbcMappings.containsKey(jdbcConnRef)) ? jdbcMappings.get(jdbcConnRef) : jdbcConnRef;
                try {
                    final List<Long> foundConns = findMatchingJdbcConnection(jdbcConnToVerify);
                    if (foundConns.isEmpty()) {
                        dryRunEvent.addMissingJdbcConnection(jdbcConnToVerify);
                    }
                } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                    throw new BundleResolver.InvalidBundleException("Could not verify JDBC Connection '" + jdbcConnToVerify + "'", e);
                }
            }
        }
    }

    /**
     * Install the contents of the configured bundle
     */
    public void installBundle()
            throws InstallationException,
            BundleResolver.UnknownBundleException,
            BundleResolver.BundleResolverException,
            InterruptedException,
            BundleResolver.InvalidBundleException,
            IOException,
            UnexpectedManagementResponse,
            AccessDeniedManagementResponse {

        logger.info("Installing bundle: " + context.getBundleInfo().getId());

        checkInterrupted();

        final Document folderBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), FOLDER, false);

        final long folderToInstallInto = context.getFolderOid();
        assert folderBundle != null;
        final Map<Long, Long> oldIdToNewFolderIds = installFolders(folderToInstallInto, folderBundle);

        checkInterrupted();

        // install policies
        final Document policyBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), POLICY, true);
        final Map<String, String> contextOldPolicyGuidsToNewGuids;
        if (policyBundle == null) {
            logger.info("No policies to install for bundle " + context.getBundleInfo());
            contextOldPolicyGuidsToNewGuids = Collections.emptyMap();
        } else {
            try {
                contextOldPolicyGuidsToNewGuids = installPolicies(
                        oldIdToNewFolderIds,
                        policyBundle);
            } catch (PreBundleSavePolicyCallback.PolicyUpdateException e) {
                throw new InstallationException(e);
            }
        }

        checkInterrupted();

        // install services
        final Document serviceBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), SERVICE, true);
        if (serviceBundle == null) {
            logger.info("No services to install for bundle " + context.getBundleInfo());
        } else {
            try {
                installServices(
                        oldIdToNewFolderIds,
                        contextOldPolicyGuidsToNewGuids,
                        serviceBundle);
            } catch (PreBundleSavePolicyCallback.PolicyUpdateException e) {
                throw new InstallationException(e);
            }
        }

        logger.info("Finished installing bundle: " + context.getBundleInfo());
    }

    public static class InstallationException extends Exception{
        public InstallationException(String message) {
            super(message);
        }

        public InstallationException(String message, Throwable cause) {
            super(message, cause);
        }

        public InstallationException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Note: Services contain nothing unique
     *
     * @param oldToNewFolderIds
     * @param contextOldPolicyGuidsToNewGuids
     *
     * @param serviceMgmtEnumeration
     * @throws BundleResolver.InvalidBundleException
     *
     * @throws UnexpectedManagementResponse
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException
     *
     * @throws InterruptedException
     * @throws AccessDeniedManagementResponse
     */
    public void installServices(@NotNull final Map<Long, Long> oldToNewFolderIds,
                                @NotNull final Map<String, String> contextOldPolicyGuidsToNewGuids,
                                @NotNull final Document serviceMgmtEnumeration)
            throws BundleResolver.InvalidBundleException,
            UnexpectedManagementResponse,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            InterruptedException,
            AccessDeniedManagementResponse {

        final Map<Long, Long> oldIdsToNewServiceIds = new HashMap<Long, Long>();
        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceMgmtEnumeration.getDocumentElement(), "Service");
        for (Element serviceElm : serviceElms) {
            checkInterrupted();

            final Element serviceElmWritable = parseQuietly(XmlUtil.nodeToStringQuiet(serviceElm)).getDocumentElement();
            final String id = serviceElmWritable.getAttribute("id");
            if (oldIdsToNewServiceIds.containsKey(Long.valueOf(id))) {
                continue;
            }

            final Element serviceDetail = getServiceDetailElement(serviceElmWritable);
            final String bundleFolderId = serviceDetail.getAttribute("folderId");

            if (!oldToNewFolderIds.containsKey(Long.valueOf(bundleFolderId))) {
                throw new BundleResolver.InvalidBundleException("Could not find updated folder for service #{" + id + "} in folder " + bundleFolderId);
            }
            final Element urlPatternWriteableEl = XmlUtil.findFirstDescendantElement(serviceDetail, MGMT_VERSION_NAMESPACE, "UrlPattern");

            // lets check if the service has a URL mapping and if so, if any service already exists with that mapping.
            // if it does, then we won't install it.
            // check if it has a URL mapping

            if (urlPatternWriteableEl != null) {
                final String existingUrl = DomUtils.getTextValue(urlPatternWriteableEl, true);
                final String maybePrefixedUrl = getPrefixedUrl(existingUrl);
                if (!existingUrl.equals(maybePrefixedUrl)) {
                    DomUtils.setTextContent(urlPatternWriteableEl, maybePrefixedUrl);
                }

                final List<Long> matchingService = findMatchingService(maybePrefixedUrl);
                if (!matchingService.isEmpty()) {
                    // Service already exists
                    logger.info("Not installing service with id #{" + id + "} and routing URI '" + maybePrefixedUrl + "' " +
                            "due to existing service with conflicting resolution URI");
                    continue;
                }
            } else {
                String message = "Service with id #{" + id + "} does not use a custom resolution URI so it is not possible to detect if there are any resolution conflicts";
                if (isPrefixValid(context.getInstallationPrefix())) {
                    message = " or to add an installation prefix";
                }
                logger.info(message);
            }

            final Long newFolderId = oldToNewFolderIds.get(Long.valueOf(bundleFolderId));
            serviceDetail.setAttribute("folderId", String.valueOf(newFolderId));

            final Element policyResourceElmWritable = getPolicyResourceElement(serviceElmWritable, "Service", id);
            if (policyResourceElmWritable == null) {
                throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
            }

            // if this service has any includes we need to update them
            final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", id);
            final Element serviceDetailElmReadOnly = getServiceDetailElement(serviceElm);
            updatePolicyDoc(serviceDetailElmReadOnly, "Service", contextOldPolicyGuidsToNewGuids, id, policyResourceElmWritable, policyDocumentFromResource);

            final String serviceXmlTemplate = XmlUtil.nodeToStringQuiet(serviceElmWritable);
            final String createServiceXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), SERVICES_MGMT_NS, serviceXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createServiceXml);

            final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
            if (createdId == null) {
                throw new GatewayManagementDocumentUtilities.UnexpectedManagementResponse("Could not get the id for service from bundle with id: #{" + id + "}");
            }

            oldIdsToNewServiceIds.put(Long.valueOf(id), createdId);
        }
    }

    /**
     * Note: Policies are unique on name across a gateway
     *
     * @param oldToNewFolderIds     gateway mgmt policy elements will reference folder ids. Before publishing a policy
     *                              we need the actualy folder id that the policy will be published into. This is a map of
     *                              the folder id contained in the enumeration document for a policy mapped to the actual
     *                              folder id on the target system which represents the same logical folder.
     * @param policyMgmtEnumeration the gateway mgmt enumeration containing all policy elements too publish.
     * @throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     *
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException
     *
     * @throws BundleResolver.InvalidBundleException
     *
     * @throws InterruptedException
     * @throws AccessDeniedManagementResponse
     */
    public final Map<String, String> installPolicies(@NotNull final Map<Long, Long> oldToNewFolderIds,
                                                     @NotNull final Document policyMgmtEnumeration)
            throws
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            BundleResolver.InvalidBundleException,
            InterruptedException,
            AccessDeniedManagementResponse {

        final List<Element> enumPolicyElms = GatewayManagementDocumentUtilities.getEntityElements(policyMgmtEnumeration.getDocumentElement(), "Policy");

        final Map<String, String> guidToName = new HashMap<String, String>();
        final Map<String, Element> allPolicyElms = new HashMap<String, Element>();
        for (Element policyElm : enumPolicyElms) {
            final Element name = XmlUtil.findFirstDescendantElement(policyElm, BundleUtils.L7_NS_GW_MGMT, "Name");
            final String policyName = DomUtils.getTextValue(name, true);
            final String guid = policyElm.getAttribute("guid");
            guidToName.put(guid, policyName);
            allPolicyElms.put(guid, policyElm);
        }

        final Map<String, String> oldGuidsToNewGuids = new HashMap<String, String>();
        // fyi: circular policy includes are not supported via the Policy Manager - assume they will not be found
        for (Element policyElm : enumPolicyElms) {
            // recursive call if policy includes an include
            getOrCreatePolicy(policyElm, oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName);
        }

        return oldGuidsToNewGuids;
    }

    /**
     * Folders will be created if needed. If folders already exist in the parent folder then they will not be modified.
     * Incoming XML will contain ids for folders, however these are ignored by the management api and new ids will be
     * assigned.
     *
     * @param parentFolderOid       oid of the parent folder to install into.
     * @param folderMgmtEnumeration Gateway management enumeration document of folders to install
     * @return map of all folders ids to ids in the folderMgmtEnumeration. The key is always the folders canned id, the id in the
     *         folderMgmtEnumeration, the value will either be a new id if the folder was created or it will be the id of the existing
     *         folder on the target gateway.
     * @throws AccessDeniedManagementResponse if the admin user does not contain the permission to create any required folder
     * @throws UnexpectedManagementResponse   if the gateway mgmt assertion returns an unexpected error status
     * @throws InterruptedException           if the installation is cancelled
     */
    public Map<Long, Long> installFolders(final long parentFolderOid,
                                          @NotNull final Document folderMgmtEnumeration)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException,
            AccessDeniedManagementResponse {
        final List<Element> folderElms = GatewayManagementDocumentUtilities.getEntityElements(folderMgmtEnumeration.getDocumentElement(), "Folder");

        // find the root node
        final Map<Long, Element> folderIdToElement = new HashMap<Long, Element>();
        Element rootFolder = null;
        for (Element folderElm : folderElms) {
            final String idAttr = folderElm.getAttribute("id");
            folderIdToElement.put(Long.valueOf(idAttr), folderElm);
            if (idAttr.equals("-5002")) {
                rootFolder = folderElm;
            }
        }

        final Stack<Element> toProcess = new Stack<Element>();
        toProcess.push(rootFolder);

        final Map<Long, Long> oldToNewIds = new HashMap<Long, Long>();
        // parent folder oid may already be -5002 when installing to the root node
        oldToNewIds.put(-5002L, parentFolderOid);

        while (!toProcess.isEmpty()) {

            checkInterrupted();

            final Element currentElm = toProcess.pop();
            final String id = currentElm.getAttribute("id");
            final String folderId = currentElm.getAttribute("folderId");

            // add all children which have currentElm as their parent
            for (Element folderElm : folderElms) {
                final String parentId = folderElm.getAttribute("folderId");
                if (parentId.equals(id)) {
                    toProcess.push(folderElm);
                }
            }

            if (id.equals("-5002")) {
                // the root node will always already exist
                continue;
            }

            final Long newParentId = oldToNewIds.get(Long.valueOf(folderId));
            if (newParentId == null) {
                throw new RuntimeException("Parent folder " + folderId + " for folder " + id + " not found. Input Folder XML must be corrupt.");
            }

            final Document document = XmlUtil.createEmptyDocument("Folder", "l7", BundleUtils.L7_NS_GW_MGMT);
            final Element folder = document.getDocumentElement();
            folder.setAttribute("folderId", String.valueOf(newParentId));
            folder.setAttribute("id", id); //this gets overwritten and ignored by mgmt assertion.

            final Element name = DomUtils.createAndAppendElementNS(folder, "Name", BundleUtils.L7_NS_GW_MGMT, "l7");
            final String folderName = getEntityName(currentElm);
            final Text nameText = document.createTextNode(folderName);
            name.appendChild(nameText);

            final String folderXmlTemplate;
            try {
                folderXmlTemplate = XmlUtil.nodeToString(document.getDocumentElement());
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception serializing template Folder XML", e);
            }
            final String createFolderXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), FOLDER_MGMT_NS, folderXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createFolderXml);
            final Long newId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
            final Long idToRecord;
            if (newId == null) {
                if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                    idToRecord = getExistingFolderId(newParentId, folderName);
                } else {
                    idToRecord = null;
                }
            } else {
                idToRecord = newId;
            }

            if (idToRecord == null) {
                throw new RuntimeException("Could not create or find id for xml folder with bundle id: " + id);
            }

            oldToNewIds.put(Long.valueOf(id), idToRecord);
        }

        return oldToNewIds;
    }

    // - PRIVATE

    @Nullable private PreBundleSavePolicyCallback savePolicyCallback;
    @NotNull private final GatewayManagementInvoker gatewayManagementInvoker;
    @NotNull private final PolicyBundleInstallerContext context;
    @NotNull private final Nullary<Boolean> cancelledCallback;
    private static final Logger logger = Logger.getLogger(PolicyBundleInstaller.class.getName());

    private String getPrefixedUrl(final String existingUrlPattern) {
        if (isPrefixValid(context.getInstallationPrefix())) {
            final String prefixToUse = "/" + context.getInstallationPrefix();
            return prefixToUse + existingUrlPattern;
        } else {
            return existingUrlPattern;
        }
    }

    private Document parseQuietly(String inputXml) {
        try {
            return XmlUtil.parse(inputXml);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected internal error parsing XML: " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param enumPolicyElmReadOnly read only access to the Policy Gateay Mgmt element
     * @param oldGuidsToNewGuids    map of the policy's guid from the gateway mgmt enumeration document to it's actual
     *                              guid once published. This avoids attempting to publish the same policy more than once.
     * @param oldToNewFolderIds
     * @param allPolicyElms
     * @param guidToName
     * @throws BundleResolver.InvalidBundleException
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException
     * @throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws InterruptedException
     * @throws AccessDeniedManagementResponse
     */
    private void getOrCreatePolicy(@NotNull final Element enumPolicyElmReadOnly,
                                   @NotNull final Map<String, String> oldGuidsToNewGuids,
                                   @NotNull final Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<String, Element> allPolicyElms,
                                   @NotNull final Map<String, String> guidToName)
            throws
            BundleResolver.InvalidBundleException,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException,
            AccessDeniedManagementResponse {

        final String policyGuid = enumPolicyElmReadOnly.getAttribute("guid");

        if (oldGuidsToNewGuids.containsKey(policyGuid)) {
            // already created
            return;
        }

        final Element enumPolicyElmWritable;
        try {
            enumPolicyElmWritable = XmlUtil.parse(XmlUtil.nodeToString(enumPolicyElmReadOnly)).getDocumentElement();
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected exception getting a writable policy element", e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception getting a writable policy element", e);
        }

        final Element policyResourceElmWritable = getPolicyResourceElement(enumPolicyElmWritable, "Policy", policyGuid);
        if (policyResourceElmWritable == null) {
            throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocWriteEl = getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", policyGuid);
        // these do not belong to the policyResourceElmWritable document
        final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(policyDocWriteEl);
        for (Element policyIncludeElm : policyIncludes) {
            final String policyInclude = policyIncludeElm.getAttribute("stringValue");
            if (!allPolicyElms.containsKey(policyInclude)) {
                throw new BundleResolver.InvalidBundleException("Policy with guid " + policyInclude + " was not included in bundle "
                        + context.getBundleInfo().getName() + "#{" + context.getBundleInfo().getId() + "}");
            }
            getOrCreatePolicy(allPolicyElms.get(policyInclude), oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName);
        }

        checkInterrupted();

        final Element policyDetailElmReadOnly = getPolicyDetailElement(enumPolicyElmReadOnly);
        // get or create
        // Create a new document and modify it
        updatePolicyDoc(policyDetailElmReadOnly, "Policy", oldGuidsToNewGuids, policyGuid, policyResourceElmWritable, policyDocWriteEl, policyIncludes);

        final Element policyDetailWritable = getPolicyDetailElement(enumPolicyElmWritable);
        final String folderId = policyDetailWritable.getAttribute("folderId");
        final Long newFolderId = oldToNewFolderIds.get(Long.valueOf(folderId));
        if (newFolderId == null) {
            throw new BundleResolver.InvalidBundleException("Policy with GUID: " + policyGuid + " is contained within unknown folder id '" + folderId + "' in the bundle");
        }

        policyDetailWritable.setAttribute("folderId", String.valueOf(newFolderId));

        final String policyNameToUse;
        // Add prefix if needed
        if (isPrefixValid(context.getInstallationPrefix())) {
            final Element nameElementWritable = GatewayManagementDocumentUtilities.getEntityNameElement(policyDetailWritable);
            final String policyName = DomUtils.getTextValue(nameElementWritable);
            policyNameToUse = getPrefixedPolicyName(policyName);
            DomUtils.setTextContent(nameElementWritable, policyNameToUse);
            assert (policyName.equals(guidToName.get(policyGuid)));
        } else {
            policyNameToUse = guidToName.get(policyGuid);
        }

        final String policyXmlTemplate;
        try {
            policyXmlTemplate = XmlUtil.nodeToString(enumPolicyElmWritable);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception serializing writable policy element", e);
        }

        final String createPolicyXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), POLICIES_MGMT_NS, policyXmlTemplate);
        final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createPolicyXml);

        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
        String guidToUse = null;
        if (createdId == null) {
            if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                guidToUse = getExistingPolicyGuid(policyNameToUse);
            }
        } else {
            // we just created it
            guidToUse = getExistingPolicyGuid(policyNameToUse);
        }

        if (guidToUse == null) {
            throw new RuntimeException("Could not create or get the GUID for policy from bundle with guid: #{" + policyGuid + "}");
        }

        oldGuidsToNewGuids.put(policyGuid, guidToUse);
    }

    private boolean isPrefixValid(String installationPrefix) {
        return installationPrefix != null && !installationPrefix.trim().isEmpty();
    }

    private String getPrefixedPolicyName(@NotNull String policyName) {
        if(isPrefixValid(context.getInstallationPrefix())) {
            return context.getInstallationPrefix() + " " + policyName;
        } else {
            return policyName;
        }
    }

    private void updatePolicyDoc(Element serviceDetailElmReadOnly,
                                 String entityType,
                                 Map<String, String> oldGuidsToNewGuids,
                                 String identifier,
                                 Element policyResourceElmWritable,
                                 Document policyDocumentFromResource)
            throws BundleResolver.InvalidBundleException, PreBundleSavePolicyCallback.PolicyUpdateException {

        final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(policyDocumentFromResource);
        updatePolicyDoc(serviceDetailElmReadOnly, entityType, oldGuidsToNewGuids, identifier, policyResourceElmWritable, policyDocumentFromResource, policyIncludes);
    }

    /**
     *
     * @param entityDetailElmReadOnly either a PolicyDetail or a ServiceDetail element which is read only
     * @param policyIncludesFromPolicyDocument These elements must come from policyDocumentFromResource's Document
     * @throws BundleResolver.InvalidBundleException
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException
     */
    private void updatePolicyDoc(Element entityDetailElmReadOnly,
                                 String entityType,
                                 Map<String, String> oldGuidsToNewGuids,
                                 String identifier,
                                 Element policyResourceElmWritable,
                                 Document policyDocumentFromResource,
                                 @NotNull List<Element> policyIncludesFromPolicyDocument)
            throws BundleResolver.InvalidBundleException, PreBundleSavePolicyCallback.PolicyUpdateException {
        updatePolicyIncludes(oldGuidsToNewGuids, identifier, entityType, policyIncludesFromPolicyDocument);

        final BundleMapping bundleMapping = context.getBundleMapping();
        if (bundleMapping != null) {
            final Map<String, String> mappedJdbcReferences = bundleMapping.getJdbcMappings();
            if (!mappedJdbcReferences.isEmpty()) {
                final List<Element> jdbcReferencesElms = findJdbcReferences(policyDocumentFromResource.getDocumentElement());
                for (Element jdbcRefElm : jdbcReferencesElms) {
                    try {
                        final Element connNameElm = findExactlyOneChildElementByName(jdbcRefElm, BundleUtils.L7_NS_POLICY, "ConnectionName");
                        final String policyConnName = connNameElm.getAttribute("stringValue").trim();
                        if (mappedJdbcReferences.containsKey(policyConnName)) {
                            connNameElm.setAttribute("stringValue", mappedJdbcReferences.get(policyConnName));
                        }

                    } catch (TooManyChildElementsException e) {
                        throw new BundleResolver.InvalidBundleException("Could not find jdbc reference to update: " + ExceptionUtils.getMessage(e));
                    } catch (MissingRequiredElementException e) {
                        throw new BundleResolver.InvalidBundleException("Could not find jdbc reference to update: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
        }

        if (savePolicyCallback != null) {
            savePolicyCallback.prePublishCallback(context.getBundleInfo(), entityDetailElmReadOnly, policyDocumentFromResource);
        }

        //write changes back to the resource document
        DomUtils.setTextContent(policyResourceElmWritable, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
    }

    /**
     * See if any existing service contains a service with the same urlMapping e.g. resolution URI
     *
     * @param urlMapping URI Resolution value for the search
     * @return list of ids of any existing service which have this routing uri
     */
    @NotNull
    private List<Long> findMatchingService(String urlMapping) throws UnexpectedManagementResponse, InterruptedException,
            AccessDeniedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                SERVICES_MGMT_NS, 10, "/l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='" + urlMapping + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    private Pair<AssertionStatus, Document> callManagementCheckInterrupted(String requestXml) throws InterruptedException,
            AccessDeniedManagementResponse, UnexpectedManagementResponse {

        final Pair<AssertionStatus, Document> documentPair;
        try {
            documentPair = callManagementAssertion(gatewayManagementInvoker, requestXml);
        } catch (UnexpectedManagementResponse e) {
            if (e.isCausedByMgmtAssertionInternalError() && cancelledCallback.call()) {
                throw new InterruptedException("Possible interruption detected due to internal error");
            } else {
                throw e;
            }
        } catch (AccessDeniedManagementResponse e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Access denied for request:" + e.getDeniedRequest());
            }
            throw e;
        }
        return documentPair;
    }

    @NotNull
    private List<Long> findMatchingPolicy(String policyName) throws InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                POLICIES_MGMT_NS, 10, "/l7:Policy/l7:PolicyDetail/l7:Name[text()='" + policyName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @NotNull
    private List<Long> findMatchingJdbcConnection(String jdbcConnection) throws AccessDeniedManagementResponse, InterruptedException, UnexpectedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                JDBC_MGMT_NS, 10, "/l7:JDBCConnection/l7:Name[text()='" + jdbcConnection + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @Nullable
    private String getExistingPolicyGuid(String policyName) throws InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse {

        final String getPolicyXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(getPolicyXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(
                    new XpathExpression("string(/env:Envelope/env:Body/l7:Policy/@guid)", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Unexpected exception performing xpath to obtain policy guid for policy name '" + policyName + "'", e);
        } catch (InvalidXpathException e) {
            throw new RuntimeException("Unexpected exception performing xpath to obtain policy guid for policy name '" + policyName + "'", e);
        }
        return xpathResult.getString();
    }

    @Nullable
    private Long getExistingFolderId(long parentId, String folderName)
            throws UnexpectedManagementResponse, InterruptedException, AccessDeniedManagementResponse {
        final String folderFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                FOLDER_MGMT_NS, 10, "/l7:Folder[@folderId='" + parentId + "']/l7:Name[text()='" + folderName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(folderFilter);
        return GatewayManagementDocumentUtilities.getCreatedId(documentPair.right);
    }

    private void checkInterrupted() throws InterruptedException {
        if (cancelledCallback.call() || Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
