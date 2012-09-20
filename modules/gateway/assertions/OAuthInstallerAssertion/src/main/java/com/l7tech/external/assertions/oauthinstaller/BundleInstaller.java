package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.oauthinstaller.GatewayManagementDocumentUtilities.*;

/**
 * Installs bundles which contain Folders, Services and Policies
 *
 * Depends on being able to access the Server Gateway Management assertion at runtime.
 *
 */
public class BundleInstaller {

    public static final String FOLDER_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/folders";
    public static final String POLICIES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/policies";
    public static final String SERVICES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/services";
    public static final String L7_NS_GW_MGMT = "http://ns.l7tech.com/2010/04/gateway-management";
    public static final String FOLDERS_CONTEXT_KEY = "FOLDERS";
    public static final String POLICY_FRAGMENT_CONTEXT_KEY = "POLICY FRAGMENTS";
    public static final String SERVICES_CONTEXT_KEY = "SERVICES";

    public BundleInstaller(@NotNull BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        configureBeans();
    }

    /**
     * @param resourcePolicyElement element which can contain a resource of type policy. Should be an element of type
     *                              &lt;l7:Resource type="policy"&gt;
     * @return
     * @throws java.io.IOException
     */
    public static Document getPolicyDocumentFromResource(@NotNull final Element resourcePolicyElement,
                                                         @NotNull final String resourceType,
                                                         @NotNull final String identifier)
            throws InvalidBundleException {

        if (!resourcePolicyElement.getLocalName().equals("Resource") ||
                !resourcePolicyElement.getNamespaceURI().equals(L7_NS_GW_MGMT) ||
                !resourcePolicyElement.getAttribute("type").equals("policy")
                ) {

            // runtime programming error
            throw new IllegalArgumentException("Invalid policy element. Cannot extract policy includes");
        }

        try {
            return XmlUtil.parse(DomUtils.getTextValue(resourcePolicyElement));
        } catch (SAXException e) {
            throw new InvalidBundleException("Could not get policy document from resource element for " + resourceType +
                    " with identifier #{" + identifier + "}");
        }
    }

    /**
     * Allow callers to deal with a not found element, so return is nullable.
     *
     * @param elementWithPolicyDescendant
     * @return
     * @throws Exception
     */
    @Nullable
    public static Element getPolicyResourceElement(@NotNull final Element elementWithPolicyDescendant,
                                                   @NotNull final String resourceType,
                                                   @NotNull final String identifier) throws InvalidBundleException {

        final ElementCursor policyCursor = new DomElementCursor(elementWithPolicyDescendant, false);
        // The xpath expression below uses '.' to make sure it runs from the current element and not over the entire document.
        final XpathResult xpathResult = XpathUtil.getXpathResultQuietly(policyCursor, getNamespaceMap(), ".//l7:Resource[@type='policy']");

        final Element returnElement;
        if (xpathResult.getType() == XpathResult.TYPE_NODESET && !xpathResult.getNodeSet().isEmpty()) {
            if (xpathResult.getNodeSet().size() != 1) {
                // mgmt api updated exception - wrong version exception
                throw new InvalidBundleException("More than one policy found for " + resourceType + " with id #{" + identifier + "}. Not supported.");
            }
            returnElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
        } else {
            returnElement = null;
        }

        return returnElement;
    }


    /**
     * Only one thread may call at a time. Not thread safe.
     *
     * @param bundleId    bundle to install
     * @param folderOid     oid of the parent folder
     * @param installFolder folder to install into. Required. May already exist.
     * @return String details
     * @throws IOException any problems
     */
    public void doInstall(@NotNull String bundleId,
                           long folderOid,
                           @Nullable String installFolder,
                           @NotNull Map<String, Object> contextMap)
            throws InstallationException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, InterruptedException {
        logger.info("Installing bundle: " + bundleId);

        // allow this code to attempt to create the install folder each time.
        final long folderToInstallInto;
        if (installFolder != null && !installFolder.trim().isEmpty()) {
            // get or create root node
            final String requestXml = MessageFormat.format(FOLDER_XML, getUuid(), String.valueOf(folderOid), installFolder);
            final Pair<AssertionStatus, Document> pair;
            try {
                pair = callManagementAssertion(requestXml);
                final Long createId = getCreatedId(pair.right);
                if (createId != null) {
                    contextMap.put("INSTALL_FOLDER", createId);   //todo define and use later
                    folderToInstallInto = createId;
                } else {
                    // validate the folder already existed
                    Long existingFolderId = null;
                    if (resourceAlreadyExists(pair.right)) {
                        existingFolderId = getExistingFolderId(folderOid, installFolder);
                    }

                    if (existingFolderId == null) {
                        throw new IOException("Folder to install into could not be created and did not already exist: " + installFolder);
                    } else {
                        folderToInstallInto = existingFolderId;
                    }
                }
            } catch (Exception e) {
                throw new InstallationException(e);
            }
        } else {
            folderToInstallInto = folderOid;
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        final Document folderBundle = bundleResolver.getBundleItem(bundleId, "Folder.xml", false);

        if (!contextMap.containsKey(FOLDERS_CONTEXT_KEY)) {
            final Map<Long, Long> newMap = new HashMap<Long, Long>();
            contextMap.put(FOLDERS_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<Long, Long> contextOldToNewFolderOids = (Map<Long, Long>) contextMap.get(FOLDERS_CONTEXT_KEY);

        final Map<Long, Long> oldIdToNewFolderIds;
        try {
            oldIdToNewFolderIds = installFolders(folderToInstallInto, folderBundle, contextOldToNewFolderOids);
        } catch (Exception e) {
            throw new InstallationException(e);
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        contextOldToNewFolderOids.putAll(oldIdToNewFolderIds);

        if (!contextMap.containsKey(POLICY_FRAGMENT_CONTEXT_KEY)) {
            final Map<String, String> newMap = new HashMap<String, String>();
            contextMap.put(POLICY_FRAGMENT_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<String, String> contextOldPolicyGuidsToNewGuids = (Map<String, String>) contextMap.get(POLICY_FRAGMENT_CONTEXT_KEY);

        // install policies
        final Document policyBundle = bundleResolver.getBundleItem(bundleId, "Policy.xml", true);
        if (policyBundle == null) {
            logger.info("No policies to install for bundle " + bundleId);
        } else {
            try {
                installPolicies(oldIdToNewFolderIds, contextOldPolicyGuidsToNewGuids, policyBundle);
            } catch (Exception e) {
                throw new InstallationException(e);
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        // two bundles should not contain the same service, however if they do we can short circuit trying to create it
        // by tracking it in this map
        if (!contextMap.containsKey(SERVICES_CONTEXT_KEY)) {
            final Map<Long, Long> newMap = new HashMap<Long, Long>();
            contextMap.put(SERVICES_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<Long, Long> contextServices = (Map<Long, Long>) contextMap.get(SERVICES_CONTEXT_KEY);

        // install services
        final Document serviceBundle = bundleResolver.getBundleItem(bundleId, "Service.xml", true);
        if (serviceBundle == null) {
            logger.info("No services to install for bundle " + bundleId);
        } else {
            try {
                installServices(oldIdToNewFolderIds, contextServices, contextOldPolicyGuidsToNewGuids, serviceBundle);
            } catch (Exception e) {
                throw new InstallationException(e);
            }
        }

        logger.info("Finished installing bundle: " + bundleId);
    }

    public static class InvalidBundleException extends Exception{
        public InvalidBundleException(String message) {
            super(message);
        }

        public InvalidBundleException(Throwable cause) {
            super(cause);
        }
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
    protected void installServices(@NotNull final Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<Long, Long> oldIdsToNewServiceIds,
                                   @NotNull final Map<String, String> contextOldPolicyGuidsToNewGuids,
                                   @NotNull final Document serviceMgmtEnumeration) throws InvalidBundleException,
            UnexpectedManagementResponse {

        final List<Element> serviceElms = XmlUtil.findChildElementsByName(serviceMgmtEnumeration.getDocumentElement(), L7_NS_GW_MGMT, "Service");
        for (Element serviceElm : serviceElms) {
            final Element serviceElmWritable = parseQuietly(XmlUtil.nodeToStringQuiet(serviceElm)).getDocumentElement();
            final String id = serviceElmWritable.getAttribute("id");
            if (oldIdsToNewServiceIds.containsKey(Long.valueOf(id))) {
                continue;
            }

            final Element serviceDetail;
            try {
                serviceDetail = XmlUtil.findExactlyOneChildElementByName(serviceElmWritable, L7_NS_GW_MGMT, "ServiceDetail");
            } catch (TooManyChildElementsException e) {
                throw new InvalidBundleException("Invalid Service XML found. Expected a single ServiceDetail element for service with id #{" + id + "}");
            } catch (MissingRequiredElementException e) {
                throw new InvalidBundleException("Invalid Service XML found. No ServiceDetail element found for service with id #{" + id + "}");
            }

            final String bundleFolderId = serviceDetail.getAttribute("folderId");

            if (!oldToNewFolderIds.containsKey(Long.valueOf(bundleFolderId))) {
                throw new InvalidBundleException("Could not find updated folder for service #{" + id + "} in folder " + bundleFolderId);
            }

            // lets check if the service has a URL mapping and if so, if any service already exists with that mapping.
            // if it does, then we won't install it.
            // check if it has a URL mapping
            final ElementCursor cursor = new DomElementCursor(serviceDetail);
            // search from the current element only
            final XpathResult xpathResult;
            try {
                xpathResult = cursor.getXpathResult(
                        new XpathExpression(".//l7:UrlPattern", getNamespaceMap()).compile());
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

            final Element policyResourceElmWritable = getPolicyResourceElement(serviceElmWritable, "Service", id);
            if (policyResourceElmWritable == null) {
                throw new InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
            }

            // if this service has any includes we need to update them
            final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", id);
            final List<Element> policyIncludes = getPolicyIncludes(policyDocumentFromResource);
            updatePolicyIncludes(contextOldPolicyGuidsToNewGuids, id, "Service", policyIncludes, policyResourceElmWritable, policyDocumentFromResource);

            final String serviceXmlTemplate = XmlUtil.nodeToStringQuiet(serviceElmWritable);
            final String createServiceXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), SERVICES_MGMT_NS, serviceXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementAssertion(createServiceXml);

            final Long createdId = getCreatedId(pair.right);
            if (createdId == null) {
                throw new UnexpectedManagementResponse("Could not get the id for service from bundle with id: #{" + id + "}");
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
    protected void installPolicies(@NotNull Map<Long, Long> oldToNewFolderIds,
                                   @NotNull Map<String, String> oldGuidsToNewGuids,
                                   @NotNull Document policyMgmtEnumeration) throws Exception {

        final List<Element> policyElms = XmlUtil.findChildElementsByName(policyMgmtEnumeration.getDocumentElement(), L7_NS_GW_MGMT, "Policy");

        final Map<String, String> guidToName = new HashMap<String, String>();
        final Map<String, Element> allPolicyElms = new HashMap<String, Element>();
        for (Element policyElm : policyElms) {
            final Element name = XmlUtil.findFirstDescendantElement(policyElm, L7_NS_GW_MGMT, "Name");
            final String policyName = DomUtils.getTextValue(name, true);
            final String guid = policyElm.getAttribute("guid");
            guidToName.put(guid, policyName);
            allPolicyElms.put(guid, policyElm);
        }

        // fyi: circular policy includes are not supported via the Policy Manager - assume they will not be found
        for (Element policyElm : policyElms) {
            // recursive call if policy includes an include
            getOrCreatePolicy(policyElm, oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName);
        }
    }

    /**
     * Folders will be created if needed. If folders already exist in the parent folder then they will not be modified.
     * Incoming XML will contain ids for folders, however these are ignored by the management api and new ids will be
     * assigned.
     *
     * @param parentFolderOid oid of the parent folder to install into.
     * @param folderMgmtDoc   Gateway management enumeration document of folders to install
     * @return map of all folders ids to ids in the folderMgmtDoc. The key is always the folders canned id, the id in the
     *         folderMgmtDoc, the value will either be a new id if the folder was created or it will be the id of the existing
     *         folder on the target gateway.
     * @throws Exception //todo define exception semantics
     */
    protected Map<Long, Long> installFolders(long parentFolderOid,
                                             @NotNull Document folderMgmtDoc,
                                             @NotNull Map<Long, Long> contextOldToNewFolderOids) throws Exception {
        final List<Element> folderElms = XmlUtil.findChildElementsByName(folderMgmtDoc.getDocumentElement(), L7_NS_GW_MGMT, "Folder");

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

            final Document document = XmlUtil.createEmptyDocument("Folder", "l7", L7_NS_GW_MGMT);
            final Element folder = document.getDocumentElement();
            folder.setAttribute("folderId", String.valueOf(newParentId));
            folder.setAttribute("id", id); //this gets overwritten and ignored by mgmt assertion.

            final Element name = DomUtils.createAndAppendElementNS(folder, "Name", L7_NS_GW_MGMT, "l7");
            final String folderName = DomUtils.getTextValue(DomUtils.findFirstChildElementByName(currentElm, L7_NS_GW_MGMT, "Name"), true);
            final Text nameText = document.createTextNode(folderName);
            name.appendChild(nameText);

            final String folderXmlTemplate = XmlUtil.nodeToString(document.getDocumentElement());
            final String createFolderXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), FOLDER_MGMT_NS, folderXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementAssertion(createFolderXml);
            final Long newId = getCreatedId(pair.right);
            final Long idToRecord;
            if (newId == null) {
                if (resourceAlreadyExists(pair.right)) {
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

    @NotNull
    protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
        final PolicyEnforcementContext context = getContext(requestXml);

        final AssertionStatus assertionStatus;
        try {
            assertionStatus = serverMgmtAssertion.checkRequest(context);
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

    // - PRIVATE

    private ServerAssertion serverMgmtAssertion; // multi threaded
    private final BundleResolver bundleResolver;
    private ApplicationContext applicationContext;
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());

    private Document parseQuietly(String inputXml) {
        try {
            return XmlUtil.parse(inputXml);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected internal error parsing XML: " + e.getMessage(), e);
        }
    }

    private void getOrCreatePolicy(@NotNull final Element policyElmReadOnly,
                                   @NotNull final Map<String, String> oldGuidsToNewGuids,
                                   @NotNull Map<Long, Long> oldToNewFolderIds,
                                   @NotNull final Map<String, Element> allPolicyElms,
                                   @NotNull final Map<String, String> guidToName) throws Exception {

        final String policyGuid = policyElmReadOnly.getAttribute("guid");

        if (oldGuidsToNewGuids.containsKey(policyGuid)) {
            // already created
            return;
        }

        final Element policyElmWritable = XmlUtil.parse(XmlUtil.nodeToString(policyElmReadOnly)).getDocumentElement();

        final Element policyResourceElmWritable = getPolicyResourceElement(policyElmWritable, "Policy", policyGuid);
        if (policyResourceElmWritable == null) {
            //todo invalid bundle element exception
            throw new IOException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", policyGuid);
        // these do not belong to the policyResourceElmWritable document
        final List<Element> policyIncludes = getPolicyIncludes(policyDocumentFromResource);
        for (Element policyIncludeElm : policyIncludes) {
            final String policyInclude = policyIncludeElm.getAttribute("stringValue");
            if (!allPolicyElms.containsKey(policyInclude)) {
                //todo missing dependency exception
                throw new IOException("Policy with guid " + policyInclude + " was not included in bundle");
            }
            getOrCreatePolicy(allPolicyElms.get(policyInclude), oldGuidsToNewGuids, oldToNewFolderIds, allPolicyElms, guidToName);
        }

        // get or create
        // Create a new document and modify it
        updatePolicyIncludes(oldGuidsToNewGuids, policyGuid, "Policy", policyIncludes, policyResourceElmWritable, policyDocumentFromResource);

        final Element policyDetail = XmlUtil.findExactlyOneChildElementByName(policyElmWritable, L7_NS_GW_MGMT, "PolicyDetail");
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

        final Long createdId = getCreatedId(pair.right);
        String guidToUse = null;
        if (createdId == null) {
            if (resourceAlreadyExists(pair.right)) {
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

    /**
     * See if any existing service contains a service with the same urlMapping e.g. resolution URI
     *
     * @param urlMapping URI Resolution value for the search
     * @return list of ids of any existing service which have this routing uri
     * @throws Exception searching
     */
    @NotNull
    private List<Long> findMatchingService(String urlMapping) throws UnexpectedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                SERVICES_MGMT_NS, 10, "/l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='" + urlMapping + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(serviceFilter);
        return getSelectorId(documentPair.right, true);
    }

    @Nullable
    private Long getExistingFolderId(long parentId, String folderName) throws Exception {

        final String folderFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                FOLDER_MGMT_NS, 10, "/l7:Folder[@folderId='" + parentId + "']/l7:Name[text()='" + folderName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(folderFilter);
        return getCreatedId(documentPair.right);
    }

    @Nullable
    private String getExistingPolicyGuid(String policyName) throws Exception {

        final String getPolicyXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        final Pair<AssertionStatus, Document> documentPair = callManagementAssertion(getPolicyXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult = cursor.getXpathResult(
                new XpathExpression("string(/env:Envelope/env:Body/l7:Policy/@guid)", getNamespaceMap()).compile());
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

    private void configureBeans() {
        if (serverMgmtAssertion == null) {
            synchronized (this) {
                if (serverMgmtAssertion == null) {
                    //todo reduce level
                    logger.info("Initializing OAuth Installer.");

                    final WspReader wspReader = applicationContext.getBean("wspReader", WspReader.class);
                    final ServerPolicyFactory serverPolicyFactory = applicationContext.getBean("policyFactory", ServerPolicyFactory.class);

                    try {
                        final Assertion assertion = wspReader.parseStrictly(GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
                        serverMgmtAssertion = serverPolicyFactory.compilePolicy(assertion, false);
                    } catch (ServerPolicyException e) {
                        // todo log and audit with stack trace
                        throw new RuntimeException(e);
                    } catch (LicenseException e) {
                        // todo log and audit with stack trace
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        // todo log and audit with stack trace
                        throw new RuntimeException(e);
                    }
                }
            }
        }
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

    private final String GATEWAY_MGMT_POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:GatewayManagement/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

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
