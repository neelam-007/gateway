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
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
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
import com.l7tech.xml.xpath.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.oauthinstaller.GatewayManagementDocumentUtilities.*;

import static com.l7tech.server.event.AdminInfo.find;

public class OAuthInstallerAdminImpl extends AsyncAdminMethodsImpl implements OAuthInstallerAdmin {

    public static final String FOLDER_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/folders";
    public static final String POLICIES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/policies";
    public static final String SERVICES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/services";
    public static final String SOAP_NS = "http://www.w3.org/2003/05/soap-envelope";
    public static final String L7_NS_GW_MGMT = "http://ns.l7tech.com/2010/04/gateway-management";
    public static final String FOLDERS_CONTEXT_KEY = "FOLDERS";
    public static final String POLICY_FRAGMENT_CONTEXT_KEY = "POLICY FRAGMENTS";
    public static final String SERVICES_CONTEXT_KEY = "SERVICES";
    public static final String NS_BUNDLE = "http://ns.l7tech.com/2012/09/policy-bundle";

    /**
     * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
     * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
     *
     * @param bundleNames   names of all bundles to install. Bundles may depend on each others items, but there is no
     *                      install dependency order.
     * @param folderOid     oid of the folder to install into.
     * @param installFolder if not null or empty, this folder will be the install into folder. It may already exist.
     *                      If it does not exist it will be created.
     * @return Job ID, which will report on which bundles were installed.
     * @throws IOException for any problem installing. Installation is cancelled on the first error.
     */
    @NotNull
    @Override
    public JobId<ArrayList> installBundles(@NotNull final Collection<String> bundleNames,
                                           final long folderOid,
                                           @Nullable final String installFolder) throws IOException {

        final FutureTask<ArrayList> future = new FutureTask<ArrayList>(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                //todo check version of bundle to ensure it's supported.

                // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
                final Map<String, Object> contextMap = new HashMap<String, Object>();

                final ArrayList<String> installedBundles = new ArrayList<String>();
                if (isInstallInProgress.compareAndSet(false, true)) {
                    try {
                        //iterate through all the bundle names to install
                        for (String bundle : bundleNames) {
                            //todo search for and updated jdbc references as needed
                            doInstall(bundle, folderOid, installFolder, contextMap);
                            installedBundles.add(bundle);
                        }

                    } finally {
                        isInstallInProgress.set(false);
                    }
                } else {
                    throw new IOException("Install is already in progress");
                }

                return installedBundles;
            }

        }));

        // Lets schedule this to run in a few seconds, not straight away...want the UI to wait just a little
        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                future.run();
            }
        }, 5000L);

        return registerJob(future, ArrayList.class);
    }

    @NotNull
    @Override
    public List<Pair<String, String>> getAllAvailableBundles() throws IOException {
        final String name = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
        final URL resource = getClass().getResource(name);

        final File directory;
        try {
            if (resource.toURI().toString().startsWith("jar:")) {
                JarURLConnection urlConnection = (JarURLConnection) resource.openConnection();
                directory = new File(urlConnection.getJarFileURL().getFile());
            } else {
                // test code
                directory = new File(resource.toURI());
            }


        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        final File[] files = directory.listFiles();
        final List<Pair<String, String>> resultList = new ArrayList<Pair<String, String>>();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    final File bundleInfo = new File(file, "BundleInfo.xml");
                    if (bundleInfo.exists()) {
                        final byte[] bytes = IOUtils.slurpFile(bundleInfo);
                        try {
                            final Document bundleInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bytes));
                            final Element nameEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Name");
                            final Element descEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Description");
                            resultList.add(new Pair<String, String>(DomUtils.getTextValue(nameEl, true), DomUtils.getTextValue(descEl, true)));
                        } catch (Exception e) {
                            logger.warning("Could not parse Bundle Info from resource file " + bundleInfo.getPath());
                        }
                    }
                }
            }
        }

        return resultList;
    }

    /**
     * Wired via OAuthInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (applicationContext != null) {
            logger.log(Level.WARNING, "OAuthInstaller module is already initialized");
        } else {
            applicationContext = context;
        }
    }

    // - PRIVATE

    private ServerAssertion serverMgmtAssertion;
    private static ApplicationContext applicationContext;
    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());

    /**
     * Only one thread may call at a time. Not thread safe.
     *
     * @param bundleName    bundle to install
     * @param folderOid     oid of the parent folder
     * @param installFolder folder to install into. Required. May already exist.
     * @return String details
     * @throws IOException any problems
     */
    private void doInstall(@NotNull String bundleName,
                           long folderOid,
                           @Nullable String installFolder,
                           @NotNull Map<String, Object> contextMap) throws IOException {
        if (serverMgmtAssertion == null) {
            configureBeans();
        }

        logger.info("Installing bundle: " + bundleName);

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
                throw new IOException(e);
            }
        } else {
            folderToInstallInto = folderOid;
        }

        final Document folderBundle = getBundle(bundleName, "Folder.xml", false);


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
            throw new IOException(e);
        }

        contextOldToNewFolderOids.putAll(oldIdToNewFolderIds);

        if (!contextMap.containsKey(POLICY_FRAGMENT_CONTEXT_KEY)) {
            final Map<String, String> newMap = new HashMap<String, String>();
            contextMap.put(POLICY_FRAGMENT_CONTEXT_KEY, newMap);
        }
        @SuppressWarnings("unchecked")
        final Map<String, String> contextOldPolicyGuidsToNewGuids = (Map<String, String>) contextMap.get(POLICY_FRAGMENT_CONTEXT_KEY);

        // install policies
        final Document policyBundle = getBundle(bundleName, "Policy.xml", true);
        if (policyBundle == null) {
            logger.info("No policies to install for bundle " + bundleName);
        } else {
            try {
                installPolicies(oldIdToNewFolderIds, contextOldPolicyGuidsToNewGuids, policyBundle);
            } catch (Exception e) {
                throw new IOException(e);
            }
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
        final Document serviceBundle = getBundle(bundleName, "Service.xml", true);
        if (serviceBundle == null) {
            logger.info("No services to install for bundle " + bundleName);
        } else {
            try {
                installServices(oldIdToNewFolderIds, contextServices, contextOldPolicyGuidsToNewGuids, serviceBundle);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        logger.info("Finished installing bundle: " + bundleName);
    }

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
                                   @NotNull final Document serviceMgmtEnumeration) throws IOException {

        final List<Element> serviceElms = XmlUtil.findChildElementsByName(serviceMgmtEnumeration.getDocumentElement(), L7_NS_GW_MGMT, "Service");
        for (Element serviceElm : serviceElms) {
            try {
                final Element serviceElmWritable = XmlUtil.parse(XmlUtil.nodeToString(serviceElm)).getDocumentElement();
                final String id = serviceElmWritable.getAttribute("id");
                if (oldIdsToNewServiceIds.containsKey(Long.valueOf(id))) {
                    continue;
                }

                final Element serviceDetail = XmlUtil.findExactlyOneChildElementByName(serviceElmWritable, L7_NS_GW_MGMT, "ServiceDetail");

                final String bundleFolderId = serviceDetail.getAttribute("folderId");

                if (!oldToNewFolderIds.containsKey(Long.valueOf(bundleFolderId))) {
                    // todo invalid bundle exception
                    throw new IOException("Could not find updated folder for service #{" + id + "} in folder " + bundleFolderId);
                }

                // lets check if the service has a URL mapping and if so, if any service already exists with that mapping.
                // if it does, then we won't install it.
                // check if it has a URL mapping
                final ElementCursor cursor = new DomElementCursor(serviceDetail);
                // search from the current element only
                final XpathResult xpathResult = cursor.getXpathResult(
                        new XpathExpression(".//l7:UrlPattern", getNamespaceMap()).compile());
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

                final Element policyResourceElmWritable = getPolicyResourceElement(serviceElmWritable);
                if (policyResourceElmWritable == null) {
                    //todo invalid bundle element exception
                    throw new IOException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
                }

                // if this service has any includes we need to update them
                final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable);
                final List<Element> policyIncludes = getPolicyIncludes(policyDocumentFromResource);
                updatePolicyIncludes(contextOldPolicyGuidsToNewGuids, id, "Service", policyIncludes, policyResourceElmWritable, policyDocumentFromResource);

                final String serviceXmlTemplate = XmlUtil.nodeToString(serviceElmWritable);
                final String createServiceXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), SERVICES_MGMT_NS, serviceXmlTemplate);

                final Pair<AssertionStatus, Document> pair = callManagementAssertion(createServiceXml);

                final Long createdId = getCreatedId(pair.right);
                if (createdId == null) {
                    throw new IOException("Could not get the id for service from bundle with id: #{" + id + "}");
                }

                oldIdsToNewServiceIds.put(Long.valueOf(id), createdId);

            } catch (Exception e) {
                throw new IOException(e);
            }
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

        final Element policyResourceElmWritable = getPolicyResourceElement(policyElmWritable);
        if (policyResourceElmWritable == null) {
            //todo invalid bundle element exception
            throw new IOException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable);
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
     * A bundle item may not exist
     *
     * @param bundleName
     * @param itemName
     * @return
     * @throws IOException
     * @throws SAXException
     */
    @Nullable
    protected Document getItemFromBundle(String bundleName, String itemName) throws IOException, SAXException {
        final String name = "/com/l7tech/external/assertions/oauthinstaller/bundles/" + bundleName + "/" + itemName;
        logger.info("Getting bundle: " + name);
        final URL itemUrl = this.getClass().getResource(name);
        Document itemDocument = null;
        if (itemUrl != null) {
            final byte[] bytes = IOUtils.slurpUrl(itemUrl);
            itemDocument = XmlUtil.parse(new ByteArrayInputStream(bytes));
        }

        return itemDocument;
    }

    // - PRIVATE

    /**
     * See if any existing service contains a service with the same urlMapping e.g. resolution URI
     *
     * @param urlMapping URI Resolution value for the search
     * @return list of ids of any existing service which have this routing uri
     * @throws Exception searching
     */
    @NotNull
    private List<Long> findMatchingService(String urlMapping) throws Exception {
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

    @Nullable
    private Document getBundle(final String bundleName, final String bundleItem, final boolean allowMissing) throws IOException {
        final Document itemFromBundle;
        try {
            itemFromBundle = getItemFromBundle(bundleName, bundleItem);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        if (itemFromBundle == null && !allowMissing) {
            throw new IOException("Bundle item " + bundleItem + " was missing from bundle " + bundleName);
        }
        return itemFromBundle;
    }

    @NotNull
    protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) throws IOException, PolicyAssertionException {
        final PolicyEnforcementContext context = getContext(requestXml);

        final AssertionStatus assertionStatus = serverMgmtAssertion.checkRequest(context);
        final Message response = context.getResponse();
        final Document document;
        try {
            document = response.getXmlKnob().getDocumentReadOnly();
        } catch (SAXException e) {
            throw new IOException(e);
        }
        return new Pair<AssertionStatus, Document>(assertionStatus, document);
    }

    private synchronized void configureBeans() throws IOException {
        if (serverMgmtAssertion == null) {
            //todo reduce level
            logger.info("Initializing OAuth Installer.");

            final WspReader wspReader = applicationContext.getBean("wspReader", WspReader.class);
            final ServerPolicyFactory serverPolicyFactory = applicationContext.getBean("policyFactory", ServerPolicyFactory.class);

            final Assertion assertion = wspReader.parseStrictly(GATEWAY_MGMT_POLICY_XML, WspReader.Visibility.omitDisabled);
            try {
                serverMgmtAssertion = serverPolicyFactory.compilePolicy(assertion, false);
            } catch (ServerPolicyException e) {
                e.printStackTrace();
                throw new IOException(e);
            } catch (LicenseException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
        }
    }

    private PolicyEnforcementContext getContext(String requestXml) throws IOException {

        final Message request = new Message();
        final ContentTypeHeader contentTypeHeader = ContentTypeHeader.SOAP_1_2_DEFAULT;
        request.initialize(contentTypeHeader, requestXml.getBytes(Charsets.UTF8));

//        final MockServletContext servletContext = new MockServletContext();
//        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
//        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

//        servletContext.setContextPath("/");
//
//        final String contentType = contentTypeHeader.getFullValue();
//        httpServletRequest.setMethod("POST");
//        httpServletRequest.setContentType(contentType);
//        httpServletRequest.addHeader("Content-Type", contentType);
//        httpServletRequest.setRemoteAddr("127.0.0.1");
//        httpServletRequest.setServerName("127.0.0.1");
//        httpServletRequest.setRequestURI("/wsman");
//        httpServletRequest.setContent(requestXml.getBytes(Charsets.UTF8));

//        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
//        request.attachHttpRequestKnob(reqKnob);

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
