package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.ENCAPSULATED_ASSERTION;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

/**
 * Install Encapsulated Assertion.
 */
public class EncapsulatedAssertionInstaller extends BaseInstaller {
    public static final String ENCAPSULATED_ASSERTIONS_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions";

    public EncapsulatedAssertionInstaller(@NotNull final PolicyBundleInstallerContext context,
                                          @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                          @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent,
                              @NotNull final Map<String, String> policyIdsNames,
                              @NotNull final Map<String, String> conflictingPolicyIdsNames)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document encapsulatedAssertionEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), ENCAPSULATED_ASSERTION, true);
        if (encapsulatedAssertionEnumDoc != null) {
            final List<Element> enumEncapsulatedAssertionElms = GatewayManagementDocumentUtilities.getEntityElements(encapsulatedAssertionEnumDoc.getDocumentElement(), "EncapsulatedAssertion");
            for (Element encapsulatedAssertionElm : enumEncapsulatedAssertionElms) {
                checkInterrupted();

                // check if Encapsulated Assertion already exists
                final String prefix = context.getInstallationPrefix();
                String encapsulatedAssertionName = GatewayManagementDocumentUtilities.getEntityName(encapsulatedAssertionElm);
                String encapsulatedAssertionGuid = GatewayManagementDocumentUtilities.getEntityGuid(encapsulatedAssertionElm);
                if (isPrefixValid(prefix)) {
                    encapsulatedAssertionName = getPrefixedName(prefix, encapsulatedAssertionName);
                    encapsulatedAssertionGuid = getPrefixedGuid(prefix, encapsulatedAssertionGuid);
                }

                try {
                    final List<Goid> matchingEncapsulatedAssertions = findMatchingEncapsulatedAssertion(encapsulatedAssertionName, encapsulatedAssertionGuid);
                    if (!matchingEncapsulatedAssertions.isEmpty()) {
                        dryRunEvent.addEncapsulatedAssertionNameWithConflict(encapsulatedAssertionName, encapsulatedAssertionGuid);
                    }
                } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                    throw new BundleResolver.InvalidBundleException("Could not check for conflict for Encapsulated Assertion name  '" + encapsulatedAssertionName + "'", e);
                }

                // check if the reference policy is in the list of policies that we are planning to install
                Element originalPolicyReferenceElm = DomUtils.findFirstChildElementByName(encapsulatedAssertionElm, MGMT_VERSION_NAMESPACE, "PolicyReference");
                String policyId = originalPolicyReferenceElm.getAttribute("id");
                if(!policyIdsNames.keySet().contains(policyId)) {
                    dryRunEvent.addEncapsulatedAssertionWithPolicyReferenceConflict(encapsulatedAssertionName + " missing reference to Policy ID " + policyId);
                }

                // check if the reference policy is in the list of policies that are in conflict
                String conflictPolicyName = conflictingPolicyIdsNames.get(originalPolicyReferenceElm.getAttribute("id"));
                if(conflictPolicyName != null) {
                    dryRunEvent.addEncapsulatedAssertionWithPolicyReferenceConflict(encapsulatedAssertionName + " references conflicting Policy " + conflictPolicyName);
                }
            }
        }
    }

    public void install(@NotNull Map<String, String> oldToNewPolicyId)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final Document encapsulatedAssertionBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), ENCAPSULATED_ASSERTION, true);
        if(encapsulatedAssertionBundle == null) {
            logger.info("No encapsulated assertions to install for bundle " + context.getBundleInfo());
        } else {
            installEncapsulatedAssertions(encapsulatedAssertionBundle, oldToNewPolicyId);
        }
    }

    protected static void updatePolicyDoc(@NotNull final Element policyResourceElmWritable,
                                   @NotNull final Document policyDocumentFromResource,
                                   @Nullable final String prefix) throws BundleResolver.InvalidBundleException, PreBundleSavePolicyCallback.PolicyUpdateException {
        // update encapsulated assertion name with prefix
        if (isPrefixValid(prefix)) {
            List<Element> encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigGuid", getNamespaceMap());
            for (Element encapsulatedAssertion : encapsulatedAssertions) {
                encapsulatedAssertion.setAttribute("stringValue", getPrefixedGuid(prefix, encapsulatedAssertion.getAttribute("stringValue")));
            }

            encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigName", getNamespaceMap());
            for (Element encapsulatedAssertion : encapsulatedAssertions) {
                encapsulatedAssertion.setAttribute("stringValue", getPrefixedName(prefix, encapsulatedAssertion.getAttribute("stringValue")));
            }

            // write changes back to the resource document
            DomUtils.setTextContent(policyResourceElmWritable, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
        }
    }

    @NotNull
    private List<Goid> findMatchingEncapsulatedAssertion(@NotNull String encapsulatedAssertionName, @NotNull final String encapsulatedAssertionGuid) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                ENCAPSULATED_ASSERTIONS_MGMT_NS, 10,
                "/l7:EncapsulatedAssertion/l7:Name[text()='" + encapsulatedAssertionName + "'] | " + "/l7:EncapsulatedAssertion/l7:Guid[text()='" + encapsulatedAssertionGuid + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    /**
     * Note: Encapsulated Assertions are unique on name across gateways
     *
     * @param encapsulatedAssertionEnum the gateway mgmt enumeration containing all encapsulated assertion elements too publish.
     *
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void installEncapsulatedAssertions(@NotNull final Document encapsulatedAssertionEnum, @NotNull Map<String, String> oldToNewPolicyId) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {

        // retrieve all encapsulated assertion elements from the enumeration
        final List<Element> enumEncapsulatedAssertionElms = GatewayManagementDocumentUtilities.getEntityElements(encapsulatedAssertionEnum.getDocumentElement(), "EncapsulatedAssertion");

        // For each Encapsulated Assertion:
        //      1) Replace original Policy ID from bundle with Policy ID generated by the Gateway on create.
        //      2) Add prefix (if applicable) to Encapsulated Assertion name.
        //      3) Call Gateway Management Client to create the Encapsulated Assertion entity on the Gateway.
        for(Element encapsulatedAssertionElm : enumEncapsulatedAssertionElms) {
            try {
                final Element enumEncapsulatedAssertionElmWritable = XmlUtil.parse(XmlUtil.nodeToString(encapsulatedAssertionElm)).getDocumentElement();
                Element originalPolicyReferenceEl = DomUtils.findFirstChildElementByName(enumEncapsulatedAssertionElmWritable, MGMT_VERSION_NAMESPACE, "PolicyReference");
                Element policyReferenceEl = (Element) originalPolicyReferenceEl.cloneNode(true);

                if(policyReferenceEl != null) {
                    // replace original policy id from bundle with id generated by Gateway
                    String id = policyReferenceEl.getAttribute("id");
                    checkInterrupted();
                    policyReferenceEl.setAttribute("id", String.valueOf(oldToNewPolicyId.get(id)));
                    originalPolicyReferenceEl.getParentNode().replaceChild(policyReferenceEl, originalPolicyReferenceEl);

                    final Element nameElementWritable = GatewayManagementDocumentUtilities.getEntityNameElement(enumEncapsulatedAssertionElmWritable);
                    String encapsulatedAssertionName = DomUtils.getTextValue(nameElementWritable);
                    final Element guidElementWritable = DomUtils.findFirstChildElementByName(enumEncapsulatedAssertionElmWritable, MGMT_VERSION_NAMESPACE, "Guid");
                    String encapsulatedAssertionGuid = DomUtils.getTextValue(guidElementWritable);

                    // add prefix if needed
                    String prefix = context.getInstallationPrefix();
                    if (isPrefixValid(prefix)) {
                        encapsulatedAssertionName = getPrefixedName(prefix, encapsulatedAssertionName);
                        DomUtils.setTextContent(nameElementWritable, encapsulatedAssertionName);

                        encapsulatedAssertionGuid = getPrefixedGuid(prefix, encapsulatedAssertionGuid);
                        DomUtils.setTextContent(guidElementWritable, encapsulatedAssertionGuid);
                    }

                    // create encapsulated assertion
                    final String encapsulatedAssertionXmlTemplate = XmlUtil.nodeToString(enumEncapsulatedAssertionElmWritable);
                    final String createPolicyXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), ENCAPSULATED_ASSERTIONS_MGMT_NS, encapsulatedAssertionXmlTemplate);
                    Pair<AssertionStatus, Document> statusDocument = callManagementCheckInterrupted(createPolicyXml);

                    // check for error
                    if (GatewayManagementDocumentUtilities.isConcurrencyErrorResponse(statusDocument.right)) {
                        throw new RuntimeException("Duplicate or concurrency error: Encapsulated Assertion name [" + encapsulatedAssertionName + "], GUID [" + encapsulatedAssertionGuid + "].  Bundle ID [" + id + "].");
                    }
                }
            } catch (SAXException | IOException e) {
                throw new RuntimeException("Unexpected exception getting a writable encapsulated assertion element", e);
            }
        }
    }

    private static String getPrefixedName(@Nullable String prefix, @NotNull String encapsulatedAssertionName) {
        return prefix + " " + encapsulatedAssertionName;
    }

    private static String getPrefixedGuid(@Nullable String prefix, @NotNull String guid) {
        return (prefix + guid).substring(0, guid.length());
    }
}
