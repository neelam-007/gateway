package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ServerAddHeaderAssertion}.
 */
public class ServerAddHeaderAssertionTest {
    AddHeaderAssertion ass = new AddHeaderAssertion();
    Message mess = new Message();
    PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());

    @Test
    public void testAddHeader_newmess() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_servletRequest() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    @BugNumber(11365)
    public void testAddHeader_servletRequest_noreplace() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setRemoveExisting(false);
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(2, headers.length);
        assertEquals("orig", headers[0]);
        assertEquals("bar", headers[1]);
    }

    @Test
    @BugNumber(11365)
    public void testAddHeader_servletRequest_replace_then_noreplace() throws Exception {
        // Original incoming request contained "foo: orig"
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));


        // We now add a "foo: bar", replacing existing
        {
            ass.setHeaderName("foo");
            ass.setHeaderValue("bar");
            ass.setRemoveExisting(true);
            ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

            assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));
            OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
            assertNotNull(ohk);
            assertTrue(ohk instanceof HttpOutboundRequestFacet);
            String[] headers = ohk.getHeaderValues("foo");
            assertEquals(1, headers.length);
            assertEquals("bar", headers[0]);
        }


        // We now add a "foo: bar2", keeping existing
        {
            ass.setHeaderName("foo");
            ass.setHeaderValue("bar2");
            ass.setRemoveExisting(false);
            ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

            assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));
            OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
            assertNotNull(ohk);
            assertTrue(ohk instanceof HttpOutboundRequestFacet);
            String[] headers = ohk.getHeaderValues("foo");
            assertEquals(2, headers.length);
            assertEquals("bar", headers[0]);
            assertEquals("bar2", headers[1]);
        }


    }

    @Test
    public void testAddHeader_servletResponse() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setTarget(TargetMessageType.RESPONSE);
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletResponse hresponse = new MockHttpServletResponse();
        mess.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);

        OutboundHeadersKnob existingKnob = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(existingKnob);
        assertTrue(existingKnob instanceof HttpServletResponseKnob);

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpServletResponseKnob);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_addNewToExisting() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        headers = ohk.getHeaderValues("foo");
        assertEquals(2, headers.length);
        assertEquals("bar", headers[0]);
        assertEquals("blat", headers[1]);
    }

    @Test
    public void testAddHeader_replaceExisting() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        ass.setRemoveExisting(true);
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("blat", headers[0]);
    }

    @Test
    public void testAddHeader_contextVarName() throws Exception {
        ass.setHeaderName("${hname}");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        pec.setVariable("hname", "foo");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_contextVarValue() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("${hvalue}");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertNull(mess.getKnob(OutboundHeadersKnob.class));

        pec.setVariable("hvalue", "bar");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        OutboundHeadersKnob ohk = mess.getKnob(OutboundHeadersKnob.class);
        assertNotNull(ohk);
        assertTrue(ohk instanceof HttpOutboundRequestFacet);
        final String[] headers = ohk.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

}
