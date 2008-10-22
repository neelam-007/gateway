package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.spring.util.SimpleSingletonBeanFactory;
import static org.junit.Assert.*;
import org.junit.*;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 *
 */
public class ServerRequestWssTimestampTest {
    private static final long MILLIS_50_YEARS = 50L * 365L * 86400L * 1000L;
    private static final long MILLIS_30_SEC = 30L * 1000L;

    @Test
    public void testExpiryPastGrace() throws Exception {
        expect(AssertionStatus.NONE, makeSass(makeServerConfig(MILLIS_30_SEC, MILLIS_50_YEARS)));
    }

    @Test
    public void testExpiryExpiredRequest() throws Exception {
        expect(AssertionStatus.BAD_REQUEST, makeSass(makeServerConfig(MILLIS_30_SEC, MILLIS_30_SEC)));
    }

    private void expect(AssertionStatus expected, ServerAssertion sass) throws IOException, SAXException, PolicyAssertionException {
        Message req = new Message();
        req.initialize(TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST));
        Message res = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(expected, result);
    }

    private ServerAssertion makeSass(final ServerConfig serverConfig) {
        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver();
        return new ServerRequestWssTimestamp(new RequestWssTimestamp(), new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("securityTokenResolver", resolver);
            put("serverConfig", serverConfig);
        }}));
    }

    private ServerConfig makeServerConfig(final long futureGrace, final long pastGrace) {
        final Map<String, String> overrides = new HashMap<String, String>() {{
            put(ServerConfig.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, Long.toString(futureGrace)); // 50 yers
            put(ServerConfig.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, Long.toString(pastGrace));
        }};

        return new ServerConfig() {
            public String getPropertyUncached(String propName) {
                String val = overrides.get(propName);
                if (val != null)
                    return val;
                return super.getPropertyUncached(propName);
            }

            public String getPropertyCached(final String propName, final long maxAge) {
                return getPropertyUncached(propName);
            }
        };
    }
}
