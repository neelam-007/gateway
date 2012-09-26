package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.server.util.JaasUtils;
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

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.*;

public class PolicyBundleInstaller {
    public static final String FOLDER_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/folders";
    public static final String POLICIES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/policies";
    public static final String SERVICES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/services";

    public static final String FOLDERS_CONTEXT_KEY = "FOLDERS";
    public static final String POLICY_FRAGMENT_CONTEXT_KEY = "POLICY FRAGMENTS";
    public static final String SERVICES_CONTEXT_KEY = "SERVICES";

    //todo delete
    public PolicyBundleInstaller(@NotNull final BundleResolver bundleResolver,
                                 @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {

        this(bundleResolver, null, gatewayManagementInvoker);
    }

    public PolicyBundleInstaller(@NotNull final BundleResolver bundleResolver,
                                 @Nullable final PreBundleSavePolicyCallback savePolicyCallback,
                                 @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        this.bundleResolver = bundleResolver;
        this.savePolicyCallback = savePolicyCallback;
        this.gatewayManagementInvoker = gatewayManagementInvoker;
    }

    /**
     *
     * @param context@return String details
     * @throws java.io.IOException any problems
     */
    public void install(PolicyBundleInstallerContext context)
            throws InstallationException,
            BundleResolver.UnknownBundleException,
            BundleResolver.BundleResolverException,
            InterruptedException,
            BundleResolver.InvalidBundleException {

        logger.info("Installing bundle: " + context.getBundleInfo().getId());

        // allow this code to attempt to create the install folder each time.
        final long folderToInstallInto;
        //noinspection ConstantConditions
        if (context.getInstallFolder() != null && !context.getInstallFolder().trim().isEmpty()) {
            // get or create root node
            final String requestXml = MessageFormat.format(FOLDER_XML, getUuid(), String.valueOf(context.getFolderOid()), context.getInstallFolder());
            final Pair<AssertionStatus, Document> pair;
            try {
                pair = callManagementAssertion(requestXml);
                final Long createId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
                if (createId != null) {
                    context.getContextMap().put("INSTALL_FOLDER", createId);   //todo define and use later
                    folderToInstallInto = createId;
                } else {
                    // validate the folder already existed
                    Long existingFolderId = null;
                    if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                        existingFolderId = getExistingFolderId(context.getFolderOid(), context.getInstallFolder());
                    }

                    if (existingFolderId == null) {
                        throw new IOException("Folder to install into could not be created and did not already exist: " + context.getInstallFolder());
                    } else {
                        folderToInstallInto = existingFolderId;
                    }
                }
            } catch (Exception e) {
                throw new InstallationException(e);
            }
        } else {
            folderToInstallInto = context.getFolderOid();
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        final Document folderBundle = bundleResolver.getBundleItem(context.getBundleInfo().getId(), FOLDER, false);

        if (!context.getContextMap().containsKey(FOLDERS_CONTEXT_KEY)) {
            final Map<Long, Long> newMap = new HashMap<Long, Long>();
            context.getContextMap().put(FOLDERS_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<Long, Long> contextOldToNewFolderOids = (Map<Long, Long>) context.getContextMap().get(FOLDERS_CONTEXT_KEY);

        final Map<Long, Long> oldIdToNewFolderIds;
        try {                                                               //todo fix
            oldIdToNewFolderIds = installFolders(folderToInstallInto, folderBundle, contextOldToNewFolderOids);
        } catch (Exception e) {
            throw new InstallationException(e);
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

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
                installPolicies(context.getBundleInfo(), oldIdToNewFolderIds, contextOldPolicyGuidsToNewGuids, policyBundle, context.getBundleMapping());
            } catch (Exception e) {
                throw new InstallationException(e);
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

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
                installServices(context.getBundleInfo(), oldIdToNewFolderIds, contextServices, contextOldPolicyGuidsToNewGuids, serviceBundle, context.getBundleMapping());
            } catch (Exception e) {
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
    protected void installServices(@NotNull final BundleInfo bundleInfo,
                                   @NotNull final Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<Long, Long> oldIdsToNewServiceIds,
                                   @NotNull final Map<String, String> contextOldPolicyGuidsToNewGuids,
                                   @NotNull final Document serviceMgmtEnumeration,
                                   @Nullable final BundleMapping bundleMapping) throws BundleResolver.InvalidBundleException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse, PreBundleSavePolicyCallback.UnknownReferenceException {

        final List<Element> serviceElms = BundleUtils.getEntityElements(serviceMgmtEnumeration.getDocumentElement(), "Service");
        for (Element serviceElm : serviceElms) {
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
                final String urlPattern = xpathResult.getNodeSet().getNodeValue(0);
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

            final Element policyResourceElmWritable = BundleUtils.getPolicyResourceElement(serviceElmWritable, "Service", id);
            if (policyResourceElmWritable == null) {
                throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
            }

            // if this service has any includes we need to update them
            final Document policyDocumentFromResource = BundleUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Service", id);
            updatePolicyDoc(bundleInfo, "Service", contextOldPolicyGuidsToNewGuids, id, policyResourceElmWritable, policyDocumentFromResource, bundleMapping);

            final String serviceXmlTemplate = XmlUtil.nodeToStringQuiet(serviceElmWritable);
            final String createServiceXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), SERVICES_MGMT_NS, serviceXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementAssertion(createServiceXml);

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
     * @param oldToNewFolderIds
     * @param oldGuidsToNewGuids
     * @param policyMgmtEnumeration
     * @return
     * @throws Exception
     */
    protected void installPolicies(@NotNull final BundleInfo bundleInfo,
                                   @NotNull Map<Long, Long> oldToNewFolderIds,
                                   @NotNull Map<String, String> oldGuidsToNewGuids,
                                   @NotNull Document policyMgmtEnumeration,
                                   @Nullable BundleMapping bundleMapping) throws Exception {

        final List<Element> policyElms = BundleUtils.getEntityElements(policyMgmtEnumeration.getDocumentElement(), "Policy");

        final Map<String, String> guidToName = new HashMap<String, String>();
        final Map<String, Element> allPolicyElms = new HashMap<String, Element>();
        for (Element policyElm : policyElms) {
            final Element name = XmlUtil.findFirstDescendantElement(policyElm, BundleUtils.L7_NS_GW_MGMT, "Name");
            final String policyName = DomUtils.getTextValue(name, true);
            final String guid = policyElm.getAttribute("guid");
            guidToName.put(guid, policyName);
            allPolicyElms.put(guid, policyElm);
        }

        // fyi: circular policy includes are not supported via the Policy Manager - assume they will not be found
        for (Element policyElm : policyElms) {
            // recursive call if policy includes an include
            getOrCreatePolicy(bundleInfo, policyElm, oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName, bundleMapping);
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
    protected Map<Long, Long> installFolders(long parentFolderOid,
                                             @NotNull Document folderMgmtEnumeration,
                                             @NotNull Map<Long, Long> contextOldToNewFolderOids) throws Exception {
        final List<Element> folderElms = BundleUtils.getEntityElements(folderMgmtEnumeration.getDocumentElement(), "Folder");

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

            final String folderXmlTemplate = XmlUtil.nodeToString(document.getDocumentElement());
            final String createFolderXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), FOLDER_MGMT_NS, folderXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementAssertion(createFolderXml);
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
    private static final Logger logger = Logger.getLogger(PolicyBundleInstaller.class.getName());

    @NotNull
    private Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
        final PolicyEnforcementContext context = getContext(requestXml);

        final AssertionStatus assertionStatus;
        try {
            assertionStatus = gatewayManagementInvoker.checkRequest(context);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected internal error invoking gateway management serivce: " + e.getMessage(), e);
        } catch (PolicyAssertionException e) {
            throw new RuntimeException("Unexpected internal error invoking gateway management serivce: " + e.getMessage(), e);
        }
        final Message response = context.getResponse();
        final Document document;
        try {
            document = response.getXmlKnob().getDocumentReadOnly();
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected internal error parsing gateway management response: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected internal error parsing gateway management response: " + e.getMessage(), e);
        }
        return new Pair<AssertionStatus, Document>(assertionStatus, document);
    }

    private Document parseQuietly(String inputXml) {
        try {
            return XmlUtil.parse(inputXml);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected internal error parsing XML: " + e.getMessage(), e);
        }
    }

    private void getOrCreatePolicy(@NotNull final BundleInfo bundleInfo,
                                   @NotNull final Element policyElmReadOnly,
                                   @NotNull final Map<String, String> oldGuidsToNewGuids,
                                   @NotNull Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<String, Element> allPolicyElms,
                                   @NotNull final Map<String, String> guidToName,
                                   @Nullable final BundleMapping bundleMapping) throws Exception {

        final String policyGuid = policyElmReadOnly.getAttribute("guid");

        if (oldGuidsToNewGuids.containsKey(policyGuid)) {
            // already created
            return;
        }

        final Element policyElmWritable = XmlUtil.parse(XmlUtil.nodeToString(policyElmReadOnly)).getDocumentElement();

        final Element policyResourceElmWritable = BundleUtils.getPolicyResourceElement(policyElmWritable, "Policy", policyGuid);
        if (policyResourceElmWritable == null) {
            //todo invalid bundle element exception
            throw new IOException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocumentFromResource = BundleUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", policyGuid);
        // these do not belong to the policyResourceElmWritable document
        final List<Element> policyIncludes = BundleUtils.getPolicyIncludes(policyDocumentFromResource);
        for (Element policyIncludeElm : policyIncludes) {
            final String policyInclude = policyIncludeElm.getAttribute("stringValue");
            if (!allPolicyElms.containsKey(policyInclude)) {
                //todo missing dependency exception
                throw new IOException("Policy with guid " + policyInclude + " was not included in bundle");
            }
            getOrCreatePolicy(bundleInfo, allPolicyElms.get(policyInclude), oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName, bundleMapping);
        }

        // get or create
        // Create a new document and modify it
        updatePolicyDoc(bundleInfo, "Policy", oldGuidsToNewGuids, policyGuid, policyResourceElmWritable, policyDocumentFromResource, bundleMapping);

        final Element policyDetail = XmlUtil.findExactlyOneChildElementByName(policyElmWritable, BundleUtils.L7_NS_GW_MGMT, "PolicyDetail");
        final String folderId = policyDetail.getAttribute("folderId");
        final Long newFolderId = oldToNewFolderIds.get(Long.valueOf(folderId));
        if (newFolderId == null) {
            //todo invalid bundle exception
            throw new IOException("Policy with GUID: " + policyGuid + " is contained within unknown folder id '" + folderId + "' in the bundle");
        }

        policyDetail.setAttribute("folderId", String.valueOf(newFolderId));

        final String policyXmlTemplate = XmlUtil.nodeToString(policyElmWritable);

        final String createPolicyXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), POLICIES_MGMT_NS, policyXmlTemplate);
        final Pair<AssertionStatus, Document> pair = callManagementAssertion(createPolicyXml);

        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
        String guidToUse = null;
        if (createdId == null) {
            if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                guidToUse = getExistingPolicyGuid(guidToName.get(policyGuid));
            }
        } else {
            // we just created it
            guidToUse = getExistingPolicyGuid(guidToName.get(policyGuid));
        }

        if (guidToUse == null) {
            throw new IOException("Could not get the GUID for policy from bundle with guid: " + policyGuid);
        }

        oldGuidsToNewGuids.put(policyGuid, guidToUse);
    }

    private void updatePolicyDoc(BundleInfo bundleInfo,
                                 String entityType,
                                 Map<String, String> oldGuidsToNewGuids,
                                 String identifier,
                                 Element policyResourceElmWritable,
                                 Document policyDocumentFromResource,
                                 @Nullable BundleMapping bundleMapping) throws BundleResolver.InvalidBundleException {
        final List<Element> policyIncludes = BundleUtils.getPolicyIncludes(policyDocumentFromResource);
        GatewayManagementDocumentUtilities.updatePolicyIncludes(oldGuidsToNewGuids, identifier, entityType, policyIncludes);

        if (bundleMapping != null) {
            final Map<String, String> mappedJdbcReferences = bundleMapping.getJdbcMappings();
            if (!mappedJdbcReferences.isEmpty()) {
                final List<Element> jdbcReferencesElms = BundleUtils.findJdbcReferences(policyDocumentFromResource.getDocumentElement());
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

        //todo provide a call back so the caller can decide to make any ad hoc modifications to the policy
        if (savePolicyCallback != null) {
            savePolicyCallback.updateReferences(bundleInfo, "Policy", policyDocumentFromResource);
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
    private List<Long> findMatchingService(String urlMapping) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                SERVICES_MGMT_NS, 10, "/l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='" + urlMapping + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @Nullable
    private Long getExistingFolderId(long parentId, String folderName) throws Exception {

        final String folderFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                FOLDER_MGMT_NS, 10, "/l7:Folder[@folderId='" + parentId + "']/l7:Name[text()='" + folderName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(folderFilter);
        return GatewayManagementDocumentUtilities.getCreatedId(documentPair.right);
    }

    @Nullable
    private String getExistingPolicyGuid(String policyName) throws Exception {

        final String getPolicyXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(getPolicyXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult = cursor.getXpathResult(
                new XpathExpression("string(/env:Envelope/env:Body/l7:Policy/@guid)", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        return xpathResult.getString();
    }

    private String getUuid() {
        return "uuid:" + UUID.randomUUID();
    }

    private PolicyEnforcementContext getContext(String requestXml) {

        final Message request = new Message();
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.SOAP_1_2_DEFAULT;
        try {
            request.initialize(contentTypeHeader, requestXml.getBytes(Charsets.UTF8));
        } catch (IOException e) {
            // this is a programming error. All requests are generated in this class.
            throw new RuntimeException("Unexpected internal error preparing gateway management request: " + e.getMessage(), e);
        }

        HttpRequestKnob requestKnob = new HttpRequestKnobAdapter(){
            @Override
            public String getRemoteAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getRequestUrl() {
                //todo fix url - check it's usage in mgmt server assertion.
                return "http://localhost:8080/wsman";
            }
        };
        request.attachKnob(HttpRequestKnob.class, requestKnob);

        HttpResponseKnob responseKnob = new AbstractHttpResponseKnob() {
            @Override
            public void addCookie(HttpCookie cookie) {

            }
        };

        final Message response = new Message();
        response.attachKnob(HttpResponseKnob.class, responseKnob);

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        final User currentUser = JaasUtils.getCurrentUser();
        if (currentUser != null) {
            // convert logged on user into a UserBean as if the user was authenticated via policy.
            final UserBean userBean = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
            userBean.setUniqueIdentifier(currentUser.getId());
            context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(
                    userBean,
                    new HttpBasicToken(currentUser.getLogin(), "".toCharArray()), null, false)
            );
        } else {
            // no action will be allowed - this will result in permission denied later
            //todo deal with this here
            logger.warning("No current user");
        }

        return context;
    }


    //todo remove and use CREATE_FOLDER_XML
    private final String FOLDER_XML = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">https://localhost:9443/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n" +
            "        <wsman:OperationTimeout>PT5M0.000S</wsman:OperationTimeout>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <l7:Folder folderId=\"{1}\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <l7:Name>{2}</l7:Name>\n" +
            "        </l7:Folder>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private final String CREATE_ENTITY_XML = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">https://localhost:9443/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:OperationTimeout>PT5M0.000S</wsman:OperationTimeout>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "       {2}\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private final String GATEWAY_MGMT_ENUMERATE_FILTER = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:RequestTotalItemsCountEstimate/>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wsen:Enumerate>\n" +
            "            <wsman:OptimizeEnumeration/>\n" +
            "            <wsman:MaxElements>{2}</wsman:MaxElements>\n" +
            "            <wsman:Filter>{3}</wsman:Filter>\n" +
            "<wsman:EnumerationMode>EnumerateObjectAndEPR</wsman:EnumerationMode>" +
            "        </wsen:Enumerate>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    /**
     * Requires in this order: UUID, Resource URI, selector name (id or name), selector value
     */
    private final String GATEWAY_MGMT_GET_ENTITY = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">{0}</wsa:MessageID>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
            "        <wsman:ResourceURI>{1}</wsman:ResourceURI>\n" +
            "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
            "        <wsman:SelectorSet>\n" +
            "            <wsman:Selector Name=\"{2}\">{3}</wsman:Selector>\n" +
            "        </wsman:SelectorSet>\n" +
            "    </env:Header>\n" +
            "    <env:Body/>\n" +
            "</env:Envelope>";
}
