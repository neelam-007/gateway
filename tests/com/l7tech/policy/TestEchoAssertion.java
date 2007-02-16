package com.l7tech.policy;

import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Bean and server impl for TestEchoAssertion, a simple echo assertion for use in core test classes.
 * <p/>
 * For a real production-quality version of this, see the EchoRoutingAssertion bundled modular assertion
 * in modules/assertions/echorouting.
 * <p/>
 * This test class is a very simple echo assertion that echoes request bytes to response, with no other fancy-pants
 * features of the real echo assertion.
 */
public class TestEchoAssertion extends RoutingAssertion {
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerTestEcho.class.getName());
        return meta;
    }

    public static class ServerTestEcho extends ServerRoutingAssertion<TestEchoAssertion> {
        public ServerTestEcho(TestEchoAssertion data, ApplicationContext applicationContext) {
            super(data, applicationContext);
        }

        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            try {
                MimeKnob requestMime = context.getRequest().getMimeKnob();
                context.getResponse().initialize(new ByteArrayStashManager(),
                                                 requestMime.getOuterContentType(),
                                                 new ByteArrayInputStream(HexUtils.slurpStream(
                                                         requestMime.getEntireMessageBodyAsInputStream())));
                return AssertionStatus.NONE;
            } catch (NoSuchPartException e) {
                throw new CausedIOException(e);
            }
        }
    }
}
