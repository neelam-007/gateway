package com.l7tech.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 10/1/12
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpServletResponseKnobTest {
    MockHttpServletResponse mockServletResponse;
    private Collection<Pair<String, Object>> headers;

    @Before
    public void setUp() throws Exception {
        mockServletResponse = new MockHttpServletResponse();
        headers = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testBeginChallenge_default() throws Exception {
        HttpServletResponseKnob fixture = new HttpServletResponseKnob(mockServletResponse);
        windowsChallenge(fixture);
    }

    @Test
    public void testBeginChallenge_windows() throws Exception {
        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, "windows");
        windowsChallenge(fixture);
    }

    @Test
    public void testBeginChallenge_invalid() throws Exception {
        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, "invalid");
        windowsChallenge(fixture);
    }

    @Test
    public void testBeginChallenge_blank() throws Exception {

        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, "   ");
        windowsChallenge(fixture);
    }


    @Test
    public void testBeginChallenge_empty() throws Exception {

        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, "");

        windowsChallenge(fixture);
    }

    @Test
    public void testBeginChallenge_null() throws Exception {

        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, null);
        windowsChallenge(fixture);
    }

    @Test
    public void testBeginChallenge_legacyOrder() throws Exception {
        HttpServletResponseKnob fixture = new TestHttpServletResponseKnob(mockServletResponse, "Reverse");
        Object[] expectedChallenges = {"NTLM ", "NTLM ", "Negotiate", "Digest", "Basic"};

        fixture.addChallenge("NTLM ");
        fixture.addChallenge("Basic");
        fixture.addChallenge("Digest");
        fixture.addChallenge("Negotiate");
        fixture.addChallenge("NTLM ");

        fixture.beginChallenge();
        List<Object> headers = mockServletResponse.getHeaders(HttpConstants.HEADER_WWW_AUTHENTICATE);
        assertArrayEquals(expectedChallenges, headers.toArray());

    }

    @Test
    public void beginResponse() throws Exception {
        final HttpServletResponseKnob knob = new HttpServletResponseKnob(mockServletResponse);
        knob.setStatus(200);
        knob.addCookie(new HttpCookie("http://localhost:8080", "/", "choc=chip"));
        headers.add(new Pair<String, Object>("foo", "bar"));
        headers.add(new Pair<String, Object>("foo", "bar2"));
        headers.add(new Pair<String, Object>("date", new Long(1234)));

        knob.beginResponse(headers);

        assertEquals(200, mockServletResponse.getStatus());
        assertEquals(2, mockServletResponse.getHeaderNames().size());
        final List<Object> fooHeaderValues = mockServletResponse.getHeaders("foo");
        assertEquals(2, fooHeaderValues.size());
        assertTrue(fooHeaderValues.contains("bar"));
        assertTrue(fooHeaderValues.contains("bar2"));
        final List<Object> dateHeaderValues = mockServletResponse.getHeaders("date");
        assertEquals(1, dateHeaderValues.size());
        assertEquals(new Long(1234), dateHeaderValues.get(0));
        final Cookie[] cookies = mockServletResponse.getCookies();
        assertEquals(1, cookies.length);
        assertEquals("choc", cookies[0].getName());
        assertEquals("chip", cookies[0].getValue());
    }

    private void windowsChallenge(HttpServletResponseKnob fixture) {
        fixture.addChallenge("NTLM ");
        fixture.addChallenge("Basic");
        fixture.addChallenge("Digest");
        fixture.addChallenge("Negotiate");

        Object[] expectedChallenges = {"Negotiate",  "NTLM ", "Digest", "Basic"};

        fixture.beginChallenge();
        List<Object> headers = mockServletResponse.getHeaders(HttpConstants.HEADER_WWW_AUTHENTICATE);
        assertArrayEquals(expectedChallenges, headers.toArray());
    }

    private static class TestHttpServletResponseKnob extends com.l7tech.message.HttpServletResponseKnob {
        public TestHttpServletResponseKnob(HttpServletResponse response, String order) {
            super(response);
            challengeOrder = getChallengeMode(order);
        }

    }


}
