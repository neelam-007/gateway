package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import static org.junit.Assert.*;

import org.junit.*;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.io.IOException;

/**
 *
 */
public class ServerRequireWssTimestampTest {
    private static final Logger logger = Logger.getLogger(ServerRequireWssTimestampTest.class.getName());
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

    @Test
    public void testLongMaxExpirary() throws Exception {
        ServerRequireWssTimestamp sass = makeSass(makeServerConfig(MILLIS_30_SEC, MILLIS_30_SEC));
        sass.getAssertion().setMaxExpiryMilliseconds(MILLIS_50_YEARS);
        expect(AssertionStatus.BAD_REQUEST, sass);
    }

    private void expect(AssertionStatus expected, ServerAssertion sass) throws IOException, SAXException, PolicyAssertionException {
        final Message req = new Message();
        req.initialize(TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST));
        final Message res = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, res);
        final SimpleSecurityTokenResolver resolver = new SimpleSecurityTokenResolver();
        final ServerRequireWssX509Cert srwxc = new ServerRequireWssX509Cert(new RequireWssX509Cert(), makeBeanFactory(null, resolver));

        req.getSecurityKnob().setProcessorResult(WSSecurityProcessorUtils.getWssResults(req, "Request", resolver, new LogOnlyAuditor(logger)));
        srwxc.checkRequest(context);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(expected, result);
    }

    private ServerRequireWssTimestamp makeSass(final ServerConfig serverConfig) {
        return new ServerRequireWssTimestamp(new RequireWssTimestamp(), makeBeanFactory(serverConfig,null));
    }

    private BeanFactory makeBeanFactory( final Config config,
                                         final SecurityTokenResolver resolver ) {
        final SecurityTokenResolver securityTokenResolver = resolver==null ? new SimpleSecurityTokenResolver() : resolver;
        return new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("securityTokenResolver", securityTokenResolver);
            put("serverConfig", config==null ? new MockConfig(new Properties()) : config);
        }});
    }

    private ServerConfig makeServerConfig(final long futureGrace, final long pastGrace) {
        final Map<String, String> overrides = new HashMap<String, String>() {{
            put(ServerConfig.PARAM_TIMESTAMP_CREATED_FUTURE_GRACE, Long.toString(futureGrace)); // 50 yers
            put(ServerConfig.PARAM_TIMESTAMP_EXPIRES_PAST_GRACE, Long.toString(pastGrace));
        }};

        return new ServerConfig() {
            @Override
            public String getPropertyUncached(String propName) {
                String val = overrides.get(propName);
                if (val != null)
                    return val;
                return super.getPropertyUncached(propName);
            }

            @Override
            public String getPropertyCached(final String propName, final long maxAge) {
                return getPropertyUncached(propName);
            }
        };
    }
}
