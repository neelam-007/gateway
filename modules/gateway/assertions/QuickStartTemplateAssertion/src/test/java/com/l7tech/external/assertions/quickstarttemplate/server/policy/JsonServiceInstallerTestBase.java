package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.external.assertions.quickstarttemplate.server.utils.L7Matchers;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.*;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 */
@SuppressWarnings("WeakerAccess")
@Ignore
public abstract class JsonServiceInstallerTestBase {

    @Rule
    public final TemporaryFolder jsonBootstrapFolder = new TemporaryFolder();

    @Mock
    protected QuickStartServiceBuilder serviceBuilder;

    @Mock
    protected ServiceManager serviceManager;

    @Mock
    protected PolicyVersionManager policyVersionManager;

    protected QuickStartJsonServiceInstaller serviceInstaller;

    protected abstract QuickStartJsonServiceInstaller createJsonServiceInstaller(
            final QuickStartServiceBuilder serviceBuilder,
            final ServiceManager serviceManager,
            final PolicyVersionManager policyVersionManager,
            final QuickStartParser parser
    ) throws Exception;


    protected void before() throws Exception {
        serviceInstaller = Mockito.spy(createJsonServiceInstaller(serviceBuilder, serviceManager, policyVersionManager, new QuickStartParser()));
        // mock our temporary folder as the bootstrap folder
        Mockito.doReturn(jsonBootstrapFolder.getRoot().toPath()).when(serviceInstaller).getBootstrapFolder();
    }

    protected void after() throws Exception {
        jsonBootstrapFolder.delete();
    }


    protected static class ExceptionArgMatcher<E extends Throwable> extends TypeSafeMatcher<E> {
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

    protected PublishedService setupSuccessfullyInstallJsonServicesWithValidPayload() throws Exception {
        final PublishedService testService = new PublishedService();
        setupSuccessfullyInstallJsonServicesWithValidPayload(testService);
        return testService;
    }

    protected void setupSuccessfullyInstallJsonServicesWithValidPayload(final PublishedService testService) throws Exception {
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

    protected void setupInstallJsonServicesWithValidPayload(
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

    protected void setupInstallJsonServices(
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

    protected void setupInstallJsonServices(
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

    protected static void writeToFile(final File file, final String payload) throws Exception {
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

    protected void doMockHandleError(
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
                Assert.assertThat(message, L7Matchers.matchesRegex(expectedErrorMessageRegex));
            }

            final Object param3 = invocation.getArguments()[2];
            Assert.assertThat("Third Param is Throwable", param3, Matchers.instanceOf(Throwable.class));
            final Throwable exception = (Throwable)param3;
            Assert.assertThat(exception, Matchers.instanceOf(expectedException));

            return invocation.callRealMethod();
        }).when(serviceInstaller).handleError(Mockito.any(), Mockito.anyString(), Mockito.any());

    }

    protected void doMockInstallJsonService(final Functions.UnaryVoid<Path> callable) throws Exception {
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

    protected void doMockInstallJsonService(
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
