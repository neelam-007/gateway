package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class EncapsulatedAssertionConfigTest {
    private static final String NAME = "testName";
    private static final Goid GOID = new Goid(0L,1234L);
    private static final String POLICY_GUID = "policyguid";
    private static final int VERSION = 1;
    private static final String GUID = "abc123";
    private EncapsulatedAssertionConfig config;
    private EncapsulatedAssertionArgumentDescriptor in;
    private Set<EncapsulatedAssertionArgumentDescriptor> ins;
    private Policy policy;
    private Map<String, String> properties;
    private EncapsulatedAssertionResultDescriptor out;
    private Set<EncapsulatedAssertionResultDescriptor> outs;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        in = new EncapsulatedAssertionArgumentDescriptor();
        in.setArgumentName(NAME);
        in.setArgumentType(DataType.STRING.getShortName());
        ins = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        ins.add(in);
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, NAME, "xml", false);
        policy.setGuid(POLICY_GUID);
        properties = new HashMap<String, String>();
        properties.put("propKey", "propValue");
        out = new EncapsulatedAssertionResultDescriptor();
        out.setResultName(NAME);
        out.setResultType(DataType.STRING.getShortName());
        outs = new HashSet<EncapsulatedAssertionResultDescriptor>();
        outs.add(out);
        config = new EncapsulatedAssertionConfig();
        config.setArgumentDescriptors(ins);
        config.setGuid(GUID);
        config.setProperties(properties);
        config.setPolicy(policy);
        config.setResultDescriptors(outs);
        config.setName(NAME);
        config.setGoid(GOID);
        config.setVersion(VERSION);
        config.setSecurityZone(zone);
    }

    @Test
    public void getCopy() {
        final EncapsulatedAssertionConfig copy = config.getCopy();
        assertIsCopy(copy);
        // ensure copy is not locked by modifying it
        copy.setName("new name");
    }

    @Test
    public void getReadOnlyCopy() {
        final EncapsulatedAssertionConfig copy = config.getReadOnlyCopy();
        assertIsCopy(copy);
        try {
            // ensure copy is locked
            copy.setName("new name");
            fail("Expected IllegalStateException");
        } catch (final IllegalStateException e) {
            assertEquals("Cannot update locked entity", e.getMessage());
        }
    }

    @Test
    public void hasAtLeastOneGuiParameter() {
        assertFalse(config.hasAtLeastOneGuiParameter());

        in.setGuiPrompt(true);
        assertTrue(config.hasAtLeastOneGuiParameter());

        config.setArgumentDescriptors(null);
        assertFalse(config.hasAtLeastOneGuiParameter());
    }

    @Test
    public void sortedArguments() {
        ins.clear();
        // Ordinal has priority over name.
        ins.add(createArgDescriptor("apple", 2));
        ins.add(createArgDescriptor("zippity", 1));
        ins.add(createArgDescriptor("dog", 3));
        ins.add(createArgDescriptor("cat", 3));
        final List<EncapsulatedAssertionArgumentDescriptor> sorted = config.sortedArguments();
        assertEquals(4, sorted.size());
        assertEquals("zippity", sorted.get(0).getArgumentName());
        assertEquals("apple", sorted.get(1).getArgumentName());
        assertEquals("cat", sorted.get(2).getArgumentName());
        assertEquals("dog", sorted.get(3).getArgumentName());
    }

    @Test(expected = IllegalStateException.class)
    public void putPropertyLocked() {
        final EncapsulatedAssertionConfig locked = config.getReadOnlyCopy();
        locked.putProperty("locked", "locked");
    }

    @Test(expected = IllegalStateException.class)
    public void removePropertyLocked() {
        final EncapsulatedAssertionConfig locked = config.getReadOnlyCopy();
        locked.removeProperty("propKey");
    }

    @Test
    public void setPolicySetsPolicyGuid() {
        final String policyGuid = "abc";
        final Policy p = new Policy(PolicyType.INCLUDE_FRAGMENT, "Test", "policyXml", false);
        p.setGuid(policyGuid);
        config.setPolicy(p);
        assertEquals(policyGuid, config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
    }

    @Test
    public void setPolicyNull() {
        config.setPolicy(null);
        assertNull(config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
    }

    @Test
    public void detachPolicyDoesNotRemovePolicyOid() {
        assertEquals(POLICY_GUID, config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
        config.detachPolicy();
        assertNull(config.getPolicy());
        assertEquals(POLICY_GUID, config.getProperty(EncapsulatedAssertionConfig.PROP_POLICY_GUID));
    }

    private void assertIsCopy(final EncapsulatedAssertionConfig copy) {
        assertEquals(config, copy);
        assertEquals(ins, copy.getArgumentDescriptors());
        assertNotSame(ins, copy.getArgumentDescriptors());
        assertEquals(in, copy.getArgumentDescriptors().iterator().next());
        assertNotSame(in, copy.getArgumentDescriptors().iterator().next());
        assertEquals(GUID, copy.getGuid());
        assertEquals(policy, copy.getPolicy());
        assertNotSame(policy, copy.getPolicy());
        assertEquals(properties, copy.getProperties());
        assertNotSame(properties, copy.getProperties());
        assertEquals(outs, copy.getResultDescriptors());
        assertNotSame(outs, copy.getResultDescriptors());
        assertEquals(out, copy.getResultDescriptors().iterator().next());
        assertNotSame(out, copy.getResultDescriptors().iterator().next());
        assertEquals(NAME, copy.getName());
        assertEquals(GOID, copy.getGoid());
        assertEquals(VERSION, copy.getVersion());
        assertEquals(zone, copy.getSecurityZone());
    }

    private EncapsulatedAssertionArgumentDescriptor createArgDescriptor(final String name, final int ordinal) {
        final EncapsulatedAssertionArgumentDescriptor arg = new EncapsulatedAssertionArgumentDescriptor();
        arg.setArgumentName(name);
        arg.setOrdinal(ordinal);
        return arg;
    }
}
