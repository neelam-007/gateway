package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.policybundleinstaller.installer.PolicyInstallerTest.CANNED_SET_VERSION_COMMENT_RESPONSE;
import static org.junit.Assert.assertEquals;

public class ServiceInstallerTest extends PolicyBundleInstallerTestBase {
    protected static final String SIMPLE_TEST_BUNDLE_SERVICE_ID = "57f541927144f77cd71f562ef2d31656";

    protected static final String SERVICES_SET_VERSION_COMMENT_ACTION = "http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment";

    @Test
    public void testInstallServices() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME);
        //OAuth_1_0
        final BundleInfo bundleInfo = getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    setResponse(context, documentPair.right);
                    return documentPair.left;

                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        // OAuth_1_0
        bundleInstaller.getServiceInstaller().install(getFolderIds(), getPolicyGuids(), bundleInstaller.getPolicyInstaller());
    }

    @Test
    public void testServicesUriPrefixedInstallation() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME);
        final Map<String, String> serviceIdToUri = new HashMap<>();
        //OAuth_1_0
        final BundleInfo bundleInfo = getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME);
        final String prefix = "version1a";
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), prefix, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToString(documentReadOnly);
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        final Document response = XmlUtil.parse(FILTER_NO_RESULTS);
                        setResponse(context, response);
                        return AssertionStatus.NONE;
                    } else if(requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")){

                        ElementCursor cursor = new DomElementCursor(documentReadOnly);
                        // validate the prefix supplied
                        try {
                            XpathResult xpathResult = cursor.getXpathResult(
                                    new XpathExpression(".//l7:UrlPattern", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            final Element urlElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String url = DomUtils.getTextValue(urlElement);

                            // Get service id
                            cursor.moveToDocumentElement();
                            xpathResult = cursor.getXpathResult(new XpathExpression(".//l7:Service", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            final Element serviceElm = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String serviceId = serviceElm.getAttribute("id");
                            if (serviceId.trim().isEmpty()) {
                                throw new RuntimeException("Service id not found");
                            }

                            serviceIdToUri.put(serviceId, url);
                            // System.out.println("Found url: " + url);
                        } catch (XPathExpressionException | InvalidXpathException e) {
                            throw new RuntimeException(e);
                        }

                        final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(documentReadOnly);
                        setResponse(context, documentPair.right);
                        return documentPair.left;
                    } else if (requestXml.contains(SERVICES_SET_VERSION_COMMENT_ACTION)) {
                        setResponse(context, CANNED_SET_VERSION_COMMENT_RESPONSE);
                        return AssertionStatus.NONE;
                    }

                    throw new RuntimeException("Unexpected request");
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        bundleInstaller.getServiceInstaller().install(getFolderIds(), getPolicyGuids(), bundleInstaller.getPolicyInstaller());

        // validate all services were found and all URIs were prefixed correctly
        assertEquals("Incorrect number of services created", 7, serviceIdToUri.size());

        // hardcoded test resources - validate each service found and correct URI was published
        assertEquals("/" + prefix + "/auth/oauth/v1/token", serviceIdToUri.get("123797510"));
        assertEquals("/" + prefix + "/auth/oauth/v1/*", serviceIdToUri.get("123797505"));
        assertEquals("/" + prefix + "/protected/resource", serviceIdToUri.get("123797504"));
        assertEquals("/" + prefix + "/auth/oauth/v1/request", serviceIdToUri.get("123797507"));
        assertEquals("/" + prefix + "/auth/oauth/v1/authorize", serviceIdToUri.get("123797506"));
        assertEquals("/" + prefix + "/oauth/v1/client", serviceIdToUri.get("123797509"));
        assertEquals("/" + prefix + "/auth/oauth/v1/authorize/website", serviceIdToUri.get("123797508"));
    }
}
