package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.security.signer.SignatureTestUtils;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.server.security.signer.SignatureVerifierStub;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.Nullable;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServerModuleFileEntityResourceTest extends RestEntityTests<ServerModuleFile, ServerModuleFileMO> {
    private static final Logger logger = Logger.getLogger(ServerModuleFileEntityResourceTest.class.getName());

    private static final String UPLOAD_ENABLED_SYS_PROP = "com.l7tech.server." + ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE;

    private Config config;
    private ServerModuleFileManager serverModuleFileManager;
    private List<ServerModuleFile> serverModuleFiles = new ArrayList<>();
    private Boolean uploadEnabledOverride = null;
    // read only set of initial modules SHA256
    private Set<String> moduleShas;

    // our signer utils object
    private static SignatureVerifier trustedSignatureVerifier;
    private static final String[] SIGNER_CERT_DNS = {
            "cn=signer.team1.apim.ca.com",
            "cn=signer.team2.apim.ca.com",
            "cn=signer.team3.apim.ca.com",
            "cn=signer.team4.apim.ca.com"
    };
    // untrusted signers
    private static SignatureVerifier untrustedSignatureVerifier;
    private static final String[] untrustedSignerCertDns = new String[] {"cn=untrusted.signer1.ca.com", "cn=untrusted.signer1.ca.com"};

    @BeforeClass
    public static void beforeClass() throws Exception {
        RestEntityTests.beforeClass();

        SignatureTestUtils.beforeClass();
        trustedSignatureVerifier = SignatureTestUtils.createSignatureVerifier(SIGNER_CERT_DNS);
        untrustedSignatureVerifier = SignatureTestUtils.createSignatureVerifier(ArrayUtils.concat(SIGNER_CERT_DNS, untrustedSignerCertDns));

        // to make this test valid modules upload should be enabled by default
        SyspropUtil.setProperty(UPLOAD_ENABLED_SYS_PROP, String.valueOf(true));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SignatureTestUtils.afterClass();
    }

    @Before
    public void before() throws Exception {
        uploadEnabledOverride = null;

        // change the default (stub) signature verifier with our own
        final SignatureVerifierStub signatureVerifierStub = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("signatureVerifier", SignatureVerifierStub.class);
        Assert.assertNotNull(signatureVerifierStub);
        signatureVerifierStub.setProxyVerifier(trustedSignatureVerifier);

        serverModuleFileManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serverModuleFileManager", ServerModuleFileManager.class);
        Assert.assertThat(serverModuleFileManager, Matchers.notNullValue());
        config = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serverConfig", ServerConfig.class);
        Assert.assertThat(config, Matchers.notNullValue());

        int ordinal = 0;

        // making sure module sha256 are generated uniquely
        final Set<String> moduleShas = new HashSet<>();

        // regular ServerModuleFile
        byte[] bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        ServerModuleFile serverModuleFile = createTestServerModuleFile(
                "module " + ++ordinal,
                ModuleType.MODULAR_ASSERTION,
                Collections.unmodifiableCollection(CollectionUtils.list(
                        Pair.pair("node1", Either.<ModuleState, String>left(ModuleState.LOADED)),
                        Pair.pair("node2", Either.<ModuleState, String>right("test error 1"))
                )),
                bytes,
                SIGNER_CERT_DNS[0],
                Collections.unmodifiableCollection(CollectionUtils.list(
                        Pair.pair(ServerModuleFile.PROP_FILE_NAME, "FileName" + ordinal + ".jar"),
                        Pair.pair(ServerModuleFile.PROP_ASSERTIONS, "Assertion1"),
                        Pair.pair(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                ))
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        // another regular ServerModuleFile
        bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFile = createTestServerModuleFile(
                "module " + ++ordinal,
                ModuleType.CUSTOM_ASSERTION,
                Collections.unmodifiableCollection(CollectionUtils.list(
                        Pair.pair("node1", Either.<ModuleState, String>left(ModuleState.LOADED)),
                        Pair.pair("node2", Either.<ModuleState, String>right("test error 1"))
                )),
                bytes,
                SIGNER_CERT_DNS[1],
                Collections.unmodifiableCollection(CollectionUtils.list(
                        Pair.pair(ServerModuleFile.PROP_FILE_NAME, "FileName" + ordinal + ".jar"),
                        Pair.pair(ServerModuleFile.PROP_ASSERTIONS, "Assertion1"),
                        Pair.pair(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                ))
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        // ServerModuleFile without props
        bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        //noinspection unchecked
        serverModuleFile = createTestServerModuleFile(
                "module " + ++ordinal,
                ModuleType.MODULAR_ASSERTION,
                (Collection)Collections.emptyList(),
                bytes,
                SIGNER_CERT_DNS[2],
                (Collection)Collections.emptyList()
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        this.moduleShas = Collections.unmodifiableSet(moduleShas);
    }

    /**
     * Utility method for creating sample {@code ServerModuleFile} with the specified properties.
     *
     * @param name          the name of the module.
     * @param type          the type of the module.
     * @param states        the list of states per node of the module. A state is a {@code Pair} of node and {@code Either} state or error message.
     * @param bytes         the module data bytes.
     * @param signerDN      the module signer cert DN.
     * @param properties    the module collection of properties.
     * @return a sample {@code ServerModuleFile} with the specified properties, never {@code null}.
     */
    private static ServerModuleFile createTestServerModuleFile(
            final String name,
            final ModuleType type,
            final Collection<
                    Pair<   // state; a pair of node and either state-enum or error message
                            String,   // node id
                            Either<   // either state or error message
                                    ModuleState,  // state
                                    String        // error message
                                    >
                            >
                    > states,
            final byte[] bytes,
            final String signerDN,
            final Collection<Pair<String, String>> properties
    ) throws Exception {
        final ServerModuleFile serverModuleFile = new ServerModuleFile();
        serverModuleFile.setName(name);
        serverModuleFile.setModuleType(type);
        for (final Pair<String, Either<ModuleState, String>> state : states) {
            if (state.right.isLeft()) {
                serverModuleFile.setStateForNode(state.left, state.right.left());
            } else {
                serverModuleFile.setStateErrorMessageForNode(state.left, state.right.right());
            }
        }
        serverModuleFile.createData(bytes, trustedSignAndGetSignature(bytes, signerDN));
        for (final Pair<String, String> property : properties) {
            serverModuleFile.setProperty(property.left, property.right);
        }
        return serverModuleFile;
    }

    @After
    public void after() throws FindException, DeleteException {
        // reset ServerModuleFile uploadEnabled property
        if (uploadEnabledOverride != null) {
            SyspropUtil.setProperty(UPLOAD_ENABLED_SYS_PROP, String.valueOf(uploadEnabledOverride));
            uploadEnabledOverride = null;
        }

        Collection<ServerModuleFile> all = serverModuleFileManager.findAll();
        for (final ServerModuleFile serverModuleFile : all) {
            serverModuleFileManager.delete(serverModuleFile.getGoid());
        }
    }

    /**
     * Utility method for signing the specified content byte array and getting the signature in one step.
     */
    private static String signAndGetSignature(final SignatureVerifier verifier, final byte[] content, final String signerCertDn) {
        Assert.assertThat(content, Matchers.notNullValue());
        Assert.assertThat(signerCertDn, Matchers.not(Matchers.isEmptyOrNullString()));
        try {
            return SignatureTestUtils.signAndGetSignature(verifier, content, signerCertDn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String trustedSignAndGetSignature(final byte[] content, final String signerCertDn) {
        return signAndGetSignature(trustedSignatureVerifier, content, signerCertDn);
    }

    private static String untrustedSignAndGetSignature(final byte[] content, final String signerCertDn) {
        return signAndGetSignature(untrustedSignatureVerifier, content, signerCertDn);
    }

    /**
     * Utility method for signing the specified content byte array and getting the signature in one step.
     */
    private static Map<String, String> getSignatureMap(final String signatureProps) {
        try {
            return signatureProps != null
                    ? ServerModuleFileTransformer.gatherSignatureProperties(signatureProps, SignerUtils.ALL_SIGNING_PROPERTIES)
                    : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() throws FindException {
        return Functions.map(serverModuleFiles, new Functions.Unary<String, ServerModuleFile>() {
            @Override
            public String call(final ServerModuleFile serverModuleFile) {
                Assert.assertThat(serverModuleFile, Matchers.notNullValue());
                return serverModuleFile.getId();
            }
        });
    }

    /**
     * Utility method for creating sample {@code ServerModuleFileMO} ({@code ServerModuleFile} managed object) with the specified properties.
     *
     * @param id            module id
     * @param name          module name
     * @param type          module type
     * @param bytes         module raw bytes
     * @param signature     module signature properties string
     * @param properties    module collection of properties
     * @return a sample {@code ServerModuleFileMO} with the specified properties, never {@code null}.
     */
    private static ServerModuleFileMO createTestServerModuleFileMO(
            @Nullable final String id,
            @Nullable final String name,
            @Nullable final ServerModuleFileMO.ServerModuleFileModuleType type,
            @Nullable final byte[] bytes,
            @Nullable final String signature,
            @Nullable final Map<String, String> properties
    ) {
        final ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        if (id != null) serverModuleFileMO.setId(id);
        if (name != null) serverModuleFileMO.setName(name);
        if (type != null) serverModuleFileMO.setModuleType(type);
        if (bytes != null) {
            serverModuleFileMO.setModuleData(bytes);
            serverModuleFileMO.setModuleSha256(ModuleDigest.hexEncodedDigest(bytes));
        }
        if (signature != null) serverModuleFileMO.setSignatureProperties(getSignatureMap(signature));
        if (properties != null) serverModuleFileMO.setProperties(properties);
        return serverModuleFileMO;
    }

    @Override
    public List<ServerModuleFileMO> getCreatableManagedObjects() {
        final List<ServerModuleFileMO> serverModuleFileMOs = new ArrayList<>();
        int ordinal = 0;

        // this is a regular ServerModuleFile
        byte[] bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        ServerModuleFileMO serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with null props
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[1]),
                null
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with empty props
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[2]),
                CollectionUtils.MapBuilder.<String, String>builder().unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_FILE_NAME
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[3]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_ASSERTIONS
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_SIZE
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_FILE_NAME
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_ASSERTIONS
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_SIZE
        bytes = ("test module data " + ++ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                getGoid().toString(),
                "CreatedServerModuleFile" + ordinal,
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        Assert.assertThat(ordinal, Matchers.is(serverModuleFileMOs.size()));

        return serverModuleFileMOs;
    }

    @Override
    public List<ServerModuleFileMO> getUpdateableManagedObjects() {
        // update is not supported for ServerModuleFile, at least for now
        // todo when ServerModuleFile update is supported add necessary tests here

        return Collections.emptyList();
    }

    @Override
    public Map<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>> getUnCreatableManagedObjects() {
        Map<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>> map = new LinkedHashMap<>();

        // make sure module 0 exists
        Assert.assertThat(serverModuleFiles.get(0), Matchers.notNullValue());

        // existing name
        byte[] bytes = ("test data for test module 1").getBytes(Charsets.UTF8);
        ServerModuleFileMO serverModuleFileMO = createTestServerModuleFileMO(
                null,
                serverModuleFiles.get(0).getName(),
                ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // existing module sha256
        bytes = serverModuleFiles.get(1).getData().getDataBytes();
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 1",
                ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(1).getModuleSha256());
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // missing module data
        bytes = serverModuleFiles.get(1).getData().getDataBytes();
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 2",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                null,  // missing bytes
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(1).getModuleSha256());
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // missing module sha256
        bytes = serverModuleFiles.get(1).getData().getDataBytes();
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 3",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        serverModuleFileMO.setModuleSha256(null);
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // missing module sha256 and module data
        bytes = serverModuleFiles.get(1).getData().getDataBytes();
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 4",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                null,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 0 and module 1 have different shas
        Assert.assertThat(serverModuleFiles.get(0).getModuleSha256(), Matchers.not(Matchers.equalTo(serverModuleFiles.get(1).getModuleSha256())));

        // module data mismatch sha256
        bytes = serverModuleFiles.get(1).getData().getDataBytes();
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 5",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(0).getModuleSha256());
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        final long maxSize = config.getLongProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE, 0L);
        Assert.assertThat(maxSize, Matchers.greaterThanOrEqualTo(0L));

        // module data exceeding allowed size
        if (maxSize > 0) {
            bytes = new byte[(int) maxSize + 2];
            serverModuleFileMO = createTestServerModuleFileMO(
                    null,
                    "another test module 6",
                    ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                    bytes,
                    trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                    null
            );
            Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
            // should fail with 400
            map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
                @Override
                public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                    Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
                }
            });
        }

        // missing module signature / unsigned module
        bytes = ("test data for test module 7").getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 7",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                null,
                null
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // module data tampered with 1
        bytes = ("test data for test module 8").getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 8",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        byte[] tamperedBytes = Arrays.copyOf(bytes, bytes.length);
        Assert.assertThat(tamperedBytes[3], Matchers.not(Matchers.is((byte)2)));
        tamperedBytes[3] = 2;
        serverModuleFileMO.setModuleData(tamperedBytes);
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // module data tampered with 2
        bytes = ("test data for test module 9").getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 9",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                trustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        tamperedBytes = Arrays.copyOf(bytes, bytes.length);
        Assert.assertThat(tamperedBytes[3], Matchers.not(Matchers.is((byte)2)));
        tamperedBytes[3] = 2;
        serverModuleFileMO.setModuleSha256(ModuleDigest.hexEncodedDigest(tamperedBytes));
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        serverModuleFileMO.setModuleData(tamperedBytes);
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // module signed with untrusted signer 1
        bytes = ("test data for test module 10").getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 10",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                untrustedSignAndGetSignature(bytes, SIGNER_CERT_DNS[0]),
                null
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // module signed with untrusted signer 2
        bytes = ("test data for test module 11").getBytes(Charsets.UTF8);
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 11",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                bytes,
                untrustedSignAndGetSignature(bytes, untrustedSignerCertDns[0]),
                null
        );
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // missing module sha256, module data and module signature
        serverModuleFileMO = createTestServerModuleFileMO(
                null,
                "another test module 12",
                ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION,
                null,
                null,
                null
        );
        // should fail with 400
        map.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        return Collections.unmodifiableMap(map);
    }

    /**
     * Utility method for overriding {@link ServerConfigParams#PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE}.<br/>
     *
     * @param newValue    the new value to set.
     */
    private void overrideUploadEnable(final boolean newValue) {
        if (uploadEnabledOverride == null) {
            // get the original version
            uploadEnabledOverride = config.getBooleanProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE, false);
        }
        SyspropUtil.setProperty(UPLOAD_ENABLED_SYS_PROP, String.valueOf(newValue));
        Assert.assertThat(config.getBooleanProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE, false), Matchers.is(newValue));
    }

    private ServerModuleFileMO builderMO (final ServerModuleFile serverModuleFile) {
        Assert.assertNotNull(serverModuleFile);

        // extract module bytes and signature
        final byte[] bytes;
        final String sig;
        try {
            final Pair<InputStream, String> streamAndSignature = serverModuleFileManager.getModuleBytesAsStreamWithSignature(serverModuleFile.getGoid());
            Assert.assertNotNull(streamAndSignature);
            Assert.assertNotNull(streamAndSignature.left);
            try (final InputStream is = streamAndSignature.left) {
                bytes = IOUtils.slurpStream(is);
                sig = StringUtils.isNotBlank(streamAndSignature.right) ? streamAndSignature.right : null;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } catch (final FindException e) {
            throw new RuntimeException(e);
        }

        // create our mo
        return createTestServerModuleFileMO(
                serverModuleFile.getId(),
                serverModuleFile.getName(),
                ServerModuleFileTransformer.convertModuleType(serverModuleFile.getModuleType()),
                bytes,
                sig,
                ServerModuleFileTransformer.gatherProperties(serverModuleFile, ServerModuleFile.getPropertyKeys())
        );
    }

    @Override
    public Map<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>> getUnUpdateableManagedObjects() {
        // update is not supported for ServerModuleFile, at least for now
        // todo when ServerModuleFile update is supported add necessary tests here
        // for now all ar un-updateable therefore expect 403 (FORBIDDEN)
        return Functions.toMap(serverModuleFiles, new Functions.Unary<Pair<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>>, ServerModuleFile>() {
            @Override
            public Pair<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>> call(final ServerModuleFile serverModuleFile) {
                return Pair.<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>>pair(
                        builderMO(serverModuleFile),
                        new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
                            @Override
                            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                                Assert.assertThat(restResponse.getStatus(), Matchers.is(403)); // FORBIDDEN
                            }
                        }
                );
            }
        });
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return CollectionUtils.MapBuilder.<String, Functions.BinaryVoid<String, RestResponse>>builder()
                .put("type=badType", new Functions.BinaryVoid<String, RestResponse>() {
                    @Override
                    public void call(final String s, final RestResponse restResponse) {
                        Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
                    }
                })
                .put("includeData=notboolean", new Functions.BinaryVoid<String, RestResponse>() {
                    @Override
                    public void call(final String s, final RestResponse restResponse) {
                        Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
                    }
                })
                .map();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(serverModuleFiles, new Functions.Unary<String, ServerModuleFile>() {
            @Override
            public String call(final ServerModuleFile serverModuleFile) {
                return serverModuleFile.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "serverModuleFiles";
    }

    @Override
    public String getType() {
        return EntityType.SERVER_MODULE_FILE.name();
    }

    @Override
    public String getExpectedTitle(final String id) throws Exception {
        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(final String id, final List<Link> links) throws Exception {
        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);

    }

    @Override
    public void verifyEntity(final String id, final ServerModuleFileMO managedObject) throws Exception {
        final ServerModuleFile entity = serverModuleFileManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);
            Assert.assertThat(managedObject.getId(), Matchers.equalTo(entity.getId()));
            Assert.assertThat(managedObject.getName(), Matchers.equalTo(entity.getName()));
            Assert.assertThat(managedObject.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(entity.getModuleType())));
            Assert.assertThat(managedObject.getModuleSha256(), Matchers.equalTo(entity.getModuleSha256()));
            final Pair<InputStream, String> streamAndSignature = serverModuleFileManager.getModuleBytesAsStreamWithSignature(entity.getGoid());
            if (streamAndSignature == null) {
                Assert.assertNull(managedObject.getModuleData());
                Assert.assertNull(managedObject.getSignatureProperties());
            } else {
                try (final InputStream is = streamAndSignature.left) {
                    Assert.assertThat(
                        managedObject.getModuleData(),
                        Matchers.anyOf(
                                Matchers.nullValue(byte[].class),
                                Matchers.equalTo(IOUtils.slurpStream(is))
                        )
                    );
                }
                if (StringUtils.isNotBlank(streamAndSignature.right)) {
                    Assert.assertThat(
                            managedObject.getSignatureProperties(),
                            Matchers.anyOf(
                                    Matchers.nullValue(Map.class),
                                    Matchers.equalTo(getSignatureMap(streamAndSignature.right))
                            )
                    );
                } else {
                    Assert.assertNull(managedObject.getSignatureProperties());
                }
            }
            if (managedObject.getProperties() != null) {
                for (final String key : managedObject.getProperties().keySet()) {
                    Assert.assertThat(entity.getProperty(key), Matchers.equalTo(managedObject.getProperties().get(key)));
                }
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        // make sure the modules used below do exist
        Assert.assertThat(serverModuleFiles.get(0), Matchers.notNullValue());
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());
        Assert.assertThat(serverModuleFiles.get(2), Matchers.notNullValue());

        try {
            return CollectionUtils.MapBuilder.<String, List<String>>builder()
                    .put("", Functions.map(serverModuleFiles, new Functions.Unary<String, ServerModuleFile>() {
                        @Override
                        public String call(final ServerModuleFile serverModuleFile) {
                            return serverModuleFile.getId();
                        }
                    }))
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8),
                            Arrays.asList(serverModuleFiles.get(0).getId()))
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(1).getName(), HttpConstants.ENCODING_UTF8),
                            Arrays.asList(serverModuleFiles.get(0).getId(), serverModuleFiles.get(1).getId()))
                    .put("name=unknownName", Collections.<String>emptyList())
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(2).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&sort=name&order=desc",
                            Arrays.asList(serverModuleFiles.get(2).getId(), serverModuleFiles.get(1).getId(), serverModuleFiles.get(0).getId()))
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(2).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&sort=name&order=asc",
                            Arrays.asList(serverModuleFiles.get(0).getId(), serverModuleFiles.get(1).getId(), serverModuleFiles.get(2).getId()))
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&sort=type&order=asc",
                            Arrays.asList(serverModuleFiles.get(1).getId(), serverModuleFiles.get(0).getId()))
                    .put("name=" + URLEncoder.encode(serverModuleFiles.get(0).getName(), HttpConstants.ENCODING_UTF8) + "&name=" + URLEncoder.encode(serverModuleFiles.get(1).getName(), HttpConstants.ENCODING_UTF8) + "&sort=type&order=desc",
                            Arrays.asList(serverModuleFiles.get(0).getId(), serverModuleFiles.get(1).getId()))
                    .put("type=custom", Arrays.asList(serverModuleFiles.get(1).getId()))
                    .put("type=modular", Arrays.asList(serverModuleFiles.get(0).getId(), serverModuleFiles.get(2).getId()))
                    .map();
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void doTestEntityWithIncludeData(boolean includeData, final Item<ServerModuleFileMO> ref) throws Exception {
        Assert.assertThat(ref, Matchers.notNullValue());
        Assert.assertThat(ref.getContent(), Matchers.notNullValue());
        final Pair<InputStream, String> streamAndSignature = serverModuleFileManager.getModuleBytesAsStreamWithSignature(Goid.parseGoid(ref.getId()));
        Assert.assertNotNull(streamAndSignature);
        Assert.assertNotNull(streamAndSignature.left);
        try (final InputStream is = streamAndSignature.left) {
            Assert.assertThat(
                    ref.getContent().getModuleData(),
                    includeData ? Matchers.equalTo(IOUtils.slurpStream(is)) : Matchers.nullValue(byte[].class)
            );
        }
        Assert.assertThat(
                ref.getContent().getSignatureProperties(),
                includeData
                        ? Matchers.<Map>equalTo(getSignatureMap(streamAndSignature.right))
                        : Matchers.nullValue(Map.class)
        );
    }

    private void doTestGetWithIncludeData(boolean includeData, @Nullable final String query) throws Exception {
        // always use id 0
        Assert.assertThat(serverModuleFiles.get(0), Matchers.notNullValue());
        final String id = serverModuleFiles.get(0).getId();
        Assert.assertThat(id, Matchers.not(Matchers.isEmptyOrNullString()));

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, query, HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        //noinspection unchecked
        Item<ServerModuleFileMO> item = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertEquals("Id's don't match", id, item.getId());
        Assert.assertEquals("Type is incorrect", getType(), item.getType());
        Assert.assertEquals("Title is incorrect", getExpectedTitle(id), item.getName());
        Assert.assertNotNull("TimeStamp must always be present", item.getDate());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + id, self.getUri());

        doTestEntityWithIncludeData(includeData, item);
    }

    private void doTestListWithIncludeData(boolean includeData, @Nullable final String query) throws Exception {
        final List<String> expectedModuleIds = getRetrievableEntityIDs();
        Assert.assertThat("expectedModuleIds cannot be empty", expectedModuleIds, Matchers.not(Matchers.empty()));

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), query, HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        //noinspection unchecked
        final ItemsList<ServerModuleFileMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", "List", item.getType());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", getType() + " List", item.getName());
        Assert.assertNotNull("TimeStamp must always be present", item.getDate());

        final List<Item<ServerModuleFileMO>> references = item.getContent();
        Assert.assertThat(references, Matchers.hasSize(expectedModuleIds.size()));
        for (final Item<ServerModuleFileMO> ref: references) {
            Assert.assertThat(
                    "Id's don't match",
                    ref.getId(),
                    Matchers.anyOf(Functions.map(
                            expectedModuleIds,
                            new Functions.Unary<Matcher<? super String>, String>() {
                                @Override
                                public Matcher<? super String> call(final String s) {
                                    return Matchers.equalTo(s);
                                }
                            }
                    ))
            );
            doTestEntityWithIncludeData(includeData, ref);
        }
    }

    @Test
    public void testIncludeModuleData() throws Exception {
        // test list
        doTestListWithIncludeData(false, "includeData=false");
        doTestListWithIncludeData(false, null); // default is false
        doTestListWithIncludeData(true, "includeData=true");

        // test get
        doTestGetWithIncludeData(false, "includeData=false");
        doTestGetWithIncludeData(false, null); // default is false
        doTestGetWithIncludeData(true, "includeData=true");
    }

    @Test
    public void testUploadDisabled() throws Exception {
        // disable modules upload
        overrideUploadEnable(false);

        // test POST and PUT
        final List<ServerModuleFileMO> modulesToCreate = getCreatableManagedObjects();
        for (final ServerModuleFileMO mo : modulesToCreate) {
            mo.setId(null);

            // use POST
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertThat("Expected successful assertion status", response.getAssertionStatus(), Matchers.is(AssertionStatus.NONE));
            Assert.assertThat("Expected 403", response.getStatus(), Matchers.is(403));
            Assert.assertThat("Expected Server Module Files functionality has been disabled.", response.getBody(), Matchers.containsString("Server Module Files functionality has been disabled"));

            //use PUT
            response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + getGoid(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertThat("Expected successful assertion status", response.getAssertionStatus(), Matchers.is(AssertionStatus.NONE));
            Assert.assertThat("Expected 403", response.getStatus(), Matchers.is(403));
            Assert.assertThat("Expected Server Module Files functionality has been disabled.", response.getBody(), Matchers.containsString("Server Module Files functionality has been disabled"));
        }

        // test DELETE
        final List<String> modudesIDsToDelete = getDeleteableManagedObjectIDs();
        for (final String id : modudesIDsToDelete) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, HttpMethod.DELETE, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertThat("Expected successful assertion status", response.getAssertionStatus(), Matchers.is(AssertionStatus.NONE));
            Assert.assertThat("Expected 403", response.getStatus(), Matchers.is(403));
            Assert.assertThat("Expected Server Module Files functionality has been disabled.", response.getBody(), Matchers.containsString("Server Module Files functionality has been disabled"));
        }
    }
}
