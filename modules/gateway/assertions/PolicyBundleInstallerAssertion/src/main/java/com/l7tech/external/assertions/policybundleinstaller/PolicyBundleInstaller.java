package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
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
import java.util.logging.Logger;

import static com.l7tech.external.assertions.policybundleinstaller.InstallerUtils.*;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.*;

//todo extract out interface. Allow installation folders, services and policies to be specified by clients.
public class PolicyBundleInstaller {

    public static final String FOLDERS_CONTEXT_KEY = "FOLDERS";
    public static final String POLICY_FRAGMENT_CONTEXT_KEY = "POLICY FRAGMENTS";
    public static final String SERVICES_CONTEXT_KEY = "SERVICES";

    public PolicyBundleInstaller(@NotNull final BundleResolver bundleResolver,
                                 @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {

        this(bundleResolver, null, gatewayManagementInvoker, null);
    }

    public PolicyBundleInstaller(@NotNull final BundleResolver bundleResolver,
                                 @Nullable final PreBundleSavePolicyCallback savePolicyCallback,
                                 @NotNull final GatewayManagementInvoker gatewayManagementInvoker,
                                 @Nullable final PolicyBundleEvent policyBundleEvent) {
        this.bundleResolver = bundleResolver;
        this.savePolicyCallback = savePolicyCallback;
        this.gatewayManagementInvoker = gatewayManagementInvoker;
        this.policyBundleEvent = policyBundleEvent;
    }

    public void dryRun(final PolicyBundleInstallerContext context,
                       final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws BundleResolver.BundleResolverException,
            BundleResolver.UnknownBundleException,
            BundleResolver.InvalidBundleException,
            InterruptedException {

        checkInterrupted(dryRunEvent);

        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document serviceEnumDoc = bundleResolver.getBundleItem(bundleInfo.getId(), SERVICE, true);
        final List<Element> allUrlPatternElms = GatewayManagementDocumentUtilities.findAllUrlPatternsFromEnumeration(serviceEnumDoc);
        for (Element allUrlPatternElm : allUrlPatternElms) {
            checkInterrupted(dryRunEvent);
            final String urlPattern = getPrefixedUrl(DomUtils.getTextValue(allUrlPatternElm), context.getInstallationPrefix());
            try {
                final List<Long> matchingServices = findMatchingService(urlPattern);
                if (!matchingServices.isEmpty()) {
                    dryRunEvent.addUrlPatternWithConflict(urlPattern);
                }
            } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for url pattern '" + urlPattern + "'", e);
            }
        }

        checkInterrupted(dryRunEvent);

        final Document policyEnumDoc = bundleResolver.getBundleItem(bundleInfo.getId(), POLICY, true);
        final List<Element> policyNamesElms = GatewayManagementDocumentUtilities.findAllPolicyNamesFromEnumeration(policyEnumDoc);
        for (Element policyNamesElm : policyNamesElms) {
            checkInterrupted(dryRunEvent);
            final String policyName = getPrefixedPolicyName(context.getInstallationPrefix(), DomUtils.getTextValue(policyNamesElm));
            try {
                final List<Long> matchingPolicies = findMatchingPolicy(policyName);
                if (!matchingPolicies.isEmpty()) {
                    dryRunEvent.addPolicyNameWithConflict(policyName);
                }
            } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for policy name  '" + policyName + "'", e);
            }
        }

        checkInterrupted(dryRunEvent);

        final Set<String> jdbcConnRefs = bundleInfo.getJdbcConnectionReferences();
        final BundleMapping bundleMapping = context.getBundleMapping();
        if (!jdbcConnRefs.isEmpty()) {
            final Map<String, String> jdbcMappings =
                    (bundleMapping != null)? bundleMapping.getJdbcMappings(): new HashMap<String, String>();

            // validate each, consider any mapping that may be present.
            for (String jdbcConnRef : jdbcConnRefs) {
                checkInterrupted(dryRunEvent);
                final String jdbcConnToVerify = (jdbcMappings.containsKey(jdbcConnRef))? jdbcMappings.get(jdbcConnRef): jdbcConnRef;
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
     *
     */
    public void install(@NotNull final InstallPolicyBundleEvent installEvent)
            throws InstallationException,
            BundleResolver.UnknownBundleException,
            BundleResolver.BundleResolverException,
            InterruptedException,
            BundleResolver.InvalidBundleException, IOException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {

        final PolicyBundleInstallerContext context = installEvent.getContext();
        logger.info("Installing bundle: " + context.getBundleInfo().getId());

        // allow this code to attempt to create the install folder each time.
        final long folderToInstallInto = context.getFolderOid();

        checkInterrupted(installEvent);

        final Document folderBundle = bundleResolver.getBundleItem(context.getBundleInfo().getId(), FOLDER, false);

        if (!context.getContextMap().containsKey(FOLDERS_CONTEXT_KEY)) {
            final Map<Long, Long> newMap = new HashMap<Long, Long>();
            context.getContextMap().put(FOLDERS_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<Long, Long> contextOldToNewFolderOids = (Map<Long, Long>) context.getContextMap().get(FOLDERS_CONTEXT_KEY);

        final Map<Long, Long> oldIdToNewFolderIds = installFolders(installEvent, folderToInstallInto, folderBundle, contextOldToNewFolderOids);

        checkInterrupted(installEvent);

        contextOldToNewFolderOids.putAll(oldIdToNewFolderIds);

        if (!context.getContextMap().containsKey(POLICY_FRAGMENT_CONTEXT_KEY)) {
            final Map<String, String> newMap = new HashMap<String, String>();
            context.getContextMap().put(POLICY_FRAGMENT_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<String, String> contextOldPolicyGuidsToNewGuids = (Map<String, String>) context.getContextMap().get(POLICY_FRAGMENT_CONTEXT_KEY);

        // install policies
        final Document policyBundle = bundleResolver.getBundleItem(context.getBundleInfo().getId(), POLICY, true);
        if (policyBundle == null) {
            logger.info("No policies to install for bundle " + context.getBundleInfo());
        } else {
            try {
                installPolicies(
                        installEvent,
                        context.getBundleInfo(),
                        oldIdToNewFolderIds,
                        contextOldPolicyGuidsToNewGuids,
                        policyBundle,
                        context.getBundleMapping(),
                        context.getInstallationPrefix());
            } catch (PreBundleSavePolicyCallback.PolicyUpdateException e) {
                throw new InstallationException(e);
            }
        }

        checkInterrupted(installEvent);

        // two bundles should not contain the same service, however if they do we can short circuit trying to create it
        // by tracking it in this map
        if (!context.getContextMap().containsKey(SERVICES_CONTEXT_KEY)) {
            final Map<Long, Long> newMap = new HashMap<Long, Long>();
            context.getContextMap().put(SERVICES_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<Long, Long> contextServices = (Map<Long, Long>) context.getContextMap().get(SERVICES_CONTEXT_KEY);

        // install services
        final Document serviceBundle = bundleResolver.getBundleItem(context.getBundleInfo().getId(), SERVICE, true);
        if (serviceBundle == null) {
            logger.info("No services to install for bundle " + context.getBundleInfo());
        } else {
            try {
                installServices(installEvent,
                        context.getBundleInfo(),
                        oldIdToNewFolderIds,
                        contextServices,
                        contextOldPolicyGuidsToNewGuids,
                        serviceBundle,
                        context.getBundleMapping(),
                        context.getInstallationPrefix());
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

    // - PROTECTED

    /**
     * Services contain nothing unique!!!
     *
     * @param oldToNewFolderIds
     * @param serviceMgmtEnumeration
     * @throws IOException
     */
    protected void installServices(@NotNull final InstallPolicyBundleEvent installEvent,
                                   @NotNull final BundleInfo bundleInfo,
                                   @NotNull final Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<Long, Long> oldIdsToNewServiceIds,
                                   @NotNull final Map<String, String> contextOldPolicyGuidsToNewGuids,
                                   @NotNull final Document serviceMgmtEnumeration,
                                   @Nullable final BundleMapping bundleMapping,
                                   @Nullable final String installationPrefix)
            throws BundleResolver.InvalidBundleException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            InterruptedException {

        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceMgmtEnumeration.getDocumentElement(), "Service");
        for (Element serviceElm : serviceElms) {
            checkInterrupted(installEvent);

            final Element serviceElmWritable = parseQuietly(XmlUtil.nodeToStringQuiet(serviceElm)).getDocumentElement();
            final String id = serviceElmWritable.getAttribute("id");
            if (oldIdsToNewServiceIds.containsKey(Long.valueOf(id))) {
                continue;
            }

            final Element serviceDetail;
            try {
                serviceDetail = XmlUtil.findExactlyOneChildElementByName(serviceElmWritable, BundleUtils.L7_NS_GW_MGMT, "ServiceDetail");
            } catch (TooManyChildElementsException e) {
                throw new BundleResolver.InvalidBundleException("Invalid Service XML found. Expected a single ServiceDetail element for service with id #{" + id + "}");
            } catch (MissingRequiredElementException e) {
                throw new BundleResolver.InvalidBundleException("Invalid Service XML found. No ServiceDetail element found for service with id #{" + id + "}");
            }

            final String bundleFolderId = serviceDetail.getAttribute("folderId");

            if (!oldToNewFolderIds.containsKey(Long.valueOf(bundleFolderId))) {
                throw new BundleResolver.InvalidBundleException("Could not find updated folder for service #{" + id + "} in folder " + bundleFolderId);
            }

            // lets check if the service has a URL mapping and if so, if any service already exists with that mapping.
            // if it does, then we won't install it.
            // check if it has a URL mapping
            final ElementCursor cursor = new DomElementCursor(serviceDetail);
            // search from the current element only
            final XpathResult xpathResult;
            try {
                xpathResult = cursor.getXpathResult(
                        new XpathExpression(".//l7:UrlPattern", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
            } catch (XPathExpressionException e) {
                throw new RuntimeException("Unexpected issue with internal xpath expression: " + e.getMessage(), e);
            } catch (InvalidXpathException e) {
                throw new RuntimeException("Unexpected issue with internal xpath expression: " + e.getMessage(), e);
            }

            if (xpathResult.matches()) {
                final String existingUrl = xpathResult.getNodeSet().getNodeValue(0);
                final String urlPattern = getPrefixedUrl(existingUrl, installationPrefix);
                if (!existingUrl.equals(urlPattern)) {
                    final Element urlPatternElmWritable = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                    DomUtils.setTextContent(urlPatternElmWritable, urlPattern);
                }

                final List<Long> matchingService = findMatchingService(urlPattern);
                if (!matchingService.isEmpty()) {
                    // Service already exists
                    logger.info("Not installing service with id #{" + id + "} and routing URI '" + urlPattern + "' " +
                            "due to existing service with conflicting resolution URI");
                    continue;
                }
            } else {
                //todo this must be possible via non mgmt api entry point
                logger.info("Service with id #{" + id + "} does not use a custom resolution URI so it is not possible to detect if there are any resolution conflicts");
            }

            final Long newFolderId = oldToNewFolderIds.get(Long.valueOf(bundleFolderId));
            serviceDetail.setAttribute("folderId", String.valueOf(newFolderId));

            final Element policyResourceElmWritable = PolicyUtils.getPolicyResourceElement(serviceElmWritable, "Service", id);
            if (policyResourceElmWritable == null) {
                throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
            }

            // if this service has any includes we need to update them
            final Document policyDocumentFromResource = PolicyUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Service", id);
            updatePolicyDoc(bundleInfo, "Service", contextOldPolicyGuidsToNewGuids, id, policyResourceElmWritable, policyDocumentFromResource, bundleMapping);

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
     * Policies are unique on name across a gateway
     *
     *
     * @param oldToNewFolderIds
     * @param oldGuidsToNewGuids
     * @param policyMgmtEnumeration
     * @param installationPrefix
     * @return
     * @throws Exception
     */
    protected void installPolicies(@NotNull final InstallPolicyBundleEvent installEvent,
                                   @NotNull final BundleInfo bundleInfo,
                                   @NotNull final Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<String, String> oldGuidsToNewGuids,
                                   @NotNull final Document policyMgmtEnumeration,
                                   @Nullable final BundleMapping bundleMapping,
                                   @Nullable final String installationPrefix)
            throws
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            BundleResolver.InvalidBundleException,
            InterruptedException {

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

        // fyi: circular policy includes are not supported via the Policy Manager - assume they will not be found
        for (Element policyElm : enumPolicyElms) {
            // recursive call if policy includes an include
            getOrCreatePolicy(installEvent, bundleInfo, policyElm, oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName, bundleMapping, installationPrefix);
        }
    }

    /**
     * Folders will be created if needed. If folders already exist in the parent folder then they will not be modified.
     * Incoming XML will contain ids for folders, however these are ignored by the management api and new ids will be
     * assigned.
     *
     * @param parentFolderOid oid of the parent folder to install into.
     * @param folderMgmtEnumeration   Gateway management enumeration document of folders to install
     * @return map of all folders ids to ids in the folderMgmtEnumeration. The key is always the folders canned id, the id in the
     *         folderMgmtEnumeration, the value will either be a new id if the folder was created or it will be the id of the existing
     *         folder on the target gateway.
     * @throws Exception //todo define exception semantics
     */
    protected Map<Long, Long> installFolders(@NotNull final InstallPolicyBundleEvent installEvent,
                                             long parentFolderOid,
                                             @NotNull Document folderMgmtEnumeration,
                                             @NotNull Map<Long, Long> contextOldToNewFolderOids)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException {
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

            checkInterrupted(installEvent);

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
                continue;
            }

            // todo Only create / search for folder if we don't already know what it's new id is e.g. another bundle already created it

            final Long newParentId = oldToNewIds.get(Long.valueOf(folderId));
            if (newParentId == null) {
                throw new RuntimeException("Parent folder " + folderId + " for folder " + id + " not found. Input Folder XML must be corrupt.");
            }

            final Document document = XmlUtil.createEmptyDocument("Folder", "l7", BundleUtils.L7_NS_GW_MGMT);
            final Element folder = document.getDocumentElement();
            folder.setAttribute("folderId", String.valueOf(newParentId));
            folder.setAttribute("id", id); //this gets overwritten and ignored by mgmt assertion.

            final Element name = DomUtils.createAndAppendElementNS(folder, "Name", BundleUtils.L7_NS_GW_MGMT, "l7");
            final String folderName = DomUtils.getTextValue(DomUtils.findFirstChildElementByName(currentElm, BundleUtils.L7_NS_GW_MGMT, "Name"), true);
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

    @NotNull private final BundleResolver bundleResolver;
    @Nullable private final PreBundleSavePolicyCallback savePolicyCallback;
    @NotNull private final GatewayManagementInvoker gatewayManagementInvoker;
    @NotNull private final PolicyBundleEvent policyBundleEvent;
    private static final Logger logger = Logger.getLogger(PolicyBundleInstaller.class.getName());

    private String getPrefixedUrl(final String existingUrlPattern, final String installationPrefix) {
        if (installationPrefix != null) {
            final String prefixToUse = "/" + installationPrefix;
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

    private void getOrCreatePolicy(@NotNull final InstallPolicyBundleEvent installEvent,
                                   @NotNull final BundleInfo bundleInfo,
                                   @NotNull final Element enumPolicyElmReadOnly,
                                   @NotNull final Map<String, String> oldGuidsToNewGuids,
                                   @NotNull Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<String, Element> allPolicyElms,
                                   @NotNull final Map<String, String> guidToName,
                                   @Nullable final BundleMapping bundleMapping,
                                   @Nullable final String installationPrefix)
            throws
            BundleResolver.InvalidBundleException,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException {

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

        final Element policyResourceElmWritable = PolicyUtils.getPolicyResourceElement(enumPolicyElmWritable, "Policy", policyGuid);
        if (policyResourceElmWritable == null) {
            throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocumentFromResource = PolicyUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", policyGuid);
        // these do not belong to the policyResourceElmWritable document
        final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(policyDocumentFromResource);
        for (Element policyIncludeElm : policyIncludes) {
            final String policyInclude = policyIncludeElm.getAttribute("stringValue");
            if (!allPolicyElms.containsKey(policyInclude)) {
                throw new BundleResolver.InvalidBundleException("Policy with guid " + policyInclude + " was not included in bundle");
            }
            getOrCreatePolicy(installEvent, bundleInfo, allPolicyElms.get(policyInclude), oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName, bundleMapping, installationPrefix);
        }

        checkInterrupted(installEvent);

        // get or create
        // Create a new document and modify it
        updatePolicyDoc(bundleInfo, "Policy", oldGuidsToNewGuids, policyGuid, policyResourceElmWritable, policyDocumentFromResource, bundleMapping);

        final Element policyDetail;
        try {
            policyDetail = XmlUtil.findExactlyOneChildElementByName(enumPolicyElmWritable, BundleUtils.L7_NS_GW_MGMT, "PolicyDetail");
        } catch (TooManyChildElementsException e) {
            throw new RuntimeException("Unexpected exception getting the PolicyDetail element from Policy element", e);
        } catch (MissingRequiredElementException e) {
            throw new RuntimeException("Unexpected exception getting the PolicyDetail element from Policy element", e);
        }
        final String folderId = policyDetail.getAttribute("folderId");
        final Long newFolderId = oldToNewFolderIds.get(Long.valueOf(folderId));
        if (newFolderId == null) {
            throw new BundleResolver.InvalidBundleException("Policy with GUID: " + policyGuid + " is contained within unknown folder id '" + folderId + "' in the bundle");
        }

        policyDetail.setAttribute("folderId", String.valueOf(newFolderId));

        final String policyNameToUse;
        // Add prefix if needed
        if (installationPrefix != null && !installationPrefix.trim().isEmpty()) {
            final Element policyDetailWritable = PolicyUtils.getPolicyDetailElement(enumPolicyElmWritable, "Policy", policyGuid);
            final Element nameElementWritable = PolicyUtils.getPolicyNameElement(policyDetailWritable, "Policy", policyGuid);
            final String policyName = DomUtils.getTextValue(nameElementWritable);
            policyNameToUse = getPrefixedPolicyName(installationPrefix, policyName);
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

    private String getPrefixedPolicyName(@Nullable String installationPrefix, @NotNull String policyName) {
        if (installationPrefix == null) {
            return policyName;
        } else {
            return installationPrefix + " " + policyName;
        }
    }

    private void updatePolicyDoc(BundleInfo bundleInfo,
                                 String entityType,
                                 Map<String, String> oldGuidsToNewGuids,
                                 String identifier,
                                 Element policyResourceElmWritable,
                                 Document policyDocumentFromResource,
                                 @Nullable BundleMapping bundleMapping) throws BundleResolver.InvalidBundleException, PreBundleSavePolicyCallback.PolicyUpdateException {
        //todo duplicate call - for policy anyway - check for service - could have caller pass in.
        final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(policyDocumentFromResource);
        PolicyUtils.updatePolicyIncludes(oldGuidsToNewGuids, identifier, entityType, policyIncludes);

        if (bundleMapping != null) {
            final Map<String, String> mappedJdbcReferences = bundleMapping.getJdbcMappings();
            if (!mappedJdbcReferences.isEmpty()) {
                final List<Element> jdbcReferencesElms = PolicyUtils.findJdbcReferences(policyDocumentFromResource.getDocumentElement());
                for (Element jdbcRefElm : jdbcReferencesElms) {
                    try {
                        final Element connNameElm = XmlUtil.findExactlyOneChildElementByName(jdbcRefElm, BundleUtils.L7_NS_POLICY, "ConnectionName");
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
            savePolicyCallback.prePublishCallback(bundleInfo, "Policy", policyDocumentFromResource);
        }

        //write changes back to the resource document
        DomUtils.setTextContent(policyResourceElmWritable, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
    }

    /**
     * See if any existing service contains a service with the same urlMapping e.g. resolution URI
     *
     * @param urlMapping URI Resolution value for the search
     * @return list of ids of any existing service which have this routing uri
     * @throws Exception searching
     */
    @NotNull
    private List<Long> findMatchingService(String urlMapping) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                SERVICES_MGMT_NS, 10, "/l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='" + urlMapping + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    private Pair<AssertionStatus, Document> callManagementCheckInterrupted(String requestXml) throws InterruptedException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse {

        final Pair<AssertionStatus, Document> documentPair;
        try {
            documentPair = callManagementAssertion(gatewayManagementInvoker, requestXml);
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
            if (e.isCausedByMgmtAssertionInternalError() && policyBundleEvent.isCancelled()) {
                throw new InterruptedException("Possible interruption detected due to internal error");
            } else {
                throw e;
            }
        }
        return documentPair;
    }

    @NotNull
    private List<Long> findMatchingPolicy(String policyName) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                POLICIES_MGMT_NS, 10, "/l7:Policy/l7:PolicyDetail/l7:Name[text()='" + policyName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @NotNull
    private List<Long> findMatchingJdbcConnection(String jdbcConnection) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                JDBC_MGMT_NS, 10, "/l7:JDBCConnection/l7:Name[text()='" + jdbcConnection + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @Nullable
    private String getExistingPolicyGuid(String policyName) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException {

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
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException {
        final String folderFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                FOLDER_MGMT_NS, 10, "/l7:Folder[@folderId='" + parentId + "']/l7:Name[text()='" + folderName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(folderFilter);
        return GatewayManagementDocumentUtilities.getCreatedId(documentPair.right);
    }

    private void checkInterrupted(WSManagementRequestEvent wsManEvent) throws InterruptedException {
        if (wsManEvent.isCancelled() || Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
