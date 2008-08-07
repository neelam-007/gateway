package com.l7tech.xml.tarari;

import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.test.BugNumber;
import com.l7tech.test.SystemPropertySwitchedRunner;
import com.l7tech.test.SystemPropertyPrerequisite;
import com.l7tech.common.TestDocuments;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Layer 7's support code for Tarari fastxpath.
 */
@RunWith(SystemPropertySwitchedRunner.class)
public class TarariXpathTest {

    @Test
    @BugNumber(4779)
    @SystemPropertyPrerequisite(require = TarariLoader.ENABLE_PROPERTY)
    public void testBug4779IsInstalled() throws Exception {
        GlobalTarariContext gtc = TarariLoader.getGlobalContext();
        assertNotNull("Tarari was not detected; Tarari is required for this test to run", gtc);

        gtc.compileAllXpaths();

        CompiledXpath xpFail = new XpathExpression("/thisIsSupposted/ToFail").compile();
        CompiledXpath xpSucc = XpathExpression.soapBodyXpathValue().compile();

        TarariMessageContextFactory mcf = TarariLoader.getMessageContextFactory();
        for (int i = 0; i < 1000; ++i) {
            TarariMessageContext tmc = null;
            try {
                tmc = mcf.makeMessageContext(TestDocuments.getInputStream(TestDocuments.DOTNET_SIGNED_REQUEST));

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
