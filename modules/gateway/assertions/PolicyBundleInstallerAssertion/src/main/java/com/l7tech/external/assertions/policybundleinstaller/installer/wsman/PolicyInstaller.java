package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller.InstallationException;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.*;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.POLICY;
import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.server.policy.bundle.PolicyUtils.updatePolicyIncludes;

/**
 * Install policy.
 */
public class PolicyInstaller extends WsmanInstaller {
    public static final String POLICIES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/policies";

    /**
     * Parameter order: {0} revision type, {1} UUID, {2} selector value, {3} comment
     */
    private static final String GATEWAY_MGMT_SET_REVISION_COMMENT_REQUEST = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"\n" +
            "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/{0}/SetVersionComment</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "       <wsa:MessageID env:mustUnderstand=\"true\">{1}</wsa:MessageID>\n" +
            "       <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
            "       <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/{0}</wsman:ResourceURI>\n" +
            "       <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
            "       <wsman:SelectorSet>\n" +
            "            <wsman:Selector Name=\"id\">{2}</wsman:Selector>\n" +
            "       </wsman:SelectorSet>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "       <VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <Comment>{3}</Comment>\n" +
            "</VersionComment>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

    @Nullable
    private PreBundleSavePolicyCallback savePolicyCallback;

    public PolicyInstaller(@NotNull final PolicyBundleInstallerContext context,
                           @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                           @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void setSavePolicyCallback(@Nullable PreBundleSavePolicyCallback savePolicyCallback) {
        this.savePolicyCallback = savePolicyCallback;
    }

    public Map<String, String> dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent,
                                             @NotNull final Map<String, String> conflictingPolicyIdsNames) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, AccessDeniedManagementResponse {
        checkInterrupted();
        final Map<String, String> policyIdsNames = new HashMap<>();
        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document policyEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), POLICY, true);
        if (policyEnumDoc != null) {
            final List<Element> policyNamesElms = findAllNamesFromEnumeration(policyEnumDoc);
            logger.finest("Dry run checking " + policyNamesElms.size() + " policies.");
            for (Element policyNamesElm : policyNamesElms) {
                checkInterrupted();
                final String policyName = getPrefixedPolicyName(DomUtils.getTextValue(policyNamesElm));
                String policyId = ((Element)policyNamesElm.getParentNode()).getAttribute("id");
                policyIdsNames.put(policyId, policyName);
                try {
                    if (hasMatchingPolicy(policyName)) {
                        dryRunEvent.addPolicyNameWithConflict(policyName);
                        conflictingPolicyIdsNames.put(policyId, policyName);
                    }
                } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                    throw new InvalidBundleException("Could not check for conflict for policy name  '" + policyName + "'", e);
                }
            }
        }

        return policyIdsNames;
    }

    public void install(@NotNull final Map<String, Goid> oldToNewFolderIds,
                        @NotNull final Map<String, String> oldToNewPolicyIds,
                        @NotNull final Map<String, String> oldToNewPolicyGuids) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {
        final Document policyBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), POLICY, true);
        install(policyBundle, oldToNewFolderIds, oldToNewPolicyIds, oldToNewPolicyGuids);
    }

    public void install(@NotNull final String subFolder,
                        @NotNull final Map<String, Goid> oldToNewFolderIds,
                        @NotNull final Map<String, String> oldToNewPolicyIds,
                        @NotNull final Map<String, String> oldToNewPolicyGuids) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {
        final Document policyBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), subFolder, POLICY, true);
        install(policyBundle, oldToNewFolderIds, oldToNewPolicyIds, oldToNewPolicyGuids);
    }

    private void install(@Nullable final Document policyBundle,
                        @NotNull final Map<String, Goid> oldToNewFolderIds,
                        @NotNull final Map<String, String> oldToNewPolicyIds,
                        @NotNull final Map<String, String> oldToNewPolicyGuids) throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {
        checkInterrupted();
        if (policyBundle == null) {
            logger.info("No policies to install for bundle " + context.getBundleInfo());
        } else {
            try {
                installPolicies(oldToNewFolderIds, oldToNewPolicyIds, oldToNewPolicyGuids, policyBundle);
            } catch (PreBundleSavePolicyCallback.PolicyUpdateException e) {
                throw new InstallationException(e);
            }
        }
    }

    protected void updatePolicyDoc(final Element entityDetailElmReadOnly,
                                   final String entityType,
                                   final Map<String, String> oldToNewGuids,
                                   final String identifier,
                                   final Element policyResourceElmWritable,
                                   final Document policyDocumentFromResource,
                                   @NotNull final List<Element> policyIncludesFromPolicyDocument,
                                   @NotNull final PolicyBundleInstallerContext context)
            throws InvalidBundleException, PreBundleSavePolicyCallback.PolicyUpdateException {
        updatePolicyIncludes(oldToNewGuids, identifier, entityType, policyIncludesFromPolicyDocument);

        BundleMapping bundleMapping = context.getBundleMapping();
        if (bundleMapping != null) {
            final Map<String, String> mappedJdbcReferences = bundleMapping.getJdbcMappings();
            JdbcConnectionInstaller.setJdbcReferencesInPolicy(policyDocumentFromResource, mappedJdbcReferences);
        }

        if (savePolicyCallback != null) {
            savePolicyCallback.prePublishCallback(context.getBundleInfo(), entityDetailElmReadOnly, policyDocumentFromResource);
        }

        //write changes back to the resource document
        DomUtils.setTextContent(policyResourceElmWritable, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
    }

    @NotNull
    private List<Element> findAllNamesFromEnumeration(@NotNull final Document policyEnumeration) {
        return XpathUtil.findElements(policyEnumeration.getDocumentElement(), ".//l7:Name", getNamespaceMap());
    }

    private boolean hasMatchingPolicy(String policyName) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {
        logger.finest("Finding policy name '" + policyName + "'.");
        final String policyGetSelector = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(policyGetSelector);
        return !hasFaultSubCodeInvalidSelectors(documentPair.right);
    }

    /**
     * Note: Policies are unique on name across a gateway
     *
     * @param oldToNewFolderIds     gateway mgmt policy elements will reference folder ids. Before publishing a policy
     *                              we need the actual folder id that the policy will be published into. This is a map of
     *                              the folder id contained in the enumeration document for a policy mapped to the actual
     *                              folder id on the target system which represents the same logical folder.
     * @param oldToNewPolicyIds     old to new policy IDs
     * @param oldToNewPolicyGuids   map of the policy's guid from the gateway mgmt enumeration document to it's actual
     *                              guid once published. This avoids attempting to publish the same policy more than once.
     * @param policyMgmtEnumeration the gateway mgmt enumeration containing all policy elements too publish.
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     *
     * @throws com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback.PolicyUpdateException
     *
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     *
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void installPolicies(@NotNull final Map<String, Goid> oldToNewFolderIds,
                                 @NotNull final Map<String, String> oldToNewPolicyIds,
                                 @NotNull final Map<String, String> oldToNewPolicyGuids,
                                 @NotNull final Document policyMgmtEnumeration)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            InvalidBundleException,
            InterruptedException,
            AccessDeniedManagementResponse {

        final List<Element> enumPolicyElms = GatewayManagementDocumentUtilities.getEntityElements(policyMgmtEnumeration.getDocumentElement(), "Policy");
        int policyElmsSize = enumPolicyElms.size();
        logger.finest("Installing " + policyElmsSize + " policies.");

        final Map<String, String> guidToName = new HashMap<>(policyElmsSize);
        final Map<String, Element> allPolicyElms = new HashMap<>(policyElmsSize);
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
            getOrCreatePolicy(policyElm, oldToNewPolicyGuids, oldToNewFolderIds, oldToNewPolicyIds, allPolicyElms, guidToName);
        }
    }

    /**
     *
     * @param enumPolicyElmReadOnly read only access to the Policy Gateay Mgmt element
     * @param oldToNewPolicyGuids    map of the policy's guid from the gateway mgmt enumeration document to it's actual
     *                              guid once published. This avoids attempting to publish the same policy more than once.
     * @param oldToNewFolderIds old to new folder IDs
     * @param oldToNewPolicyIds old to new policy IDs
     * @param allPolicyElms oldToNewFolderIds
     * @param guidToName GUID to Name
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     * @throws com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback.PolicyUpdateException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void getOrCreatePolicy(@NotNull final Element enumPolicyElmReadOnly,
                                   @NotNull final Map<String, String> oldToNewPolicyGuids,
                                   @NotNull final Map<String, Goid> oldToNewFolderIds,
                                   @NotNull final Map<String, String> oldToNewPolicyIds,
                                   @NotNull final Map<String, Element> allPolicyElms,
                                   @NotNull final Map<String, String> guidToName)
            throws InvalidBundleException,
            PreBundleSavePolicyCallback.PolicyUpdateException,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException,
            AccessDeniedManagementResponse {

        final String policyId = enumPolicyElmReadOnly.getAttribute("id");
        final String policyGuid = enumPolicyElmReadOnly.getAttribute("guid");

        if (oldToNewPolicyGuids.containsKey(policyGuid)) {
            // already created
            logger.finest("Policy with GUID '" + policyGuid + "' already created.");
            return;
        }

        final Element enumPolicyElmWritable;
        try {
            enumPolicyElmWritable = XmlUtil.parse(XmlUtil.nodeToString(enumPolicyElmReadOnly)).getDocumentElement();
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unexpected exception getting a writable policy element", e);
        }

        final Element policyResourceElmWritable = getPolicyResourceElement(enumPolicyElmWritable, "Policy", policyGuid);
        if (policyResourceElmWritable == null) {
            throw new InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource");
        }

        // we want elements that we can modify
        final Document policyDocWriteEl = getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", policyGuid);
        // these do not belong to the policyResourceElmWritable document
        final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(policyDocWriteEl);
        for (Element policyIncludeElm : policyIncludes) {
            final String policyInclude = policyIncludeElm.getAttribute("stringValue");
            if (oldToNewPolicyGuids.containsKey(policyInclude)) {
                // already created
                logger.finest("Policy with GUID '" + policyGuid + "' already created.");
            } else if (!allPolicyElms.containsKey(policyInclude)) {
                throw new InvalidBundleException("Policy with guid " + policyInclude + " was not included in bundle "
                        + context.getBundleInfo().getName() + "#{" + context.getBundleInfo().getId() + "}");
            } else {
                getOrCreatePolicy(allPolicyElms.get(policyInclude), oldToNewPolicyGuids, oldToNewFolderIds, oldToNewPolicyIds, allPolicyElms, guidToName);
            }
        }

        checkInterrupted();

        // update any encapsulated assertions in this policy
        EncapsulatedAssertionInstaller.updatePolicyDoc(policyResourceElmWritable, policyDocWriteEl, context.getInstallationPrefix());

        final Element policyDetailElmReadOnly = getPolicyDetailElement(enumPolicyElmReadOnly);
        // get or create
        // Create a new document and modify it
        updatePolicyDoc(policyDetailElmReadOnly, "Policy", oldToNewPolicyGuids, policyGuid, policyResourceElmWritable, policyDocWriteEl, policyIncludes, context);

        final Element policyDetailWritable = getPolicyDetailElement(enumPolicyElmWritable);
        final String folderId = policyDetailWritable.getAttribute("folderId");
        final Goid newFolderId = oldToNewFolderIds.get(folderId);
        if (newFolderId == null) {
            throw new InvalidBundleException("Policy with GUID: " + policyGuid + " is contained within unknown folder id '" + folderId + "' in the bundle");
        }

        policyDetailWritable.setAttribute("folderId", String.valueOf(newFolderId));

        final String policyNameToUse;
        // Add prefix if needed
        if (isValidVersionModifier(context.getInstallationPrefix())) {
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
        logger.finest("Creating policy '" + policyNameToUse + "' .");
        final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createPolicyXml);

        final Goid createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
        String idToUse = null;
        String guidToUse = null;
        if (createdId == null) {
            if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                guidToUse = getExistingPolicyGuid(policyNameToUse);
            }
        } else {
            // we just created it
            setRevisionComment(createdId, false);
            idToUse = getExistingPolicyId(policyNameToUse);
            guidToUse = getExistingPolicyGuid(policyNameToUse);
        }

        if (guidToUse == null) {
            throw new RuntimeException("Could not create or get the GUID for policy from bundle with guid: #{" + policyGuid + "}");
        }

        oldToNewPolicyIds.put(policyId, idToUse);
        oldToNewPolicyGuids.put(policyGuid, guidToUse);
    }

    @Nullable
    private String getExistingPolicyGuid(String policyName) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {

        final String getPolicyXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        logger.finest("Getting GUID for policy name '" + policyName + "'.");
        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(getPolicyXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(
                    new XpathExpression("string(/env:Envelope/env:Body/l7:Policy/@guid)", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        } catch (XPathExpressionException | InvalidXpathException e) {
            throw new RuntimeException("Unexpected exception performing xpath to obtain policy guid for policy name '" + policyName + "'", e);
        }
        return xpathResult.getString();
    }

    protected void setRevisionComment(@NotNull final Goid goid, boolean isServiceGoid)
            throws InterruptedException,
            AccessDeniedManagementResponse,
            GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        String comment = getPolicyRevisionComment(context.getBundleInfo());
        String revisionType = isServiceGoid ? "services" : "policies";
        logger.finest("Setting revision comment for " + revisionType + " GOID '" +  goid + "'.");
        callManagementCheckInterrupted(MessageFormat.format(GATEWAY_MGMT_SET_REVISION_COMMENT_REQUEST, revisionType, getUuid(), goid, comment));
    }

    @Nullable
    private String getExistingPolicyId(String policyName) throws InterruptedException, UnexpectedManagementResponse, AccessDeniedManagementResponse {

        final String getPolicyXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), POLICIES_MGMT_NS, "name", policyName);

        logger.finest("Getting GOID for policy name '" + policyName + "'.");
        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(getPolicyXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(
                    new XpathExpression("string(/env:Envelope/env:Body/l7:Policy/@id)", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        } catch (XPathExpressionException | InvalidXpathException e) {
            throw new RuntimeException("Unexpected exception performing xpath to obtain policy id for policy name '" + policyName + "'", e);
        }
        return xpathResult.getString();
    }
}
