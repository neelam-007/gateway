package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifierServer;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.server.solutionkit.SolutionKitManagerStub;
import com.l7tech.util.*;
import org.apache.commons.lang.CharEncoding;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hamcrest.Matchers;
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

    private static SignatureVerifierServer TRUSTED_SIGNATURE_VERIFIER;
    private static final String[] TRUSTED_SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com"
    };

    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.solutionkit.SkarProcessor#SK_FILENAME}
     */
    private static final String SK_FILENAME = "SolutionKit.xml";
    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.solutionkit.SkarProcessor#SK_INSTALL_BUNDLE_FILENAME}
     */
    private static final String SK_INSTALL_BUNDLE_FILENAME = "InstallBundle.xml";


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

    @Spy
    private SolutionKitManager solutionKitManager = new SolutionKitManagerStub();

    private SolutionKitManagerResource solutionKitResource;

    private SolutionKit parentSolutionKit,solutionKit1, solutionKit2, solutionKit3;

    @BeforeClass
    public static void beforeClass() throws Exception {
        SignatureTestUtils.beforeClass();

        TRUSTED_SIGNATURE_VERIFIER = SignatureTestUtils.createSignatureVerifier(TRUSTED_SIGNER_CERT_DNS);
        Assert.assertNotNull("signature verifier is created", TRUSTED_SIGNATURE_VERIFIER);
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
        solutionKitResource.setSignatureVerifier(TRUSTED_SIGNATURE_VERIFIER);
        solutionKitResource.setIdentityProviderConfigManager(identityProviderConfigManager);
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
            byte[] signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            Response response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
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
            signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
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
            childSkarsStream = creteSignedSampleChildScars(2, TRUSTED_SIGNATURE_VERIFIER, signerDN);
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
            signedSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNATURE_VERIFIER, new ByteArrayInputStream(sampleSkarBytes), signerDN);
            Assert.assertNotNull(signedSampleSkarBytes);
            // try to install our signed skar
            response = solutionKitResource.installOrUpgrade(
                    new ByteArrayInputStream(signedSampleSkarBytes),
                    null, // don't care about instanceModifier
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
        final SignatureVerifierServer untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

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
            assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));

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
            assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));

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
            assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));
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
        final SignatureVerifierServer untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

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
                TRUSTED_SIGNATURE_VERIFIER,
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
                TRUSTED_SIGNATURE_VERIFIER,
                TRUSTED_SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature property
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        // flip random byte
                        final byte[] modSignBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty("signature", HexUtils.encodeBase64(modSignBytes));
                        assertThat(signatureB64, Matchers.not(Matchers.equalTo((String) sigProps.get("signature"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
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
                TRUSTED_SIGNATURE_VERIFIER,
                TRUSTED_SIGNER_CERT_DNS[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signer cert property
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        // flip random byte
                        final byte[] modSignerCertBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty("cert", HexUtils.encodeBase64(modSignerCertBytes));
                        assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about instanceModifier
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
                        Matchers.containsString("Failed to verify signer certificate"),
                        Matchers.containsString("Signature not verified"),
                        Matchers.containsString("Failed to verify and extract signer certificate")
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
        final byte[] signedTrustedAnotherSampleSkarBytes = SignatureTestUtils.sign(TRUSTED_SIGNATURE_VERIFIER, new ByteArrayInputStream(anotherSampleSkarBytes), TRUSTED_SIGNER_CERT_DNS[0]);
        // make sure this is trusted
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(signedTrustedAnotherSampleSkarBytes),
                null, // don't care about instanceModifier
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
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get("signature");
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get("cert");
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
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        final byte[] signatureBytes = HexUtils.decodeBase64(signatureB64);
                        Assert.assertNotNull(signatureBytes);
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        final byte[] signerCertBytes = HexUtils.decodeBase64(signerCertB64);
                        Assert.assertNotNull(signerCertBytes);
                        // get the trusted signature properties bytes
                        final Properties trustedSigProps = SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes);
                        final String trustedSigB64 = (String) trustedSigProps.get("signature");
                        Assert.assertNotNull(trustedSigB64);
                        final byte[] trustedSigBytes = HexUtils.decodeBase64(trustedSigB64);
                        Assert.assertNotNull(trustedSigBytes);
                        final String trustedSignerCertB64 = (String) trustedSigProps.get("cert");
                        Assert.assertNotNull(trustedSignerCertB64);
                        final byte[] trustedSignerCertBytes = HexUtils.decodeBase64(trustedSignerCertB64);
                        Assert.assertNotNull(trustedSignerCertBytes);
                        // make sure bot signature and signing certs are different
                        Assert.assertFalse(Arrays.equals(signatureBytes, trustedSigBytes));
                        Assert.assertFalse(Arrays.equals(signerCertBytes, trustedSignerCertBytes));

                        // swap signing cert property
                        sigProps.setProperty("cert", HexUtils.encodeBase64(trustedSignerCertBytes));
                        // make sure after the swap the signer cert is different
                        assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));
                        // make sure after the swap the signature is unchanged
                        assertThat(signatureB64, Matchers.equalTo((String) sigProps.get("signature")));

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
    public void setSelectedGuidAndImForHeadlessUpgradeSuccess() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selecting non-parent solution kit for upgrade success
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        solutionKitsConfig = new SolutionKitsConfig();
        Map<String, Pair<String, String>> selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();
        assertEquals(selectedGuidAndImForHeadlessUpgrade.size(), 0);

        //Test for solution kit upgrade child success
        solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(false, "1f87436b-7ca5-41c8-9418-21d7a7848853", solutionKitsConfig, "im1", null );

        //Expect the solution kit is put on the map "selectedGuidAndImForHeadlessUpgrade" for upgrade
        assertEquals(selectedGuidAndImForHeadlessUpgrade.get("1f87436b-7ca5-41c8-9418-21d7a7848853").left, "im1");
        assertEquals(selectedGuidAndImForHeadlessUpgrade.size(),1);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selecting for parent upgrade success, only solution kit with matching instanceModifiers will be added
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        initializeSolutionKits();

        final List<SolutionKit> childKits = new ArrayList<>(2);
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        childKits.add(solutionKit3);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test
        solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im2::newIM", null );

        selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();

        //Expect only the kit with IM "im2" to be selected for upgrade
        assertEquals(selectedGuidAndImForHeadlessUpgrade.size(),1);
        assertTrue(selectedGuidAndImForHeadlessUpgrade.containsKey(solutionKit2.getSolutionKitGuid()));

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selected solution kit for upgrade based on solutionKitSelect
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        solutionKitsConfig = new SolutionKitsConfig();

        //same instance modifiers as solutionKit1
        solutionKit2.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1");
        solutionKit3.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1");

        // only select solutionkit1 with instance modifier im1
        FormDataBodyPart solutionKitSelect = new FormDataBodyPart("solutionKitSelect", solutionKit1.getSolutionKitGuid()+"::im1");
        solutionKitsConfig.setSolutionKitsToUpgrade(childKits);

        //Test, select for only solutionKit1
        solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im1::im2", Collections.singletonList(solutionKitSelect));

        selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();

        //expect only solutionKit1 to be selected
        assertEquals(selectedGuidAndImForHeadlessUpgrade.size(),1);
        assertTrue(selectedGuidAndImForHeadlessUpgrade.containsKey(solutionKit1.getSolutionKitGuid()));

    }

    @Test
    public void setSelectedGuidAndImForHeadlessUpgradeError() throws Exception {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selecting for parent upgrade, same child error
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        //same GUID as solutionKit1
        solutionKit2.setSolutionKitGuid(solutionKit1.getSolutionKitGuid());

        final List<SolutionKit> childKits = new ArrayList<>(2);
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, null, null);
            fail("Same child error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(), "Upgrade failed: at least two child solution kits with a same GUID (" + "1f87436b-7ca5-41c8-9418-21d7a7848999" + ") are selected for upgrade at same time." + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selecting for parent upgrade, no IM error
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        solutionKitsConfig = new SolutionKitsConfig();
        //reset back to original Guid
        solutionKit2.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848988");
        childKits.clear();
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);

        //Test for an invalid IM
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "INVALID_IM", null);
            fail("No child with IM error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(), "Cannot find any to-be-upgraded solution kit(s), which matches the instance modifier (INVALID_IM) specified by the parameter 'instanceModifier'" + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selecting parent upgrade, no parent error
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(null);
        //Test
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, null, null);
            fail("Parent solution kit not found error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(),  "Upgrade failed: cannot find a parent solution kit with GUID,  '" + parentSolutionKit.getSolutionKitGuid() + "'" + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Cannot find selected solutionKit based on solutionKitSelect
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //same instance modifier as solutionKit1
        solutionKit2.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1");

        //select invalid solution kit
        FormDataBodyPart invalidSolutionKitSelect = new FormDataBodyPart("solutionKitSelect", solutionKit1.getSolutionKitGuid()+"INVALID");
        childKits.clear();
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        solutionKitsConfig.setSolutionKitsToUpgrade(childKits);

        //Test, select for an invalid solution kit
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im1", Collections.singletonList(invalidSolutionKitSelect));
            fail("Invalid selected solution kit error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(),
                    "Cannot find any to-be-upgraded solution kit, whose GUID matches to the given GUID (" +
                    solutionKit1.getSolutionKitGuid()+"INVALID) specified from the parameter 'solutionKitSelect'" + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // duplicate GUID error based on solutionKitSelects
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //same instance modifier as solutionKit1 and 2
        solutionKit3.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1");

        childKits.add(solutionKit3);

        //select solution kits with duplicate guid
        invalidSolutionKitSelect = new FormDataBodyPart("solutionKitSelect", solutionKit2.getSolutionKitGuid());
        FormDataBodyPart invalidSolutionKitSelect2 = new FormDataBodyPart("solutionKitSelect", solutionKit2.getSolutionKitGuid());
        List<FormDataBodyPart> invalidSolutionKitSelects = new ArrayList<>();
        invalidSolutionKitSelects.add(invalidSolutionKitSelect);
        invalidSolutionKitSelects.add(invalidSolutionKitSelect2);

        solutionKitsConfig.setSolutionKitsToUpgrade(childKits);

        //Test, select for kits with a duplicate guid
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im1", invalidSolutionKitSelects);
            fail("Duplicate solution kit selected should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
           assertEquals(e.getResponse().getEntity().toString(),
                    "Upgrade failed: at least two 'solutionKitSelect' parameters specify a same GUID (" +
                            solutionKit2.getSolutionKitGuid() + "), since two solution kit instances cannot be upgraded at the same time." + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // select for solution kit with invalid instance modifier
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //select solution kits invalid instance modifier
        invalidSolutionKitSelect = new FormDataBodyPart("solutionKitSelect", solutionKit3.getSolutionKitGuid()+"::INVALID_IM");

        //Test, select for kit with invalid instance modifier
        try {
            solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im1", Collections.singletonList(invalidSolutionKitSelect));
            fail("Invalid selected instance modifier error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(),
                    "Cannot find any to-be-upgraded solution kit, which matches the given GUID (" + solutionKit3.getSolutionKitGuid() +
                            ") and the given Instance Modifier (INVALID_IM)" + System.lineSeparator());
        }
    }

    @Test
    public void updateSolutionKitsToUpgradeBasedOnGivenParametersSuccess() throws Exception{
        initializeSolutionKits();

        final List<SolutionKit> childKits = new ArrayList<>(3);
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        childKits.add(solutionKit3);

        solutionKitsConfig.setSolutionKitsToUpgrade(childKits);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Assume the list to be unchanged before test
        solutionKitsConfig.setSolutionKitsToUpgrade(childKits);
        assertEquals(solutionKitsConfig.getSolutionKitsToUpgrade().size(),3);

        //Test
        solutionKitResource.setSelectedGuidAndImForHeadlessUpgrade(true, parentSolutionKit.getSolutionKitGuid(), solutionKitsConfig, "im2", null );
        solutionKitResource.updateSolutionKitsToUpgradeBasedOnGivenParameters(solutionKitsConfig);

        //Expect the solutionKitsConfig solution Kits upgrade list to be updated with solutionKit2
        assertEquals(solutionKitsConfig.getSolutionKitsToUpgrade().size(), 1);
        assertEquals(solutionKitsConfig.getSolutionKitsToUpgrade().get(0).getSolutionKitGuid(), solutionKit2.getSolutionKitGuid());
    }

    @Test
    public void uninstallSuccess() throws Exception {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate single solution kit uninstall based on GUID and IM
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);

        Collection<SolutionKit> solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 3);

        when(solutionKitManager.findBySolutionKitGuid(solutionKit1.getSolutionKitGuid())).thenReturn(Collections.singletonList(solutionKit1));
        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit1.getSolutionKitGuid(), "im1")).thenReturn(solutionKit1);

        //Test
        Response resultResponse = solutionKitResource.uninstall(solutionKit1.getSolutionKitGuid()+ "::im1", null);

        //Expect solutionKit1 to be uninstalled
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 2);
        assertFalse(solutionKitsInManager.contains(solutionKit1));
        assertTrue(solutionKitsInManager.contains(solutionKit2));
        assertTrue(solutionKitsInManager.contains(solutionKit3));

        //no content response for perfect uninstallation
        assertEquals(resultResponse.getStatus(), 204);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate parent and all children are uninstalled successfully
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        final List<SolutionKit> childKits = new ArrayList<>(3);
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        childKits.add(solutionKit3);

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(parentSolutionKit, solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test uninstall parent kit
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),null);

        //expect all children and parent to be deleted
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 0);

        //no content response for perfect uninstallation
        assertEquals(resultResponse.getStatus(), 204);


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selected children are uninstalled
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(parentSolutionKit, solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        //Children to uninstall
        List<String> childrenToUninstall = new ArrayList<>();
        childrenToUninstall.add(solutionKit1.getSolutionKitGuid()+"::im1");
        childrenToUninstall.add(solutionKit2.getSolutionKitGuid()+"::im2");

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);
        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit1.getSolutionKitGuid(), "im1")).thenReturn(solutionKit1);
        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit2.getSolutionKitGuid(), "im2")).thenReturn(solutionKit2);

        //Test uninstall parent kit with specified child for uninstall
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),childrenToUninstall);

        //expect only parent kit and solutionkit3 to remain
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 2);
        assertTrue(solutionKitsInManager.contains(solutionKit3));
        assertTrue(solutionKitsInManager.contains(parentSolutionKit));

        //expect response to show which solution kits are successfully uninstalled
        assertEquals(resultResponse.getStatus(), 204);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selected children are uninstalled based on global instance modifier
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();
        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(parentSolutionKit, solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test uninstall all children with the instance modifier "im2", which is solutionKit2
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid() + "::im2", null);

        //expect only solutionKit2 to be removed
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 3);
        assertTrue(solutionKitsInManager.contains(solutionKit3));
        assertTrue(solutionKitsInManager.contains(parentSolutionKit));
        assertTrue(solutionKitsInManager.contains(solutionKit1));
        assertFalse(solutionKitsInManager.contains(solutionKit2));

        //expect response to show that solution kit 2 is successfully uninstalled
        assertEquals( 204,
                resultResponse.getStatus());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //simulate selected children without im are uninstalled with empty im
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        //change the im to null
        solutionKit2.setProperty(SK_PROP_INSTANCE_MODIFIER_KEY, "");

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(parentSolutionKit, solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        childKits.clear();
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        childKits.add(solutionKit3);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test uninstall all children with the instance modifier "", which is solutionKit2
        resultResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid() + PARAMETER_DELIMINATOR, null);

        //expect only solutionKit2 to be removed
        solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 3);
        assertTrue(solutionKitsInManager.contains(solutionKit3));
        assertTrue(solutionKitsInManager.contains(parentSolutionKit));
        assertTrue(solutionKitsInManager.contains(solutionKit1));
        assertFalse(solutionKitsInManager.contains(solutionKit2));

        //expect response to show that solution kit 2 is successfully uninstalled with no im specified
        assertEquals(204, resultResponse.getStatus());
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
        // cannot find solution kit GUID and IM
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        when(solutionKitManager.findBySolutionKitGuid(solutionKit1.getSolutionKitGuid())).thenReturn(Collections.singletonList(solutionKit1));

        //Test
        errorResponse = solutionKitResource.uninstall(solutionKit1.getSolutionKitGuid()+"::INVALID_IM", null);

        //expect solution kit does not exist error
        assertEquals(errorResponse.getEntity(), "Uninstall failed: cannot find any existing solution kit (GUID = '" + solutionKit1.getSolutionKitGuid() +
        "',  Instance Modifier = 'INVALID_IM') for uninstall." + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // no child with matching guid
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        final List<SolutionKit> childKits = new ArrayList<>(3);
        childKits.add(solutionKit1);
        childKits.add(solutionKit2);
        childKits.add(solutionKit3);

        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(parentSolutionKit, solutionKit1, solutionKit2, solutionKit3));
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        Collection<SolutionKit> solutionKitsInManager = solutionKitManager.findAll();
        assertEquals(solutionKitsInManager.size(), 4);

        when(solutionKitManager.findBySolutionKitGuid(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singletonList(parentSolutionKit));
        when(solutionKitManager.findAllChildrenByParentGoid(parentSolutionKit.getGoid())).thenReturn(childKits);

        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),Collections.singletonList("NO_MATCH_GUID_1f87436b-7ca5-41c8-9418-21d7a7855555"));

        //expect child kit selected to uninstall does not match any children from parent
        assertEquals(errorResponse.getEntity(), "UNINSTALL ERRORS:" + System.lineSeparator() + "Uninstall failed: Cannot find any child solution kit matching the GUID 'NO_MATCH_GUID_1f87436b-7ca5-41c8-9418-21d7a7855555'" + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // no child with matching guid and IM
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),Collections.singletonList(solutionKit1.getSolutionKitGuid()+"::INVALID_IM"));

        //expect child kit with IM selected does not match existing child kits
        assertEquals(errorResponse.getEntity(),"UNINSTALL ERRORS:" + System.lineSeparator() + "Uninstall failed: cannot find any existing solution kit (GUID = '" + solutionKit1.getSolutionKitGuid() +
                "',  Instance Modifier = 'INVALID_IM') for uninstall." + System.lineSeparator());

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // when child kits are selected, show which child kits uninstalled successfully, and which have errors
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //Children to uninstall
        List<String> childrenToUninstall = new ArrayList<>();
        childrenToUninstall.add(solutionKit1.getSolutionKitGuid()+"::INVALID_IM");
        childrenToUninstall.add(solutionKit2.getSolutionKitGuid()+"::im2");

        when(solutionKitManager.findBySolutionKitGuidAndIM(solutionKit2.getSolutionKitGuid(), "im2")).thenReturn(solutionKit2);

        //Test
        errorResponse = solutionKitResource.uninstall(parentSolutionKit.getSolutionKitGuid(),childrenToUninstall);

        //expect solutionKit1 uninstallation to fail with error, and solutionKit2 to be successfully uninstalled
        assertEquals(errorResponse.getEntity(),"Uninstalled solution kits:" + System.lineSeparator() +
                "Successfully uninstalled child solution kit with guid: '1f87436b-7ca5-41c8-9418-21d7a7848988' and instance modifier: 'im2'" + System.lineSeparator() +
                System.lineSeparator() +
                "Total Solution Kits deleted: 1" + System.lineSeparator() +
                System.lineSeparator() +
                "Solution kits selected for uninstall that failed:" + System.lineSeparator() +
                "Uninstall failed: cannot find any existing solution kit (GUID = '1f87436b-7ca5-41c8-9418-21d7a7848999',  Instance Modifier = 'INVALID_IM') for uninstall." + System.lineSeparator());
    }

    @Test
    public void selectSolutionKitsForInstallSuccess() throws Exception{
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //install all solution kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        initializeSolutionKits();

        Map<SolutionKit, Bundle> loaded = new HashMap<>();
        loaded.put(solutionKit1, null);
        loaded.put(solutionKit2, null);
        loaded.put(solutionKit3, null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);

        //test Install solution kits with IM "global IM"
        solutionKitResource.selectSolutionKitsForInstall(solutionKitsConfig, "global IM", null);

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
        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKit1.getSolutionKitGuid()+"::im1");
        FormDataBodyPart solutionKitSelect2 = new FormDataBodyPart("solutionKitSelect", solutionKit2.getSolutionKitGuid());
        List<FormDataBodyPart> solutionKitSelects = new ArrayList<>();
        solutionKitSelects.add(solutionKitSelect1);
        solutionKitSelects.add(solutionKitSelect2);

        //Only Install
        solutionKitResource.selectSolutionKitsForInstall(solutionKitsConfig, "global IM", solutionKitSelects);

        //Expect only solutionKit 1 and 2 to be added
        selected = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(selected.size(), 2);
        for (SolutionKit solutionKit : selected) {
            assertTrue(solutionKit.getSolutionKitGuid().equals(solutionKit1.getSolutionKitGuid()) ||
            solutionKit.getSolutionKitGuid().equals(solutionKit2.getSolutionKitGuid()));
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
        loaded.put(solutionKit2, null);
        loaded.put(solutionKit3, null);

        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loaded);

        FormDataBodyPart solutionKitSelect1 = new FormDataBodyPart("solutionKitSelect", solutionKit1.getSolutionKitGuid()+"::im1");

        //test try to Install solution kits with IM "global IM" with no solution kits loaded
        try {
            solutionKitResource.selectSolutionKitsForInstall(solutionKitsConfig, "global IM", Collections.singletonList(solutionKitSelect1));
            fail("Solution kit to install not found in skar error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            assertEquals(e.getResponse().getEntity().toString(), "Solution Kit ID to install: " +
                    solutionKit1.getSolutionKitGuid() + " not found in the skar." + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // selected solution kits are empty
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // this is supposed to test for lines 824-826 in SolutionKitManagerResource, however I think it's impossible to hit these lines because there is an earlier
        // condition for empty selected SKs. Please Review
    }

    @Test
    public void selectSolutionKitsForUpgradeSuccess() throws Exception {
        initializeSolutionKits();
        Map<String, Pair<String, String>> selectedGuidAndIm = new HashMap<>();

        //Selected solutionKit1 and 2 for upgrade
        selectedGuidAndIm.put(solutionKit1.getSolutionKitGuid(), new Pair<>("im1", "newIM"));
        selectedGuidAndIm.put(solutionKit2.getSolutionKitGuid(), new Pair<>("im2", "newIM"));
        when(solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade()).thenReturn(selectedGuidAndIm);

        //Loaded all three solution kits
        Map<SolutionKit, Bundle> loadedSolutionKits = new HashMap<>();
        loadedSolutionKits.put(solutionKit1, null);
        loadedSolutionKits.put(solutionKit2, null);
        loadedSolutionKits.put(solutionKit3, null);
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        //Test
        solutionKitResource.selectSolutionKitsForUpgrade(solutionKitsConfig);

        //Expect the 2 solution kits selected to be upgraded from the ones already loaded
        Set<SolutionKit> updateSelectedSK = solutionKitsConfig.getSelectedSolutionKits();
        assertEquals(updateSelectedSK.size(), 2);

        //Expect all two solution kits to be in the set of updated solution Kit
        assertTrue(updateSelectedSK.contains(solutionKit1));
        assertTrue(updateSelectedSK.contains(solutionKit2));

        //Expect that the instance modifier is updated to "newIM"
        assertEquals(solutionKit1.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY), "newIM");
        assertEquals(solutionKit2.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY), "newIM");

    }

    @Test
    public void selectSolutionKitsForUpgradeFail() throws Exception {
        initializeSolutionKits();

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // selected Guid and IM are empty
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        try {
            //test
            solutionKitResource.selectSolutionKitsForUpgrade(solutionKitsConfig);
            fail("Empty selected Guid and IM error should be thrown");
        } catch (IllegalArgumentException e) {
            //expect
            assertEquals(e.getMessage(), "A map of guid and instance modifier for selected to-be-upgraded solution kits has not been initialized.");
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // selected Guid and IM are not in the set of loaded solution Kits
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //Selected solutionKit1
        Map<String, Pair<String, String>> selectedGuidAndIm = new HashMap<>();
        selectedGuidAndIm.put(solutionKit1.getSolutionKitGuid(), new Pair<>("im1", "newIM"));
        when(solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade()).thenReturn(selectedGuidAndIm);

        //Loaded solutionKit2 and 3
        Map<SolutionKit, Bundle> loadedSolutionKits = new HashMap<>();
        loadedSolutionKits.put(solutionKit2, null);
        loadedSolutionKits.put(solutionKit3, null);
        when(solutionKitsConfig.getLoadedSolutionKits()).thenReturn(loadedSolutionKits);

        try {
            //test
            solutionKitResource.selectSolutionKitsForUpgrade(solutionKitsConfig);
            fail("Selected Guid and IM not in loaded SKs error should be thrown");
        } catch (SolutionKitManagerResource.SolutionKitManagerResourceException e) {
            //expect
            assertEquals(e.getResponse().getEntity(), "Solution Kit ID to upgrade: " +
                    solutionKit1.getSolutionKitGuid() + " not found in the skar." + System.lineSeparator());
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // selected SKs are empty
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // this is supposed to test for lines 882-883 in SolutionKitManagerResource, however I think it's impossible to hit these lines because there is an earlier
        // condition that checks for empty selected Guid and IM. Please Review
    }

    private static InputStream[] creteSignedSampleChildScars(
            final int numChildren,
            final SignatureVerifierServer signer,
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
            @Nullable final SignatureVerifierServer signer,
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
        Goid parentGoidSame = new Goid("1f87436b7ca541c8941821d7a7848111");

        parentSolutionKit = new SolutionKit();
        parentSolutionKit.setGoid(parentGoidSame);
        parentSolutionKit.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848853");
        parentSolutionKit.setProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "true");
        parentSolutionKit.setMappings("");

        solutionKit1 = new SolutionKit();
        solutionKit1.setParentGoid(parentGoidSame);
        solutionKit1.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848999");
        solutionKit1.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im1");
        solutionKit1.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 1");
        solutionKit1.setName("SolutionKit1");
        solutionKit1.setSolutionKitVersion("1");
        solutionKit1.setGoid(new Goid("1f87436b7ca541c8941821d7a7848999"));

        solutionKit2 = new SolutionKit();
        solutionKit2.setParentGoid(parentGoidSame);
        solutionKit2.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848988");
        solutionKit2.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im2");
        solutionKit2.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 2");
        solutionKit2.setName("SolutionKit2");
        solutionKit2.setSolutionKitVersion("1");
        solutionKit2.setGoid(new Goid("1f87436b7ca541c8941821d7a7848988"));

        solutionKit3 = new SolutionKit();
        solutionKit3.setParentGoid(parentGoidSame);
        solutionKit3.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a7848977");
        solutionKit3.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "im3");
        solutionKit3.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Description 3");
        solutionKit3.setName("SolutionKit3");
        solutionKit3.setSolutionKitVersion("1");
        solutionKit3.setGoid(new Goid("1f87436b7ca541c8941821d7a7848977"));
    }
}
