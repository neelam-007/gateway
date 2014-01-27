package com.l7tech.skunkworks.rest;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.RunOnNightly;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
*
*/
@ConditionalIgnore(condition = RunOnNightly.class)
public class DependencySecurePasswordTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencySecurePasswordTest.class.getName());

    private Item<StoredPasswordMO> securePasswordItem;

    @Before
    public void before() throws Exception {
        super.before();

        //create secure password
        StoredPasswordMO storedPasswordMO = ManagedObjectFactory.createStoredPassword();
        storedPasswordMO.setName("MyPassword");
        storedPasswordMO.setPassword("password");
        storedPasswordMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("usageFromVariable", true)
                .put("type", "Password")
                .map());
        RestResponse response = getEnvironment().processRequest("passwords", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(storedPasswordMO)));
        assertOkCreatedResponse(response);
        securePasswordItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        securePasswordItem.setContent(storedPasswordMO);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();

        RestResponse response = getEnvironment().processRequest("passwords/" + securePasswordItem.getId(), HttpMethod.DELETE, null, "");
        assertOKDeleteResponse(response);

    }

    @Test
    public void addSecurityTokenAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddWssSecurityToken>\n" +
                "            <L7p:IncludePassword booleanValue=\"true\"/>\n" +
                "            <L7p:Password stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                "            <L7p:Target target=\"REQUEST\"/>\n" +
                "            <L7p:UseLastGatheredCredentials booleanValue=\"false\"/>\n" +
                "            <L7p:Username stringValue=\"asdg\"/>" +
                "        </L7p:AddWssSecurityToken>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);

            }
        });
    }

    @Test
    public void ftpRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:FtpRoutingAssertion>\n" +
                        "            <L7p:Arguments stringValue=\"Args\"/>\n" +
                        "            <L7p:CredentialsSource credentialsSource=\"specified\"/>\n" +
                        "            <L7p:Directory stringValue=\"Dir\"/>\n" +
                        "            <L7p:DownloadedContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
                        "            <L7p:FileNameSource fileNameSource=\"argument\"/>\n" +
                        "            <L7p:FtpMethod ftpCommand=\"RETR\"/>\n" +
                        "            <L7p:HostName stringValue=\"myHost\"/>\n" +
                        "            <L7p:PasswordGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                        "                <L7p:Target target=\"RESPONSE\"/>\n" +
                        "            </L7p:ResponseTarget>\n" +
                        "            <L7p:Security security=\"ftp\"/>\n" +
                        "            <L7p:UserName stringValue=\"myUser\"/>\n" +
                        "        </L7p:FtpRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    @Test
    public void httpRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:HttpRoutingAssertion>\n" +
                        "            <L7p:ProtectedServiceUrl stringValue=\"https://blah\"/>\n" +
                        "            <L7p:ProxyHost stringValue=\"asd\"/>\n" +
                        "            <L7p:ProxyPassword stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                        "            <L7p:ProxyPort intValue=\"374\"/>\n" +
                        "            <L7p:ProxyUsername stringValue=\"user\"/>\n" +
                        "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                </L7p:Rules>\n" +
                        "            </L7p:RequestHeaderRules>\n" +
                        "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                        "            </L7p:RequestParamRules>\n" +
                        "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                </L7p:Rules>\n" +
                        "            </L7p:ResponseHeaderRules>\n" +
                        "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
                        "        </L7p:HttpRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    @Test
    public void bridgeRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:BridgeRoutingAssertion>\n" +
                        "            <L7p:Login stringValue=\"a\"/>\n" +
                        "            <L7p:NtlmHost stringValue=\"aweg\"/>\n" +
                        "            <L7p:Password stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                        "            <L7p:ProtectedServiceUrl stringValue=\"http://blah\"/>\n" +
                        "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
                        "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
                        "            <L7p:Realm stringValue=\"gag\"/>\n" +
                        "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                </L7p:Rules>\n" +
                        "            </L7p:RequestHeaderRules>\n" +
                        "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                        "            </L7p:RequestParamRules>\n" +
                        "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                        "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                        "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                        "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                        "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                        "                    </L7p:item>\n" +
                        "                </L7p:Rules>\n" +
                        "            </L7p:ResponseHeaderRules>\n" +
                        "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
                        "        </L7p:BridgeRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    @Test
    public void sshRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:SshRouteAssertion>\n" +
                        "            <L7p:CommandTypeVariableName stringValue=\"\"/>\n" +
                        "            <L7p:CredentialsSourceSpecified booleanValue=\"true\"/>\n" +
                        "            <L7p:Directory stringValue=\"wage\"/>\n" +
                        "            <L7p:DownloadContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
                        "            <L7p:FileName stringValue=\"eawg\"/>\n" +
                        "            <L7p:Host stringValue=\"agwe\"/>\n" +
                        "            <L7p:NewFileName stringValue=\"\"/>\n" +
                        "            <L7p:PasswordGoid goidValue=\""+securePasswordItem.getId()+"\"/>\n" +
                        "            <L7p:Username stringValue=\"awge\"/>\n" +
                        "        </L7p:SshRouteAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    @Test
    public void emailAlertAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:EmailAlert>\n" +
                        "            <L7p:AuthPassword stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                        "            <L7p:AuthUsername stringValue=\"agwe\"/>\n" +
                        "            <L7p:Authenticate booleanValue=\"true\"/>\n" +
                        "            <L7p:Base64message stringValue=\"YWdnYXdlZ2F3ZWdhd2Vn\"/>\n" +
                        "            <L7p:ContextVarPassword booleanValue=\"true\"/>\n" +
                        "            <L7p:Protocol Protocol=\"SSL\"/>\n" +
                        "            <L7p:SmtpPort stringValue=\"465\"/>\n" +
                        "            <L7p:TargetBCCEmailAddress stringValue=\"agew\"/>\n" +
                        "            <L7p:TargetCCEmailAddress stringValue=\"egwa\"/>\n" +
                        "            <L7p:TargetEmailAddress stringValue=\"gawe\"/>\n" +
                        "        </L7p:EmailAlert>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    @Test
    public void contextVariableTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                        "        </L7p:AuditDetailAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }


    @Test
    public void badContextVariableTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"${secpass.basdbads}\"/>\n" +
                        "        </L7p:AuditDetailAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<DependencyAnalysisMO>(){

            @Override
            public void call(DependencyAnalysisMO dependencyAnalysisMO) {
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordItem);
            }
        });
    }

    protected void verifyItem(Item item, Item<StoredPasswordMO> jdbcConnectionItem){
        assertEquals(jdbcConnectionItem.getId(), item.getId());
        assertEquals(jdbcConnectionItem.getName(), item.getName());
        assertEquals(EntityType.SECURE_PASSWORD.toString(), item.getType());
    }
}
