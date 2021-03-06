package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.solutionkit.SolutionKitAdminHelper;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.server.solutionkit.SolutionKitManagerStub;
import com.l7tech.test.util.TestUtils;
import com.l7tech.util.*;
import org.apache.commons.lang.CharEncoding;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.SolutionKitManagerResource.PARAMETER_DELIMINATOR;
import static com.l7tech.gateway.common.solutionkit.SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Solution Kit Manager Resource tests
 *
 * TODO: extend ServerRestGatewayManagementAssertionTestBase (which uses testGatewayManagementContext.xml) in case you need real or stubbed beans not mocks
 * TODO: In that case MockitoJUnitRunner cannot be used (remove that line)
 *
 * TODO: it seems the tests took long time to complete (~30s)... perhaps do not run them on Daily
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitManagerResourceTest {
    private static final Logger logger = Logger.getLogger(SolutionKitManagerResourceTest.class.getName());

    private static TrustedSignerCertsManager TRUSTED_SIGNER_MANAGER;
    private static final String[] TRUSTED_SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com"
    };

    private static final String SK_FILENAME = TestUtils.getFieldValue(SkarPayload.class, "SK_FILENAME", String.class);
    private static final String SK_INSTALL_BUNDLE_FILENAME = TestUtils.getFieldValue(SkarPayload.class, "SK_INSTALL_BUNDLE_FILENAME", String.class);


    private static final String SAMPLE_SOLUTION_KIT_META_TEMPLATE = "<l7:SolutionKit xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Id>{0}</l7:Id>\n" +
            "    <l7:Name>{1}</l7:Name>\n" +
            "    <l7:Description>{2}</l7:Description>\n" +
            "    <l7:Version>{3}</l7:Version>\n" +
            "    <l7:TimeStamp>2015-05-11T11:56:35.603-08:00</l7:TimeStamp>\n" +
            "    <l7:IsCollection>{4}</l7:IsCollection>\n" +
            "    <l7:FeatureSet>{5}</l7:FeatureSet>\n" +
            "</l7:SolutionKit>";

    private static final String SAMPLE_SOLUTION_KIT_META_XML = MessageFormat.format(
            SAMPLE_SOLUTION_KIT_META_TEMPLATE,
            "33b16742-d62d-4095-8f8d-4db707e9ad52",
            "Simple Solution Kit",
            "This is a simple Solution Kit example.",
            "1.0",
            "{0}",
            ""
    );

    private static final String SAMPLE_INSTALL_BUNDLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "\t<l7:References>\n" +
            "\t\t<l7:Item>\n" +
            "\t\t\t<l7:Name>aa</l7:Name>\n" +
            "\t\t\t<l7:Id>7a08ed0cf997ebba787cfe797913b456</l7:Id>\n" +
            "\t\t\t<l7:Type>SERVICE</l7:Type>\n" +
            "\t\t\t<l7:TimeStamp>2015-08-20T17:06:25.068-07:00</l7:TimeStamp>\n" +
            "\t\t\t<l7:Resource>\n" +
            "\t\t\t\t<l7:Service id=\"7a08ed0cf997ebba787cfe797913b456\" version=\"2\">\n" +
            "\t\t\t\t\t<l7:ServiceDetail folderId=\"7a08ed0cf997ebba787cfe797913b415\" id=\"7a08ed0cf997ebba787cfe797913b456\" version=\"2\">\n" +
            "\t\t\t\t\t\t<l7:Name>aa</l7:Name>\n" +
            "\t\t\t\t\t\t<l7:Enabled>true</l7:Enabled>\n" +
            "\t\t\t\t\t\t<l7:ServiceMappings>\n" +
            "\t\t\t\t\t\t\t<l7:HttpMapping>\n" +
            "\t\t\t\t\t\t\t\t<l7:UrlPattern>/aa</l7:UrlPattern>\n" +
            "\t\t\t\t\t\t\t\t<l7:Verbs>\n" +
            "\t\t\t\t\t\t\t\t\t<l7:Verb>GET</l7:Verb>\n" +
            "\t\t\t\t\t\t\t\t\t<l7:Verb>POST</l7:Verb>\n" +
            "\t\t\t\t\t\t\t\t\t<l7:Verb>PUT</l7:Verb>\n" +
            "\t\t\t\t\t\t\t\t\t<l7:Verb>DELETE</l7:Verb>\n" +
            "\t\t\t\t\t\t\t\t</l7:Verbs>\n" +
            "\t\t\t\t\t\t\t</l7:HttpMapping>\n" +
            "\t\t\t\t\t\t</l7:ServiceMappings>\n" +
            "\t\t\t\t\t\t<l7:Properties>\n" +
            "\t\t\t\t\t\t\t<l7:Property key=\"internal\">\n" +
            "\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
            "\t\t\t\t\t\t\t</l7:Property>\n" +
            "\t\t\t\t\t\t\t<l7:Property key=\"policyRevision\">\n" +
            "\t\t\t\t\t\t\t\t<l7:LongValue>2</l7:LongValue>\n" +
            "\t\t\t\t\t\t\t</l7:Property>\n" +
            "\t\t\t\t\t\t\t<l7:Property key=\"soap\">\n" +
            "\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
            "\t\t\t\t\t\t\t</l7:Property>\n" +
            "\t\t\t\t\t\t\t<l7:Property key=\"tracingEnabled\">\n" +
            "\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
            "\t\t\t\t\t\t\t</l7:Property>\n" +
            "\t\t\t\t\t\t\t<l7:Property key=\"wssProcessingEnabled\">\n" +
            "\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
            "\t\t\t\t\t\t\t</l7:Property>\n" +
            "\t\t\t\t\t\t</l7:Properties>\n" +
            "\t\t\t\t\t</l7:ServiceDetail>\n" +
            "\t\t\t\t\t<l7:Resources>\n" +
            "\t\t\t\t\t\t<l7:ResourceSet tag=\"policy\">\n" +
            "\t\t\t\t\t\t\t<l7:Resource type=\"policy\" version=\"1\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
            "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
            "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
            "        &lt;L7p:HardcodedResponse&gt;\n" +
            "            &lt;L7p:Base64ResponseBody stringValue=&quot;b28=&quot;/&gt;\n" +
            "        &lt;/L7p:HardcodedResponse&gt;\n" +
            "    &lt;/wsp:All&gt;\n" +
            "&lt;/wsp:Policy&gt;\n" +
            "\t\t\t\t\t\t\t</l7:Resource>\n" +
            "\t\t\t\t\t\t</l7:ResourceSet>\n" +
            "\t\t\t\t\t</l7:Resources>\n" +
            "\t\t\t\t</l7:Service>\n" +
            "\t\t\t</l7:Resource>\n" +
            "\t\t</l7:Item>\n" +
            "\t</l7:References>\n" +
            "\t<l7:Mappings>\n" +
            "\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"7a08ed0cf997ebba787cfe797913b415\" srcUri=\"http://david90.ca.com:8080/restman/1.0/folders/7a08ed0cf997ebba787cfe797913b415\" type=\"FOLDER\">\n" +
            "\t\t\t<l7:Properties>\n" +
            "\t\t\t\t<l7:Property key=\"FailOnNew\">\n" +
            "\t\t\t\t\t<l7:BooleanValue>true</l7:BooleanValue>\n" +
            "\t\t\t\t</l7:Property>\n" +
            "\t\t\t</l7:Properties>\n" +
            "\t\t</l7:Mapping>\n" +
            "\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"7a08ed0cf997ebba787cfe797913b456\" srcUri=\"http://david90.ca.com:8080/restman/1.0/services/7a08ed0cf997ebba787cfe797913b456\" type=\"SERVICE\"/>\n" +
            "\t</l7:Mappings>\n" +
            "</l7:Bundle>";

    @Mock
    private LicenseManager licenseManager;
    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;
    @Mock
    private FormDataMultiPart formDataMultiPart;
    @Mock
    private SolutionKitsConfig solutionKitsConfig;
    @Mock
    private SolutionKitAdminHelper solutionKitAdminHelper;

    @Spy
    private SolutionKitManager solutionKitManager = new SolutionKitManagerStub();

    private SolutionKitManagerResource solutionKitResource;

    private List<SolutionKit> solutionKitList; //3 items, one parent with two children
    private List<SolutionKit> solutionKitList2; //2 items, one parent with one child


    @BeforeClass
    public static void beforeClass() throws Exception {
        SignatureTestUtils.beforeClass();

        TRUSTED_SIGNER_MANAGER = SignatureTestUtils.createSignerManager(TRUSTED_SIGNER_CERT_DNS);
        Assert.assertNotNull("signature verifier is created", TRUSTED_SIGNER_MANAGER);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SignatureTestUtils.afterClass();
    }

    @Before
    public void before() throws Exception {
        // for now mock import bundle and return empty mappings
        // TODO: add real logic here or use ServerRestGatewayManagementAssertionTestBase to create real/stub solutionKitManager
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocationOnMock) throws Throwable {
                // for now always return empty mappings
                return "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>Bundle mappings</l7:Name>\n" +
                        "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
                        "    <l7:TimeStamp>2015-07-31T09:06:18.772-07:00</l7:TimeStamp>\n" +
                        "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?test=true\"/>\n" +
                        "    <l7:Resource>\n" +
                        "        <l7:Mappings>" +
                        "        </l7:Mappings>\n" +
                        "    </l7:Resource>\n" +
                        "</l7:Item>";
            }
        }).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());

        // create our SolutionKitManagerResource
        solutionKitResource = new SolutionKitManagerResource();
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitResource.setLicenseManager(licenseManager);
        solutionKitResource.setTrustedSignerCertsManager(TRUSTED_SIGNER_MANAGER);
        solutionKitResource.setIdentityProviderConfigManager(identityProviderConfigManager);
        solutionKitResource.setSolutionKitAdminHelper(solutionKitAdminHelper);
    }

    @After
    public void after() throws Exception {
        // clear all added SK entities
        final Collection<SolutionKitHeader> entities = new ArrayList<>(solutionKitManager.findAllHeaders());
        for (final EntityHeader entity : entities) {
            solutionKitManager.delete(entity.getGoid());
        }
    }


    @Test
    public void entityIdDecodeAndSplit() throws Exception {
        Map<String, String> entityIdReplaceMap = new HashMap<>(2);

        String entityIdOld = "f1649a0664f1ebb6235ac238a6f71a6d";
        String entityIdNew = "66461b24787941053fc65a626546e4bd";
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + PARAMETER_DELIMINATOR + entityIdNew, entityIdReplaceMap);
        assertEquals(entityIdNew, entityIdReplaceMap.get(entityIdOld));

        entityIdOld = "0567c6a8f0c4cc2c9fb331cb03b4de6f";
        entityIdNew = "1e3299eab93e2935adafbf35860fc8d9";
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + URLEncoder.encode(PARAMETER_DELIMINATOR, CharEncoding.UTF_8) + entityIdNew, entityIdReplaceMap);
        assertEquals(entityIdNew, entityIdReplaceMap.get(entityIdOld));
    }

    @SuppressWarnings("JavadocReference")
    @Test
    public void unsignedSkar() throws Exception {
        // create our sample skar
        byte[] sampleSkar = buildSampleSkar(SAMPLE_SOLUTION_KIT_META_XML, SAMPLE_INSTALL_BUNDLE_XML, null);
        Assert.assertNotNull(sampleSkar);
        // try to install unsigned skar
        Response response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(sampleSkar),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about instanceModifier\nnull, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Invalid signed Zip"));

        // test unsigned skar of skars

        // create sample child skars
        final InputStream[] childSkars = creteUnsignedSampleChildScars(3);
        // crate sample parent skar
        sampleSkar = buildSampleSkar(
                MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                        java.util.UUID.randomUUID(),
                        "Simple Parent Solution Kit",
                        "This is a simple Parent Solution Kit example.",
                        "1.0",
                        "{0}",
                        ""
                ),
                null,
                childSkars
        );
        Assert.assertNotNull(sampleSkar);
        // install the skar of skars
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(sampleSkar),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about instanceModifier\nnull, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // should fail with BAD_REQUEST and error message containing "Invalid signed Zip"
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Invalid signed Zip"));
    }

    @Test
    public void signedSkar() throws Exception {
        // test with all trusted signers
        for (final String signerDN : TRUSTED_SIGNER_CERT_DNS) {
            // create our sample skar
            byte[] sampleSkarBytes = buildSampleSkar(SAMPLE_SOLUTION_KIT_META_XML, SAMPLE_INSTALL_BUNDLE_XML, null);
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            byte[] signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNER_MANAGER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            Response response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about instanceModifier\nnull, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    new FormDataMultiPart()
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // test skar-of-skars (children unsigned)
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // create sample child unsigned child skars
            InputStream[] childSkarsStream = creteUnsignedSampleChildScars(2);
            // crate sample parent skar
            sampleSkarBytes = buildSampleSkar(
                    MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                            java.util.UUID.randomUUID(),
                            "Simple Parent Solution Kit",
                            "This is a simple Parent Solution Kit example.",
                            "1.0",
                            "{0}",
                            ""
                    ),
                    null,
                    childSkarsStream
            );
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNER_MANAGER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    new FormDataMultiPart()
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // test skar-of-skars (children signed)
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // create sample child signed child skars
            childSkarsStream = creteSignedSampleChildScars(2, TRUSTED_SIGNER_MANAGER, signerDN);
            // crate sample parent skar
            sampleSkarBytes = buildSampleSkar(
                    MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                            java.util.UUID.randomUUID(),
                            "Simple Parent Solution Kit",
                            "This is a simple Parent Solution Kit example.",
                            "1.0",
                            "{0}",
                            ""
                    ),
                    null,
                    childSkarsStream
            );
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNER_MANAGER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Missing required file SolutionKit.xml"));
        }
    }

    @Test
    public void untrustedSkar() throws Exception {
        // create untrusted signer with same DNs plus a new one
        final String[] untrustedDNs = ArrayUtils.concat(
                TRUSTED_SIGNER_CERT_DNS,
                new String[] {
                        "cn=signer.untrusted.apim.ca.com"
                }
        );
        final TrustedSignerCertsManager untrustedSigner = SignatureTestUtils.createSignerManager(untrustedDNs);

        // test using untrusted signer with all signing cert DNs
        for (final String signerDN : untrustedDNs) {
            // create our sample skar
            byte[] sampleSkarBytes = buildSampleSkar(SAMPLE_SOLUTION_KIT_META_XML, SAMPLE_INSTALL_BUNDLE_XML, null);
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            byte[] signedSampleSkarBytes = SignatureTestUtils.sign(untrustedSigner, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            Response response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Signature could not be verified"));

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // test skar-of-skars (children unsigned)
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // create sample child unsigned child skars
            InputStream[] childSkarsStream = creteUnsignedSampleChildScars(2);
            // crate sample parent skar
            sampleSkarBytes = buildSampleSkar(
                    MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                            java.util.UUID.randomUUID(),
                            "Simple Parent Solution Kit",
                            "This is a simple Parent Solution Kit example.",
                            "1.0",
                            "{0}",
                            ""
                    ),
                    null,
                    childSkarsStream
            );
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            signedSampleSkarBytes = SignatureTestUtils.sign(untrustedSigner, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Signature could not be verified"));

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // test skar-of-skars (children signed)
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // create sample child signed child skars
            childSkarsStream = creteSignedSampleChildScars(2, untrustedSigner, signerDN);
            // crate sample parent skar
            sampleSkarBytes = buildSampleSkar(
                    MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                            java.util.UUID.randomUUID(),
                            "Simple Parent Solution Kit",
                            "This is a simple Parent Solution Kit example.",
                            "1.0",
                            "{0}",
                            ""
                    ),
                    null,
                    childSkarsStream
            );
            Assert.assertNotNull(sampleSkarBytes);
            // sign
            signedSampleSkarBytes = SignatureTestUtils.sign(untrustedSigner, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
                    null, // don't care about failOnExist
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            assertThat((String) response.getEntity(), Matchers.containsString("Signature could not be verified"));
        }
    }

    @Test
    public void tamperWithSignedSkar() throws Exception {
        // create untrusted signer with same DNs plus a new one
        final String[] untrustedDNs = ArrayUtils.concat(
                TRUSTED_SIGNER_CERT_DNS,
                new String[] {
                        "cn=signer.untrusted.apim.ca.com"
                }
        );
        final TrustedSignerCertsManager untrustedSigner = SignatureTestUtils.createSignerManager(untrustedDNs);

        // create sample skar of skars
        final InputStream[] childSkarsStream = creteUnsignedSampleChildScars(2);
        final byte[] sampleSkarBytes = buildSampleSkar(
                MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                        java.util.UUID.randomUUID(),
                        "Simple Parent Solution Kit",
                        "This is a simple Parent Solution Kit example.",
                        "1.0",
                        "{0}",
                        ""
                ),
                null,
                childSkarsStream
        );
        Assert.assertNotNull(sampleSkarBytes);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // tamper with bytes after signing
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        byte[] tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                TRUSTED_SIGNER_MANAGER,
                TRUSTED_SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);
                        return Pair.pair(SignatureTestUtils.flipRandomByte(dataBytes), sigProps);
                    }
                }
        );
        // test
        Response response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // tamper with signature after signing (flipping random byte)
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                TRUSTED_SIGNER_MANAGER,
                TRUSTED_SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        // flip random byte
                        final byte[] modSignBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty(SignatureTestUtils.SIGNATURE_PROP, HexUtils.encodeBase64(modSignBytes));
                        assertThat(signatureB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.either(Matchers.containsString("Signature not verified")).or(Matchers.containsString("Could not verify signature")));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // tamper with signer cert after signing (flipping random byte)
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                TRUSTED_SIGNER_MANAGER,
                TRUSTED_SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signer cert property
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        // flip random byte
                        final byte[] modSignerCertBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty(SignatureTestUtils.SIGNING_CERT_PROPS, HexUtils.encodeBase64(modSignerCertBytes));
                        assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat(
                (String) response.getEntity(),
                Matchers.anyOf(
                        Matchers.containsString("Signature could not be verified"),
                        Matchers.containsString("Signature not verified")
                )
        );
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        // crate another sample skar of skars
        final InputStream[] anotherChildSkarsStream = creteUnsignedSampleChildScars(1);
        final byte[] anotherSampleSkarBytes = buildSampleSkar(
                MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                        java.util.UUID.randomUUID(),
                        "Simple Parent Solution Kit",
                        "This is a simple Parent Solution Kit example.",
                        "1.0",
                        "{0}",
                        ""
                ),
                null,
                anotherChildSkarsStream
        );
        Assert.assertNotNull(sampleSkarBytes);
        // sign with trusted signer
        final byte[] signedTrustedAnotherSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNER_MANAGER, new ByteArrayInputStream(anotherSampleSkarBytes), TRUSTED_SIGNER_CERT_DNS[0]);
        // make sure this is trusted
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(signedTrustedAnotherSampleSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                new FormDataMultiPart()
        );
        // SKAR is signed so it should succeed
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // sign our first sample using untrusted signer and swap the signature from signedTrustedAnotherSampleSkarBytes
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertThat(untrustedDNs[0], Matchers.equalTo(TRUSTED_SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, trustedSigProps);
                    }
                }
        );
        // verify the tamperedSignedSkarBytes have the same signature properties raw-bytes as signedTrustedAnotherSampleSkarBytes
        assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedSkarBytes), Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes)));
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // same as above but instead of swapping the entire signature properties bytes, swap only the signer cert and leave signature unchanged
        // sign our first sample using untrusted signer and swap the signing cert from signedTrustedAnotherSampleSkarBytes
        assertThat(untrustedDNs[0], Matchers.equalTo(TRUSTED_SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature and signer cert property
                        final String signatureB64 = (String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNATURE_PROP);
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS);
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // swap signing cert property
                        sigProps.setProperty(SignatureTestUtils.SIGNING_CERT_PROPS, HexUtils.encodeBase64(trustedSignerCertBytes));
                        // make sure after the swap the signer cert is different
                        assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNING_CERT_PROPS))));
                        // make sure after the swap the signature is unchanged
                        assertThat(signatureB64, Matchers.equalTo((String) sigProps.get(SignatureTestUtils.SIGNATURE_PROP)));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // verify that the tamperedSignedSkarBytes signature properties raw-bytes differ than signedTrustedAnotherSampleSkarBytes
        assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedSkarBytes), Matchers.not(Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes))));
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
                null, // don't care about failOnExist
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void uninstallSuccess() throws Exception {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate single solution kit uninstall based on GUID and IM
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(solutionKitList.toArray(new SolutionKit[solutionKitList.size()])));
        solutionKitResource.setSolutionKitManager(solutionKitManager);

        Collection<SolutionKit> solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        final SolutionKit solutionKit1 = solutionKitList.get(1);
        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit1.getSolutionKitGuid(), "im1")).thenReturn(solutionKit1);

        //Test
        Response resultResponse = solutionKitResource.uninstall(solutionKit1.getSolutionKitGuid()+ "::im1", Collections.emptyList());

        //Expect solutionKit1 to be uninstalled
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 3);
        assertFalse(solutionKitsInManager.contains(solutionKit1));
        assertTrue(solutionKitsInManager.contains(solutionKitList.get(0))); //parent solution kit still exists
        assertTrue(solutionKitsInManager.contains(solutionKitList.get(2))); //solutionKit2 still exists
        assertTrue(solutionKitsInManager.contains(solutionKitList.get(3))); //solutionKit3 still exists


        //no content response for perfect uninstallation
        assertEquals(resultResponse.getStatus(), 204);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate parent and all children are uninstalled successfully
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        final List<SolutionKit> childKits = solutionKitList.subList(1,4);
        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(solutionKitList.toArray(new SolutionKit[solutionKitList.size()])));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        final SolutionKit parentSolutionKit = solutionKitList.get(0);
        when(solutionKitManager.findBySolutionKitGuidAndIM(parentSolutionKit.getSolutionKitGuid(),"im1")).thenReturn(parentSolutionKit);
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test uninstall parent kit
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid()+"::im1", Collections.emptyList());

        //expect all children and parent to be deleted
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 0);

        //no content response for perfect uninstallation
        assertEquals(resultResponse.getStatus(), 204);


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selected children are uninstalled
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(solutionKitList.toArray(new SolutionKit[solutionKitList.size()])));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        when(solutionKitManager.findBySolutionKitGuidAndIM(parentSolutionKit.getSolutionKitGuid(), "im1")).thenReturn(parentSolutionKit);
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);
        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit1.getSolutionKitGuid(), "im1")).thenReturn(solutionKit1);

        //Test uninstall parent kit with specified child for uninstall
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),Collections.singletonList(solutionKit1.getSolutionKitGuid()+"::im1"));

        //expect only parent kit, solutionKit2, and solutionKit3 remain
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 3);
        assertTrue(solutionKitsInManager.contains(solutionKitList.get(2)));
        assertTrue(solutionKitsInManager.contains(solutionKitList.get(3)));
        assertTrue(solutionKitsInManager.contains(parentSolutionKit));

        //expect response to show which solution kits are successfully uninstalled
        assertEquals(resultResponse.getStatus(), 204);
    }

    @Test
    public void uninstallFail() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // solution kit to uninstall fields are empty
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        Response errorResponse = solutionKitResource.uninstall(null, null);

        //expect solution kit id empty error
        assertEquals(errorResponse.getEntity(), "Solution Kit ID to uninstall is empty." + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // parent solution kit IM specified, child IM different from parent
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall("test::a", Collections.singletonList("child::aaa"));

        // expect invalid params error
        assertTrue(errorResponse.getEntity().toString().contains("Error: if child solution kit instance modifiers are specified, it must be the same as parent instance modifier."));
        // expect parent instance modifier listed
        assertTrue(errorResponse.getEntity().toString().contains("Parent Solution Kit Instance Modifier: a"));
        // expect list of child sk listed
        assertTrue(errorResponse.getEntity().toString().contains("child::aaa"));

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // parent solution kit IM specified as default, child IM is not default
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall("test::", Collections.singletonList("child::aaa"));

        // expect invalid params error
        assertTrue(errorResponse.getEntity().toString().contains("Error: if child solution kit instance modifiers are specified, it must be the same as parent instance modifier.\n"));
        // expect parent instance modifier listed
        assertTrue(errorResponse.getEntity().toString().contains("Parent Solution Kit Instance Modifier: N/A"));
        // expect list of child sk listed
        assertTrue(errorResponse.getEntity().toString().contains("child::aaa"));

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // parent solution kit IM not specified, child IM is not same as each other ( a != b)
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall("test", Arrays.asList("c1_guid::a","c2_guid::b"));

        //expect invalid params error
        assertTrue(errorResponse.getEntity().toString().contains("Error: all child solution kit " +
                "instance modifiers must be the same."));
        // expect list of child sk listed
        assertTrue(errorResponse.getEntity().toString().contains("c1_guid::a"));
        assertTrue(errorResponse.getEntity().toString().contains("c2_guid::b"));


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // parent solution kit IM not specified, child IM is not same as each other ("" != b)
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall("test", Arrays.asList("c1_guid::","c2_guid::b"));

        //expect invalid params error
        assertTrue(errorResponse.getEntity().toString().contains("Error: all child solution kit " +
                "instance modifiers must be the same."));
        // expect list of child sk listed
        assertTrue(errorResponse.getEntity().toString().contains("c1_guid::"));
        assertTrue(errorResponse.getEntity().toString().contains("c2_guid::b"));

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // parent solution kit IM not specified, child IM is not same as each other ("" != b)
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall("test", Arrays.asList("c1_guid","c2_guid::b"));

        //expect invalid params error
        assertTrue(errorResponse.getEntity().toString().contains("Error: all child solution kit " +
                "instance modifiers must be the same."));
        // expect list of child sk listed
        assertTrue(errorResponse.getEntity().toString().contains("c1_guid"));
        assertTrue(errorResponse.getEntity().toString().contains("c2_guid::b"));

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // cannot find solution kit GUID and IM
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();
        final SolutionKit solutionKit1 = solutionKitList.get(1);

        when(solutionKitManager.findBySolutionKitGuid(solutionKit1.getSolutionKitGuid())).thenReturn(Collections.singletonList(solutionKit1));

        //Test
        errorResponse = solutionKitResource.uninstall(solutionKit1.getSolutionKitGuid()+"::INVALID_IM", Collections.emptyList());

        //expect solution kit does not exist error
        assertEquals(errorResponse.getEntity(), "Uninstall failed: Cannot find any existing solution kit (GUID = '" +
                solutionKit1.getSolutionKitGuid() + "', and Instance Modifier = 'INVALID_IM') for uninstall." + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // no child with matching guid
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        final List<SolutionKit> childKits = solutionKitList.subList(1,4);
        final SolutionKit parentSolutionKit = solutionKitList.get(0);

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(solutionKitList.toArray(new SolutionKit[solutionKitList.size()])));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        Collection<SolutionKit> solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        when(solutionKitManager.findBySolutionKitGuidAndIM(parentSolutionKit.getSolutionKitGuid(),"im1")).thenReturn(parentSolutionKit);
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),Collections.singletonList("NO_MATCH_GUID_1f87436b-7ca5-41c8-9418-21d7a7855555::im1"));

        //expect child kit selected to uninstall does not match any children from parent
        assertEquals(errorResponse.getEntity(), "UNINSTALL ERRORS:" + System.lineSeparator() + "Uninstall failed: Cannot find any child solution kit matching the GUID = 'NO_MATCH_GUID_1f87436b-7ca5-41c8-9418-21d7a7855555'" + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // no parent holding a child with an invalid instance modifier
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),Collections.singletonList(solutionKit1.getSolutionKitGuid()+"::INVALID_IM"));

        //expect child kit with IM selected does not match existing child kits
        assertEquals(errorResponse.getEntity(),"Uninstall failed: Cannot find any existing solution kit (GUID = '" +
                parentSolutionKit.getSolutionKitGuid() + "', and Instance Modifier = 'INVALID_IM') for uninstall." + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // when child kits are selected, show which child kits uninstalled successfully, and which have errors
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //Children to uninstall
        List<String> childrenToUninstall = new ArrayList<>();
        final SolutionKit solutionKit2 = solutionKitList.get(2);
        childrenToUninstall.add(solutionKit1.getSolutionKitGuid()+"INVALID::im1");
        childrenToUninstall.add(solutionKit2.getSolutionKitGuid()+"::im1");

        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit2.getSolutionKitGuid(), "im1")).thenReturn(solutionKit2);

        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),childrenToUninstall);

        //expect solutionKit1 uninstallation to fail with error, and solutionKit2 to be successfully uninstalled
        assertEquals(errorResponse.getEntity(),"Uninstalled solution kits:" + System.lineSeparator() +
                "- 'SolutionKit2' (GUID = '1f87436b-7ca5-41c8-9418-21d7a7848988', and Instance Modifier = 'im1')" + System.lineSeparator() +
                System.lineSeparator() +
                "Total Solution Kits deleted: 1" + System.lineSeparator() +
                System.lineSeparator() +
                "Solution kits selected for uninstall that failed:" + System.lineSeparator() +
                "Uninstall failed: Cannot find any child solution kit matching the GUID = '1f87436b-7ca5-41c8-9418-21d7a7848999INVALID'" + System.lineSeparator());
    }

    @Test
    public void selectSolutionKitsForInstallSuccess() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install all solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        Map<SolutionKit, Bundle> loaded = new HashMap<>();
        loaded.put(solutionKitList.get(1), null);
        loaded.put(solutionKitList.get(2), null);
        loaded.put(solutionKitList.get(3), null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);

        //test Install solution kits with IM "global IM"
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, "global IM", null, true);

        //expect all the solution kits installed have IM "global IM"
        Set<SolutionKit> selected = solutionKitsConfig.getSelectedSolutionKits();
        for(SolutionKit solutionKit : selected) {
            assertEquals((solutionKit.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY)),"global IM");
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install selected solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);

        // only select solutionkit1 and 2
        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(1).getSolutionKitGuid()+"::im1");
        FormDataBodyPart solutionKitSelect2 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(2).getSolutionKitGuid());
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(solutionKitSelect1);
        solutionKitSelects.add(solutionKitSelect2);

        //Only Install
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, "global IM", solutionKitSelects, true);

        //Expect only solutionKit 1 and 2 to be added
        selected = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selected.size(), 2);
        for (SolutionKit solutionKit : selected) {
            assertTrue(solutionKit.getSolutionKitGuid().equals(solutionKitList.get(1).getSolutionKitGuid()) ||
            solutionKit.getSolutionKitGuid().equals(solutionKitList.get(2).getSolutionKitGuid()));
        }
    }

    @Test
    public void selectSolutionKitsForInstallWithFailOnExistFalse() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install all solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        Map<SolutionKit, Bundle> loaded = new HashMap<>();
        loaded.put(solutionKitList.get(1), null);
        loaded.put(solutionKitList.get(2), null);
        loaded.put(solutionKitList.get(3), null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);
        when(solutionKitAdminHelper.find(solutionKitList.get(2).getSolutionKitGuid())).thenReturn(CollectionUtils.set(solutionKitList.get(2)));

        //test Install solution kits with IM "global IM"
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, "global IM", null, false);

        //expect all the solution kits installed have IM "global IM"
        Set<SolutionKit> selected = solutionKitsConfig.getSelectedSolutionKits();
        for(SolutionKit solutionKit : selected) {
            assertEquals((solutionKit.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY)),"global IM");
        }
        assertEquals("Expecting only 2 solution kits to be selected", 2, selected.size());
        assertFalse("The second solution should not be selected.", selected.contains(solutionKitList.get(2)));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install selected solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);
        when(solutionKitAdminHelper.find(solutionKitList.get(2).getSolutionKitGuid())).thenReturn(CollectionUtils.set(solutionKitList.get(2)));

        // only select solutionkit1 and 2
        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(1).getSolutionKitGuid()+"::im1");
        FormDataBodyPart solutionKitSelect2 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(2).getSolutionKitGuid());
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(solutionKitSelect1);
        solutionKitSelects.add(solutionKitSelect2);

        //Only Install
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, "global IM", solutionKitSelects, false);

        //Expect only solutionKit 1
        selected = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selected.size(), 1);
        for (SolutionKit solutionKit : selected) {
            assertTrue(solutionKit.getSolutionKitGuid().equals(solutionKitList.get(1).getSolutionKitGuid()));
        }
    }

    @Test
    public void selectSolutionKitsForInstallWithFailOnExistFalseAndNoInstanceModifier() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install all solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        Map<SolutionKit, Bundle> loaded = new HashMap<>();
        loaded.put(solutionKitList.get(1), null);
        loaded.put(solutionKitList.get(2), null);
        loaded.put(solutionKitList.get(3), null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);
        when(solutionKitAdminHelper.find(solutionKitList.get(2).getSolutionKitGuid())).thenReturn(CollectionUtils.set(solutionKitList.get(2)));

        //test Install solution kits with IM "global IM"
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, null, null, false);

        //expect all the solution kits installed have IM "global IM"
        Set<SolutionKit> selected = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals("Expecting only 2 solution kits to be selected", 2, selected.size());
        assertFalse("The second solution should not be selected.", selected.contains(solutionKitList.get(2)));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install selected solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);
        when(solutionKitAdminHelper.find(solutionKitList.get(2).getSolutionKitGuid())).thenReturn(CollectionUtils.set(solutionKitList.get(2)));

        // only select solutionkit1 and 2
        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(1).getSolutionKitGuid());
        FormDataBodyPart solutionKitSelect2 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(2).getSolutionKitGuid());
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(solutionKitSelect1);
        solutionKitSelects.add(solutionKitSelect2);

        //Only Install
        solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, null, solutionKitSelects, false);

        //Expect only solutionKit 1
        selected = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selected.size(), 1);
        for (SolutionKit solutionKit : selected) {
            assertTrue(solutionKit.getSolutionKitGuid().equals(solutionKitList.get(1).getSolutionKitGuid()));
        }
    }

    @Test
    public void selectSolutionKitsForInstallFail() throws Exception {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // guid not found in skar
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        //missing SolutionKit1
        Map<SolutionKit, Bundle> loaded = new HashMap<>();
        loaded.put(solutionKitList.get(2), null);
        loaded.put(solutionKitList.get(3), null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);

        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKitList.get(1).getSolutionKitGuid()+"::im1");

        //test try to Install solution kits with IM "global IM" with no solution kits loaded
        try {
            solutionKitResource.setSelectedSolutionKitsForInstall(solutionKitsConfig, "global IM", Collections.singletonList(solutionKitSelect1), true);
            fail("Solution kit to install not found in skar error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(), "Solution Kit ID to install: " +
                    solutionKitList.get(1).getSolutionKitGuid() + " not found in the skar." + System.lineSeparator());
        }
    }

    @Test
    public void selectSolutionKitsForUpgradeSuccess() throws Exception {
        initializeSolutionKits();

        ////////////////////////////////////////////////////////////////
        // Select a parent and all children solution kits for upgrade //
        ////////////////////////////////////////////////////////////////

        // The selected list is empty.  No specific child solution kits selected means all child solution kits selected.
        final List<String> selectedGuidList = new ArrayList<>();

        // The initial upgrade candidate list contains a parent and all three solution kits.
        List<SolutionKit> upgradeList = new ArrayList<>(solutionKitList.size());
        for (final SolutionKit sk: solutionKitList) upgradeList.add(sk);

        // The loaded solution kit list contains all three solution kits, each of which has not set an instance modifier,
        // b/c when any solution kit is loaded from the skar, its instance modifier has not been set.
        Map<SolutionKit, Bundle> loadedSolutionKits = new HashMap<>();
        loadedSolutionKits.put(createLoadedSolutionKit(solutionKitList.get(1)), null);
        loadedSolutionKits.put(createLoadedSolutionKit(solutionKitList.get(2)), null);
        loadedSolutionKits.put(createLoadedSolutionKit(solutionKitList.get(3)), null);

        when(solutionKitsConfig.getSolutionKitsToUpgrade()).thenReturn(upgradeList);
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        // Test by passing a parent solution kit without child solution kits selected.
        solutionKitResource.setSelectedSolutionKitsForUpgrade(solutionKitList.get(0), "im1", selectedGuidList, solutionKitsConfig);

        //Expect the three solution kits selected to be upgraded from the ones already loaded
        Set<SolutionKit> selectedLoadedSKs = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selectedLoadedSKs.size(), 3);

        for (final SolutionKit selected: selectedLoadedSKs) {
            // Expect all selected solution kits to be in the set of updated solution Kit
            assertTrue(loadedSolutionKits.keySet().contains(selected));

            // Expect that the instance modifier is updated to "im1" instead of default instance modifier (i.e., null).
            assertEquals(selected.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY), "im1");
        }

        /////////////////////////////////////////////////
        // Select some child solution kits for upgrade //
        /////////////////////////////////////////////////

        // Select two solution kits for upgrade
        selectedGuidList.clear();
        selectedGuidList.add(solutionKitList.get(1).getSolutionKitGuid());
        selectedGuidList.add(solutionKitList.get(2).getSolutionKitGuid());

        // The upgrade candidate list still has four solution kits (parent + 3 children).
        when(solutionKitsConfig.getSolutionKitsToUpgrade()).thenReturn(upgradeList);

        // The loaded solution kit list still have three solution kits.
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        // Test by passing a parent solution kit with two child solution kits selected.
        solutionKitResource.setSelectedSolutionKitsForUpgrade(solutionKitList.get(0), "im1", selectedGuidList, solutionKitsConfig);

        //Expect the three solution kits selected to be upgraded from the ones already loaded
        selectedLoadedSKs = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selectedLoadedSKs.size(), 2);

        for (final SolutionKit selected: selectedLoadedSKs) {
            // Expect all selected solution kits to be in the set of updated solution Kit
            assertTrue(loadedSolutionKits.keySet().contains(selected));

            // Expect that the instance modifier is updated to "im1" instead of default instance modifier (i.e., null).
            assertEquals(selected.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY), "im1");
        }

        /////////////////////////////////////////////////////////
        // Select a single non-parent solution kit for upgrade //
        /////////////////////////////////////////////////////////

        // No selected solution kits specified
        selectedGuidList.clear();

        // The upgrade candidate lis has one solution kit, which is not a parent.
        upgradeList.clear();
        upgradeList.add(solutionKitList.get(1));
        when(solutionKitsConfig.getSolutionKitsToUpgrade()).thenReturn(upgradeList);

        // The loaded solution kit list still have three solution kits.
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        // Test by passing a non-parent solution kit without solution kits selected.
        solutionKitResource.setSelectedSolutionKitsForUpgrade(solutionKitList.get(1), "im1", selectedGuidList, solutionKitsConfig);

        //Expect the three solution kits selected to be upgraded from the ones already loaded
        selectedLoadedSKs = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selectedLoadedSKs.size(), 1);

        for (final SolutionKit selected: selectedLoadedSKs) {
            // Expect all selected solution kits to be in the set of updated solution Kit
            assertTrue(loadedSolutionKits.keySet().contains(selected));

            // Expect that the instance modifier is updated to "im1" instead of default instance modifier (i.e., null).
            assertEquals(selected.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY), "im1");
        }
    }

    @Test
    public void selectSolutionKitsForUpgradeFail() throws Exception {
        initializeSolutionKits();

        //////////////////////////////////////////////
        // Selected SK is not in the loaded SK list //
        //////////////////////////////////////////////

        final List<String> selectedGuidList = new ArrayList<>();
        selectedGuidList.add(solutionKitList.get(1).getSolutionKitGuid());
        selectedGuidList.add(solutionKitList.get(2).getSolutionKitGuid());
        selectedGuidList.add(solutionKitList.get(3).getSolutionKitGuid());

        // Loaded all three solution kits
        Map<SolutionKit, Bundle> loadedSolutionKits = new HashMap<>();
        loadedSolutionKits.put(createLoadedSolutionKit(solutionKitList.get(1)), null);
        loadedSolutionKits.put(createLoadedSolutionKit(solutionKitList.get(2)), null);

        final List<SolutionKit> upgradeList = new ArrayList<>(solutionKitList.size());
        for (final SolutionKit sk: solutionKitList) upgradeList.add(sk);

        when(solutionKitsConfig.getSolutionKitsToUpgrade()).thenReturn(upgradeList);
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        try {
            solutionKitResource.setSelectedSolutionKitsForUpgrade(solutionKitList.get(0), "im1", selectedGuidList, solutionKitsConfig);
            fail("SolutionKitManagerResourceException should be thrown since there isn't any solution kit in the skar file to match the selected solution kit for upgrade.");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            final Response response = e.getResponse();
            assertNotNull(response);
            assertTrue("Checking response status", response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
            assertEquals(
                "Checking response message",
                "There isn't any solution kit in the uploaded skar to match a selected solution kit (GUID='1f87436b-7ca5-41c8-9418-21d7a7848977', Instance Modifier='im1')" + System.lineSeparator(),
                response.getEntity()
            );
        }
    }

    @Test
    public void testBackwardsCompatibilitySuccess() {
        final String upgradeGuid = "PARENT_SK_GUID";  // A fake GUID b/c the guid is not important in this test.

        //////////////////////////////////////////////
        // The global instance modifier is not set. //
        //////////////////////////////////////////////

        String instanceModifierParameter = null; // The global instance modifier parameter is not set, which means using default instance modifier.
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_1"));     // Use the global default IM
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_2::"));   // No New IM specified
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_3::::")); // New IM is specified and same as Current IM.

        try {
            final Pair<String, List<String>> resultPair = solutionKitResource.getValidatedUpgradeInfo(upgradeGuid, instanceModifierParameter, solutionKitSelects);
            assertNotNull(resultPair);

            // Verify the derived instance modifier
            final String instanceModifierDerived = resultPair.left;
            assertNull("All derived instance modifiers must be same and equal to default instance modifier.", instanceModifierDerived);

            // Verify the selected solution kit GUIDs.
            final List<String> selectedSKs = resultPair.right;
            assertEquals(selectedSKs.size(), 3);
            assertEquals("Selected child sk 1 GUID", "CHILD_GUID_1", selectedSKs.get(0));
            assertEquals("Selected child sk 2 GUID", "CHILD_GUID_2", selectedSKs.get(1));
            assertEquals("Selected child sk 2 GUID", "CHILD_GUID_3", selectedSKs.get(2));
        } catch (Exception e) {
            fail("This is a successful test case.  Should not reach here!");
        }

        //////////////////////////////////////////
        // The global instance modifier is set. //
        //////////////////////////////////////////

        instanceModifierParameter = "IM1"; // The global instance modifier parameter is set and the value is "IM1".
        solutionKitSelects.clear();
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_1"));           // Use the global IM 'IM1'
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_2::IM1"));      // No New IM specified
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_3::IM1::IM1")); // New IM is specified and same as Current IM.

        try {
            final Pair<String, List<String>> resultPair = solutionKitResource.getValidatedUpgradeInfo(upgradeGuid, instanceModifierParameter, solutionKitSelects);
            assertNotNull(resultPair);

            // Verify the derived instance modifier
            final String instanceModifierDerived = resultPair.left;
            assertEquals("All derived instance modifiers must be same and equal to 'IM1'.", "IM1", instanceModifierDerived);

            // Verify the selected solution kit GUIDs.
            final List<String> selectedSKs = resultPair.right;
            assertEquals(selectedSKs.size(), 3);
            assertEquals("Selected child sk 1 GUID", "CHILD_GUID_1", selectedSKs.get(0));
            assertEquals("Selected child sk 2 GUID", "CHILD_GUID_2", selectedSKs.get(1));
            assertEquals("Selected child sk 2 GUID", "CHILD_GUID_3", selectedSKs.get(2));
        } catch (Exception e) {
            fail("This is a successful test case.  Should not reach here!");
        }
    }

    @Test
    public void testBackwardsCompatibilityFail() {
        final String upgradeGuid = "PARENT_SK_GUID";  // A fake GUID b/c the guid is not important in this test.

        ////////////////////////////////////////////////////////////////
        //         Current instance modifiers are different.          //
        // Two child solution kits have different instance modifiers. //
        ////////////////////////////////////////////////////////////////

        String instanceModifierParameter = null; // The global instance modifier parameter is not set, which means using default instance modifier.
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_1"));      // Use the global default IM
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_2::IM1")); // Use 'IM1' as Current IM
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_3::IM2")); // Use 'IM2' as Current IM

        try {
            solutionKitResource.getValidatedUpgradeInfo(upgradeGuid, instanceModifierParameter, solutionKitSelects);
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            final Response response = e.getResponse();
            assertNotNull(response);
            assertTrue("Checking response status", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
            assertEquals(
                "Checking response message",
                "Cannot upgrade child solution kits with different instance modifiers specified." + System.lineSeparator() +
                    "Failure detail: Solution Kit 1 (ID: CHILD_GUID_1) has instance modifier 'N/A' specified." + System.lineSeparator() +
                    "                Solution Kit 2 (ID: CHILD_GUID_2) has instance modifier 'IM1' specified." + System.lineSeparator(),
                response.getEntity()
            );
        } catch (UnsupportedEncodingException e) {
            fail("This test case does not test URLDecoder, so the type of UnsupportedEncodingException should not happen here.");
        }

        ///////////////////////////////////////////////////////////
        //             New instance modifiers are us.            //
        //  One solution kit uses different instance modifiers.  //
        ///////////////////////////////////////////////////////////

        instanceModifierParameter = null; // The global instance modifier parameter is not set.
        solutionKitSelects.clear();
        solutionKitSelects.add(new FormDataBodyPart("solutionKitSelect", "CHILD_GUID_1::IM0::IM1")); // Set current and new instance modifiers

        try {
            solutionKitResource.getValidatedUpgradeInfo(upgradeGuid, instanceModifierParameter, solutionKitSelects);
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            final Response response = e.getResponse();
            assertNotNull(response);
            assertTrue("Checking response status", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
            assertEquals(
                "Cannot upgrade a solution kit and change its instance modifier at the same time." + System.lineSeparator() +
                    "Failure detail: Solution Kit 1 (ID: CHILD_GUID_1) currently has instance modifier 'IM0', which cannot be changed to 'IM1'." + System.lineSeparator(),
                response.getEntity()
            );
        } catch (UnsupportedEncodingException e) {
            fail("This test case does not test URLDecoder, so the type of UnsupportedEncodingException should not happen here.");
        }
    }

    private static InputStream[] creteSignedSampleChildScars(
            final int numChildren,
            final TrustedSignerCertsManager signer,
            final String signerDn
    ) throws Exception {
        return creteSampleChildScars(numChildren, true, signer, signerDn);
    }

    private static InputStream[] creteUnsignedSampleChildScars(
            final int numChildren
    ) throws Exception {
        return creteSampleChildScars(numChildren, false, null, null);
    }

    /**
     * Create a sample child solution kit and optionally sign it using the specified {@code signer} and {@code signerDn}.
     */
    private static InputStream[] creteSampleChildScars(
            final int numChildren,
            final boolean sign,
            @Nullable final TrustedSignerCertsManager signer,
            @Nullable final String signerDn
    ) throws Exception {
        assertThat(numChildren, Matchers.greaterThanOrEqualTo(1));
        if (sign) {
            // these are mandatory to sign the SK
            Assert.assertNotNull(signer);
            Assert.assertNotNull(signerDn);
        }

        final InputStream[] streams = new InputStream[numChildren];
        for (int i = 0; i < numChildren; ++i) {
            final byte[] skBytes = buildSampleSkar(
                    MessageFormat.format(SAMPLE_SOLUTION_KIT_META_TEMPLATE,
                            java.util.UUID.randomUUID(),
                            "Simple Child Solution Kit " + String.valueOf(i),
                            "This is a simple Child Solution Kit " + String.valueOf(i) + " example.",
                            "1.0",
                            "{0}",
                            ""
                    ),
                    SAMPLE_INSTALL_BUNDLE_XML,
                    null
            );
            if (sign) {
                streams[i] = new ByteArrayInputStream(SignatureTestUtils.sign(signer, new ByteArrayInputStream(skBytes), signerDn));
            } else {
                streams[i] = new ByteArrayInputStream(skBytes);
            }
        }
        return streams;
    }

    /**
     * Create a sample Skar file using the specified meta, install bundle xml and optional child skar streams
     */
    private static byte[] buildSampleSkar(
            String metaXml,
            final String installBundleXml,
            final InputStream[] childSkars
    ) throws Exception {
        assertThat(metaXml, Matchers.not(Matchers.isEmptyOrNullString()));
        final boolean isSkarOfSkars = childSkars != null && childSkars.length > 0;
        if (!isSkarOfSkars) {
            // if not skar-of-skars then install bundle is mandatory
            assertThat(installBundleXml, Matchers.not(Matchers.isEmptyOrNullString()));
        }
        metaXml = MessageFormat.format(metaXml, String.valueOf(isSkarOfSkars));

        final ByteArrayOutputStream outputZip = new ByteArrayOutputStream(1024);
        try (final ZipOutputStream zos = new ZipOutputStream(outputZip)) {
            // SK_FILENAME
            zos.putNextEntry(new ZipEntry(SK_FILENAME));
            IOUtils.spewStream(metaXml.getBytes(Charsets.UTF8), zos);

            if (isSkarOfSkars) {
                int i = 0;
                final String childSkarFileNameTemplate = "ChildSkar{0}.skar";
                for (final InputStream childStream : childSkars) {
                    Assert.assertNotNull(childStream);
                    zos.putNextEntry(new ZipEntry(MessageFormat.format(childSkarFileNameTemplate, String.valueOf(i++))));
                    IOUtils.copyStream(childStream, zos);
                }
            } else {
                zos.putNextEntry(new ZipEntry(SK_INSTALL_BUNDLE_FILENAME));
                IOUtils.spewStream(installBundleXml.getBytes(Charsets.UTF8), zos);
            }
        }

        // finally return the bytes
        return outputZip.toByteArray();
    }

    private void initializeSolutionKits() {
        solutionKitsConfig = Mockito.spy(new SolutionKitsConfig());
        final Goid parentGoidSame = new Goid("1f87436b7ca541c8941821d7a7848111");

        final SolutionKit parentSolutionKit1 = new SolutionKitBuilder()
                .goid(parentGoidSame)
                .name("parent")
                .skGuid("1f87436b-7ca5-41c8-9418-21d7a7848853")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1")
                .mappings("")
                .build();

        final SolutionKit solutionKit1 = new SolutionKitBuilder()
                .name("SolutionKit1")
                .parent(parentGoidSame)
                .skGuid("1f87436b-7ca5-41c8-9418-21d7a7848999")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 1")
                .skVersion("1")
                .goid(new Goid("1f87436b7ca541c8941821d7a7848999"))
                .build();

        final SolutionKit solutionKit2 = new SolutionKitBuilder()
                .name("SolutionKit2")
                .parent(parentGoidSame)
                .skGuid("1f87436b-7ca5-41c8-9418-21d7a7848988")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 2")
                .skVersion("1")
                .goid(new Goid("1f87436b7ca541c8941821d7a7848988"))
                .build();

        final Goid parent2Goid = new Goid("1f87436b7ca541c8941821d7a7848100");
        final SolutionKit parentSolutionKit2 = new SolutionKitBuilder()
                .goid(parent2Goid)
                .name("parent")
                .skGuid("1f87436b-7ca5-41c8-9418-21d7a7848853")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im2")
                .mappings("")
                .build();

        final SolutionKit solutionKit3 = new SolutionKitBuilder()
                .name("SolutionKit3")
                .parent(parent2Goid)
                .skGuid("1f87436b-7ca5-41c8-9418-21d7a7848977")
                .addProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 3")
                .skVersion("1")
                .goid(new Goid("1f87436b7ca541c8941821d7a7848977"))
                .build();

        solutionKitList = Arrays.asList(parentSolutionKit1, solutionKit1, solutionKit2, solutionKit3);

    }

    /**
     * Make a new loaded solution kit from the source solution kit except copying parent GOID and instance modifier.
     *
     * @param source a solution kit used as a source for copy
     * @return a new solution kit having same attributes with source except parent GOID and instance modifier.
     */
    private SolutionKit createLoadedSolutionKit(@NotNull final SolutionKit source) {
        final SolutionKit solutionKit = new SolutionKit();
        solutionKit.setSolutionKitGuid(source.getSolutionKitGuid());
        solutionKit.setSolutionKitVersion(source.getSolutionKitVersion());
        solutionKit.setName(source.getName());
        solutionKit.setMappings(source.getMappings());
        solutionKit.setLastUpdateTime(source.getLastUpdateTime());
        solutionKit.setXmlProperties(source.getXmlProperties());
        solutionKit.setInstallationXmlProperties(source.getInstallationXmlProperties());
        solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, null);

        return solutionKit;
    }

    @Test
    public void testMethodParams() throws Exception {
        Assert.assertThat(
                SolutionKitManagerResource.InstallOrUpgradeParams.Form.all,
                Matchers.containsInAnyOrder(
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.file,
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.instanceModifier,
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.failOnExist,
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.solutionKitSelect,
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.entityIdReplace,
                        SolutionKitManagerResource.InstallOrUpgradeParams.Form.bundle
                )
        );
        Assert.assertThat(
                SolutionKitManagerResource.InstallOrUpgradeParams.Query.all,
                Matchers.contains(SolutionKitManagerResource.InstallOrUpgradeParams.Query.id)
        );

        Assert.assertThat(
                SolutionKitManagerResource.UninstallParams.Form.all,
                Matchers.emptyCollectionOf(String.class)
        );
        Assert.assertThat(
                SolutionKitManagerResource.UninstallParams.Query.all,
                Matchers.contains(
                        SolutionKitManagerResource.UninstallParams.Query.id,
                        SolutionKitManagerResource.UninstallParams.Query.childId
                )
        );
    }
}
