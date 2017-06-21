package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.Service;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.service.resolution.NonUniqueServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickStartServiceBuilderTest extends ServiceBuilderTestBase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final Function<Pair<EncassMocker, Map<String, String>>, Pair<EncapsulatedAssertion, Map<String, String>>> TRANSFORMER = from -> {
        Assert.assertNotNull(from);
        Assert.assertNotNull(from.left);
        return Pair.pair(from.left.get(), from.right);
    };

    @BeforeClass
    public static void init() throws Exception {
        ServiceBuilderTestBase.beforeClass();
    }

    @Before
    public void setUp() throws Exception {
        super.before();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void createService() throws Exception {
        final PublishedService publishedService = serviceBuilder.createService(testServiceContainer);
        verifyService(
                publishedService,
                testServiceContainer.service,
                TRANSFORMER.apply(testEncassTemplates.get("RequireSSL")),
                TRANSFORMER.apply(testEncassTemplates.get("Cors")),
                TRANSFORMER.apply(testEncassTemplates.get("RateLimit"))
        );
    }

    @Test
    public void createEmptyService() throws Exception {
        final ServiceContainer testServiceContainer = parseJson("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [ ]\n" +
                "  }\n" +
                "}");
        final PublishedService publishedService = serviceBuilder.createService(testServiceContainer);
        Assert.assertNotNull(publishedService);
        Assert.assertThat(publishedService.getName(), Matchers.equalTo("MyService1"));
        Assert.assertThat(publishedService.getRoutingUri(), Matchers.equalTo("/MyService1"));
        Assert.assertThat(publishedService.getHttpMethodsReadOnly(), Matchers.containsInAnyOrder(HttpMethod.GET, HttpMethod.PUT));
        Assert.assertThat(publishedService.getPolicy(), Matchers.notNullValue());
        Assert.assertThat(publishedService.getPolicy().getAssertion(), Matchers.instanceOf(AllAssertion.class));
        //noinspection ConstantConditions
        Assert.assertThat(((AllAssertion)publishedService.getPolicy().getAssertion()).getChildren(), Matchers.emptyCollectionOf(Assertion.class));

        verifyService(publishedService, testServiceContainer.service);
    }

    @Test
    public void createServiceWithUnknownEncass() throws Exception {
        expectedException.expect(QuickStartPolicyBuilderException.class);
        expectedException.expectMessage(Matchers.equalToIgnoringCase("Unable to find assertion for policy template item named : UnknownEncass"));

        final ServiceContainer testServiceContainer = parseJson("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"UnknownEncass\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
            serviceBuilder.createService(testServiceContainer);
    }

    @Test
    public void createServiceWithExperimentalEncass() throws Exception {

        String experimentalAssertionName = "RequestSizeLimit";

        Assertion experimentalAssertion = mock(Assertion.class);

        final EncassMocker experimentalAssertionTemplate = EncassMocker.mock(experimentalAssertionName, ImmutableMap.of("protect", DataType.STRING));
        Mockito.doReturn(experimentalAssertionTemplate.clone()).when(assertionLocator).findAssertion(experimentalAssertionName);

        when(cachedConfig.getBooleanProperty("quickStart.allAssertions.enabled", false)).thenReturn(true);

        final ServiceContainer testServiceContainer = parseJson("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"" + experimentalAssertionName + "\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        serviceBuilder.createService(testServiceContainer);
    }

    @Test
    public void createServiceWithExperimentalEncassFailed() throws Exception {

        String experimentalAssertionName = "RequestSizeLimit";

        expectedException.expect(QuickStartPolicyBuilderException.class);
        expectedException.expectMessage(Matchers.equalToIgnoringCase("Unable to find assertion for policy template item named : " + experimentalAssertionName));

        Assertion experimentalAssertion = mock(Assertion.class);

        when(cachedConfig.getBooleanProperty("quickStart.allAssertions.enabled", false)).thenReturn(false);

        final ServiceContainer testServiceContainer = parseJson("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"" + experimentalAssertionName + "\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        serviceBuilder.createService(testServiceContainer);
    }


    @Test
    public void createServiceWithConflictResolution() throws Exception {
        expectedException.expect(QuickStartPolicyBuilderException.class);
        expectedException.expectMessage(Matchers.startsWith("Resolution parameters conflict for service '" + testServiceContainer.service.name + "' "));

        final Goid conflictServiceGoid = new Goid(1, 1);
        final NonUniqueServiceResolutionException exceptionMock = Mockito.mock(NonUniqueServiceResolutionException.class);
        Mockito.doReturn(ImmutableSet.of(conflictServiceGoid)).when(exceptionMock).getConflictingServices();
        Mockito.doReturn(ImmutableSet.of(Triple.triple("/someConflictServicePath", "soap Action", "soap namespace"))).when(exceptionMock).getParameters(Mockito.eq(conflictServiceGoid));
        Mockito.doReturn("ConflictServiceName").when(exceptionMock).getServiceName(Mockito.eq(conflictServiceGoid), Mockito.anyBoolean());
        Mockito.doThrow(exceptionMock).when(serviceCache).checkResolution(Mockito.any());
        serviceBuilder.createService(testServiceContainer);
    }

    @Test
    public void createServiceWithConflictResolutionFailure() throws Exception {
        expectedException.expect(QuickStartPolicyBuilderException.class);
        expectedException.expectMessage(Matchers.equalToIgnoringCase("Error checking for service resolution conflict."));
        expectedException.expectCause(Matchers.instanceOf(FindException.class));

        Mockito.doThrow(new ServiceResolutionException("sample ServiceResolutionException")).when(serviceCache).checkResolution(Mockito.any());
        serviceBuilder.createService(testServiceContainer);
    }

    @Test
    public void updateService() throws Exception {
        final PublishedService publishedService = serviceBuilder.createService(testServiceContainer);
        verifyService(
                publishedService,
                testServiceContainer.service,
                TRANSFORMER.apply(testEncassTemplates.get("RequireSSL")),
                TRANSFORMER.apply(testEncassTemplates.get("Cors")),
                TRANSFORMER.apply(testEncassTemplates.get("RateLimit"))
        );

        Mockito.doReturn(publishedService).when(serviceLocator).findByGoid(Mockito.any(Goid.class));

        final PublishedService updatedPublishedService = sampleUpdateService();
        Mockito.verify(serviceBuilder, Mockito.times(1)).createService(Mockito.any());
        Mockito.verify(serviceBuilder, Mockito.times(1)).updateService(Mockito.any(), Mockito.any());
        Assert.assertThat(updatedPublishedService, Matchers.sameInstance(publishedService));
    }

    @Test
    public void updateServiceWillCreateNewServiceWhenGoidIsMissing() throws Exception {
        final PublishedService publishedService = serviceBuilder.createService(testServiceContainer);
        verifyService(
                publishedService,
                testServiceContainer.service,
                TRANSFORMER.apply(testEncassTemplates.get("RequireSSL")),
                TRANSFORMER.apply(testEncassTemplates.get("Cors")),
                TRANSFORMER.apply(testEncassTemplates.get("RateLimit"))
        );

        Mockito.doReturn(null).when(serviceLocator).findByGoid(Mockito.any(Goid.class));

        final PublishedService updatedPublishedService = sampleUpdateService();
        Mockito.verify(serviceBuilder, Mockito.times(2)).createService(Mockito.any());
        Mockito.verify(serviceBuilder, Mockito.times(1)).updateService(Mockito.any(), Mockito.any());
        Assert.assertThat(updatedPublishedService, Matchers.not(Matchers.sameInstance(publishedService)));
        Assert.assertThat(updatedPublishedService.getGoid(), Matchers.not(Matchers.equalTo(publishedService.getGoid())));
        Assert.assertThat(updatedPublishedService.getName(), Matchers.not(Matchers.equalTo(publishedService.getName())));
    }

    private PublishedService sampleUpdateService() throws Exception {
        final ServiceContainer anotherServiceContainer = parseJson("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService2\",\n" +
                "    \"gatewayUri\": \"/MyService2\",\n" +
                "    \"httpMethods\": [ \"post\", \"put\", \"delete\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"mandatory\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 100,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"counter\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"CodeInjectionProtection\": {\n" +
                "          \"protect\": [ \"urlPath\", \"body\" ]\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"RouteHttp\": {\n" +
                "            \"targetUrl\": \"http://www.some_url.com\",\n" +
                "            \"httpMethod\": \"GET\"\n" +
                "        }\n" +
                "      }" +
                "    ]\n" +
                "  }\n" +
                "}");
        final EncassMocker codeInjectionProtectionTemplate = EncassMocker.mock("CodeInjectionProtection", ImmutableMap.of("protect", DataType.STRING));
        final EncassMocker routeHttpTemplate = EncassMocker.mock("RouteHttp", ImmutableMap.of("targetUrl", DataType.STRING, "httpMethod", DataType.STRING));
        Mockito.doReturn(codeInjectionProtectionTemplate.clone()).when(assertionLocator).findEncapsulatedAssertion("CodeInjectionProtection");
        Mockito.doReturn(routeHttpTemplate.get()).when(assertionLocator).findEncapsulatedAssertion("RouteHttp");

        final PublishedService updatedPublishedService = serviceBuilder.updateService(new Goid(1, 1), anotherServiceContainer);
        verifyService(
                updatedPublishedService,
                anotherServiceContainer.service,
                Pair.pair(
                        testEncassTemplates.get("RequireSSL").left.clone(),
                        ImmutableMap.of("clientCert", "mandatory")
                ),
                Pair.pair(
                        testEncassTemplates.get("RateLimit").left.clone(),
                        ImmutableMap.of("maxRequestsPerSecond", "100", "hardLimit", "true", "counterName", "counter")
                ),
                Pair.pair(
                        codeInjectionProtectionTemplate.get(),
                        ImmutableMap.of("protect", "urlPath;body")
                ),
                Pair.pair(
                        routeHttpTemplate.get(),
                        ImmutableMap.of("targetUrl", "http://www.some_url.com", "httpMethod", "GET")
                )
        );

        return updatedPublishedService;
    }

    /**
     * Verifies that the specified {@code serviceToVerify} matches expected {@code jsonServiceReference}
     * as well as it's policy matches specified {@code expectedEncassesAndParamsInOrder}
     *
     * @param serviceToVerify                     {@link PublishedService} to test.  Mandatory and cannot be {@code null}.
     * @param jsonServiceReference                {@link Service JSON service} as a reference.  Mandatory and cannot be {@code null}.
     * @param expectedEncassesAndParamsInOrder    expected {@link EncapsulatedAssertion} and it's {@link EncapsulatedAssertion#parameters} in order.  Optional in case the service policy is empty.
     */
    @SafeVarargs
    private final void verifyService(
            final PublishedService serviceToVerify,
            final Service jsonServiceReference,
            final Pair<EncapsulatedAssertion, Map<String, String>>... expectedEncassesAndParamsInOrder
    ) throws Exception {
        Assert.assertNotNull(jsonServiceReference);
        Assert.assertNotNull(serviceToVerify);
        Assert.assertThat(serviceToVerify.getName(), Matchers.equalTo(jsonServiceReference.name));
        Assert.assertThat(serviceToVerify.getFolder(), Matchers.sameInstance(rootFolder));
        Assert.assertThat(serviceToVerify.isDisabled(), Matchers.is(false));
        Assert.assertThat(serviceToVerify.isSoap(), Matchers.is(false));
        Assert.assertThat(serviceToVerify.isTracingEnabled(), Matchers.is(false));
        Assert.assertThat(serviceToVerify.getRoutingUri(), Matchers.equalTo(jsonServiceReference.gatewayUri));
        Assert.assertThat(serviceToVerify.getHttpMethodsReadOnly(), Matchers.containsInAnyOrder(jsonServiceReference.httpMethods.toArray()));
        Assert.assertThat(serviceToVerify.getWsdlUrl(), Matchers.isEmptyOrNullString());
        Assert.assertThat(serviceToVerify.getWsdlXml(), Matchers.isEmptyOrNullString());

        final Policy policy = serviceToVerify.getPolicy();
        Assert.assertNotNull(policy);
        Assert.assertThat(policy.getGuid(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(policy.getXml(), Matchers.not(Matchers.isEmptyOrNullString()));
        final Assertion rootAssertion = policy.getAssertion();
        Assert.assertNotNull(rootAssertion);
        Assert.assertThat(rootAssertion, Matchers.instanceOf(AllAssertion.class));
        final List<Assertion> children = ((AllAssertion)rootAssertion).getChildren();
        Assert.assertNotNull(children);
        Assert.assertThat(children.size(), Matchers.is(expectedEncassesAndParamsInOrder == null ? 0 : expectedEncassesAndParamsInOrder.length));
        if (expectedEncassesAndParamsInOrder != null) {
            for (int i = 0; i < children.size(); ++i) {
                Assert.assertThat(children.get(i), Matchers.instanceOf(EncapsulatedAssertion.class));
                Assert.assertThat((EncapsulatedAssertion)children.get(i), EncassMatcher.encassMatcher(expectedEncassesAndParamsInOrder[i]));
            }
        }
    }

    /**
     * Custom Matcher to match encapsulated assertion with its parameters
     */
    private static final class EncassMatcher extends BaseMatcher<EncapsulatedAssertion> {
        @NotNull
        private final ImmutablePair<EncapsulatedAssertion, Map<String, String>> expected;

        private EncassMatcher(final Pair<EncapsulatedAssertion, Map<String, String>> expected) {
            Assert.assertNotNull(expected);
            Assert.assertNotNull(expected.left);
            Assert.assertNotNull(expected.right);
            this.expected = ImmutablePair.pair(expected);
        }

        @Override
        public void describeTo(final Description description) {
            description
                    .appendValue(describeEncass(expected.left))
                    .appendText(" and params ")
                    .appendValue(toSortedMap(expected.right));
        }

        @Override
        public void describeMismatch(final Object item, final Description description) {
            description.appendText("was ");
            if (item instanceof EncapsulatedAssertion) {
                description
                        .appendText(describeEncass((EncapsulatedAssertion) item))
                        .appendText(" and params ")
                        .appendValue(toSortedMap(((EncapsulatedAssertion) item).getParameters()));
            } else {
                description.appendValue(item);
            }
        }

        @Override
        public boolean matches(final Object obj) {
            Assert.assertNotNull(obj);
            Assert.assertThat(obj, Matchers.instanceOf(EncapsulatedAssertion.class));
            final EncapsulatedAssertion encassToVerify = (EncapsulatedAssertion)obj;

            return StringUtils.equals(encassToVerify.getEncapsulatedAssertionConfigName(), expected.left.getEncapsulatedAssertionConfigName()) &&
                    StringUtils.equals(encassToVerify.getEncapsulatedAssertionConfigGuid(), expected.left.getEncapsulatedAssertionConfigGuid()) &&
                    matchesParams(encassToVerify.getParameters(), expected.right);
        }

        private boolean matchesParams(final Map<String, String> toVerify, final Map<String, String> expected) {
            return toVerify == expected || (toVerify != null && expected != null && toVerify.equals(expected));
        }

        private static String describeEncass(final EncapsulatedAssertion encapsulatedAssertion) {
            return encapsulatedAssertion != null ?
                    "EncapsulatedAssertion with name '" +
                            encapsulatedAssertion.getEncapsulatedAssertionConfigName() +
                            "' GUID '" +
                            encapsulatedAssertion.getEncapsulatedAssertionConfigGuid()
                    :
                    null;
        }

        private static Map<String, String> toSortedMap(final Map<String, String> map) {
            if (map == null)
                return null;
            else if (map instanceof SortedMap)
                return map;
            return new TreeMap<>(map);
        }

        private static EncassMatcher encassMatcher(final Pair<EncapsulatedAssertion, Map<String, String>> expected) {
            return new EncassMatcher(expected);
        }
    }

    /**
     * Utility read-only {@link Pair} wrapper class.
     */
    static final class ImmutablePair<L, R> extends Pair<L, R> {
        private ImmutablePair(@NotNull final Pair<L, R> pair) {
            this(pair.left, pair.right);
        }

        private ImmutablePair(final L left, final R right) {
            super(left, right);
        }

        @Override
        public R setValue(final R value) {
            throw new UnsupportedOperationException("immutable pair");
        }

        @NotNull
        static <L, R> ImmutablePair<L, R> pair(final Pair<L, R> pair) {
            Assert.assertNotNull(pair);
            return new ImmutablePair<>(pair);
        }

        @SuppressWarnings("unused")
        @NotNull
        public static <L,R> ImmutablePair<L,R> pair(final L left, final R right) {
            return new ImmutablePair<>(left, right);
        }
    }
}