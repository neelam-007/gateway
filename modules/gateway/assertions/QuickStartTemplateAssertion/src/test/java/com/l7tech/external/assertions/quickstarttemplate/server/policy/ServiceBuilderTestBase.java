package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.collect.ImmutableMap;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.Charsets;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("WeakerAccess")
@Ignore
public abstract class ServiceBuilderTestBase {

    @Mock
    protected ServiceCache serviceCache;

    @Mock
    protected FolderManager folderManager;

    @Mock
    protected Folder rootFolder;

    @Mock
    protected QuickStartPublishedServiceLocator serviceLocator;

    @Mock
    protected QuickStartEncapsulatedAssertionLocator assertionLocator;

    @Mock
    protected ClusterPropertyManager clusterPropertyManager;

    protected QuickStartParser parser = new QuickStartParser();

    protected ServiceContainer testServiceContainer;
    protected Map<
            String, // name of the encass
            Pair<  // pair of encass and parameters
                    EncassMocker,
                    Map<String, String> // parameters
                    >
            > testEncassTemplates;

    protected QuickStartServiceBuilder serviceBuilder;

    protected static void beforeClass() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
    }

    protected void before() throws Exception {
        Mockito.doReturn(rootFolder).when(folderManager).findRootFolder();
        Mockito.doReturn(rootFolder).when(folderManager).findByPrimaryKey(Folder.ROOT_FOLDER_ID);
        Mockito.doReturn(Folder.ROOT_FOLDER_ID).when(rootFolder).getGoid();
        Mockito.doReturn(Folder.ROOT_FOLDER_ID.toString()).when(rootFolder).getId();

        testServiceContainer = parseJson("{\n" +
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
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
        Assert.assertNotNull(testServiceContainer);
        testEncassTemplates = ImmutableMap.of(
                "RequireSSL",
                Pair.pair(
                        EncassMocker.mock("RequireSSL", ImmutableMap.of("clientCert", DataType.STRING)),
                        ImmutableMap.of("clientCert", "optional")
                ),
                "Cors",
                Pair.pair(
                        EncassMocker.mock("Cors", ImmutableMap.of()),
                        ImmutableMap.of()
                ),
                "RateLimit",
                Pair.pair(
                        EncassMocker.mock("RateLimit", ImmutableMap.of("maxRequestsPerSecond", DataType.INTEGER, "hardLimit", DataType.BOOLEAN, "counterName", DataType.STRING)),
                        ImmutableMap.of(
                                "maxRequestsPerSecond", "250",
                                "hardLimit", "true",
                                "counterName", "RateLimit-${request.clientId}-b0938b7ad6ff"
                        )
                )
        );
        Mockito.doReturn(testEncassTemplates.get("RequireSSL").left.clone()).when(assertionLocator).findEncapsulatedAssertion("RequireSSL");
        Mockito.doReturn(testEncassTemplates.get("Cors").left.clone()).when(assertionLocator).findEncapsulatedAssertion("Cors");
        Mockito.doReturn(testEncassTemplates.get("RateLimit").left.clone()).when(assertionLocator).findEncapsulatedAssertion("RateLimit");

        Mockito.doNothing().when(serviceCache).checkResolution(Mockito.any(PublishedService.class));

        serviceBuilder = Mockito.spy(new QuickStartServiceBuilder(serviceCache, folderManager, serviceLocator, assertionLocator, clusterPropertyManager));
    }


    /**
     * Utility {@link EncapsulatedAssertion} mocker that also has the ability to clone the encass.
     */
    protected static final class EncassMocker {
        private final EncapsulatedAssertion encass;
        private final Callable<EncapsulatedAssertion> mocker;

        private EncassMocker(final String name, final Map<String, DataType> descriptorMap) throws Exception {
            final String goid = UUID.randomUUID().toString();
            this.mocker = () -> mockEncass(name, goid, descriptorMap);
            this.encass = this.mocker.call();
        }

        EncapsulatedAssertion get() {
            return encass;
        }

        /**
         * Used when mocking {@link QuickStartEncapsulatedAssertionLocator#findEncapsulatedAssertion(String)} so that a different
         * instance of {@link EncapsulatedAssertion} then the one stored in test repo {@code testEncassTemplates} is returned
         * when building {@link PublishedService#getPolicy() service policy}.
         */
        @SuppressWarnings("CloneDoesntCallSuperClone")
        public EncapsulatedAssertion clone() {
            try {
                return mocker.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static EncassMocker mock(final String name, final Map<String, DataType> descriptorMap) throws Exception {
            return new EncassMocker(name, descriptorMap);
        }
    }

    protected static EncapsulatedAssertion mockEncass(final String name, final String guid, final Map<String, DataType> descriptorMap) throws Exception {
        Assert.assertThat(name, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(guid, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(descriptorMap, Matchers.notNullValue());

        final EncapsulatedAssertionConfig encassConfig = new EncapsulatedAssertionConfig();
        encassConfig.setName(name);
        encassConfig.setGuid(guid);
        encassConfig.setArgumentDescriptors(descriptorMap.entrySet().stream().map(e -> mockEncassDescriptor(e.getKey(), e.getValue())).collect(Collectors.toSet()));
        return new EncapsulatedAssertion(encassConfig);
    }

    protected static EncapsulatedAssertionArgumentDescriptor mockEncassDescriptor(final String name, final DataType type) {
        final EncapsulatedAssertionArgumentDescriptor descriptor = Mockito.mock(EncapsulatedAssertionArgumentDescriptor.class);
        Mockito.doReturn(name).when(descriptor).getArgumentName();
        Mockito.doReturn(type).when(descriptor).dataType();
        return descriptor;
    }

    protected ServiceContainer parseJson(final String jsonPayload) throws Exception {
        Assert.assertNotNull(jsonPayload);
        try (final InputStream is = new ByteArrayInputStream(jsonPayload.getBytes(Charsets.UTF8))) {
            return parser.parseJson(is);
        }
    }
}
