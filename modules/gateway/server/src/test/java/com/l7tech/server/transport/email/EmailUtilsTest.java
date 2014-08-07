package com.l7tech.server.transport.email;

import com.l7tech.server.transport.email.EmailUtils.StartTlsSocketFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.util.TestUtils;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class EmailUtilsTest {

    @Test
    public void testDelegation() {
        TestUtils.assertOverridesAllMethods( SSLSocketFactory.class, StartTlsSocketFactory.class );
    }

    @Test
    @BugId( "SSG-9060" )
    public void testSanitizeSubject() throws Exception {
        String subj = "Test subject\r\nwith multiple\rlines\n";
        assertEquals( "Test subjectwith multiplelines", EmailUtils.sanitizeSubject( subj ) );

        assertEquals( "", EmailUtils.sanitizeSubject( null ) );
    }
}
