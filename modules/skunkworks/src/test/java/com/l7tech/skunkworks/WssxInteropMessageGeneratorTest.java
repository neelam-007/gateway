package com.l7tech.skunkworks;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.util.HexUtils;
import org.junit.*;

import java.util.logging.Logger;

/**
 *
 */
public class WssxInteropMessageGeneratorTest {
    private static final Logger log = Logger.getLogger(WssxInteropMessageGeneratorTest.class.getName());


    @Test
    public void testUsernameTokenRequest() throws Exception {
        WssxInteropMessageGenerator gen = new WssxInteropMessageGenerator();
        final UsernameTokenImpl utok = new UsernameTokenImpl("Alice", "ecilA".toCharArray(), null, HexUtils.randomBytes(16), true);
        gen.dreq().setUsernameTokenCredentials(utok);
        log.info(XmlUtil.nodeToFormattedString(gen.generateRequest()));
    }
}
