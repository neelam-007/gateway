package com.l7tech.message;

import static org.junit.Assert.assertArrayEquals;
import com.l7tech.common.http.HttpConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.List;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 10/1/12
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpServletResponseKnobTest {


    MockHttpServletResponse mockServletResponse;

    @Before
    public void setUp() throws Exception {
        mockServletResponse = new MockHttpServletResponse();

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
