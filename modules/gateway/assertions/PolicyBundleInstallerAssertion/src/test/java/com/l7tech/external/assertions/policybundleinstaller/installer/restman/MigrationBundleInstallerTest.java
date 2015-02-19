package com.l7tech.external.assertions.policybundleinstaller.installer.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.MigrationDryRunResult;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MigrationBundleInstallerTest extends PolicyBundleInstallerTestBase {
    @Mock
    private PolicyBundleInstallerContext context;
    @Mock
    private Functions.Nullary<Boolean> cancelledCallback;
    @Mock
    private GatewayManagementInvoker gatewayManagementInvoker;
    @Mock
    private DryRunInstallPolicyBundleEvent dryRunEvent;

    private RestmanMessage requestMessage1; // To hold a request message, where the bundle folder is not the same as the component folder.
    private RestmanMessage requestMessage2; // To hold a request message, where the bundle folder is the same as the component folder.

    private MigrationBundleInstaller migrationBundleInstaller;


    @Before
    public void setup() throws IOException, SAXException {
        migrationBundleInstaller = new MigrationBundleInstaller(context, cancelledCallback, gatewayManagementInvoker);

        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/external/assertions/policybundleinstaller/installer/restman/MigrationBundleCase1.xml"));
        String requestXml = new String(bytes, RestmanInvoker.UTF_8);
        requestMessage1 = new RestmanMessage(requestXml);

        bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/external/assertions/policybundleinstaller/installer/restman/MigrationBundleCase2.xml"));
        requestXml = new String(bytes, RestmanInvoker.UTF_8);
        requestMessage2 = new RestmanMessage(requestXml);

        // Return a new folder goid for a simulated and selected folder
        when(context.getFolderGoid()).then(new Returns(Goid.parseGoid("2d41aa636524442706fd09ad724f78fa")));
    }

    @Test
    public void testSetTargetIdInRootFolderMapping() throws IOException {
        // Given: requestMessage1 has no root folder mapping in request message xml.
        assertFalse("requestMessage1 has no root folder mapping", requestMessage1.hasRootFolderMapping());
        migrationBundleInstaller.setTargetIdInRootFolderMapping(requestMessage1);
        assertTrue("requestMessage1 now has a new root folder mapping", requestMessage1.hasRootFolderMapping());

        // Given: requestMessage2 has a root folder mapping without targetId attribute in request message xml.
        assertTrue("requestMessage2 has no root folder mapping", requestMessage2.hasRootFolderMapping());
        String requestXml = requestMessage2.getAsString();
        String targetId = "targetId=\"" + context.getFolderGoid() + "\"";
        assertFalse("No targetId attribute exists", requestXml.contains(targetId));

        // requestMessage2 should have targetId after setTargetIdInRootFolderMapping is called to create a new one.
        migrationBundleInstaller.setTargetIdInRootFolderMapping(requestMessage2);
        requestXml = requestMessage2.getAsString();
        assertTrue("A targetId attribute exists", requestXml.contains(targetId));
    }

    @Test
    public void testConvertToDryRunResult() throws SAXException, IOException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse {
        final Element mappingError = XmlUtil.stringToDocument(SAMPLE_SERVICE_MAPPING_ERROR).getDocumentElement();
        final MigrationDryRunResult migrationDryRunResult = migrationBundleInstaller.convertToDryRunResult(mappingError, requestMessage1);

        assertEquals("errorTypeStr matched", "TargetExists", migrationDryRunResult.getErrorTypeStr());
        assertEquals("entityTypeStr matched", "SERVICE", migrationDryRunResult.getEntityTypeStr());
        assertEquals("srcId matched", "7bf91daabff1558dd35b12b9f1f3ab7b", migrationDryRunResult.getSrcId());
        assertEquals("errorMessage matched", SAMPLE_ERROR_MESSAGE, migrationDryRunResult.getErrorMessage().trim());
        assertEquals("name matched", "t1", migrationDryRunResult.getName());
        assertEquals("policyResourceXml matched", SAMPLE_POLICY_XML, migrationDryRunResult.getPolicyResourceXml());
    }

    @Test
    public void testGetPolicyXmlForErrorMapping() throws IOException {
        String policyXml = migrationBundleInstaller.getPolicyXmlForErrorMapping("TargetNotFound", null, null, null);
        assertNull("Any error type except TargetExists will not be accepted", policyXml);

        policyXml = migrationBundleInstaller.getPolicyXmlForErrorMapping("TargetExists", EntityType.FOLDER, null, null);
        assertNull("Any entity type except SERVICE or POLICY will not be accepted", policyXml);

        policyXml = migrationBundleInstaller.getPolicyXmlForErrorMapping("TargetExists", EntityType.SERVICE, "7bf91daabff1558dd35b12b9f1f3ab7b", requestMessage1);
        assertNotNull("The service is found and returned", policyXml);
        assertEquals("The returned policy xml is matched and correct", SAMPLE_POLICY_XML, policyXml);
    }

    private static final String SAMPLE_SERVICE_MAPPING_ERROR =
        "<l7:Mapping action=\"NewOrExisting\" errorType=\"TargetExists\" srcId=\"7bf91daabff1558dd35b12b9f1f3ab7b\" srcUri=\"/1.0/services/7bf91daabff1558dd35b12b9f1f3ab7b\" type=\"SERVICE\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
        "    <l7:Properties>\n" +
        "        <l7:Property key=\"ErrorMessage\">\n" +
        "            <l7:StringValue>Target entity exists but was not expected: Fail on existing specified and target exists.. Source Entity: EntityHeader. Name=t1, id=7bf91daabff1558dd35b12b9f1f3ab7b, description=t1, type = SERVICE</l7:StringValue>\n" +
        "        </l7:Property>\n" +
        "        <l7:Property key=\"FailOnExisting\">\n" +
        "            <l7:BooleanValue>true</l7:BooleanValue>\n" +
        "        </l7:Property>\n" +
        "    </l7:Properties>\n" +
        "</l7:Mapping>";

    private static final String SAMPLE_POLICY_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "                                <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
        "                                <wsp:All wsp:Usage=\"Required\"/>\n" +
        "                                </wsp:Policy>";

    private static final String SAMPLE_ERROR_MESSAGE = "TargetExists: type=SERVICE, name=t1, srcId=7bf91daabff1558dd35b12b9f1f3ab7b, Target entity exists but was not expected: Fail on existing specified and target exists.. Source Entity: EntityHeader. Name=t1, id=7bf91daabff1558dd35b12b9f1f3ab7b, description=t1, type = SERVICE";
}