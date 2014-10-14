package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.ASSERTION;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;

/**
 * Check that Assertion(s) exist on the Gateway.
 */
public class AssertionInstaller extends WsmanInstaller {
    public AssertionInstaller(@NotNull final PolicyBundleInstallerContext context,
                              @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                              @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        // Check assertion existence if it is required
        if (context.isCheckingAssertionExistenceRequired()) {
            final BundleInfo bundleInfo = context.getBundleInfo();
            final Document enumerationDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), ASSERTION, true);
            if (enumerationDoc != null) {
                final List<Element> assertionElms = GatewayManagementDocumentUtilities.getEntityElements(enumerationDoc.getDocumentElement(), "Assertion");
                logger.finest("Dry run checking " + assertionElms.size() + " assertion(s).");
                for (Element assertionElm: assertionElms) {
                    final Element assertionNameElm = XmlUtil.findFirstDescendantElement(assertionElm, MGMT_VERSION_NAMESPACE, "Name");
                    final Element policyElm = XmlUtil.findFirstDescendantElement(assertionElm, GatewayManagementDocumentUtilities.getNamespaceMap().get("wsp"), "Policy");
                    final boolean isCustomAssertion = XmlUtil.findFirstDescendantElement(assertionElm, GatewayManagementDocumentUtilities.getNamespaceMap().get("L7p"), "CustomAssertion") != null;

                    boolean assertionNotFound = false;
                    try {
                        Assertion assertions = WspReader.getDefault().parseStrictly(XmlUtil.nodeToString(policyElm), WspReader.OMIT_DISABLED);
                        // Scan for UnknownAssertion
                        Iterator it = assertions.preorderIterator();
                        while (it.hasNext()) {
                            final Object assertion = it.next();
                            if (assertion instanceof UnknownAssertion) {
                                // Custom Assertion class not found because they reside in a different Subversion project
                                assertionNotFound = true;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // Assertion is not installed
                        assertionNotFound = true;
                    }

                    if (assertionNotFound) {
                        if (assertionNameElm == null) {
                            throw new IllegalArgumentException("Assertion xml does not contain a Name element.");
                        }

                        dryRunEvent.addMissingAssertion(XmlUtil.getTextValue(assertionNameElm) + (isCustomAssertion ? " (Custom Assertion)" : ""));
                    }
                }
            }

        }
    }
}
