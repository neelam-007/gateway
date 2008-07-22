package com.l7tech.xml.tarari;

import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;
import com.l7tech.xml.tarari.TarariMessageContext;
//import com.l7tech.common.TestDocuments;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.test.BugNumber;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test Layer 7's support code for Tarari fastxpath.
 *
 * TODO [steve] create profile for this tarari test?
 */
public class TarariXpathTest {
    private static final Logger log = Logger.getLogger(TarariXpathTest.class.getName());

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @BugNumber(4779)
    public void testBug4779IsInstalled() throws Exception {
        System.setProperty(TarariLoader.ENABLE_PROPERTY, "true");
        GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        assertNotNull("Tarari was not detected; Tarari is required for this test to run", gtc);

        gtc.compileAllXpaths();

        CompiledXpath xpFail = new XpathExpression("/thisIsSupposted/ToFail").compile();
        CompiledXpath xpSucc = XpathExpression.soapBodyXpathValue().compile();

        TarariMessageContextFactory mcf = TarariLoader.getMessageContextFactory();
        for (int i = 0; i < 1000; ++i) {
            TarariMessageContext tmc = null;
            try {
                tmc = mcf.makeMessageContext(null/*TestDocuments.getInputStream(TestDocuments.DOTNET_SIGNED_REQUEST)*/);

                ElementCursor cursor = tmc.getElementCursor();
                cursor.moveToRoot();

                assertFalse("Should fail; iteration #" + i, cursor.matches(xpFail));
                assertTrue("Should succeed; iteration #" + i, cursor.matches(xpSucc));
            } finally {
                if (tmc != null) tmc.close();
            }
        }
    }
}
