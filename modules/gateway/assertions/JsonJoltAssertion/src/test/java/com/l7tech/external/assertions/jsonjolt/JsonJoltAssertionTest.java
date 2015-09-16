package com.l7tech.external.assertions.jsonjolt;

import com.bazaarvoice.jolt.Chainr;
import com.l7tech.policy.AllAssertionsTest;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test the JsonJoltAssertion.
 */
public class JsonJoltAssertionTest {

    private static final Logger log = Logger.getLogger(JsonJoltAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new JsonJoltAssertion() );
    }

}
