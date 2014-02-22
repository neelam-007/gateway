package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.policy.PolicyVersionManager;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This was created: 10/23/13 as 4:47 PM
 *
 * @author Victor Kazakov
 */
public class PolicyVersionRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PolicyVersionRestServerGatewayManagementAssertionTest.class.getName());

    private static final Policy policy1 = new Policy(PolicyType.INCLUDE_FRAGMENT, "Policy1", "", false);
    private static PolicyManagerStub policyManager;
    private static final String comment = "MyComment 1";
    private static final String policyBasePath = "policies/";
    private static final String policyVersionsPath = "versions/";
    private Folder rootFolder;
    private PolicyVersionManager policyVersionManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();
        policyVersionManager = applicationContext.getBean("policyVersionManager", PolicyVersionManager.class);

        FolderManagerStub folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        rootFolder = new Folder("ROOT FOLDER", null);
        folderManager.save(rootFolder);

        policy1.setFolder(rootFolder);
        policy1.setXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n");
        policy1.setGuid(UUID.randomUUID().toString());
        policyManager = applicationContext.getBean("policyManager", PolicyManagerStub.class);
        policyManager.save(policy1);

        policyVersionManager.checkpointPolicy(policy1,true,comment,true);
    }

    @After
    public void after() throws Exception {
        super.after();
        ArrayList<PolicyHeader> policies = new ArrayList<>(policyManager.findAllHeaders());
        for(EntityHeader policy : policies){
            policyManager.delete(policy.getGoid());
        }

        ArrayList<EntityHeader> policyVersions = new ArrayList<>(policyVersionManager.findAllHeaders());
        for(EntityHeader policyVersion : policyVersions){
            policyVersionManager.delete(policyVersion.getGoid());
        }
    }

    @Test
    public void getPolicyVersionTest() throws Exception {
        RestResponse response = processRequest(policyBasePath + policy1.getId() + "/" + policyVersionsPath + "1", HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        PolicyVersionMO policyVersionReturned = (PolicyVersionMO) item.getContent();

        Assert.assertEquals(comment, policyVersionReturned.getComment());
        Assert.assertEquals(policy1.getXml(), policyVersionReturned.getXml());
    }

    @Test
    public void updatePolicyVersionCommentTest() throws Exception {
        final String newPolicyComment = "Policy Version Comment Updated";
        RestResponse response = processRequest(policyBasePath + policy1.getId() + "/" + policyVersionsPath + "1/comment", HttpMethod.PUT, null, newPolicyComment);
        logger.info(response.toString());

        PolicyVersion policyVersionSaved = policyVersionManager.findPolicyVersionForPolicy(policy1.getGoid(), 1);
        Assert.assertEquals(newPolicyComment, policyVersionSaved.getName());
    }

    @Test
    public void listPolicyVersions() throws Exception {
        RestResponse response = processRequest(policyBasePath + policy1.getId() + "/" + policyVersionsPath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource( new StringReader(response.getBody()) );

        ItemsList<PolicyVersionMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
    }
}
