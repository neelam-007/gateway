package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public class EncapsulatedAssertionTest {
    private static final Goid CONFIG_ID = new Goid(0L,1L);
    private static final String CONFIG_ID_STR = CONFIG_ID.toString();
    private static final String CONFIG_GUID = UUID.randomUUID().toString();
    private static final String CONFIG_NAME = "My Special Encass";
    private EncapsulatedAssertion assertion;
    private EncapsulatedAssertionConfig config;

    @Before
    public void setup() throws Exception {
        assertion = new EncapsulatedAssertion();
        config = new EncapsulatedAssertionConfig();
        config.setGuid(CONFIG_GUID);
        config.setName(CONFIG_NAME);
        config.setGoid(CONFIG_ID);
    }

    @Test
    public void setConfig() {
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        final AssertionMetadata metaBeforeConfig = assertion.meta();

        assertion.config(config);

        assertEquals(config, assertion.config());
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
        assertEquals(CONFIG_NAME, assertion.getEncapsulatedAssertionConfigName());
        // meta should have been re-instantiated
        assertNotSame(metaBeforeConfig, assertion.meta());
    }

    @Test
    public void setConfigNull() {
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        final AssertionMetadata metaBeforeConfig = assertion.meta();

        assertion.config(null);

        assertNull(assertion.config());
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        assertNull(assertion.getEncapsulatedAssertionConfigName());
        // meta should have been re-instantiated
        assertNotSame(metaBeforeConfig, assertion.meta());
    }

    @Test
    public void constructorWithConfig() {
        assertion = new EncapsulatedAssertion(config);
        assertTrue(config == assertion.config());
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
        assertEquals(CONFIG_NAME, assertion.getEncapsulatedAssertionConfigName());
    }

    @Test
    public void constructorWithConfigNull() {
        assertion = new EncapsulatedAssertion(null);
        assertNull(assertion.config());
        assertNull(assertion.config());
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        assertNull(assertion.getEncapsulatedAssertionConfigName());
    }

    @Test
    public void params() {
        // default empty
        assertTrue(assertion.getParameterNames().isEmpty());

        // put out of order
        assertion.putParameter("c", "3");
        assertion.putParameter("a", "1");
        assertion.putParameter("B", "2");

        assertEquals(3, assertion.getParameterNames().size());
        final Iterator<String> iterator = assertion.getParameterNames().iterator();
        // should be in case-insensitive order
        assertEquals("a", iterator.next());
        assertEquals("B", iterator.next());
        assertEquals("c", iterator.next());

        assertion.removeParameter("B");
        assertEquals(2, assertion.getParameters().size());
        assertFalse(assertion.getParameterNames().contains("B"));
        assertNull(assertion.getParameter("B"));
    }

    @Test
    public void setParams() {
        final Map<String, String> before = assertion.getParameters();
        final Map<String, String> newParams = Collections.singletonMap("a", "1");

        assertion.setParameters(newParams);

        final Map<String, String> after = assertion.getParameters();
        assertSame(before, after);
        assertNotSame(newParams, after);
        assertEquals(1, after.size());
        assertTrue(assertion.getParameterNames().contains("a"));

        // setting null should clear the params
        assertion.setParameters(null);
        assertTrue(assertion.getParameters().isEmpty());
    }

    @Test
    public void getEntitiesUsedNullOrDefaultConfigId() {
        assertion.config(null);
        assertEquals(0, assertion.getEntitiesUsed().length);

        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        assertNull(assertion.getEncapsulatedAssertionConfigName());
        assertEquals(0, assertion.getEntitiesUsed().length);
    }

    @Test
    public void getEntitiesUsed() {
        assertion.config(config);
        assertEquals(1, assertion.getEntitiesUsed().length);
        final GuidEntityHeader header = (GuidEntityHeader)assertion.getEntitiesUsed()[0];
        assertEquals(CONFIG_ID_STR, header.getStrId());
        assertEquals(CONFIG_GUID, header.getGuid());
        assertEquals(CONFIG_NAME, header.getName());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, header.getType());
        assertNull(header.getDescription());
    }

    @Test
    public void replaceEntityWithoutName() {
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        final GuidEntityHeader header = new GuidEntityHeader("foo", EntityType.ENCAPSULATED_ASSERTION, null, null);
        header.setGuid(CONFIG_GUID);
        assertion.replaceEntity(null, header);
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
        assertNull(assertion.getEncapsulatedAssertionConfigName());
    }

    @Test
    public void replaceEntity() {
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        final GuidEntityHeader header = new GuidEntityHeader("foo", EntityType.ENCAPSULATED_ASSERTION, CONFIG_NAME, null);
        header.setGuid(CONFIG_GUID);
        assertion.replaceEntity(null, header);
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
        assertEquals(CONFIG_NAME, assertion.getEncapsulatedAssertionConfigName());
    }

    @Test
    public void replaceEntityNullOrWrongType() {
        assertion.config(config);
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());

        assertion.replaceEntity(null, null);
        // should not have changed
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());

        assertion.replaceEntity(null, new EntityHeader("foo", EntityType.POLICY, null, null));
        // should not have changed
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
    }

    @Test
    public void metaDefaultConfig() {
        assertion.config(null);
        final AssertionMetadata meta = assertion.meta();
        assertEquals("Encapsulated", meta.get(SHORT_NAME));
        assertNull(meta.get(BASE_64_NODE_IMAGE));
        assertEquals(ICON_RESOURCE_DIRECTORY + DEFAULT_ICON_RESOURCE_FILENAME, meta.get(PALETTE_NODE_ICON));
        assertNotNull(meta.get(ASSERTION_FACTORY));
        assertEquals(0, ((String[]) meta.get(PALETTE_FOLDERS)).length);
    }

    @Test
    public void meta() {
        config.setName("testName");
        config.putProperty(PROP_ICON_BASE64, "testIcon");
        config.putProperty(PROP_ICON_RESOURCE_FILENAME, "testFileName");
        config.putProperty(PROP_PALETTE_FOLDER, "testFolder");
        config.setArgumentDescriptors(new HashSet<EncapsulatedAssertionArgumentDescriptor>());
        assertion.config(config);

        final AssertionMetadata meta = assertion.meta();
        assertEquals("testName", meta.get(SHORT_NAME));
        assertEquals("testIcon", meta.get(BASE_64_NODE_IMAGE));
        assertEquals(ICON_RESOURCE_DIRECTORY + "testFileName", meta.get(PALETTE_NODE_ICON));
        assertNotNull(meta.get(ASSERTION_FACTORY));
        final String[] folders = (String[]) meta.get(PALETTE_FOLDERS);
        assertEquals(1, folders.length);
        assertEquals("testFolder", folders[0]);
        assertNull(meta.get(PROPERTIES_EDITOR_CLASSNAME));
    }

    @Test
    public void metaWithPropertiesDialog() {
        config.setName("testName");
        config.putProperty(PROP_ICON_BASE64, "testIcon");
        config.putProperty(PROP_ICON_RESOURCE_FILENAME, "testFileName");
        config.putProperty(PROP_PALETTE_FOLDER, "testFolder");
        final EncapsulatedAssertionArgumentDescriptor arg1 = new EncapsulatedAssertionArgumentDescriptor();
        arg1.setArgumentName("in1");
        arg1.setArgumentType(DataType.MESSAGE.getShortName());
        arg1.setGuiPrompt(true);
        config.setArgumentDescriptors(Collections.singleton(arg1));
        assertion.config(config);

        final AssertionMetadata meta = assertion.meta();
        assertEquals("com.l7tech.console.panels.encass.EncapsulatedAssertionPropertiesDialog", meta.get(PROPERTIES_EDITOR_CLASSNAME));
    }

    @Test
    public void getVariablesSetNullConfig() {
        assertion.config(null);
        assertEquals(0, assertion.getVariablesSet().length);
    }

    @Test
    public void getVariablesSet() {
        final EncapsulatedAssertionResultDescriptor out1 = new EncapsulatedAssertionResultDescriptor();
        out1.setResultName("out1");
        out1.setResultType(DataType.STRING.getShortName());
        final EncapsulatedAssertionResultDescriptor out2 = new EncapsulatedAssertionResultDescriptor();
        out2.setResultName("out2");
        out2.setResultType(DataType.MESSAGE.getShortName());
        final Set<EncapsulatedAssertionResultDescriptor> outs = new HashSet<EncapsulatedAssertionResultDescriptor>();
        outs.add(out1);
        outs.add(out2);
        config.setResultDescriptors(outs);
        assertion.config(config);

        final VariableMetadata[] variablesSet = assertion.getVariablesSet();
        assertEquals(2, variablesSet.length);
        assertEquals("out1", variablesSet[0].getName());
        assertEquals(DataType.STRING, variablesSet[0].getType());
        assertTrue(variablesSet[0].isMultivalued());
        assertFalse(variablesSet[0].isPrefixed());
        assertEquals("out2", variablesSet[1].getName());
        assertEquals(DataType.MESSAGE, variablesSet[1].getType());
        assertTrue(variablesSet[1].isMultivalued());
        assertFalse(variablesSet[1].isPrefixed());
    }

    @Test
    public void getVariablesUsedNullConfig() {
        assertion.config(null);
        assertEquals(0, assertion.getVariablesUsed().length);
    }

    @Test
    public void getVariablesUsed() {
        final EncapsulatedAssertionArgumentDescriptor arg1 = new EncapsulatedAssertionArgumentDescriptor();
        arg1.setArgumentName("in1");
        arg1.setArgumentType(DataType.MESSAGE.getShortName());
        arg1.setGuiPrompt(false);
        final EncapsulatedAssertionArgumentDescriptor arg2 = new EncapsulatedAssertionArgumentDescriptor();
        arg2.setArgumentName("in2");
        arg2.setGuiPrompt(true);
        arg2.setArgumentType(DataType.STRING.getShortName());
        final Set<EncapsulatedAssertionArgumentDescriptor> args = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        args.add(arg1);
        args.add(arg2);
        config.setArgumentDescriptors(args);
        assertion.config(config);
        assertion.putParameter("in2", "${in2a}${in2b}");

        final String[] varsUsed = assertion.getVariablesUsed();
        assertEquals(3, varsUsed.length);
        assertEquals("in1", varsUsed[0]);
        assertEquals("in2a", varsUsed[1]);
        assertEquals("in2b", varsUsed[2]);
    }

    @Test
    public void getEntitiesUsedAtDesignTime() {
        assertion.config(null);
        assertion.setEncapsulatedAssertionConfigGuid(CONFIG_GUID);
        assertEquals(1, assertion.getEntitiesUsedAtDesignTime().length);
        assertArrayEquals(assertion.getEntitiesUsed(), assertion.getEntitiesUsedAtDesignTime());
        final EntityHeader header = assertion.getEntitiesUsedAtDesignTime()[0];
        assertEquals(PersistentEntity.DEFAULT_GOID.toString(), header.getStrId());
        assertEquals(CONFIG_GUID, ((GuidEntityHeader) header).getGuid());
        assertEquals(EntityType.ENCAPSULATED_ASSERTION, header.getType());
        assertNull(header.getName());
        assertNull(header.getDescription());
    }

    @Test
    public void needsProvideEntityNullConfig() {
        assertion.config(null);
        assertTrue(assertion.needsProvideEntity(new EntityHeader("", EntityType.ENCAPSULATED_ASSERTION, null, null)));
    }

    @Test
    public void needsProvideEntityNonNullConfig() {
        assertion.config(config);
        assertFalse(assertion.needsProvideEntity(new EntityHeader("", EntityType.ENCAPSULATED_ASSERTION, null, null)));
    }

    @Test
    public void needsProvideEntityWrongType() {
        assertFalse(assertion.needsProvideEntity(new EntityHeader("", EntityType.POLICY, null, null)));
    }

    @Test
    public void provideEntity() {
        assertion.config(null);
        assertion.provideEntity(new EntityHeader(), config);
        assertEquals(config, assertion.config());
        assertEquals(CONFIG_GUID, assertion.getEncapsulatedAssertionConfigGuid());
    }

    @Test
    public void provideEntityInvalidType() {
        assertion.config(null);
        assertion.provideEntity(new EntityHeader(), new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false));
        assertNull(assertion.config());
        assertNull(assertion.getEncapsulatedAssertionConfigGuid());
        assertNull(assertion.getEncapsulatedAssertionConfigName());
    }

    @Test
    public void updateTemporaryData() {
        final EncapsulatedAssertionConfig newConfig = new EncapsulatedAssertionConfig();
        final EncapsulatedAssertion newAssertion = new EncapsulatedAssertion(newConfig);
        assertion.updateTemporaryData(newAssertion);
        assertEquals(newConfig, assertion.config());
    }

    @Test
    public void updateTemporaryDataWrongType() {
        assertion.config(config);
        assertion.updateTemporaryData(new AllAssertion());
        // should not have changed
        assertEquals(config, assertion.config());
    }

    @Test
    public void doClone() {
        final EncapsulatedAssertionArgumentDescriptor arg1 = new EncapsulatedAssertionArgumentDescriptor();
        arg1.setArgumentName("in1");
        arg1.setArgumentType(DataType.MESSAGE.getShortName());
        arg1.setGuiPrompt(false);
        final EncapsulatedAssertionResultDescriptor out1 = new EncapsulatedAssertionResultDescriptor();
        out1.setResultName("out1");
        out1.setResultType(DataType.STRING.getShortName());
        config.setArgumentDescriptors(Collections.singleton(arg1));
        config.setResultDescriptors(Collections.singleton(out1));
        assertion.config(config);
        assertion.putParameter("param1", "param1Val");

        final EncapsulatedAssertion clone = (EncapsulatedAssertion) assertion.clone();

        assertEquals(clone.getParameters(), assertion.getParameters());
        assertEquals(clone.config(), assertion.config());
    }

    @Test
    public void testPolicyName() {
        assertion.config( config );
        AssertionNodeNameFactory<EncapsulatedAssertion> nameFactory = assertion.meta().get( AssertionMetadata.POLICY_NODE_NAME_FACTORY );
        String policyName = nameFactory.getAssertionName( assertion, true );
        assertEquals( CONFIG_NAME, policyName );
    }

    @Test
    public void testPolicyName_configMissing() {
        assertion.config( null );
        AssertionNodeNameFactory<EncapsulatedAssertion> nameFactory = assertion.meta().get( AssertionMetadata.POLICY_NODE_NAME_FACTORY );
        String policyName = nameFactory.getAssertionName( assertion, true );
        assertTrue( policyName.contains( "(Missing)" ) );
    }

    @Test
    public void testPolicyName_configMissing_optional() {
        assertion.config( null );
        assertion.setNoOpIfConfigMissing( true );
        AssertionNodeNameFactory<EncapsulatedAssertion> nameFactory = assertion.meta().get( AssertionMetadata.POLICY_NODE_NAME_FACTORY );
        String policyName = nameFactory.getAssertionName( assertion, true );
        assertTrue( policyName.contains( "(Not Available in Current Environment)" ) );
    }


}
