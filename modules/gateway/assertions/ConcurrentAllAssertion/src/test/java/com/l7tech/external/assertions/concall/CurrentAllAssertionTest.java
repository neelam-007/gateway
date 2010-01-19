package com.l7tech.external.assertions.concall;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.Arrays;

/**
 *
 */
public class CurrentAllAssertionTest {
    private static AssertionRegistry assertionRegistry;

    @BeforeClass
    public static void initWsp() throws Exception {
        assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        assertionRegistry.registerAssertion(ConcurrentAllAssertion.class);
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("set:modularAssertions", new ConcurrentAllAssertion().getFeatureSetName());
    }

    @Test
    public void testSerialization() throws Exception {
        ConcurrentAllAssertion concall = new ConcurrentAllAssertion(Arrays.asList(new TrueAssertion(), new SslAssertion()));
        assertEquals(2, concall.getChildren().size());

        String xml = WspWriter.getPolicyXml(concall);
        System.out.println("Policy xml: " + xml);
        concall = (ConcurrentAllAssertion) WspReader.getDefault().parsePermissively(xml, WspReader.Visibility.omitDisabled);
        assertNotNull(concall);
        assertNotNull(concall.getChildren());
        assertEquals(2, concall.getChildren().size());
        assertEquals(SslAssertion.class.getName(), concall.getChildren().get(1).getClass().getName());
    }
}
