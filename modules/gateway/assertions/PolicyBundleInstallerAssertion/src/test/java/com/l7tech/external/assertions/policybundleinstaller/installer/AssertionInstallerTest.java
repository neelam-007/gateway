package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AssertionInstallerTest extends PolicyBundleInstallerTestBase {
    @Test
    public void testCheckingAssertionExistenceRequired () throws Exception {

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME),
                null, null, getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME), false);

        final DryRunInstallPolicyBundleEvent dryRunEvent = new DryRunInstallPolicyBundleEvent(this, context);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                // For policies and services, return that they already exist. For JDBC conns return that they do not exist.
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);

                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        if (requestXml.contains(JdbcConnectionInstaller.JDBC_MGMT_NS)) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else if (requestXml.contains("/l7:Service/l7:ServiceDetail[@id='"+new Goid(0,123345678)+"']/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern")) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, new Goid(0, 123345678))));
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, getCancelledCallback(dryRunEvent));

        installer.dryRunInstallBundle(dryRunEvent);
        final List<String> missingAssertions = dryRunEvent.getMissingAssertions();
        assertTrue((missingAssertions.isEmpty()));
    }
}
