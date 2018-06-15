package com.l7tech.server.search.processors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.module.AssertionModuleFinder;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.server.search.objects.BrokenDependency;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAssertionDependencyProcessorTest {

    @Mock
    private ClusterPropertyManager clusterPropertyManager;

    @Mock
    private EntityCrud entityCrud;

    @Mock
    private AssertionModuleFinder<ModularAssertionModule> modularAssertionModuleFinder;

    @InjectMocks
    private DefaultAssertionDependencyProcessor<Assertion> processor = new DefaultAssertionDependencyProcessor<>();

    private DefaultDependencyProcessor defaultProcessor = new DefaultDependencyProcessor();

    private final Map<com.l7tech.search.Dependency.DependencyType, InternalDependencyProcessor> dependencyProcessorMap =
            ImmutableMap.of(com.l7tech.search.Dependency.DependencyType.ASSERTION, processor,
                    com.l7tech.search.Dependency.DependencyType.ANY, defaultProcessor);

    private DependencyFinder dependencyFinder = new DependencyFinder(new HashMap<>(),
            new DependencyProcessorStore(dependencyProcessorMap));

    private static final String ENCAPSULATED_ASS_NAME = "encas name";
    private static final String ENCAPSULATED_ASS_GUID = UUID.randomUUID().toString();
    private static final String ENCAPSULATED_ASS_INPUT_ARG_NAME = "encas input argument name";
    private static final String CLUSTER_WIDE_PROP_NAME = "cluster wide property name";

    private EncapsulatedAssertion encapsulatedAssertion;
    private EncapsulatedAssertionConfig encapsulatedAssertionConfig;

    @Before
    public void setUp() {
        Mockito.when(modularAssertionModuleFinder.getModuleForClassLoader(Mockito.any())).thenReturn(null);
    }

    /**
     * Test that the dependency processor properly returns a cluster-wide property as a dependency
     * for an encapsulated assertion which takes that cluster-wide property as an input
     */
    @BugId("DE348956")
    @Test
    public void findDependencies_encasWithClusPropInput_containsClusPropDependency() throws Exception {
        initializeEncasWithClusterPropertyInput();
        Mockito.when(entityCrud.find(Mockito.any(EntityHeader.class))).thenReturn(encapsulatedAssertionConfig);

        List<Dependency> dependencies = processor.findDependencies(encapsulatedAssertion, dependencyFinder);

        // Check that there is a dependency on a CLUSTER_PROPERTY and on ENCAPSULATED_ASSERTION
        boolean clusterWidePropertyFound = false;
        boolean encasFound = false;

        Assert.assertEquals(2, dependencies.size());

        for (Dependency dependency : dependencies) {
            if (com.l7tech.search.Dependency.DependencyType.CLUSTER_PROPERTY.equals(dependency.getDependent().getDependencyType())) {
                Assert.assertEquals(CLUSTER_WIDE_PROP_NAME, dependency.getDependent().getName());
                clusterWidePropertyFound = true;
            } else if (com.l7tech.search.Dependency.DependencyType.ENCAPSULATED_ASSERTION.equals(dependency.getDependent().getDependencyType())) {
                Assert.assertEquals(ENCAPSULATED_ASS_NAME, dependency.getDependent().getName());
                encasFound = true;
            } else {
                Assert.fail("Unexpected dependency.");
            }
        }
        Assert.assertTrue("Dependency processor should find a dependency on a cluster-wide property", clusterWidePropertyFound);
        Assert.assertTrue("Dependency processor should find a dependency on encapsulated assertion config entity", encasFound);
    }

    /**
     * Test that an unavailable encas config results in no dependency on cluster-wide property (and does not throw an exception)
     */
    @BugId("DE348956")
    @Test
    public void findDependencies_encasWithUnretrievableConfig_hasBrokenDependency() throws Exception {
        initializeEncasWithClusterPropertyInput();
        Mockito.when(entityCrud.find(Mockito.any(EntityHeader.class))).thenReturn(null);

        List<Dependency> dependencies = processor.findDependencies(encapsulatedAssertion, dependencyFinder);

        Assert.assertEquals(1, dependencies.size());

        final Dependency dependency = dependencies.get(0);
        Assert.assertTrue(dependency instanceof BrokenDependency);
        Assert.assertEquals(com.l7tech.search.Dependency.DependencyType.ENCAPSULATED_ASSERTION, dependency.getDependent().getDependencyType());
    }

    public void initializeEncasWithClusterPropertyInput() throws Exception {
        encapsulatedAssertion = new EncapsulatedAssertion();
        encapsulatedAssertion.setEncapsulatedAssertionConfigGuid(ENCAPSULATED_ASS_GUID);
        encapsulatedAssertion.setEncapsulatedAssertionConfigName(ENCAPSULATED_ASS_NAME);
        // Pass-in cluster-wide property as an input argument.
        encapsulatedAssertion.putParameter(ENCAPSULATED_ASS_INPUT_ARG_NAME, "${" + "gateway." + CLUSTER_WIDE_PROP_NAME + "}");

        encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGuid(ENCAPSULATED_ASS_GUID);
        encapsulatedAssertionConfig.setName(ENCAPSULATED_ASS_NAME);

        final EncapsulatedAssertionArgumentDescriptor argumentDescriptor = new EncapsulatedAssertionArgumentDescriptor();
        argumentDescriptor.setArgumentName(ENCAPSULATED_ASS_INPUT_ARG_NAME);
        argumentDescriptor.setArgumentType(DataType.STRING.getShortName());
        argumentDescriptor.setGuiPrompt(true);
        encapsulatedAssertionConfig.setArgumentDescriptors(ImmutableSet.of(argumentDescriptor));

        Mockito.when(clusterPropertyManager.findByUniqueName(CLUSTER_WIDE_PROP_NAME)).thenReturn(new ClusterProperty(CLUSTER_WIDE_PROP_NAME, "cluster wide property value"));
    }
}