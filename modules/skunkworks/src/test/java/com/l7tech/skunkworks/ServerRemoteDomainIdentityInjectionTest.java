package com.l7tech.skunkworks;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.proxy.util.DomainIdInjectorTest;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnobStub;
import static org.junit.Assert.assertEquals;

import com.l7tech.server.policy.assertion.transport.ServerRemoteDomainIdentityInjection;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for ServerRemoteDomainIdentityInjection.
 */
public class ServerRemoteDomainIdentityInjectionTest {

    @Test
    public void testNonHttpRequest() throws Exception {
        ServerRemoteDomainIdentityInjection sass = new ServerRemoteDomainIdentityInjection(new RemoteDomainIdentityInjection());
        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message()) );
        assertEquals(AssertionStatus.NOT_APPLICABLE, result);
    }

    @Test
    public void testMissingDomainIdStatusHeader() throws Exception {
        ServerRemoteDomainIdentityInjection sass = new ServerRemoteDomainIdentityInjection(new RemoteDomainIdentityInjection());
        Message request = new Message();
        request.attachHttpRequestKnob(new HttpRequestKnobAdapter());

        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message()) );

        assertEquals(AssertionStatus.FALSIFIED, result);
    }
    
    @Test
    public void testCorruptDomainIdStatusHeader() throws Exception {
        ServerRemoteDomainIdentityInjection sass = new ServerRemoteDomainIdentityInjection(new RemoteDomainIdentityInjection());
        Message request = new Message();
        List<HttpHeader> headers = new ArrayList<HttpHeader>();
        headers.add(new GenericHttpHeader(SecureSpanConstants.HttpHeaders.HEADER_DOMAINIDSTATUS, "lakjhsfaolkjsdh asldkjfhasldfha sdflaskdjfhas;asgh asgkjahgfalkjhg"));
        request.attachHttpRequestKnob(new HttpRequestKnobStub(headers));

        AssertionStatus result = sass.checkRequest( PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message()) );

        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }
    
    @Test
    public void testBasicUseCase() throws Exception {
        ServerRemoteDomainIdentityInjection sass = new ServerRemoteDomainIdentityInjection(new RemoteDomainIdentityInjection());
        Message request = new Message();
        request.attachHttpRequestKnob(new HttpRequestKnobStub(DomainIdInjectorTest.injectSampleHeaders().getExtraHeaders()));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(DomainIdInjectorTest.SAMPLE_USERNAME, context.getVariable("injected.user"));
        assertEquals(DomainIdInjectorTest.SAMPLE_DOMAIN, context.getVariable("injected.domain"));
        assertEquals(DomainIdInjectorTest.SAMPLE_PROGRAM, context.getVariable("injected.program"));
    }

    @Test
    public void testUnicodeValues() throws Exception {
        ServerRemoteDomainIdentityInjection sass = new ServerRemoteDomainIdentityInjection(new RemoteDomainIdentityInjection());
        Message request = new Message();
        request.attachHttpRequestKnob(new HttpRequestKnobStub(DomainIdInjectorTest.injectUnicodeHeaders().getExtraHeaders()));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(DomainIdInjectorTest.UNICODE_USERNAME, context.getVariable("injected.user"));
        assertEquals(DomainIdInjectorTest.UNICODE_DOMAIN, context.getVariable("injected.domain"));
        assertEquals(DomainIdInjectorTest.UNICODE_PROGRAM, context.getVariable("injected.program"));
    }

}
