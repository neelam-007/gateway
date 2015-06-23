package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.*;
import org.apache.http.entity.ContentType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.Nullable;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServerModuleFileEntityResourceTest extends RestEntityTests<ServerModuleFile, ServerModuleFileMO> {
    private static final Logger logger = Logger.getLogger(ServerModuleFileEntityResourceTest.class.getName());

    private static final String MAX_UPLOAD_SIZE_SYS_PROP = "com.l7tech.server." + ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE;
    private static final String UPLOAD_ENABLED_SYS_PROP = "com.l7tech.server." + ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_ENABLE;

    private Config config;
    private ServerModuleFileManager serverModuleFileManager;
    private List<ServerModuleFile> serverModuleFiles = new ArrayList<>();
    private Long maxUploadSizeOverride = null;
    private Boolean uploadEnabledOverride = null;
    // read only set of initial modules SHA256
    private Set<String> moduleShas;

    @BeforeClass
    public static void beforeClass() throws Exception {
        RestEntityTests.beforeClass();

        // to make this test valid modules upload should be enabled by default
        SyspropUtil.setProperty(UPLOAD_ENABLED_SYS_PROP, String.valueOf(true));
    }

    @Before
    public void before() throws SaveException {
        maxUploadSizeOverride = null;
        uploadEnabledOverride = null;

        serverModuleFileManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serverModuleFileManager", ServerModuleFileManager.class);
        Assert.assertThat(serverModuleFileManager, Matchers.notNullValue());
        config = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serverConfig", ServerConfig.class);
        Assert.assertThat(config, Matchers.notNullValue());

        int ordinal = 0;

        // making sure module sha256 are generated uniquely
        final Set<String> moduleShas = new HashSet<>();

        // regular ServerModuleFile
        ServerModuleFile serverModuleFile = new ServerModuleFile();
        serverModuleFile.setName("module " + ++ordinal);
        serverModuleFile.setModuleType(ModuleType.MODULAR_ASSERTION);
        serverModuleFile.setStateForNode("node1", ModuleState.LOADED);
        serverModuleFile.setStateErrorMessageForNode("node2", "test error 1");
        byte[] bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFile.createData(bytes);
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "FileName" + ordinal + ".jar");
        serverModuleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "Assertion1");
        serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length));
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        // another regular ServerModuleFile
        serverModuleFile = new ServerModuleFile();
        serverModuleFile.setName("module " + ++ordinal);
        serverModuleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        serverModuleFile.setStateForNode("node1", ModuleState.LOADED);
        serverModuleFile.setStateErrorMessageForNode("node2", "test error 1");
        bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFile.createData(bytes);
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        serverModuleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "FileName" + ordinal + ".jar");
        serverModuleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "Assertion1");
        serverModuleFile.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length));
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        // ServerModuleFile without props
        serverModuleFile = new ServerModuleFile();
        serverModuleFile.setName("module " + ++ordinal);
        serverModuleFile.setModuleType(ModuleType.MODULAR_ASSERTION);
        bytes = ("test bytes for module " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFile.createData(bytes);
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFile.getModuleSha256())));
        moduleShas.add(serverModuleFile.getModuleSha256());
        // add it
        serverModuleFileManager.save(serverModuleFile);
        serverModuleFiles.add(serverModuleFile);

        this.moduleShas = Collections.unmodifiableSet(moduleShas);
    }

    @After
    public void after() throws FindException, DeleteException {
        // reset ServerModuleFile maxUploadSize property
        if (maxUploadSizeOverride != null) {
            SyspropUtil.setProperty(MAX_UPLOAD_SIZE_SYS_PROP, String.valueOf(maxUploadSizeOverride));
            maxUploadSizeOverride = null;
        }

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

    @Override
    public List<ServerModuleFileMO> getCreatableManagedObjects() {
        final List<ServerModuleFileMO> serverModuleFileMOs = new ArrayList<>();
        int ordinal = 0;

        // this is a regular ServerModuleFile
        ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION);
        byte[] bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with null props
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with empty props
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().unmodifiableMap());
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_FILE_NAME
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_ASSERTIONS
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with only PROP_SIZE
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_FILE_NAME
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_ASSERTIONS
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_SIZE, String.valueOf(bytes.length))
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

        // this is a ServerModuleFile with missing PROP_SIZE
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(getGoid().toString());
        serverModuleFileMO.setName("CreatedServerModuleFile" + ++ordinal);
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = ("test module data " + ordinal).getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        serverModuleFileMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "fileName" + ordinal + ".jar")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .unmodifiableMap()
        );
        serverModuleFileMOs.add(serverModuleFileMO);

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
        CollectionUtils.MapBuilder<ServerModuleFileMO, Functions.BinaryVoid<ServerModuleFileMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        // make sure module 0 exists
        Assert.assertThat(serverModuleFiles.get(0), Matchers.notNullValue());

        // existing name
        ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName(serverModuleFiles.get(0).getName());
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION);
        byte[] bytes = ("test data for test module 1").getBytes(Charsets.UTF8);
        serverModuleFileMO.setModuleData(bytes);
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // existing module sha256
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 1");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION);
        serverModuleFileMO.setModuleData(serverModuleFiles.get(1).getData().getDataBytes());
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(1).getModuleSha256());
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // missing module data
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 2");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(1).getModuleSha256());
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 1 exists
        Assert.assertThat(serverModuleFiles.get(1), Matchers.notNullValue());

        // missing module sha256
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 3");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        serverModuleFileMO.setModuleData(serverModuleFiles.get(1).getData().getDataBytes());
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // missing module sha256 and module data
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 3");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        // make sure module 0 and module 1 have different shas
        Assert.assertThat(serverModuleFiles.get(0).getModuleSha256(), Matchers.not(Matchers.equalTo(serverModuleFiles.get(1).getModuleSha256())));

        // module data mismatch sha256
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 4");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        serverModuleFileMO.setModuleSha256(serverModuleFiles.get(0).getModuleSha256());
        serverModuleFileMO.setModuleData(serverModuleFiles.get(1).getData().getDataBytes());
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        overrideMaxUploadSize(100L);

        // module data exceeding allowed size
        serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setName("another test module 5");
        serverModuleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        bytes = new byte[101];
        serverModuleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        Assert.assertThat(moduleShas, Matchers.not(Matchers.hasItem(serverModuleFileMO.getModuleSha256())));
        serverModuleFileMO.setModuleData(bytes);
        // should fail with 400
        builder.put(serverModuleFileMO, new Functions.BinaryVoid<ServerModuleFileMO, RestResponse>() {
            @Override
            public void call(final ServerModuleFileMO serverModuleFileMO, final RestResponse restResponse) {
                Assert.assertThat(restResponse.getStatus(), Matchers.is(400));
            }
        });

        return builder.map();
    }

    /**
     * Utility method for overriding {@link ServerConfigParams#PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE}.<br/>
     *
     * @param newValue    the new value to set.
     */
    private void overrideMaxUploadSize(final long newValue) {
        if (maxUploadSizeOverride == null) {
            // get the original version
            maxUploadSizeOverride = config.getLongProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE, 0L);
        }
        SyspropUtil.setProperty(MAX_UPLOAD_SIZE_SYS_PROP, String.valueOf(newValue));
        Assert.assertThat(config.getLongProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE, 0L), Matchers.is(newValue));
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
        Assert.assertThat(serverModuleFile, Matchers.notNullValue());
        ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(serverModuleFile.getId());
        serverModuleFileMO.setName(serverModuleFile.getName());
        serverModuleFileMO.setModuleType(ServerModuleFileTransformer.convertModuleType(serverModuleFile.getModuleType()));
        serverModuleFileMO.setProperties(ServerModuleFileTransformer.gatherProperties(serverModuleFile, ServerModuleFile.getPropertyKeys()));
        serverModuleFileMO.setModuleSha256(serverModuleFile.getModuleSha256());
        try {
            serverModuleFileMO.setModuleData(serverModuleFileManager.getModuleBytes(serverModuleFile.getGoid()));
        } catch (final FindException e) {
            throw new RuntimeException(e);
        }
        return serverModuleFileMO;
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
            Assert.assertThat(
                    managedObject.getModuleData(),
                    Matchers.anyOf(
                            Matchers.nullValue(byte[].class),
                            Matchers.equalTo(serverModuleFileManager.getModuleBytes(entity.getGoid()))
                    )
            );

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
        Assert.assertThat(serverModuleFileManager.getModuleBytes(Goid.parseGoid(ref.getId())), Matchers.notNullValue());
        Assert.assertThat(
                ref.getContent().getModuleData(),
                includeData ? Matchers.equalTo(serverModuleFileManager.getModuleBytes(Goid.parseGoid(ref.getId()))) : Matchers.nullValue(byte[].class)
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
        // todo: since backend is using derby getting the module data will fail with IOException (see SSG-11350), re-enable the test below once fixed
        //doTestListWithIncludeData(true, "includeData=true");

        // test get
        doTestGetWithIncludeData(false, "includeData=false");
        doTestGetWithIncludeData(false, null); // default is false
        // todo: since backend is using derby getting the module data will fail with IOException (see SSG-11350), re-enable the test below once fixed
        //doTestGetWithIncludeData(true, "includeData=true");
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
