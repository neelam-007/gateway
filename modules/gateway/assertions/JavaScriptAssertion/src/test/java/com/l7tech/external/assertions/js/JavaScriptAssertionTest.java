package com.l7tech.external.assertions.js;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the JavaScriptAssertion.
 */
public class JavaScriptAssertionTest {

    private static WspReader wspReader;

    @BeforeClass
    public static void beforeClassSetup() {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.registerAssertion(JavaScriptAssertion.class);
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
        SyspropUtil.setProperty( "com.l7tech.policy.wsp.checkAccessors", "true" );
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new JavaScriptAssertion() );
    }

    @Test
    public void testEncodeDecodeScript() {
        final String script = "var xml='<hi>Hello World!</hi>; return true;";
        final String encodedScript = "var xml='&lt;hi>Hello World!&lt;/hi>; return true;";
        final JavaScriptAssertion assertion = new JavaScriptAssertion();

        assertion.setScript(script);
        assertEquals(script, assertion.getScript());
        assertTrue(WspWriter.getPolicyXml(assertion).contains(encodedScript));
    }

    @Test
    public void testDoubleEncodeDecodeScript() {
        final String script = "var xml='<hi>&amp;&lt;&gt;&quot;&apos;Hello World!</hi>; return true;";
        final String encodedScript = "var xml='&lt;hi>&amp;amp;&amp;lt;&amp;gt;&amp;quot;&amp;apos;Hello World!&lt;/hi>; return true;";
        final JavaScriptAssertion assertion = new JavaScriptAssertion();

        assertion.setScript(script);
        assertEquals(script, assertion.getScript());
        assertTrue(WspWriter.getPolicyXml(assertion).contains(encodedScript));
    }

}
