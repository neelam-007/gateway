package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Level;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickStartJsonServiceInstallerTest {

    @Rule
    public final TemporaryFolder jsonBootstrapFolder = new TemporaryFolder();

    @Mock
    private QuickStartServiceBuilder serviceBuilder;

    @Mock
    private ServiceManager serviceManager;

    @Mock
    private PolicyVersionManager policyVersionManager;

    private static final Functions.UnaryVoid<Path> nothingToDoCallback = path -> {/* NOTHING TO DO*/};

    private QuickStartJsonServiceInstaller serviceInstaller;

    @Before
    public void setUp() throws Exception {
        serviceInstaller = Mockito.spy(new QuickStartJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, new QuickStartParser()));
        // mock our temporary folder as the bootstrap folder
        Mockito.doReturn(jsonBootstrapFolder.getRoot().toPath()).when(serviceInstaller).getBootstrapFolder();
    }

    @After
    public void tearDown() throws Exception {
        jsonBootstrapFolder.delete();
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

    private static class ExceptionArgMatcher<E extends Throwable> extends TypeSafeMatcher<E> {
        private final Class<E> exClass;

        ExceptionArgMatcher(final Class<E> exClass) {
            Assert.assertNotNull(exClass);
            this.exClass = exClass;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("matches exception of type \"" + exClass.getName() + "\"");
        }

        @Override
        protected boolean matchesSafely(final E ex) {
            Assert.assertThat(ex, Matchers.notNullValue());
            return exClass.equals(ex.getClass());
        }

        static <E extends Throwable> ExceptionArgMatcher<E> matchesException(final Class<E> exClass) {
            Assert.assertThat(exClass, Matchers.notNullValue());
            return new ExceptionArgMatcher<>(exClass);
        }
    }

    private static class RegexMatcher extends TypeSafeMatcher<String> {
        private final String regex;

        RegexMatcher(final String regex) {
            this.regex = regex;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("matches regular expression=\"" + regex + "\"");
        }

        @Override
        public boolean matchesSafely(final String string) {
            Assert.assertThat(string, Matchers.notNullValue());
            return string.matches(regex);
        }

        // matcher method you can call on this matcher class
        static RegexMatcher matchesRegex(final String regex) {
            Assert.assertThat(regex, Matchers.not(Matchers.isEmptyOrNullString()));
            return new RegexMatcher(regex);
        }
    }

    private PublishedService setupSuccessfullyInstallJsonServicesWithValidPayload() throws Exception {
        final PublishedService testService = new PublishedService();
        setupSuccessfullyInstallJsonServicesWithValidPayload(testService);
        return testService;
    }

    private void setupSuccessfullyInstallJsonServicesWithValidPayload(final PublishedService testService) throws Exception {
        setupInstallJsonServicesWithValidPayload(
                testService,
                serviceContainer -> {
                    Assert.assertNotNull(serviceContainer);
                    // don't care about the service so we are gonna return our test service
                    return testService;
                },
                service -> {
                    Assert.assertThat(service, Matchers.sameInstance(testService));
                    return Goid.DEFAULT_GOID; // don;t really care about the result
                },
                (policy, activated, newEntity) -> {
                    Assert.assertThat(policy, Matchers.sameInstance(testService.getPolicy()));
                    Assert.assertNotNull(activated);
                    Assert.assertNotNull(newEntity);
                    return null;
                },
                service -> Assert.assertThat(service, Matchers.sameInstance(testService))
        );
    }

    private void setupInstallJsonServicesWithValidPayload(
            final PublishedService testService,
            final Functions.UnaryThrows<PublishedService, ServiceContainer, Exception> serviceBuilderCreateServiceCallback,
            final Functions.UnaryThrows<Goid, PublishedService, Exception> serviceManagerSaveCallback,
            final Functions.TernaryThrows<PolicyVersion, Policy, Boolean, Boolean, Exception> policyVersionManagerCheckpointPolicyCallback,
            final Functions.UnaryVoidThrows<PublishedService, Exception> serviceManagerCreateRolesCallback
    ) throws Exception {
        setupInstallJsonServices(
                testService,
                "{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"TestService1\",\n" +
                "    \"gatewayUri\": \"/test1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\"],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"CredentialSourceHttpBasic\": { }\n" +
                "      },\n" +
                "      {\n" +
                "        \"CodeInjectionProtection\": {\n" +
                "            \"protect\": [ \"urlPath\", \"body\" ]\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"RouteHttp\": {\n" +
                "            \"targetUrl\": \"http://www.test1.com\",\n" +
                "            \"httpMethod\": \"GET\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}",
                serviceBuilderCreateServiceCallback,
                serviceManagerSaveCallback,
                policyVersionManagerCheckpointPolicyCallback,
                serviceManagerCreateRolesCallback
        );
    }

    private void setupInstallJsonServices(
            final PublishedService testService,
            final String payload,
            final Functions.UnaryThrows<PublishedService, ServiceContainer, Exception> serviceBuilderCreateServiceCallback,
            final Functions.UnaryThrows<Goid, PublishedService, Exception> serviceManagerSaveCallback,
            final Functions.TernaryThrows<PolicyVersion, Policy, Boolean, Boolean, Exception> policyVersionManagerCheckpointPolicyCallback,
            final Functions.UnaryVoidThrows<PublishedService, Exception> serviceManagerCreateRolesCallback
    ) throws Exception {
        setupInstallJsonServices(
                testService,
                payload,
                null,
                serviceBuilderCreateServiceCallback,
                serviceManagerSaveCallback,
                policyVersionManagerCheckpointPolicyCallback,
                serviceManagerCreateRolesCallback
        );
    }

    private void setupInstallJsonServices(
            final PublishedService testService,
            final String payload,
            final String fileName,
            final Functions.UnaryThrows<PublishedService, ServiceContainer, Exception> serviceBuilderCreateServiceCallback,
            final Functions.UnaryThrows<Goid, PublishedService, Exception> serviceManagerSaveCallback,
            final Functions.TernaryThrows<PolicyVersion, Policy, Boolean, Boolean, Exception> policyVersionManagerCheckpointPolicyCallback,
            final Functions.UnaryVoidThrows<PublishedService, Exception> serviceManagerCreateRolesCallback
    ) throws Exception {
        Assert.assertNotNull(testService);

        final File jsonFile;
        if (StringUtils.isNotBlank(fileName)) {
            jsonFile = jsonBootstrapFolder.newFile(fileName);
        } else {
            jsonFile = File.createTempFile("testJson", ".json", jsonBootstrapFolder.getRoot());
        }
        writeToFile(jsonFile, payload);

        doMockInstallJsonService(serviceBuilderCreateServiceCallback, serviceManagerSaveCallback, policyVersionManagerCheckpointPolicyCallback, serviceManagerCreateRolesCallback);
        Assert.assertThat(
                testService.getProperties(),
                Matchers.not(
                        Matchers.hasEntry(
                                QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD,
                                String.valueOf(QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP)
                        )
                )
        );
    }

    private static void writeToFile(final File file, final String payload) throws Exception {
        Assert.assertNotNull(file);
        Assert.assertNotNull(file.exists());
        Assert.assertNotNull(!file.isDirectory());
        Assert.assertNotNull(file.isFile());
        Assert.assertNotNull(file.canWrite());
        if (StringUtils.isNotBlank(payload)) {
            try (
                    final OutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
                    final InputStream iStream = new ByteArrayInputStream(payload.getBytes(Charsets.UTF8))
            ) {
                IOUtils.copyStream(iStream, oStream);
            }
            Assert.assertThat(new String(IOUtils.slurpFile(file)), Matchers.equalTo(payload));
        }
    }

    private void doMockHandleError(
            final Level expectedLevel,
            final String expectedErrorMessageRegex, // nullable
            final Class<? extends Throwable> expectedException
    ) {
        Assert.assertNotNull(expectedLevel);
        Assert.assertNotNull(expectedException);

        Mockito.doAnswer(invocation -> {
            Assert.assertThat(invocation, Matchers.notNullValue());
            Assert.assertThat("Three params", invocation.getArguments().length, Matchers.is(3));
            final Object param1 = invocation.getArguments()[0];
            Assert.assertThat("First Param is Level", param1, Matchers.instanceOf(Level.class));
            final Level level = (Level)param1;
            Assert.assertThat(level, Matchers.sameInstance(expectedLevel));

            final Object param2 = invocation.getArguments()[1];
            Assert.assertThat("Second Param is String", param2, Matchers.instanceOf(String.class));
            final String message = (String)param2;
            if (StringUtils.isBlank(expectedErrorMessageRegex)) {
                Assert.assertThat(message, Matchers.isEmptyOrNullString());
            } else {
                Assert.assertThat(message, RegexMatcher.matchesRegex(expectedErrorMessageRegex));
            }

            final Object param3 = invocation.getArguments()[2];
            Assert.assertThat("Third Param is Throwable", param3, Matchers.instanceOf(Throwable.class));
            final Throwable exception = (Throwable)param3;
            Assert.assertThat(exception, Matchers.instanceOf(expectedException));

            return invocation.callRealMethod();
        }).when(serviceInstaller).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());

    }

    private void doMockInstallJsonService(final Functions.UnaryVoid<Path> callable) throws Exception {
        Assert.assertNotNull(callable);

        Mockito.doAnswer(invocation -> {
            Assert.assertThat(invocation, Matchers.notNullValue());
            Assert.assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
            final Object param1 = invocation.getArguments()[0];
            Assert.assertThat("First Param is Path", param1, Matchers.instanceOf(Path.class));
            final Path path = (Path)param1;
            Assert.assertThat(path, Matchers.notNullValue());
            // get the module from our repository
            callable.call(path);
            return null;
        }).when(serviceInstaller).installJsonService(Mockito.any(Path.class));
    }

    private void doMockInstallJsonService(
            final Functions.UnaryThrows<PublishedService, ServiceContainer, Exception> serviceBuilderCreateServiceCallback,
            final Functions.UnaryThrows<Goid, PublishedService, Exception> serviceManagerSaveCallback,
            final Functions.TernaryThrows<PolicyVersion, Policy, Boolean, Boolean, Exception> policyVersionManagerCheckpointPolicyCallback,
            final Functions.UnaryVoidThrows<PublishedService, Exception> serviceManagerCreateRolesCallback
    ) throws Exception {
        if (serviceBuilderCreateServiceCallback != null) {
            Mockito.doAnswer(invocation -> {
                Assert.assertThat(invocation, Matchers.notNullValue());
                Assert.assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                Assert.assertThat("First Param is ServiceContainer", param1, Matchers.instanceOf(ServiceContainer.class));
                final ServiceContainer serviceContainer = (ServiceContainer) param1;
                return serviceBuilderCreateServiceCallback.call(serviceContainer);
            }).when(serviceBuilder).createService(Mockito.any());
        }

        if (serviceManagerSaveCallback != null) {
            Mockito.doAnswer(invocation -> {
                Assert.assertThat(invocation, Matchers.notNullValue());
                Assert.assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                Assert.assertThat("First Param is PublishedService", param1, Matchers.instanceOf(PublishedService.class));
                final PublishedService publishedService = (PublishedService) param1;
                return serviceManagerSaveCallback.call(publishedService);
            }).when(serviceManager).save(Mockito.any());
        }

        if (policyVersionManagerCheckpointPolicyCallback != null) {
            Mockito.doAnswer(invocation -> {
                Assert.assertThat(invocation, Matchers.notNullValue());
                Assert.assertThat("Three params", invocation.getArguments().length, Matchers.is(3));
                final Object param1 = invocation.getArguments()[0];
                Assert.assertThat("First Param is Policy", param1, Matchers.instanceOf(Policy.class));
                final Policy policy = (Policy) param1;
                final Object param2 = invocation.getArguments()[1];
                Assert.assertThat("Second Param is Boolean", param2, Matchers.instanceOf(Boolean.class));
                final Boolean activated = (Boolean) param2;
                final Object param3 = invocation.getArguments()[2];
                Assert.assertThat("Third Param is Boolean", param3, Matchers.instanceOf(Boolean.class));
                final Boolean newEntity = (Boolean) param3;
                return policyVersionManagerCheckpointPolicyCallback.call(policy, activated, newEntity);
            }).when(policyVersionManager).checkpointPolicy(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        }

        if (serviceManagerCreateRolesCallback != null) {
            Mockito.doAnswer(invocation -> {
                Assert.assertThat(invocation, Matchers.notNullValue());
                Assert.assertThat("Only one param", invocation.getArguments().length, Matchers.is(1));
                final Object param1 = invocation.getArguments()[0];
                Assert.assertThat("First Param is PublishedService", param1, Matchers.instanceOf(PublishedService.class));
                final PublishedService publishedService = (PublishedService) param1;
                serviceManagerCreateRolesCallback.call(publishedService);
                return null;
            }).when(serviceManager).createRoles(Mockito.any());
        }
    }
}