package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class RestmanServiceBundleBuilderTest extends ServiceBuilderTestBase {

    private RestmanServiceBundleBuilder restmanServiceBundleBuilder;
    private PublishedService testPublishedService;

    @BeforeClass
    public static void init() throws Exception {
        beforeClass();
    }

    @Before
    public void setUp() throws Exception {
        super.before();

        testPublishedService = serviceBuilder.createService(testServiceContainer);
        Assert.assertNotNull(testPublishedService);
        restmanServiceBundleBuilder = new RestmanServiceBundleBuilder(testPublishedService);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void createBundleAsString() throws Exception {
        Assert.assertNotNull(restmanServiceBundleBuilder);
        testPublishedService.putProperty(QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD, QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP.toString());

        final String bundle = restmanServiceBundleBuilder.createBundle(String.class);
        Assert.assertThat(bundle, Matchers.not(Matchers.isEmptyOrNullString()));
        verifyBundle(bundle, testPublishedService);
    }

    @Test
    public void createBundleAsServiceMO() throws Exception {
        Assert.assertNotNull(restmanServiceBundleBuilder);
        testPublishedService.putProperty(QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD, QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP.toString());

        testService(restmanServiceBundleBuilder.createBundle(ServiceMO.class), testPublishedService);
    }

    @Test
    public void createBundleAsDocument() throws Exception {
        Assert.assertNotNull(restmanServiceBundleBuilder);
        testPublishedService.putProperty(QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD, QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP.toString());

        final Document bundleDoc = restmanServiceBundleBuilder.createBundle(Document.class);
        Assert.assertThat(bundleDoc, Matchers.notNullValue());
        verifyBundle(XmlUtil.nodeToString(bundleDoc), testPublishedService);
    }

    @Test
    public void createBundleAsMessage() throws Exception {
        Assert.assertNotNull(restmanServiceBundleBuilder);
        testPublishedService.putProperty(QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD, QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP.toString());

        final Message bundleMessage = restmanServiceBundleBuilder.createBundle(Message.class);
        Assert.assertThat(bundleMessage, Matchers.notNullValue());
        try (final InputStream inputStream = bundleMessage.getMimeKnob().getEntireMessageBodyAsInputStream()) {
            testService(ManagedObjectFactory.read(inputStream, ServiceMO.class), testPublishedService);
        }
    }

    private static void verifyBundle(final String bundle, final PublishedService testPublishedService) throws Exception {
        Assert.assertThat(bundle, Matchers.not(Matchers.isEmptyOrNullString()));
        testService(ManagedObjectFactory.read(bundle, ServiceMO.class), testPublishedService);
    }

    private static void testService(final ServiceMO serviceMO, final PublishedService testPublishedService) throws Exception {
        Assert.assertNotNull(testPublishedService);
        Assert.assertNotNull(testPublishedService.getPolicy());
        Assert.assertNotNull(serviceMO);
        Assert.assertNotNull(serviceMO.getServiceDetail());

        Assert.assertThat(serviceMO.getServiceDetail().getProperties(), MapMatcher.mapMatcher(testPublishedService.getProperties()));
        Assert.assertThat(
                serviceMO.getServiceDetail().getProperties(),
                Matchers.hasEntry(
                        String.valueOf(
                                RestmanServiceBundleBuilder.PROPERTY_PREFIX + QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD)
                        ,
                        QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP.toString()
                )
        );
        Assert.assertThat(serviceMO.getServiceDetail().getName(), Matchers.equalTo(testPublishedService.getName()));
        Assert.assertThat(serviceMO.getServiceDetail().getEnabled(), Matchers.is(!testPublishedService.isDisabled()));
        Assert.assertThat(serviceMO.getServiceDetail().getFolderId(), Matchers.equalTo(testPublishedService.getFolder().getGoid().toString()));
        Assert.assertThat(serviceMO.getServiceDetail().getServiceMappings(), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.empty())));

        final List<ServiceDetail.HttpMapping> httpMappings = serviceMO.getServiceDetail().getServiceMappings().stream()
                .filter(mapping -> mapping instanceof ServiceDetail.HttpMapping)
                .map(mapping -> (ServiceDetail.HttpMapping)mapping)
                .collect(Collectors.toList());
        Assert.assertThat(httpMappings, Matchers.allOf(Matchers.<ServiceDetail.HttpMapping>notNullValue(), Matchers.hasSize(1)));
        Assert.assertThat(httpMappings.get(0), Matchers.notNullValue());
        Assert.assertThat(httpMappings.get(0).getUrlPattern(), Matchers.equalTo(testPublishedService.getRoutingUri()));
        Assert.assertThat(httpMappings.get(0).getVerbs().stream().map(HttpMethod::valueOf).collect(Collectors.toList()), Matchers.containsInAnyOrder(testPublishedService.getHttpMethodsReadOnly().toArray()));

        Assert.assertThat(serviceMO.getResourceSets(), Matchers.notNullValue());
        final List<Resource> policyResources = serviceMO.getResourceSets().stream()
                .filter(resource -> RestmanServiceBundleBuilder.POLICY_TAG.equals(resource.getTag()))
                .flatMap(resourceSet -> resourceSet.getResources().stream().filter(resource -> RestmanServiceBundleBuilder.POLICY_TYPE.equals(resource.getType())))
                .collect(Collectors.toList());
        Assert.assertThat(policyResources, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(1)));
        Assert.assertThat(policyResources.get(0).getContent(), Matchers.allOf(Matchers.not(Matchers.isEmptyOrNullString()), Matchers.equalTo(testPublishedService.getPolicy().getXml())));
    }

    /**
     * Custom Matcher to match different impl of maps
     */
    private static final class MapMatcher extends TypeSafeMatcher<Map<?, ?>> {
        private final Map<?, ?> expected;

        private MapMatcher(final Map<?, ?> expected) {
            Assert.assertNotNull(expected);
            this.expected = expected.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(RestmanServiceBundleBuilder.PROPERTY_PREFIX + e.getKey()), Map.Entry::getValue));
        }

        @Override
        protected void describeMismatchSafely(final Map<?, ?> item, final Description description) {
            description.appendText("was ").appendValue(toSortedMap(item));
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(toSortedMap(expected));
        }

        @Override
        protected boolean matchesSafely(final Map<?, ?> item) {
            return item == expected || (item != null && expected != null && item.equals(expected));
        }

        private static <K, V> Map<K, V> toSortedMap(final Map<K, V> map) {
            if (map == null)
                return null;
            else if (map instanceof SortedMap)
                return map;
            return new TreeMap<>(map);
        }

        private static MapMatcher mapMatcher(final Map<?, ?> expected) {
            return new MapMatcher(expected);
        }
    }
}