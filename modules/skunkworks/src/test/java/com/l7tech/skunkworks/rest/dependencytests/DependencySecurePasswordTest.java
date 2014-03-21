package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.DependencyTreeMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencySecurePasswordTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencySecurePasswordTest.class.getName());

    private final SecurePassword securePassword =  new SecurePassword();
    private final SecurePassword securePasswordZoned =  new SecurePassword();
    private final SecurityZone securityZone =  new SecurityZone();
    private SecurePasswordManager securePasswordManager;
    private SecurityZoneManager securityZoneManager;

    @Before
    public void before() throws Exception {
        super.before();

        securePasswordManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);

        //create secure password
        securePassword.setName("MyPassword");
        securePassword.setEncodedPassword("password");
        securePassword.setUsageFromVariable(true);
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePasswordManager.save(securePassword);


        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        //create secure password
        securePasswordZoned.setName("MyPasswordZoned");
        securePasswordZoned.setEncodedPassword("password");
        securePasswordZoned.setUsageFromVariable(true);
        securePasswordZoned.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePasswordZoned.setSecurityZone(securityZone);
        securePasswordManager.save(securePasswordZoned);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
        securePasswordManager.delete(securePassword);
        securePasswordManager.delete(securePasswordZoned);
        securityZoneManager.delete(securityZone);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);

            }
        });
    }

    @Test
    public void addSecurityTokenAssertionZonedPasswordTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AddWssSecurityToken>\n" +
                        "            <L7p:IncludePassword booleanValue=\"true\"/>\n" +
                        "            <L7p:Password stringValue=\"${secpass.MyPasswordZoned.plaintext}\"/>\n" +
                        "            <L7p:Target target=\"REQUEST\"/>\n" +
                        "            <L7p:UseLastGatheredCredentials booleanValue=\"false\"/>\n" +
                        "            <L7p:Username stringValue=\"asdg\"/>" +
                        "        </L7p:AddWssSecurityToken>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePasswordZoned);

                // verify security zone dependency
                assertEquals(1,dep.getDependencies().size());
                DependencyMO passwordDep  = dep.getDependencies().get(0);
                assertEquals(securityZone.getId(), passwordDep.getDependentObject().getId());
                assertEquals(securityZone.getName(), passwordDep.getDependentObject().getName());
                assertEquals(EntityType.SECURITY_ZONE.toString(), passwordDep.getDependentObject().getType());

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
                        "            <L7p:PasswordGoid goidValue=\""+securePassword.getId()+"\"/>\n" +
                        "            <L7p:ResponseTarget MessageTarget=\"included\">\n" +
                        "                <L7p:Target target=\"RESPONSE\"/>\n" +
                        "            </L7p:ResponseTarget>\n" +
                        "            <L7p:Security security=\"ftp\"/>\n" +
                        "            <L7p:UserName stringValue=\"myUser\"/>\n" +
                        "        </L7p:FtpRoutingAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
            }
        });
    }

    @Test
    public void httpRoutingAssertionKerberosPasswordTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:HttpRoutingAssertion>\n" +
                        "            <L7p:KrbConfiguredAccount stringValue=\"dude\"/>\n" +
                        "            <L7p:KrbConfiguredPassword stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                        "            <L7p:ProtectedServiceUrl stringValue=\"http://blah\"/>\n" +
                        "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
                        "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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
                        "            <L7p:PasswordGoid goidValue=\""+securePassword.getId()+"\"/>\n" +
                        "            <L7p:Username stringValue=\"awge\"/>\n" +
                        "        </L7p:SshRouteAssertion>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
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

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){
            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNull(dependencyItem.getContent().getDependencies());
            }
        });
    }

    @Test
    public void kerberosAuthenticationAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:KerberosAuthentication>\n" +
                "            <L7p:KrbConfiguredAccount stringValue=\"aweg\"/>\n" +
                "            <L7p:KrbSecurePasswordReference goidValue=\""+ securePassword.getId()+"\"/>\n" +
                "            <L7p:KrbUseGatewayKeytab booleanValue=\"true\"/>\n" +
                "            <L7p:LastAuthenticatedUser booleanValue=\"true\"/>\n" +
                "            <L7p:Realm stringValue=\"hfd.com\"/>\n" +
                "            <L7p:S4U2Self booleanValue=\"true\"/>\n" +
                "            <L7p:ServicePrincipalName stringValue=\"http/service@DOMAIN.COM\"/>\n" +
                "            <L7p:UserRealm stringValue=\"\"/>\n" +
                "        </L7p:KerberosAuthentication>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
            }
        });
    }

    @Test
    public void wssDigestAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:WssDigest>\n" +
                "            <L7p:RequiredPassword stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                "            <L7p:RequiredUsername stringValue=\"asdf\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "        </L7p:WssDigest>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
            }
        });
    }

    @Test
    public void addWssSecurityTokenAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddWssSecurityToken>\n" +
                "            <L7p:IncludePassword booleanValue=\"true\"/>\n" +
                "            <L7p:Password stringValue=\"${secpass.MyPassword.plaintext}\"/>\n" +
                "            <L7p:UseLastGatheredCredentials booleanValue=\"false\"/>\n" +
                "            <L7p:Username stringValue=\"asdf\"/>\n" +
                "        </L7p:AddWssSecurityToken>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoid<Item<DependencyTreeMO>>(){

            @Override
            public void call(Item<DependencyTreeMO> dependencyItem) {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyTreeMO dependencyAnalysisMO = dependencyItem.getContent();

                assertEquals(1,dependencyAnalysisMO.getDependencies().size());
                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep.getDependentObject(),securePassword);
            }
        });
    }

    protected void verifyItem(Item item, SecurePassword secPassword ){
        assertEquals(secPassword.getId(), item.getId());
        assertEquals(secPassword.getName(), item.getName());
        assertEquals(EntityType.SECURE_PASSWORD.toString(), item.getType());
    }
}
