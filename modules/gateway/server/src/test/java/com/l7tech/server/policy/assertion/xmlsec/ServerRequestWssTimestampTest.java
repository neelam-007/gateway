package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.spring.util.SimpleSingletonBeanFactory;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ServerRequestWssTimestampTest {

    @Test
    public void testExpiryPastGrace() throws Exception {
        final Map<String, String> overrides = new HashMap<String, String>() {{
            put(ServerConfig.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, Long.toString(50L * 365L * 86400L * 1000L)); // 50 yers
            put(ServerConfig.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, Long.toString(50L * 365L * 86400L * 1000L));
        }};

        final ServerConfig serverConfig = new ServerConfig() {
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

        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver();

        ServerAssertion sass = new ServerRequestWssTimestamp(new RequestWssTimestamp(), new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("securityTokenResolver", resolver);
            put("serverConfig", serverConfig);
        }}));

        Message req = new Message();
        req.initialize(TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST));
        Message res = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(req, res);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }
}
