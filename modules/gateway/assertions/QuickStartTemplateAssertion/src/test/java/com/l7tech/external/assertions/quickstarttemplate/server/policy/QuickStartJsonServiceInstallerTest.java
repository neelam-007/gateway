package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Functions;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickStartJsonServiceInstallerTest extends JsonServiceInstallerTestBase {

    private static final Functions.UnaryVoid<Path> nothingToDoCallback = path -> {/* NOTHING TO DO*/};

    @Before
    public void setUp() throws Exception {
        super.before();
        Assert.assertThat(serviceInstaller, Matchers.instanceOf(QuickStartJsonServiceInstaller.class));
    }

    @After
    public void tearDown() throws Exception {
        super.after();
    }

    @Override
    protected QuickStartJsonServiceInstaller createJsonServiceInstaller(
            final QuickStartServiceBuilder serviceBuilder,
            final ServiceManager serviceManager,
            final PolicyVersionManager policyVersionManager,
            final QuickStartParser parser
    ) throws Exception {
        Assert.assertNotNull(serviceBuilder);
        Assert.assertNotNull(serviceManager);
        Assert.assertNotNull(policyVersionManager);
        Assert.assertNotNull(parser);
        return new QuickStartJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, parser);
    }

    @Test
    public void test_installJsonServices_BootstrapIgnoreSubfolder() throws Exception {
        jsonBootstrapFolder.newFile("test1.json");
        jsonBootstrapFolder.newFile("test2.json");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());

        doMockInstallJsonService(nothingToDoCallback);
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).installJsonService(Mockito.any(Path.class));
    }

    @Test
    public void test_installJsonServices_BootstrapFilesAreSorted() throws Exception {
        final File file1 = jsonBootstrapFolder.newFile("test2.json");
        final File file2 = jsonBootstrapFolder.newFile("test1.json");
        final File file3 = jsonBootstrapFolder.newFile("test4.json");
        final File file4 = jsonBootstrapFolder.newFile("test3.json");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());

        doMockInstallJsonService(nothingToDoCallback);
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(4)).installJsonService(Mockito.any(Path.class));
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file2.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file1.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file4.toPath()));
        inOrder.verify(serviceInstaller).installJsonService(Mockito.eq(file3.toPath()));
    }

    @Test
    public void test_installJsonServices_BootstrapIgnoresNonJsonFiles() throws Exception {
        jsonBootstrapFolder.newFile("test1.json");
        jsonBootstrapFolder.newFile("test2.json");
        jsonBootstrapFolder.newFile("test3json");
        jsonBootstrapFolder.newFile("test.json.blah");
        jsonBootstrapFolder.newFile("testjson.blah");
        jsonBootstrapFolder.newFile("test.jsonblah");
        jsonBootstrapFolder.newFile("test.j.s.o.n");

        final File testFolder = jsonBootstrapFolder.newFolder("test");
        final File testJsonFile1 = new File(testFolder, "test_test1.json");
        Assert.assertTrue(testJsonFile1.createNewFile());
        final File testJsonFile2 = new File(testFolder, "test_test2.json");
        Assert.assertTrue(testJsonFile2.createNewFile());
        final File testNonJsonFile = new File(testFolder, "test_test_blah.blah");
        Assert.assertTrue(testNonJsonFile.createNewFile());

        doMockInstallJsonService(nothingToDoCallback);
        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).installJsonService(Mockito.any(Path.class));
    }

    @Test
    public void test_installJsonServices_InvalidBootstrapFolder() throws Exception {
        final Path nonexistentFolder = Paths.get("some_invalid_folder");
        Assert.assertTrue(!Files.exists(nonexistentFolder));
        Assert.assertTrue(!Files.isDirectory(nonexistentFolder));
        Mockito.doReturn(nonexistentFolder).when(serviceInstaller).getBootstrapFolder();
        doMockHandleError(Level.FINE, "(?s)JSON services bootstrap folder.*doesn't exist", NoSuchFileException.class);
        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.times(1)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());

        final Path invalidFolder = jsonBootstrapFolder.newFile("some.file").toPath();
        Assert.assertTrue(Files.exists(invalidFolder));
        Assert.assertTrue(!Files.isDirectory(invalidFolder));
        Mockito.doReturn(invalidFolder).when(serviceInstaller).getBootstrapFolder();
        doMockHandleError(Level.FINE, "(?s)JSON services bootstrap folder.*doesn't exist", NotDirectoryException.class);
        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void test_installJsonServices_ValidPayload() throws Exception {
        final PublishedService testService = setupSuccessfullyInstallJsonServicesWithValidPayload();
        Assert.assertNotNull(testService);

        serviceInstaller.installJsonServices();
        Assert.assertThat(
                testService.getProperties(),
                Matchers.hasEntry(
                        QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD,
                        String.valueOf(QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP)
                )
        );
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void test_installJsonServices_UnhandledException() throws Exception {
        {
            final PublishedService testService = new PublishedService();
            setupInstallJsonServicesWithValidPayload(
                    testService,
                    serviceContainer -> {
                        throw new RuntimeException("unhandled error");
                    },
                    service -> null,
                    (policy, aBoolean, aBoolean2) -> null,
                    service -> { /*nothing to do*/ }
            );
            doMockHandleError(Level.WARNING, "Failed to install JSON services: unhandled error", RuntimeException.class);
            serviceInstaller.installJsonServices();
            Mockito.verify(serviceInstaller, Mockito.times(1)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        }

        {
            final PublishedService testService = new PublishedService();
            class MyException extends RuntimeException {
                private MyException(final String message) {
                    super(message);
                }
            }
            setupInstallJsonServicesWithValidPayload(
                    testService,
                    serviceContainer -> {
                        throw new MyException("MyException error");
                    },
                    service -> null,
                    (policy, aBoolean, aBoolean2) -> null,
                    service -> { /*nothing to do*/ }
            );
            doMockHandleError(Level.WARNING, "Failed to install JSON services: MyException error", MyException.class);
            serviceInstaller.installJsonServices();
            Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        }
    }

    @Test
    public void test_installJsonServices_InvalidPayloadMissingName() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Service must have a name.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(JsonMappingException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.ParseJsonPayload.class))
        );
    }

    @Test
    public void test_installJsonServices_InvalidPayloadUnrecognizedProperty() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"foo\": \"bar\",\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Unrecognized property \"foo\" for object \"Service\".*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(UnrecognizedPropertyException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.ParseJsonPayload.class))
        );
    }

    @Test
    public void test_installJsonServices_InvalidPayloadMalformedJsonSyntax() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \n" +
                        "    \"foo\": \"bar\",\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Unable to parse JSON service payload.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(JsonParseException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.ParseJsonPayload.class))
        );
    }

    @Test
    public void test_installJsonServices_CreateServiceFailedWithQuickStartPolicyBuilderException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> { throw new QuickStartPolicyBuilderException("test exception"); },
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.eq("test exception"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartPolicyBuilderException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.CreateServiceException.class))
        );
    }

    @Test
    public void test_installJsonServices_CreateServiceFailedWithQuickStartFindException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> { throw new FindException("test exception"); },
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.eq("Unable to create service: test exception"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(FindException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.CreateServiceException.class))
        );
    }

    @Test
    public void test_installJsonServices_CreateServiceFailedWithUnhandledException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> { throw new RuntimeException("test exception"); },
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );
        doMockHandleError(Level.WARNING, "Failed to install JSON services: test exception", RuntimeException.class);

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.times(1)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void test_installJsonServices_IOException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> { throw new IOException("test exception"); },
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );
        doMockHandleError(Level.WARNING, "(?s)Error while reading JSON service file:.*", IOException.class);

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.times(1)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void test_installJsonServices_SaveServiceFailedWithObjectModelException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> { throw new ObjectModelException("test exception"); },
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.eq("Unable to save service: test exception"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(ObjectModelException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.SaveServiceException.class))
        );
    }

    @Test
    public void test_installJsonServices_SaveServiceCheckpointPolicyFailedWithObjectModelException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> { throw new ObjectModelException("test exception"); },
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.eq("Unable to save service: test exception"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(ObjectModelException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.SaveServiceException.class))
        );
    }

    @Test
    public void test_installJsonServices_SaveServiceCreateRolesFailedWithObjectModelException() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { throw new ObjectModelException("test exception"); }
        );

        serviceInstaller.installJsonServices();

        Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        final InOrder inOrder = Mockito.inOrder(serviceInstaller);
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.eq("Unable to save service: test exception"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(ObjectModelException.class))
        );
        inOrder.verify(serviceInstaller).handleError(
                Mockito.eq(Level.WARNING),
                Mockito.matches("(?s)Failed to install JSON service file.*"),
                Mockito.argThat(ExceptionArgMatcher.matchesException(QuickStartJsonServiceInstaller.SaveServiceException.class))
        );
    }

    @Test
    public void test_installJsonServices_SaveServiceFailedWithUnhandledException() throws Exception {
        {
            final PublishedService testService = new PublishedService();
            setupInstallJsonServices(
                    testService,
                    "{\n" +
                            "  \"Service\": {\n" +
                            "    \"name\": \"TestService1\",\n" +
                            "    \"gatewayUri\": \"/test1\",\n" +
                            "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                            "    \"policy\": [ ]\n" +
                            "  }\n" +
                            "}",
                    serviceContainer -> testService,
                    service -> {
                        throw new RuntimeException("test exception1");
                    },
                    (policy, aBoolean, aBoolean2) -> null,
                    service -> { /*nothing to do*/ }
            );
            doMockHandleError(Level.WARNING, "Failed to install JSON services: test exception1", RuntimeException.class);
            serviceInstaller.installJsonServices();
            Mockito.verify(serviceInstaller, Mockito.times(1)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        }

        {
            final PublishedService testService = new PublishedService();
            setupInstallJsonServices(
                    testService,
                    "{\n" +
                            "  \"Service\": {\n" +
                            "    \"name\": \"TestService1\",\n" +
                            "    \"gatewayUri\": \"/test1\",\n" +
                            "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                            "    \"policy\": [ ]\n" +
                            "  }\n" +
                            "}",
                    serviceContainer -> testService,
                    service -> null,
                    (policy, aBoolean, aBoolean2) -> {
                        throw new RuntimeException("test exception2");
                    },
                    service -> { /*nothing to do*/ }
            );
            doMockHandleError(Level.WARNING, "Failed to install JSON services: test exception2", RuntimeException.class);
            serviceInstaller.installJsonServices();
            Mockito.verify(serviceInstaller, Mockito.times(2)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        }

        {
            final PublishedService testService = new PublishedService();
            setupInstallJsonServices(
                    testService,
                    "{\n" +
                            "  \"Service\": {\n" +
                            "    \"name\": \"TestService1\",\n" +
                            "    \"gatewayUri\": \"/test1\",\n" +
                            "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                            "    \"policy\": [ ]\n" +
                            "  }\n" +
                            "}",
                    serviceContainer -> testService,
                    service -> null,
                    (policy, aBoolean, aBoolean2) -> null,
                    service -> {
                        throw new RuntimeException("test exception3");
                    }
            );
            doMockHandleError(Level.WARNING, "Failed to install JSON services: test exception3", RuntimeException.class);
            serviceInstaller.installJsonServices();
            Mockito.verify(serviceInstaller, Mockito.times(3)).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        }
    }

    @Test
    public void installJsonServices_canBeCalledMultipleTimes() throws Exception {
        final PublishedService testService = new PublishedService();
        setupInstallJsonServices(
                testService,
                "{\n" +
                        "  \"Service\": {\n" +
                        "    \"name\": \"TestService1\",\n" +
                        "    \"gatewayUri\": \"/test1\",\n" +
                        "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                        "    \"policy\": [ ]\n" +
                        "  }\n" +
                        "}",
                serviceContainer -> testService,
                service -> null,
                (policy, aBoolean, aBoolean2) -> null,
                service -> { /*nothing to do*/ }
        );

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(1)).installJsonService(Mockito.any());

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(2)).installJsonService(Mockito.any());

        serviceInstaller.installJsonServices();
        Mockito.verify(serviceInstaller, Mockito.never()).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());
        Mockito.verify(serviceInstaller, Mockito.times(3)).installJsonService(Mockito.any());
    }
}