package com.l7tech.skunkworks.rest.dependencytests;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.DependencyTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.*;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*
*/
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DependencyTrustedCertTest extends DependencyTestBase{
    private static final Logger logger = Logger.getLogger(DependencyTrustedCertTest.class.getName());
    private final SecurityZone securityZone =  new SecurityZone();
    private TrustedCert trustedCert = new TrustedCert();
    private TrustedCert trustedCertWithRevocationPolicy = new TrustedCert();
    private TrustedCert trustedCertRevCheckSigner = new TrustedCert();
    private TrustedCert trustedCertWithRevocationPolicySigner = new TrustedCert();
    private RevocationCheckPolicy revocationCheckPolicy = new RevocationCheckPolicy();
    private RevocationCheckPolicy revocationCheckPolicyWithSigner = new RevocationCheckPolicy();
    private TrustedCertManager trustedCertManager;
    private RevocationCheckPolicyManager revocationCheckPolicyManager;
    private SecurityZoneManager securityZoneManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DependencyTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();

        trustedCertManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("trustedCertManager", TrustedCertManager.class);
        revocationCheckPolicyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("revocationCheckPolicyManager", RevocationCheckPolicyManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);

        X509Certificate certificate = new TestCertificateGenerator().subject("cn=test").generate();
        trustedCert.setName(CertUtils.extractFirstCommonNameFromCertificate(certificate));
        trustedCert.setCertificate(certificate);
        trustedCert.setTrustAnchor(false);
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
        trustedCertManager.save(trustedCert);

        //create security zone
        securityZone.setName("Test security zone");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.REVOCATION_CHECK_POLICY));
        securityZone.setDescription("stuff");
        securityZoneManager.save(securityZone);

        // create revocation check policy
        revocationCheckPolicy.setName("Test Revocation check policy");
        revocationCheckPolicy.setSecurityZone(securityZone);
        revocationCheckPolicyManager.save(revocationCheckPolicy);

        // create trusted cert using revocation check policy
        X509Certificate newCertificate = new TestCertificateGenerator().subject("cn=revcheck").generate();
        trustedCertWithRevocationPolicy.setName(CertUtils.extractFirstCommonNameFromCertificate(newCertificate));
        trustedCertWithRevocationPolicy.setCertificate(newCertificate);
        trustedCertWithRevocationPolicy.setTrustAnchor(false);
        trustedCertWithRevocationPolicy.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
        trustedCertWithRevocationPolicy.setRevocationCheckPolicyOid(revocationCheckPolicy.getGoid());
        trustedCertManager.save(trustedCertWithRevocationPolicy);

        // create trusted cert used by revocation check policy
        newCertificate = new TestCertificateGenerator().subject("cn=usedbyrevcheck").generate();
        trustedCertRevCheckSigner.setName(CertUtils.extractFirstCommonNameFromCertificate(newCertificate));
        trustedCertRevCheckSigner.setCertificate(newCertificate);
        trustedCertRevCheckSigner.setTrustAnchor(false);
        trustedCertRevCheckSigner.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
        trustedCertManager.save(trustedCertRevCheckSigner);

        // create revocation check policy
        revocationCheckPolicyWithSigner.setName("Test Revocation check policy with trusted cert signer");
        RevocationCheckPolicyItem item = new RevocationCheckPolicyItem();
        item.setType(RevocationCheckPolicyItem.Type.CRL_FROM_CERTIFICATE);
        item.setUrl(".*");
        item.setTrustedSigners(CollectionUtils.list(trustedCertRevCheckSigner.getGoid()));
        revocationCheckPolicyWithSigner.setRevocationCheckItems(CollectionUtils.list(item));
        revocationCheckPolicyManager.save(revocationCheckPolicyWithSigner);

        // create trusted cert using revocation check policy
        newCertificate = new TestCertificateGenerator().subject("cn=revchecksigner").generate();
        trustedCertWithRevocationPolicySigner.setName(CertUtils.extractFirstCommonNameFromCertificate(newCertificate));
        trustedCertWithRevocationPolicySigner.setCertificate(newCertificate);
        trustedCertWithRevocationPolicySigner.setTrustAnchor(false);
        trustedCertWithRevocationPolicySigner.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
        trustedCertWithRevocationPolicySigner.setRevocationCheckPolicyOid(revocationCheckPolicyWithSigner.getGoid());
        trustedCertManager.save(trustedCertWithRevocationPolicySigner);

    }

    @After
    public void after() throws Exception {
        super.after();
        trustedCertManager.delete(trustedCert);
        trustedCertManager.delete(trustedCertWithRevocationPolicy);
        trustedCertManager.delete(trustedCertWithRevocationPolicySigner);
        trustedCertManager.delete(trustedCertRevCheckSigner);
        revocationCheckPolicyManager.delete(revocationCheckPolicy);
        revocationCheckPolicyManager.delete(revocationCheckPolicyWithSigner);
        securityZoneManager.delete(securityZone);
    }

    @Test
    public void WsSecurityAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:WsSecurity>\n" +
                "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+trustedCert.getId()+"\"/>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "        </L7p:WsSecurity>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void WsSecurityAssertionByNameTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateName stringValue=\""+trustedCert.getName()+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void NonSoapVerifyXMLElementAssertionByGoidTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:NonSoapVerifyElement>\n" +
                "            <L7p:Target target=\"RESPONSE\"/>\n" +
                "            <L7p:VariablePrefix stringValueNull=\"null\"/>\n" +
                "            <L7p:VerifyCertificateGoid goidValue=\""+trustedCert.getId()+"\"/>\n" +
                "        </L7p:NonSoapVerifyElement>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void NonSoapVerifyXMLElementAssertionByNameTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:NonSoapVerifyElement>\n" +
                        "            <L7p:VariablePrefix stringValueNull=\"null\"/>\n" +
                        "            <L7p:VerifyCertificateName stringValue=\""+trustedCert.getName()+"\"/>\n" +
                        "        </L7p:NonSoapVerifyElement>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void LookupTrustedCertificateAssertionTestName() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:LookupTrustedCertificate>\n" +
                "            <L7p:TrustedCertificateName stringValue=\""+trustedCert.getName()+"\"/>\n" +
                "        </L7p:LookupTrustedCertificate>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1, dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void LookupTrustedCertificateAssertionTestThumbprint() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:LookupTrustedCertificate>\n" +
                        "            <L7p:CertThumbprintSha1 stringValue=\""+trustedCert.getThumbprintSha1()+"\"/>\n" +
                        "            <L7p:LookupType security=\"CERT_THUMBPRINT_SHA1\"/>\n" +
                        "        </L7p:LookupTrustedCertificate>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }


    @Test
    public void LookupTrustedCertificateAssertionTestSKI() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:LookupTrustedCertificate>\n" +
                        "            <L7p:CertSubjectKeyIdentifier stringValue=\""+trustedCert.getSki()+"\"/>\n" +
                        "            <L7p:LookupType security=\"CERT_SKI\"/>\n" +
                        "        </L7p:LookupTrustedCertificate>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void LookupTrustedCertificateAssertionTestSubjectDn() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:LookupTrustedCertificate>\n" +
                        "            <L7p:CertSubjectDn stringValue=\""+trustedCert.getSubjectDn()+"\"/>\n" +
                        "            <L7p:LookupType security=\"CERT_SUBJECT_DN\"/>\n" +
                        "        </L7p:LookupTrustedCertificate>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void LookupTrustedCertificateAssertionTestIssuerSerial() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:LookupTrustedCertificate>\n" +
                        "            <L7p:CertIssuerDn stringValue=\""+trustedCert.getIssuerDn()+"\"/>\n" +
                        "            <L7p:CertSerialNumber stringValue=\""+trustedCert.getSerial().toString()+"\"/>\n" +
                        "            <L7p:LookupType security=\"CERT_ISSUER_SERIAL\"/>\n" +
                        "        </L7p:LookupTrustedCertificate>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(1,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    @Test
    public void HttpRoutingAssertionTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://blah\"/>\n" +
                "            <L7p:ProxyHost stringValue=\"http://blah\"/>\n" +
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
                "            <L7p:TlsTrustedCertGoids goidArrayValue=\"included\">\n" +
                "                <L7p:item goidValue=\""+trustedCert.getId()+"\"/>\n" +
                "            </L7p:TlsTrustedCertGoids>\n" +
                "            <L7p:TlsTrustedCertNames stringArrayValue=\"included\">\n" +
                "                <L7p:item stringValue=\"user\"/>\n" +
                "            </L7p:TlsTrustedCertNames>\n" +
                "        </L7p:HttpRoutingAssertion>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(2,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = dependencyAnalysisMO.getDependencies().get(0);
                verifyItem(dep, trustedCert);
            }
        });
    }

    protected void verifyItem(DependencyMO item, TrustedCert trustedCert){
        assertEquals(trustedCert.getId(), item.getId());
        assertEquals(trustedCert.getName(), item.getName());
        assertEquals(EntityType.TRUSTED_CERT.toString(), item.getType());
    }


    @Test
    public void revocationCheckPolicyTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+trustedCertWithRevocationPolicy.getId()+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(3,dependencyAnalysisMO.getDependencies().size());

                DependencyMO dep  = getDependency(dependencyAnalysisMO,EntityType.TRUSTED_CERT);
                verifyItem(dep, trustedCertWithRevocationPolicy);
                assertNotNull("Missing dependency:" + revocationCheckPolicy.getId(), getDependency(dep.getDependencies(), revocationCheckPolicy.getId()));

                DependencyMO revDep  = getDependency(dependencyAnalysisMO,EntityType.REVOCATION_CHECK_POLICY);
                assertNotNull("Missing dependency:" + revocationCheckPolicy.getId(),revDep);
                assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), revDep.getType());
                assertEquals(revocationCheckPolicy.getId(), revDep.getId());
                assertEquals(revocationCheckPolicy.getName(), revDep.getName());
                assertNotNull("Missing dependency:" + securityZone.getId(), getDependency(revDep.getDependencies(), securityZone.getId()));

                DependencyMO zoneDep  = getDependency(dependencyAnalysisMO,EntityType.SECURITY_ZONE);
                assertNotNull("Missing dependency:" + securityZone.getId(),zoneDep);
                assertEquals(EntityType.SECURITY_ZONE.toString(), zoneDep.getType());
                assertEquals(securityZone.getId(), zoneDep.getId());
                assertEquals(securityZone.getName(), zoneDep.getName());

            }
        });
    }

    @Test
    public void revocationCheckPolicyWithSignerTest() throws Exception {

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+trustedCertWithRevocationPolicySigner.getId()+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(3,dependencyAnalysisMO.getDependencies().size());

                DependencyMO signer  = dependencyAnalysisMO.getDependencies().get(0);
                assertEquals(EntityType.TRUSTED_CERT.toString(), signer.getType());
                verifyItem(signer, trustedCertRevCheckSigner);

                DependencyMO revDep  = dependencyAnalysisMO.getDependencies().get(1);
                assertEquals(EntityType.REVOCATION_CHECK_POLICY.toString(), revDep.getType());
                assertEquals(revocationCheckPolicyWithSigner.getId(), revDep.getId());
                assertEquals(revocationCheckPolicyWithSigner.getName(), revDep.getName());

                DependencyMO cert  = dependencyAnalysisMO.getDependencies().get(2);
                assertEquals(EntityType.TRUSTED_CERT.toString(), cert.getType());
                verifyItem(cert, trustedCertWithRevocationPolicySigner);

            }
        });
    }

    @Test
    public void brokenRevocationCheckPolicyReferenceTest() throws Exception {

        final Goid brokenReferenceGoid = new Goid(trustedCertWithRevocationPolicy.getGoid().getLow(),trustedCertWithRevocationPolicy.getGoid().getHi());

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+brokenReferenceGoid+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0,dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.TRUSTED_CERT.toString(), brokenDep.getType());
                assertEquals(brokenReferenceGoid.toString(), brokenDep.getId());
            }
        });
    }

    @Test
    public void brokenReferenceByNameTest() throws Exception {

        final String brokenReferenceName = "brokenReference";

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateName stringValue=\""+brokenReferenceName+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0,dependencyAnalysisMO.getDependencies().size());
            }
        });
    }

    @Test
    public void brokenReferenceByIdTest() throws Exception {

        final Goid brokenReferenceGoid = new Goid(trustedCert.getGoid().getLow(),trustedCert.getGoid().getHi());

        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:WsSecurity>\n" +
                        "            <L7p:RecipientTrustedCertificateGoid goidValue=\""+brokenReferenceGoid+"\"/>\n" +
                        "            <L7p:Target target=\"RESPONSE\"/>\n" +
                        "        </L7p:WsSecurity>" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";

        TestPolicyDependency(assXml, new Functions.UnaryVoidThrows<Item<DependencyListMO>,Exception>(){

            @Override
            public void call(Item<DependencyListMO> dependencyItem) throws Exception {
                assertNotNull(dependencyItem.getContent().getDependencies());
                DependencyListMO dependencyAnalysisMO = dependencyItem.getContent();
                assertEquals(0,dependencyAnalysisMO.getDependencies().size());

                Assert.assertNotNull(dependencyItem.getContent().getMissingDependencies());
                assertEquals(1, dependencyAnalysisMO.getMissingDependencies().size());
                DependencyMO brokenDep  = dependencyAnalysisMO.getMissingDependencies().get(0);
                Assert.assertNotNull(brokenDep);
                assertEquals(EntityType.TRUSTED_CERT.toString(), brokenDep.getType());
                assertEquals(brokenReferenceGoid.toString(), brokenDep.getId());
            }
        });
    }
}
