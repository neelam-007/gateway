package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.ENCAPSULATED_ASSERTION;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.*;
import static com.l7tech.util.Charsets.UTF8;
import static java.util.logging.Level.WARNING;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Install Encapsulated Assertion.
 */
public class EncapsulatedAssertionInstaller extends WsmanInstaller {
    public static final String ENCAPSULATED_ASSERTIONS_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions";

    public EncapsulatedAssertionInstaller(@NotNull final PolicyBundleInstallerContext context,
                                          @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                          @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent,
                              @NotNull final Map<String, String> policyIdsNames,
                              @NotNull final Map<String, String> conflictingPolicyIdsNames)
            throws PolicyBundleInstaller.InstallationException, InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document encapsulatedAssertionEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), ENCAPSULATED_ASSERTION, true);
        if (encapsulatedAssertionEnumDoc != null) {
            final List<Element> enumEncapsulatedAssertionElms = GatewayManagementDocumentUtilities.getEntityElements(encapsulatedAssertionEnumDoc.getDocumentElement(), "EncapsulatedAssertion");
            logger.finest("Dry run checking " + enumEncapsulatedAssertionElms.size() + " encapsulated assertion(s).");
            for (Element encapsulatedAssertionElm : enumEncapsulatedAssertionElms) {
                checkInterrupted();

                // check if Encapsulated Assertion already exists
                final String prefix = context.getInstallationPrefix();
                String encapsulatedAssertionName = GatewayManagementDocumentUtilities.getEntityName(encapsulatedAssertionElm);
                String encapsulatedAssertionGuid = GatewayManagementDocumentUtilities.getEntityGuid(encapsulatedAssertionElm);

                if (isValidVersionModifier(prefix)) {
                    encapsulatedAssertionName = getPrefixedEncapsulatedAssertionName(prefix, encapsulatedAssertionName);
                    encapsulatedAssertionGuid = getVersionModifiedEncapsulatedAssertionGuid(prefix, encapsulatedAssertionGuid);
                }

                try {
                    logger.finest("Finding encapsulated assertion name '" + encapsulatedAssertionName + "'.");
                    List<Goid> matchingEncapsulatedAssertions = findMatchingEncapsulatedAssertion("/l7:EncapsulatedAssertion/l7:Name[text()='" + encapsulatedAssertionName + "']");
                    if (!matchingEncapsulatedAssertions.isEmpty()) {
                        dryRunEvent.addEncapsulatedAssertionConflict(encapsulatedAssertionName);
                    } else {
                        logger.finest("Finding encapsulated assertion GUID '" + encapsulatedAssertionGuid + "'.");
                        matchingEncapsulatedAssertions = findMatchingEncapsulatedAssertion("/l7:EncapsulatedAssertion/l7:Guid[text()='" + encapsulatedAssertionGuid + "']");
                        if (!matchingEncapsulatedAssertions.isEmpty()) {
                            String name = getExistingEncapsulatedAssertionName(matchingEncapsulatedAssertions.get(0));
                            dryRunEvent.addEncapsulatedAssertionConflict("GUID " + encapsulatedAssertionGuid + " already exists as " + name);
                        }
                    }
                } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                    throw new BundleResolver.InvalidBundleException("Could not check for conflict for Encapsulated Assertion name  '" + encapsulatedAssertionName + "'", e);
                }

                // check if the reference policy is in the list of policies that we are planning to install
                Element originalPolicyReferenceElm = DomUtils.findFirstChildElementByName(encapsulatedAssertionElm, MGMT_VERSION_NAMESPACE, "PolicyReference");
                String policyId = originalPolicyReferenceElm.getAttribute("id");
                if(!policyIdsNames.keySet().contains(policyId)) {
                    dryRunEvent.addEncapsulatedAssertionConflict(encapsulatedAssertionName + " missing reference to Policy ID " + policyId);
                }

                // check if the reference policy is in the list of policies that are in conflict
                String conflictPolicyName = conflictingPolicyIdsNames.get(originalPolicyReferenceElm.getAttribute("id"));
                if(conflictPolicyName != null) {
                    dryRunEvent.addEncapsulatedAssertionConflict(encapsulatedAssertionName + " references conflicting Policy " + conflictPolicyName);
                }
            }
        }
    }

    public void install(@NotNull Map<String, String> oldToNewPolicyId)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final Document encapsulatedAssertionBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), ENCAPSULATED_ASSERTION, true);
        install(encapsulatedAssertionBundle, oldToNewPolicyId);
    }

    public void install(@NotNull final String subFolder, @NotNull Map<String, String> oldToNewPolicyId)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final Document encapsulatedAssertionBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), subFolder, ENCAPSULATED_ASSERTION, true);
        if (encapsulatedAssertionBundle == null) {
            logger.info("No encapsulated assertion item found in folder: " + subFolder);
        }
        install(encapsulatedAssertionBundle, oldToNewPolicyId);
    }

    public void install(@Nullable final Document encapsulatedAssertionBundle, @NotNull Map<String, String> oldToNewPolicyId)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        if(encapsulatedAssertionBundle == null) {
            logger.info("No encapsulated assertions to install for bundle " + context.getBundleInfo());
        } else {
            installEncapsulatedAssertions(encapsulatedAssertionBundle, oldToNewPolicyId);
        }
    }

    protected static void updatePolicyDoc(@NotNull final Element policyResourceElmWritable,
                                   @NotNull final Document policyDocumentFromResource,
                                   @Nullable final String prefix)
            throws BundleResolver.InvalidBundleException, PolicyBundleInstallerCallback.CallbackException, PolicyBundleInstaller.InstallationException {

        // update encapsulated assertion name with prefix
        if (isValidVersionModifier(prefix)) {
            List<Element> encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigGuid", getNamespaceMap());
            for (Element encapsulatedAssertion : encapsulatedAssertions) {
                encapsulatedAssertion.setAttribute("stringValue", getVersionModifiedEncapsulatedAssertionGuid(prefix, encapsulatedAssertion.getAttribute("stringValue")));
            }

            encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigName", getNamespaceMap());
            for (Element encapsulatedAssertion : encapsulatedAssertions) {
                encapsulatedAssertion.setAttribute("stringValue", getPrefixedEncapsulatedAssertionName(prefix, encapsulatedAssertion.getAttribute("stringValue")));
            }

            // write changes back to the resource document
            DomUtils.setTextContent(policyResourceElmWritable, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
        }
    }

    @NotNull
    private List<Goid> findMatchingEncapsulatedAssertion(@NotNull String filterString) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(), ENCAPSULATED_ASSERTIONS_MGMT_NS, 10, filterString);
        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    @Nullable
    private String getExistingEncapsulatedAssertionName(@NotNull final Goid goid) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final String getEncapsulatedAssertionXml = MessageFormat.format(GATEWAY_MGMT_GET_ENTITY, getUuid(), ENCAPSULATED_ASSERTIONS_MGMT_NS, "id", goid);

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(getEncapsulatedAssertionXml);
        final ElementCursor cursor = new DomElementCursor(documentPair.right);

        final XpathResult xpathResult;
        try {
            xpathResult = cursor.getXpathResult(new XpathExpression("string(/env:Envelope/env:Body/l7:EncapsulatedAssertion/l7:Name)", getNamespaceMap()).compile());
        } catch (XPathExpressionException | InvalidXpathException e) {
            throw new RuntimeException("Unexpected exception performing xpath to obtain Encapsulated Assertion name for goid '" + goid + "' ", e);
        }

        return xpathResult.getString();
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
    private void installEncapsulatedAssertions(@NotNull final Document encapsulatedAssertionEnum, @NotNull Map<String, String> oldToNewPolicyId)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse, PolicyBundleInstaller.InstallationException {

        // retrieve all encapsulated assertion elements from the enumeration
        final List<Element> enumEncapsulatedAssertionElms = GatewayManagementDocumentUtilities.getEntityElements(encapsulatedAssertionEnum.getDocumentElement(), "EncapsulatedAssertion");
        logger.finest("Installing " + enumEncapsulatedAssertionElms.size() + " encapsulated assertion(s).");

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
                    if (isValidVersionModifier(prefix)) {
                        encapsulatedAssertionName = getPrefixedEncapsulatedAssertionName(prefix, encapsulatedAssertionName);
                        DomUtils.setTextContent(nameElementWritable, encapsulatedAssertionName);

                        encapsulatedAssertionGuid = getVersionModifiedEncapsulatedAssertionGuid(prefix, encapsulatedAssertionGuid);
                        DomUtils.setTextContent(guidElementWritable, encapsulatedAssertionGuid);
                    }

                    // create encapsulated assertion
                    final String encapsulatedAssertionXmlTemplate = XmlUtil.nodeToString(enumEncapsulatedAssertionElmWritable);
                    final String createPolicyXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), ENCAPSULATED_ASSERTIONS_MGMT_NS, encapsulatedAssertionXmlTemplate);
                    logger.finest("Creating encapsulated assertion '" + encapsulatedAssertionName + "'.");
                    Pair<AssertionStatus, Document> statusDocument = callManagementCheckInterrupted(createPolicyXml);

                    // check for error
                    if (isConcurrencyErrorResponse(statusDocument.right)) {
                        logger.log(WARNING, "Duplicate or concurrency error: Encapsulated Assertion name [" + encapsulatedAssertionName +
                                "], GUID [" + encapsulatedAssertionGuid + "].  Bundle ID [" + id + "]. Will continue with any remaining installation.");
                    }
                }
            } catch (SAXException | IOException e) {
                throw new RuntimeException("Unexpected exception getting a writable encapsulated assertion element", e);
            }
        }
    }

    /**
     * Deterministically version modify the GUID by getting the first 128 bits (16 bytes) of SHA-256( version modifier + original_guid ).
     */
    public static String getVersionModifiedEncapsulatedAssertionGuid(@Nullable final String versionModifier, @NotNull final String guid) throws PolicyBundleInstaller.InstallationException {
        if (isEmpty(versionModifier)) {
            logger.info("Call to version modify Encapsulated Assertion GUID contains empty version modifier.");
            return guid;
        } else {
            try {
                final MessageDigest md = MessageDigest.getInstance("SHA-256");
                return HexUtils.hexDump(md.digest((versionModifier + guid).getBytes(UTF8)), 0, 16);
            }  catch (NoSuchAlgorithmException e) {
                throw new PolicyBundleInstaller.InstallationException("Could not version modify Encapsulated Assertion GUID: " + guid +
                        ", version modifier: " + versionModifier, e);
            }
        }
    }
}
