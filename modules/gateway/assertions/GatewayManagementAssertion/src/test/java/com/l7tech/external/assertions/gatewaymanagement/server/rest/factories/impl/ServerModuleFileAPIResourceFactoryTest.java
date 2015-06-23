package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.ServerModuleFileResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.ServerModuleFileTransformer;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.module.ServerModuleFileTestBase;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileAPIResourceFactoryTest extends ServerModuleFileTestBase {
    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    private Map<Goid, ServerModuleFile> moduleFiles;

    @Mock
    private RbacAccessService rbacAccessService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ServerModuleFileManager manager;

    @Spy
    private ServerModuleFileTransformer transformer;

    @InjectMocks
    private final ServerModuleFileAPIResourceFactory factory = new ServerModuleFileAPIResourceFactory();

    @Before
    public void setUp() throws Exception {
        int goidInc = 0;

        transformer.setFactory(factory);

        // generate sample custom and modular assertions
        moduleFiles = CollectionUtils.<Goid, ServerModuleFile>mapBuilder()
                // Custom Assertions Modules
                .put(
                        new Goid(GOID_HI_START, goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.CUSTOM_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.CUSTOM_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.CUSTOM_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.CUSTOM_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.CUSTOM_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                // Modular Assertions Modules
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.MODULAR_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.MODULAR_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.MODULAR_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.MODULAR_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .put(
                        new Goid(GOID_HI_START, ++goidInc),
                        create_test_module_without_states(
                                goidInc,
                                ModuleType.MODULAR_ASSERTION,
                                ("content for module " + goidInc).getBytes(Charsets.UTF8)
                        )
                )
                .unmodifiableMap();

        // mock ServerModuleFileManager findByPrimaryKey and getModuleBytesAsStream
        Mockito.doAnswer(new Answer<ServerModuleFile>() {
            @Override
            public ServerModuleFile answer(final InvocationOnMock invocation) throws Throwable {
                assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                assertThat("First Param is Goid", param1, Matchers.instanceOf(Goid.class));
                final Goid goid = (Goid)param1;
                assertThat(goid, Matchers.notNullValue());
                // get the module from our repository
                return moduleFiles.get(goid);
            }
        }).when(manager).findByPrimaryKey(Mockito.<Goid>any());

        Mockito.doAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(final InvocationOnMock invocation) throws Throwable {
                assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                assertThat("First Param is Goid", param1, Matchers.instanceOf(Goid.class));
                final Goid goid = (Goid)param1;
                assertThat(goid, Matchers.notNullValue());
                // get the module from our repository
                return getModuleFileBytesStream(goid);
            }
        }).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());
    }

    @Override
    protected MyServerModuleFile create_test_module_without_states(long ordinal, ModuleType moduleType, File moduleBytes) {
        return super.create_test_module_without_states(ordinal, moduleType, moduleBytes);
    }

    private InputStream getModuleFileBytesStream(final Goid goid) throws FileNotFoundException {
        assertThat(goid, Matchers.notNullValue());
        final ServerModuleFile moduleFile = moduleFiles.get(goid);
        if (moduleFile != null) {
            return getModuleFileBytes(moduleFile);
        }
        return null;
    }

    private InputStream getModuleFileBytes(final ServerModuleFile moduleFile) throws FileNotFoundException {
        assertThat(moduleFile, Matchers.instanceOf(MyServerModuleFile.class));
        assertThat(((MyServerModuleFile) moduleFile).getModuleContentStream(), Matchers.notNullValue());
        return ((MyServerModuleFile) moduleFile).getModuleContentStream();
    }

    @Test
    public void testGetResourceEntityType() throws Exception {
        assertThat(factory.getResourceEntityType(), Matchers.is(EntityType.SERVER_MODULE_FILE));
    }

    @Test
    public void testGetEntityManager() throws Exception {
        assertThat(factory.getEntityManager(), Matchers.is(manager));
    }

    @Test
    public void testGetResource() throws Exception {
        final Goid testGoid = new Goid(GOID_HI_START, 0);

        ServerModuleFileMO moduleFileMO = factory.getResource(testGoid.toString(), false);
        assertThat(moduleFileMO, Matchers.notNullValue());
        assertThat(moduleFileMO.getModuleData(), Matchers.nullValue());

        moduleFileMO = factory.getResource(testGoid.toString(), true);
        assertThat(moduleFileMO, Matchers.notNullValue());
        assertThat(moduleFileMO.getModuleData(), Matchers.equalTo(IOUtils.slurpStream(getModuleFileBytesStream(testGoid))));
    }

    @Test
    public void testGetResource_moduleNotFound() throws Exception {
        final Goid testGoid = new Goid(GOID_HI_START, 1000);

        try {
            factory.getResource(testGoid.toString(), false);
            fail("getResource should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }

        try {
            factory.getResource(testGoid.toString(), true);
            fail("getResource should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }
    }

    @Test
    public void testGetResource_moduleFoundInGetResource_butNotFoundOrFailInSetModuleData() throws Exception {
        final Goid testGoid = new Goid(GOID_HI_START, 0);

        // first throw FindException
        Mockito.doThrow(FindException.class).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());

        // first make sure the module exists without gathering the module data
        final ServerModuleFileMO moduleFileMO = factory.getResource(testGoid.toString(), false);
        assertThat(moduleFileMO, Matchers.notNullValue());
        assertThat(moduleFileMO.getModuleData(), Matchers.nullValue());

        // now try to get the module data on the same module
        try {
            factory.getResource(testGoid.toString(), true);
            fail("getResource should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }

        // second throw FindException
        Mockito.doThrow(IOException.class).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());

        // now try to get the module data on the same module
        try {
            factory.getResource(testGoid.toString(), true);
            fail("getResource should have failed with ResourceAccessException");
        } catch (ResourceFactory.ResourceAccessException e) {
            // OK
        }

        // finally return null
        Mockito.doReturn(null).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());

        // now try to get the module data on the same module
        try {
            factory.getResource(testGoid.toString(), true);
            fail("getResource should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }
    }

    @Test
    public void testUploadDisabled() throws Exception {
        // upload is disabled
        Mockito.doReturn(false).when(manager).isModuleUploadEnabled();

        // crate a new sample module file
        final ServerModuleFileMO moduleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        moduleFileMO.setName("new custom assertion module 1");
        moduleFileMO.setModuleType(ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION);
        byte[] bytes = "new custom assertion module 1 bytes".getBytes(Charsets.UTF8);
        moduleFileMO.setModuleSha256(ServerModuleFile.calcBytesChecksum(bytes));
        moduleFileMO.setModuleData(bytes);

        // test POST (upload)
        try {
            // upload is disabled this should fail with DisabledException
            factory.createResource(moduleFileMO);
            fail("createResource should have failed with DisabledException");
        } catch (ServerModuleFileResource.DisabledException e) {
            // OK
        }

        // generate new guid and make sure it doesn't exist already
        final Goid newGoid = new Goid(GOID_HI_START, 100);
        Assert.assertThat(moduleFiles, Matchers.not(Matchers.hasEntry(Matchers.is(newGoid), Matchers.any(ServerModuleFile.class))));

        // test PUT (update)
        try {
            // upload is disabled this should fail with DisabledException
            factory.createResource(newGoid.toString(), moduleFileMO);
            fail("createResource should have failed with DisabledException");
        } catch (ServerModuleFileResource.DisabledException e) {
            // OK
        }

        // make sure goid "1" exists
        final Goid existingGoid = new Goid(GOID_HI_START, 1);
        Assert.assertThat(moduleFiles, Matchers.hasEntry(Matchers.is(existingGoid), Matchers.any(ServerModuleFile.class)));

        // test PUT (update)
        try {
            // upload is disabled this should fail with DisabledException
            factory.deleteResource(existingGoid.toString());
            fail("deleteResource should have failed with DisabledException");
        } catch (ServerModuleFileResource.DisabledException e) {
            // OK
        }
    }

    @Test
    public void testSetModuleData() throws Exception {
        // test for null value
        try {
            //noinspection ConstantConditions
            factory.setModuleData(null);
            fail("setModuleData don't accept null");
        } catch (IllegalArgumentException e) {
            // OK
        }

        // crate a new sample module file
        final ServerModuleFile moduleFile = create_test_module_without_states(
                100,
                ModuleType.MODULAR_ASSERTION,
                "new test module 100".getBytes(Charsets.UTF8)
        );
        assertThat(moduleFile, Matchers.notNullValue());
        // mock getModuleBytesAsStream to return InputStream towards our test module.
        Mockito.doAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(final InvocationOnMock invocation) throws Throwable {
                assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                assertThat("First Param is Goid", param1, Matchers.instanceOf(Goid.class));
                final Goid goid = (Goid)param1;
                assertThat(goid, Matchers.equalTo(moduleFile.getGoid()));
                return getModuleFileBytes(moduleFile);
            }
        }).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());

        ServerModuleFileMO moduleFileMO = transformer.convertToMO(moduleFile, false); // exclude bytes
        assertThat(moduleFileMO.getModuleData(), Matchers.nullValue());
        factory.setModuleData(moduleFileMO); // now set the module data
        assertThat(moduleFileMO.getModuleData(), Matchers.equalTo(IOUtils.slurpStream(getModuleFileBytes(moduleFile))));

        // throw FindException
        Mockito.doThrow(FindException.class).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());
        moduleFileMO = transformer.convertToMO(moduleFile, false); // exclude bytes
        // now try to get the module data on the same module
        try {
            factory.setModuleData(moduleFileMO); // now set the module data
            fail("setModuleData should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }

        // throw IOException
        Mockito.doThrow(IOException.class).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());
        moduleFileMO = transformer.convertToMO(moduleFile, false); // exclude bytes
        // now try to get the module data on the same module
        try {
            factory.setModuleData(moduleFileMO); // now set the module data
            fail("setModuleData should have failed with ResourceAccessException");
        } catch (ResourceFactory.ResourceAccessException e) {
            // OK
        }

        // finally return null
        Mockito.doReturn(null).when(manager).getModuleBytesAsStream(Mockito.<Goid>any());
        moduleFileMO = transformer.convertToMO(moduleFile, false); // exclude bytes
        // now try to get the module data on the same module
        try {
            factory.setModuleData(moduleFileMO); // now set the module data
            fail("setModuleData should have failed with ResourceNotFoundException");
        } catch (ResourceFactory.ResourceNotFoundException e) {
            // OK
        }
    }
}