package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.server.solutionkit.SolutionKitManagerStub;
import com.l7tech.util.*;
import org.apache.commons.lang.CharEncoding;
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

import static com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.SolutionKitManagerResource.ID_DELIMINATOR;
import static org.junit.Assert.assertEquals;

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

    private static SignatureVerifier TRUSTED_SIGNATURE_VERIFIER;
    private static final String[] TRUSTED_SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com"
    };

    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.api.solutionkit.SkarProcessor#SK_FILENAME}
     */
    private static final String SK_FILENAME = "SolutionKit.xml";
    /**
     * IMPORTANT: Keep the values as per {@link com.l7tech.gateway.common.api.solutionkit.SkarProcessor#SK_INSTALL_BUNDLE_FILENAME}
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
    @Spy
    private SolutionKitManager solutionKitManager = new SolutionKitManagerStub();

    private SolutionKitManagerResource solutionKitResource;

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
        }).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());

        // create our SolutionKitManagerResource
        solutionKitResource = new SolutionKitManagerResource();
        solutionKitResource.setSolutionKitManager(solutionKitManager);
        solutionKitResource.setLicenseManager(licenseManager);
        solutionKitResource.setSignatureVerifier(TRUSTED_SIGNATURE_VERIFIER);
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
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + ID_DELIMINATOR + entityIdNew, entityIdReplaceMap);
        assertEquals(entityIdNew, entityIdReplaceMap.get(entityIdOld));

        entityIdOld = "0567c6a8f0c4cc2c9fb331cb03b4de6f";
        entityIdNew = "1e3299eab93e2935adafbf35860fc8d9";
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + URLEncoder.encode(ID_DELIMINATOR, CharEncoding.UTF_8) + entityIdNew, entityIdReplaceMap);
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
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Invalid signed Zip"));

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
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // should fail with BAD_REQUEST and error message containing "Invalid signed Zip"
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Invalid signed Zip"));
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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    new FormDataMultiPart()
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));

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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    new FormDataMultiPart()
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));

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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR is signed so it should succeed
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Missing required file SolutionKit.xml"));
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
        final SignatureVerifier untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));

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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));

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
                    null, // don't care about solutionKitSelects
                    null, // don't care about entityIdReplaces
                    null, // don't care about upgradeGuid
                    null // don't care about other form data
            );
            // SKAR signer is not trusted so it should fail with BAD_REQUEST
            Assert.assertNotNull(response);
            Assert.assertNotNull(response.getStatusInfo());
            logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
            Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
            Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
            Assert.assertThat((String) response.getEntity(), Matchers.containsString("Failed to verify signer certificate"));
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
        final SignatureVerifier untrustedSigner = SignatureTestUtils.createSignatureVerifier(untrustedDNs);

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
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);
                        return Pair.pair(SignatureTestUtils.flipRandomByte(dataBytes), sigProps);
                    }
                }
        );
        // test
        Response response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
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
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signature property
                        final String signatureB64 = (String) sigProps.get("signature");
                        Assert.assertNotNull(signatureB64);
                        // flip random byte
                        final byte[] modSignBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signatureB64));
                        // store modified signature
                        sigProps.setProperty("signature", HexUtils.encodeBase64(modSignBytes));
                        Assert.assertThat(signatureB64, Matchers.not(Matchers.equalTo((String) sigProps.get("signature"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.either(Matchers.containsString("Signature not verified")).or(Matchers.containsString("Could not verify signature")));
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
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
                        Assert.assertNotNull(sigProps);

                        // read the signer cert property
                        final String signerCertB64 = (String) sigProps.get("cert");
                        Assert.assertNotNull(signerCertB64);
                        // flip random byte
                        final byte[] modSignerCertBytes = SignatureTestUtils.flipRandomByte(HexUtils.decodeBase64(signerCertB64));
                        // store modified signature
                        sigProps.setProperty("cert", HexUtils.encodeBase64(modSignerCertBytes));
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat(
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
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                new FormDataMultiPart()
        );
        // SKAR is signed so it should succeed
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.OK.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Request completed successfully"));


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // sign our first sample using untrusted signer and swap the signature from signedTrustedAnotherSampleSkarBytes
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertThat(untrustedDNs[0], Matchers.equalTo(TRUSTED_SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
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
        Assert.assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedSkarBytes), Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes)));
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // same as above but instead of swapping the entire signature properties bytes, swap only the signer cert and leave signature unchanged
        // sign our first sample using untrusted signer and swap the signing cert from signedTrustedAnotherSampleSkarBytes
        Assert.assertThat(untrustedDNs[0], Matchers.equalTo(TRUSTED_SIGNER_CERT_DNS[0])); // make sure same DN is used (simply to look more legit)
        tamperedSignedSkarBytes = SignatureTestUtils.signAndTamperWithContent(
                new ByteArrayInputStream(sampleSkarBytes),
                untrustedSigner,
                untrustedDNs[0],
                new Functions.BinaryThrows<Pair<byte[], Properties>, byte[], Properties, Exception>() {
                    @Override
                    public Pair<byte[], Properties> call(final byte[] dataBytes, final Properties sigProps) throws Exception {
                        Assert.assertNotNull(dataBytes);
                        Assert.assertThat(dataBytes.length, Matchers.greaterThan(0));
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
                        Assert.assertThat(signerCertB64, Matchers.not(Matchers.equalTo((String) sigProps.get("cert"))));
                        // make sure after the swap the signature is unchanged
                        Assert.assertThat(signatureB64, Matchers.equalTo((String) sigProps.get("signature")));

                        // return a pair of unchanged data-bytes and modified signature props
                        return Pair.pair(dataBytes, sigProps);
                    }
                }
        );
        // verify that the tamperedSignedSkarBytes signature properties raw-bytes differ than signedTrustedAnotherSampleSkarBytes
        Assert.assertThat(SignatureTestUtils.getSignatureProperties(tamperedSignedSkarBytes), Matchers.not(Matchers.equalTo(SignatureTestUtils.getSignatureProperties(signedTrustedAnotherSampleSkarBytes))));
        // test
        response = solutionKitResource.installOrUpgrade(
                new ByteArrayInputStream(tamperedSignedSkarBytes),
                null, // don't care about solutionKitSelects
                null, // don't care about entityIdReplaces
                null, // don't care about upgradeGuid
                null // don't care about other form data
        );
        // SKAR tampered with so it should fail with BAD_REQUEST
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getStatusInfo());
        logger.log(Level.INFO, "installOrUpgrade:" + System.lineSeparator() + "response: " + response + System.lineSeparator() + "entity: " + response.getEntity());
        Assert.assertThat(response.getStatus(), Matchers.is(Response.Status.BAD_REQUEST.getStatusCode()));
        Assert.assertThat(response.getEntity(), Matchers.instanceOf(String.class));
        Assert.assertThat((String) response.getEntity(), Matchers.containsString("Signature not verified"));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private static InputStream[] creteSignedSampleChildScars(
            final int numChildren,
            final SignatureVerifier signer,
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
            @Nullable final SignatureVerifier signer,
            @Nullable final String signerDn
    ) throws Exception {
        Assert.assertThat(numChildren, Matchers.greaterThanOrEqualTo(1));
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
        Assert.assertThat(metaXml, Matchers.not(Matchers.isEmptyOrNullString()));
        final boolean isSkarOfSkars = childSkars != null && childSkars.length > 0;
        if (!isSkarOfSkars) {
            // if not skar-of-skars then install bundle is mandatory
            Assert.assertThat(installBundleXml, Matchers.not(Matchers.isEmptyOrNullString()));
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

    // TODO more tests
}
