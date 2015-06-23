package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServerModuleFileAPIResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileTransformerTest {

    @Mock
    private Config config;

    @Mock
    private SecretsEncryptor secretsEncryptor;

    @Mock
    private ServerModuleFileAPIResourceFactory factory;

    @InjectMocks
    private final ServerModuleFileTransformer transformer = new ServerModuleFileTransformer();

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(10*1024*1024L /* 10MB */).when(config).getLongProperty(Mockito.eq(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE), Mockito.anyLong());
    }

    @Test
    public void testGetResourceType() throws Exception {
        assertThat(transformer.getResourceType(), Matchers.equalTo(EntityType.SERVER_MODULE_FILE.toString()));
    }

    @Test
    public void testConvertToMO() throws Exception {
        final ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.setGoid(new Goid(100, 101));
        moduleFile.setName("test custom server module file");
        moduleFile.setVersion(10);
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        moduleFile.setStateForNode("node1", ModuleState.LOADED);  // states should be ignored by MO
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "module FileName");
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "1000");
        moduleFile.setProperty("unknown", "unknown data"); // should be ignored
        moduleFile.createData("new data".getBytes(Charsets.UTF8));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertThat("Only one param", invocation.getArguments().length, Matchers.equalTo(1));
                final Object param1 = invocation.getArguments()[0];
                assertThat("First Param is ServerModuleFileMO", param1, Matchers.instanceOf(ServerModuleFileMO.class));
                final ServerModuleFileMO serverModuleFileMO = (ServerModuleFileMO)param1;
                assertThat("Same GOID as moduleFile", serverModuleFileMO.getId(), Matchers.equalTo(moduleFile.getId()));
                final byte[] bytes = moduleFile.getData().getDataBytes();
                serverModuleFileMO.setModuleData(bytes != null ? Arrays.copyOf(bytes, bytes.length) : null);
                return null;
            }
        }).when(factory).setModuleData(Mockito.<ServerModuleFileMO>any());

        // without data
        ServerModuleFileMO moduleFileMO = transformer.convertToMO(moduleFile);

        assertThat(moduleFileMO.getId(), Matchers.equalTo(moduleFile.getId()));
        assertThat(moduleFileMO.getName(), Matchers.equalTo(moduleFile.getName()));
        assertThat(moduleFileMO.getVersion(), Matchers.equalTo(moduleFile.getVersion()));
        assertThat(moduleFileMO.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFile.getModuleType())));
        assertThat(moduleFileMO.getModuleSha256(), Matchers.equalTo(moduleFile.getModuleSha256()));
        assertThat(moduleFileMO.getModuleData(), Matchers.nullValue());

        Map<String, String> moProperties = moduleFileMO.getProperties();
        assertThat(moProperties.get(ServerModuleFile.PROP_FILE_NAME), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)));
        assertThat(moProperties.get(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS)));
        assertThat(moProperties.get(ServerModuleFile.PROP_SIZE), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));
        assertThat(moProperties.get("unknown"), Matchers.nullValue());

        // with data
        moduleFileMO = transformer.convertToMO(moduleFile, true);

        assertThat(moduleFileMO.getId(), Matchers.equalTo(moduleFile.getId()));
        assertThat(moduleFileMO.getName(), Matchers.equalTo(moduleFile.getName()));
        assertThat(moduleFileMO.getVersion(), Matchers.equalTo(moduleFile.getVersion()));
        assertThat(moduleFileMO.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFile.getModuleType())));
        assertThat(moduleFileMO.getModuleSha256(), Matchers.equalTo(moduleFile.getModuleSha256()));
        assertThat(moduleFileMO.getModuleData(), Matchers.equalTo(moduleFile.getData().getDataBytes()));

        moProperties = moduleFileMO.getProperties();
        assertThat(moProperties.get(ServerModuleFile.PROP_FILE_NAME), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)));
        assertThat(moProperties.get(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS)));
        assertThat(moProperties.get(ServerModuleFile.PROP_SIZE), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));
        assertThat(moProperties.get("unknown"), Matchers.nullValue());
    }

    @Test
    public void testConvertToMO_moduleDataFail_withoutData() throws Exception {
        final ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.setGoid(new Goid(100, 101));
        moduleFile.setName("test custom server module file");
        moduleFile.setVersion(10);
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        moduleFile.setStateForNode("node1", ModuleState.LOADED);  // states should be ignored by MO
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "module FileName");
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "1000");
        moduleFile.setProperty("unknown", "unknown data"); // should be ignored
        moduleFile.createData("new data".getBytes(Charsets.UTF8));

        Mockito.doThrow(ResourceFactory.ResourceNotFoundException.class).when(factory).setModuleData(Mockito.<ServerModuleFileMO>any());

        // without data
        ServerModuleFileMO moduleFileMO = transformer.convertToMO(moduleFile);

        assertThat(moduleFileMO.getId(), Matchers.equalTo(moduleFile.getId()));
        assertThat(moduleFileMO.getName(), Matchers.equalTo(moduleFile.getName()));
        assertThat(moduleFileMO.getVersion(), Matchers.equalTo(moduleFile.getVersion()));
        assertThat(moduleFileMO.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFile.getModuleType())));
        assertThat(moduleFileMO.getModuleSha256(), Matchers.equalTo(moduleFile.getModuleSha256()));
        assertThat(moduleFileMO.getModuleData(), Matchers.nullValue());

        Map<String, String> moProperties = moduleFileMO.getProperties();
        assertThat(moProperties.get(ServerModuleFile.PROP_FILE_NAME), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME)));
        assertThat(moProperties.get(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS)));
        assertThat(moProperties.get(ServerModuleFile.PROP_SIZE), Matchers.equalTo(moduleFile.getProperty(ServerModuleFile.PROP_SIZE)));
        assertThat(moProperties.get("unknown"), Matchers.nullValue());
    }

    @Test(expected = ResourceFactory.ResourceAccessException.class)
    public void testConvertToMO_moduleDataFail_withData() throws Exception {
        final ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.setGoid(new Goid(100, 101));
        moduleFile.setName("test custom server module file");
        moduleFile.setVersion(10);
        moduleFile.setModuleType(ModuleType.CUSTOM_ASSERTION);
        moduleFile.setStateForNode("node1", ModuleState.LOADED);  // states should be ignored by MO
        moduleFile.setProperty(ServerModuleFile.PROP_FILE_NAME, "module FileName");
        moduleFile.setProperty(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2");
        moduleFile.setProperty(ServerModuleFile.PROP_SIZE, "1000");
        moduleFile.setProperty("unknown", "unknown data"); // should be ignored
        moduleFile.createData("new data".getBytes(Charsets.UTF8));

        Mockito.doThrow(ResourceFactory.ResourceNotFoundException.class).when(factory).setModuleData(Mockito.<ServerModuleFileMO>any());

        // with data
        transformer.convertToMO(moduleFile, true);
        fail("moduleData missing should fail");
    }

    @Test
    public void testConvertFromMO() throws Exception {
        int idInc = 100;

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test normal mo
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(++idInc, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .put("unknown", "unknown data")
                        .map())
        );
        byte[] bytes = String.valueOf("new data " + idInc).getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        ServerModuleFile moduleFile = transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
        assertThat(moduleFile.getId(), Matchers.equalTo(moduleFileMO.getId()));
        assertThat(moduleFile.getName(), Matchers.equalTo(moduleFileMO.getName()));
        assertThat(moduleFile.getVersion(), Matchers.equalTo(moduleFileMO.getVersion()));
        assertThat(moduleFile.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFileMO.getModuleType())));
        assertThat(moduleFile.getModuleSha256(), Matchers.equalTo(moduleFileMO.getModuleSha256()));
        assertThat(moduleFile.getData().getDataBytes(), Matchers.equalTo(moduleFileMO.getModuleData()));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.equalTo(moduleFileMO.getProperties().get(ServerModuleFile.PROP_FILE_NAME)));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.equalTo(moduleFileMO.getProperties().get(ServerModuleFile.PROP_ASSERTIONS)));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), Matchers.equalTo(moduleFileMO.getProperties().get(ServerModuleFile.PROP_SIZE)));
        assertThat(moduleFileMO.getProperties().get("unknown"), Matchers.equalTo("unknown data"));
        assertThat(moduleFile.getProperty("unknown"), Matchers.nullValue());
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test empty props
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(++idInc, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(Collections.<String, String>emptyMap());
        assertThat(moduleFileMO.getProperties().get(ServerModuleFile.PROP_FILE_NAME), Matchers.nullValue());
        assertThat(moduleFileMO.getProperties().get(ServerModuleFile.PROP_ASSERTIONS), Matchers.nullValue());
        assertThat(moduleFileMO.getProperties().get(ServerModuleFile.PROP_SIZE), Matchers.nullValue());
        assertThat(moduleFileMO.getProperties().get("unknown"), Matchers.nullValue());
        bytes = String.valueOf("new data " + idInc).getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        moduleFile = transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
        assertThat(moduleFile.getId(), Matchers.equalTo(moduleFileMO.getId()));
        assertThat(moduleFile.getName(), Matchers.equalTo(moduleFileMO.getName()));
        assertThat(moduleFile.getVersion(), Matchers.equalTo(moduleFileMO.getVersion()));
        assertThat(moduleFile.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFileMO.getModuleType())));
        assertThat(moduleFile.getModuleSha256(), Matchers.equalTo(moduleFileMO.getModuleSha256()));
        assertThat(moduleFile.getData().getDataBytes(), Matchers.equalTo(moduleFileMO.getModuleData()));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.nullValue());
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.nullValue());
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), Matchers.is(String.valueOf(bytes.length)));
        assertThat(moduleFile.getProperty("unknown"), Matchers.nullValue());
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test null props
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(++idInc, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        //moduleFileMO.setProperties(null);
        assertThat(moduleFileMO.getProperties(), Matchers.nullValue());
        bytes = String.valueOf("new data " + idInc).getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        moduleFile = transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
        assertThat(moduleFile.getId(), Matchers.equalTo(moduleFileMO.getId()));
        assertThat(moduleFile.getName(), Matchers.equalTo(moduleFileMO.getName()));
        assertThat(moduleFile.getVersion(), Matchers.equalTo(moduleFileMO.getVersion()));
        assertThat(moduleFile.getModuleType(), Matchers.equalTo(ServerModuleFileTransformer.convertModuleType(moduleFileMO.getModuleType())));
        assertThat(moduleFile.getModuleSha256(), Matchers.equalTo(moduleFileMO.getModuleSha256()));
        assertThat(moduleFile.getData().getDataBytes(), Matchers.equalTo(moduleFileMO.getModuleData()));
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_FILE_NAME), Matchers.nullValue());
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_ASSERTIONS), Matchers.nullValue());
        assertThat(moduleFile.getProperty(ServerModuleFile.PROP_SIZE), Matchers.is(String.valueOf(bytes.length)));
        assertThat(moduleFile.getProperty("unknown"), Matchers.nullValue());
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void testConvertFromMO_missingOrBlankSha256() throws Exception {
        // try with missing module sha256
        ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleData(bytes);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO without sha256 should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES));
        }

        // try with blank module sha256
        moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(101, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setModuleSha256("");
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        // use same bytes
        moduleFileMO.setModuleData(bytes);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO with blank sha256 should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES));
        }
    }

    @Test
    public void testConvertFromMO_missingOrEmptyData() throws Exception {
        // missing module data
        ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO without module data should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES));
        }

        // module data empty
        moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(101, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        // use same bytes
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(new byte[0]);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO with empty module data should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES));
        }
    }

    @Test
    public void testConvertFromMO_dataSha256Mismatch() throws Exception {
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256("bla bla");
        moduleFileMO.setModuleData(bytes);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO with module data not matching sha256 should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES));
        }
    }

    @Test
    public void testConvertFromMO_dataOverMaxSize() throws Exception {
        final long MAX_BYTES = 10;

        Mockito.doReturn(MAX_BYTES).when(config).getLongProperty(Mockito.eq(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE), Mockito.anyLong());
        assertThat(config.getLongProperty(ServerConfigParams.PARAM_SERVER_MODULE_FILE_UPLOAD_MAXSIZE, 1000L), Matchers.is(MAX_BYTES));

        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("this is more than MAX_BYTES bytes").getBytes(Charsets.UTF8);
        assertThat((long)bytes.length, Matchers.greaterThan(MAX_BYTES));
        moduleFileMO.setModuleSha256("bla bla");
        moduleFileMO.setModuleData(bytes);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("module data exceeding allowed size should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES));
        }
    }

    @Test
    public void testConvertFromMO_missingModuleType() throws Exception {
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        //moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        try {
            transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
            fail("MO missing module type should fail");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertThat(e.getType(), Matchers.is(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES));
        }
    }

    @Test
    public void testConvertFromMO_missingId() throws Exception {
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        //moduleFileMO.setId(new Goid(100, 101).toString());
        moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        final ServerModuleFile moduleFile = transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
        assertThat(moduleFile.getId(), Matchers.is(Goid.DEFAULT_GOID.toString()));
    }

    @Test
    public void testConvertFromMO_missingName() throws Exception {
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setId(new Goid(100, 101).toString());
        //moduleFileMO.setName("test custom server module file");
        moduleFileMO.setVersion(10);
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        moduleFileMO.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(ServerModuleFile.PROP_FILE_NAME, "module FileName")
                        .put(ServerModuleFile.PROP_ASSERTIONS, "Assertion1,Assertion2")
                        .put(ServerModuleFile.PROP_SIZE, "1000")
                        .map())
        );
        final byte[] bytes = String.valueOf("data123").getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        final ServerModuleFile moduleFile = transformer.convertFromMO(moduleFileMO, secretsEncryptor).getEntity();
        assertThat(moduleFile.getName(), Matchers.nullValue());
    }

    @Test
    public void testAsName() throws Exception {
        doTestAsName("test");
        doTestAsName("t est");
        doTestAsName("te   st");
    }

    private void doTestAsName(final String name) throws Exception{
        assertThat(ServerModuleFileTransformer.asName(name), Matchers.equalTo(name));
        assertThat(ServerModuleFileTransformer.asName(name + " "), Matchers.equalTo(name));
        assertThat(ServerModuleFileTransformer.asName(" " + name), Matchers.equalTo(name));
        assertThat(ServerModuleFileTransformer.asName(" " + name + " "), Matchers.equalTo(name));
    }

    @Test
    public void testConvertModuleType() throws Exception {
        assertThat(
                ServerModuleFileTransformer.convertModuleType(ModuleType.CUSTOM_ASSERTION),
                Matchers.equalTo(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION)
        );
        assertThat(
                ServerModuleFileTransformer.convertModuleType(ModuleType.MODULAR_ASSERTION),
                Matchers.equalTo(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION)
        );
        try {
            ServerModuleFileTransformer.convertModuleType((ModuleType) null);
            fail("convertModuleType should fail when null ModuleType is passed");
        } catch (ResourceFactory.ResourceAccessException e) {
            // OK
        }


        assertThat(
                ServerModuleFileTransformer.convertModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION),
                Matchers.equalTo(ModuleType.CUSTOM_ASSERTION)
        );
        assertThat(
                ServerModuleFileTransformer.convertModuleType(ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION),
                Matchers.equalTo(ModuleType.MODULAR_ASSERTION)
        );
        try {
            ServerModuleFileTransformer.convertModuleType((ServerModuleFileMO.ServerModuleFileModuleType) null);
            fail("convertModuleType should fail when null ServerModuleFileMO.ServerModuleFileModuleType is passed");
        } catch (ResourceFactory.InvalidResourceException e) {
            // OK
        }
    }

    @Test
    public void testGatherProperties() throws Exception {
        final ServerModuleFile moduleFile = new ServerModuleFile();
        moduleFile.setProperty("param1", "param1_value");
        moduleFile.setProperty("param2", "param2_value");
        moduleFile.setProperty("param3", "param3_value");
        moduleFile.setProperty("param4", "param4_value");
        moduleFile.setProperty("param5", "param5_value");

        Map<String, String> props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param2", "param4"});
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param1"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param2", "param2_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param3"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param4", "param4_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param5"), Matchers.any(String.class))));

        props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param1", "param4"});
        assertThat(props, Matchers.hasEntry("param1", "param1_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param2"), Matchers.any(String.class))));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param3"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param4", "param4_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param5"), Matchers.any(String.class))));

        props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param1", "param3", "param4"});
        assertThat(props, Matchers.hasEntry("param1", "param1_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param2"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param3", "param3_value"));
        assertThat(props, Matchers.hasEntry("param4", "param4_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param5"), Matchers.any(String.class))));

        props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param1", "param3", "param5"});
        assertThat(props, Matchers.hasEntry("param1", "param1_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param2"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param3", "param3_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param4"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param5", "param5_value"));

        props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param2", "param5"});
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param1"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param2", "param2_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param3"), Matchers.any(String.class))));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param4"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param5", "param5_value"));

        props = ServerModuleFileTransformer.gatherProperties(moduleFile, new String[]{"param2", "param4", "param5"});
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param1"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param2", "param2_value"));
        assertThat(props, Matchers.not(Matchers.hasEntry(Matchers.is("param3"), Matchers.any(String.class))));
        assertThat(props, Matchers.hasEntry("param4", "param4_value"));
        assertThat(props, Matchers.hasEntry("param5", "param5_value"));
    }

    @Test
    public void testSetProperties() throws Exception {
        ServerModuleFile moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_value")
                        .put("prop2", "prop2_value")
                        .put("prop3", "prop3_value")
                        .put("prop4", "prop4_value")
                        .put("prop5", "prop5_value")
                        .put("prop6", "prop6_value")
                        .map()),
                moduleFile,
                new String[]{"prop2", "prop5"},
                Collections.<String, String>emptyMap() // no default values
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop2"), Matchers.is("prop2_value"));
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop5"), Matchers.is("prop5_value"));
        assertThat(moduleFile.getProperty("prop6"), Matchers.nullValue());

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_value")
                        .put("prop2", "prop2_value")
                        .put("prop3", "prop3_value")
                        .put("prop4", "prop4_value")
                        .put("prop5", "prop5_value")
                        .put("prop6", "prop6_value")
                        .map()),
                moduleFile,
                new String[]{"prop1", "prop3", "prop5"},
                Collections.<String, String>emptyMap() // no default values
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.is("prop1_value"));
        assertThat(moduleFile.getProperty("prop2"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop3"), Matchers.is("prop3_value"));
        assertThat(moduleFile.getProperty("prop4"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop5"), Matchers.is("prop5_value"));
        assertThat(moduleFile.getProperty("prop6"), Matchers.nullValue());

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_value")
                        .put("prop2", "prop2_value")
                        .put("prop3", "prop3_value")
                        .put("prop4", "prop4_value")
                        .put("prop5", "prop5_value")
                        .put("prop6", "prop6_value")
                        .map()),
                moduleFile,
                new String[]{"prop2", "prop4", "prop6"},
                Collections.<String, String>emptyMap() // no default values
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop2"), Matchers.is("prop2_value"));
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.is("prop4_value"));
        assertThat(moduleFile.getProperty("prop5"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop6"), Matchers.is("prop6_value"));

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_value")
                        .put("prop2", "prop2_value")
                        .put("prop3", "prop3_value")
                        .put("prop4", "prop4_value")
                        .put("prop5", "prop5_value")
                        .put("prop6", "prop6_value")
                        .map()),
                moduleFile,
                new String[]{"prop1", "prop4", "prop6"},
                Collections.<String, String>emptyMap() // no default values
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.is("prop1_value"));
        assertThat(moduleFile.getProperty("prop2"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.is("prop4_value"));
        assertThat(moduleFile.getProperty("prop5"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop6"), Matchers.is("prop6_value"));

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.<String, String>emptyMap(),
                moduleFile,
                new String[]{"prop1", "prop4", "prop6"},
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_def_value")
                        .put("prop4", "prop4_def_value")
                        .put("prop6", "prop6_def_value")
                        .map())
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.is("prop1_def_value"));
        assertThat(moduleFile.getProperty("prop2"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.is("prop4_def_value"));
        assertThat(moduleFile.getProperty("prop5"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop6"), Matchers.is("prop6_def_value"));

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                null,
                moduleFile,
                new String[]{"prop1", "prop4", "prop6"},
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_def_value")
                        .put("prop4", "prop4_def_value")
                        .put("prop6", "prop6_def_value")
                        .map())
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.is("prop1_def_value"));
        assertThat(moduleFile.getProperty("prop2"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.is("prop4_def_value"));
        assertThat(moduleFile.getProperty("prop5"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop6"), Matchers.is("prop6_def_value"));

        moduleFile = new ServerModuleFile();
        ServerModuleFileTransformer.setProperties(
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_value")
                        .put("prop2", "prop2_value")
                        .put("prop3", "prop3_value")
                        .put("prop4", "prop4_value")
                        .put("prop5", "prop5_value")
                        .put("prop6", "prop6_value")
                        .map()),
                moduleFile,
                new String[]{"prop1", "prop4", "prop6"},
                Collections.unmodifiableMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("prop1", "prop1_def_value")
                        .put("prop2", "prop2_def_value")
                        .put("prop3", "prop3_def_value")
                        .put("prop4", "prop4_def_value")
                        .put("prop5", "prop5_def_value")
                        .put("prop6", "prop6_def_value")
                        .map())
        );
        assertThat(moduleFile.getProperty("prop1"), Matchers.is("prop1_value"));
        assertThat(moduleFile.getProperty("prop2"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop3"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop4"), Matchers.is("prop4_value"));
        assertThat(moduleFile.getProperty("prop5"), Matchers.nullValue());
        assertThat(moduleFile.getProperty("prop6"), Matchers.is("prop6_value"));
    }
}