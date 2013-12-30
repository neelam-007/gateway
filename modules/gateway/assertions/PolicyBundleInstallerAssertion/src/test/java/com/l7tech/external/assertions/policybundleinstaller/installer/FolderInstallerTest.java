package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class FolderInstallerTest extends PolicyBundleInstallerTestBase {

    /**
     * Test the success case when no folders already exist.
     */
    @Test
    public void testInstallFolders_NoneExist() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        final BundleInfo bundleInfo = resultList.get(0);

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);
        final Functions.Nullary<Boolean> cancelledCallback = getCancelledCallback(installEvent);
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
        }, context, cancelledCallback);

        final Map<String, Goid> oldToNewMap = bundleInstaller.getFolderInstaller().install();
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<String, Goid> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Test the success case when all folders already exist.
     *
     * @throws Exception
     */
    @Test
    public void testInstallFolders_AllExist() throws Exception {
        final Map<String, Integer> nameToIdMap = new HashMap<>();

        final BundleResolver bundleResolver = getBundleResolver();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        final BundleInfo bundleInfo = resultList.get(0);    // OAuth_1_0

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final String requestXml = XmlUtil.nodeToString(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")) {
                        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.parse(requestXml)));
                        setResponse(context, alreadyExistsResponse);
                        return AssertionStatus.NONE;
                    } else {
                        // it's an enumerate request
                        final int beginIndex = requestXml.indexOf("folderId='") + 10;
                        final int endIndex = requestXml.indexOf("'", beginIndex + 1);
                        final String folderId = requestXml.substring(beginIndex, endIndex);

                        final int i = requestXml.indexOf("text()='") + 8;
                        final int i1 = requestXml.indexOf("'", i + 1);
                        final String name = requestXml.substring(i, i1);

                        int idToUse;
                        if (nameToIdMap.containsKey(name)) {
                            idToUse = nameToIdMap.get(name);
                        } else {
                            idToUse = 1000 + nextOid++;
                            nameToIdMap.put(name, idToUse);
                        }

                        final Goid folderIdGoid = Goid.parseGoid(folderId);
                        System.out.println("Requesting lookup for folder: " + folderIdGoid + ": " + requestXml);
                        final String response = MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, String.valueOf(new Goid(0,idToUse)));
                        setResponse(context, response);
                        return AssertionStatus.NONE;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        final Map<String, Goid> oldToNewMap = bundleInstaller.getFolderInstaller().install();
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<String, Goid> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
