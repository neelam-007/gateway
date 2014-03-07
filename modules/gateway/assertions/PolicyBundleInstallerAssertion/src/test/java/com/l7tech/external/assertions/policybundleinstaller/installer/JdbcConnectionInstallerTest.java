package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.*;
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
import java.util.Set;

import static com.l7tech.external.assertions.policybundleinstaller.installer.PolicyInstallerTest.CANNED_SET_VERSION_COMMENT_RESPONSE;
import static com.l7tech.external.assertions.policybundleinstaller.installer.ServiceInstallerTest.SERVICES_SET_VERSION_COMMENT_ACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcConnectionInstallerTest extends PolicyBundleInstallerTestBase {

    /**
     * IF a policy contains a JDBC connection and the client wants to update that reference, then the policy should
     * be updated before the call to the management service to install it happens
     * @throws Exception
     */
    @Test
    public void testJdbcReferencesUpdatedCorrectly() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME);

        final boolean[] invoked = new boolean[1];
        final Map<String, Boolean> servicesFound = new HashMap<>();
        final Map<String, Set<String>> jdbcPerService = new HashMap<>();

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME),
                null, null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                invoked[0] = true;
                final String requestXml;
                final Document requestDoc;
                try {
                    requestDoc = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    requestXml = XmlUtil.nodeToString(requestDoc);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }

                if (requestXml.contains("Enumerate")) {
                    // reply with a does not exist
                    // just reply with any non created response
                    setResponse(context, alreadyExistsResponse);
                    return AssertionStatus.NONE;
                }  else if (requestXml.contains(SERVICES_SET_VERSION_COMMENT_ACTION)) {
                    setResponse(context, CANNED_SET_VERSION_COMMENT_RESPONSE);
                    return AssertionStatus.NONE;
                } else {
                    // Validate any JDBC references
                    if (requestXml.contains("http://ns.l7tech.com/2010/04/gateway-management/services")) {
                        //it's a service
                        final ElementCursor cursor;
                        try {
                            cursor = new DomElementCursor(XmlUtil.parse(requestXml));
                        } catch (SAXException e) {
                            throw new RuntimeException(e);
                        }
                        // search from the current element only
                        final XpathResult xpathResult;
                        try {
                            xpathResult = cursor.getXpathResult(
                                    new XpathExpression(".//l7:Service", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                        } catch (XPathExpressionException | InvalidXpathException e) {
                            throw new RuntimeException("Unexpected issue with internal xpath expression: " + e.getMessage(), e);
                        }

                        final Element serviceElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                        final String id = serviceElement.getAttribute("id");
                        servicesFound.put(id, true);
                        // System.out.println("Testing service: " + id);
                        try {
                            final Element policyResourceElement = GatewayManagementDocumentUtilities.getPolicyResourceElement(serviceElement, "Service not important", id);
                            // System.out.println(XmlUtil.nodeToFormattedString(policyResourceElement));
                            final Set<String> jdbcConnsFound = BundleUtils.searchForJdbcReferences(policyResourceElement, "Service", id);
                            jdbcPerService.put(id, jdbcConnsFound);

                        } catch (BundleResolver.InvalidBundleException e) {
                            throw new RuntimeException(e);
                        }

                    } else if (requestXml.contains(" to do policy")) {

                    }

                    // pretend it was created
                    // validate the requestXml contains updated JDBC Connections
                    final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(requestDoc);
                    setResponse(context, documentPair.right);
                    return documentPair.left;
                }
            }
        }, context, getCancelledCallback(installEvent));

        bundleInstaller.installBundle();

//        assertEquals(1, jdbcConnsFound.size());
//        assertEquals("Invalid JDBC connection name found", "OAuth", jdbcConnsFound.iterator().next());

        assertTrue("Call to management service was not invoked", invoked[0]);

        assertTrue("Required service not found", servicesFound.get("123797510"));
        assertTrue("Required service not found", servicesFound.get("123797504"));
        assertTrue("Required service not found", servicesFound.get("123797505"));
        assertTrue("Required service not found", servicesFound.get("123797508"));
        assertTrue("Required service not found", servicesFound.get("123797509"));
        assertTrue("Required service not found", servicesFound.get("123797506"));
        assertTrue("Required service not found", servicesFound.get("123797507"));

        for (Map.Entry<String, Set<String>> entry : jdbcPerService.entrySet()) {
            final Set<String> values = entry.getValue();
            if (entry.getKey().equals("123797509")) {
                assertEquals(1, values.size());
                assertEquals("OAuth", values.iterator().next());
            } else {
                assertTrue(values.isEmpty());
            }
        }

    }

}
