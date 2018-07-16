package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.util.BuildInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: spreibisch
 * Date: 8/23/11
 * Time: 9:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogMessageToSysLogAssertionTest {

    private LogMessageToSysLogAssertion lmtsla;

    @Before
    public void setup()
    {
        lmtsla = new LogMessageToSysLogAssertion();
        lmtsla.setCEFEnabled(true);
    }

    @After
    public void tearDown()
    {
        lmtsla = null;
    }

    @Test
    public void testIsCEFEnabled()
    {
        assertEquals(true, lmtsla.isCEFEnabled());
    }

    @Test
    public void testGetCEFHeader()
    {
        assertEquals("CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|", LogMessageToSysLogAssertion.CEFHeaderFixed);
    }

    @Test
    public void testStringBuilderNull()
    {
        assertEquals("", new StringBuilder("").toString());
    }
}
