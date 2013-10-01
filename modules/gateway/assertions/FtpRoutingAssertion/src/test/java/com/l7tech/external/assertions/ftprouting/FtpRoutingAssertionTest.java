package com.l7tech.external.assertions.ftprouting;

import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * @author jbufu
 * @author jwilliams
 */
public class FtpRoutingAssertionTest {

    public static final String POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
        "    <L7p:FtpRoutingAssertion>\n" +
        "        <L7p:ClientCertKeyAlias stringValue=\"SSL\"/>\n" +
        "        <L7p:CredentialsSource credentialsSource=\"specified\"/>\n" +
        "        <L7p:Directory stringValue=\"/somepath\"/>\n" +
        "        <L7p:Arguments stringValue=\"ftp_aa\"/>\n" +
        "        <L7p:FileNameSource fileNameSource=\"argument\"/>\n" +
        "        <L7p:HostName stringValue=\"somehost\"/>\n" +
        "        <L7p:Password stringValue=\"pass123\"/>\n" +
        "        <L7p:Port intValue=\"990\"/>\n" +
        "        <L7p:Security security=\"ftpsImplicit\"/>\n" +
        "        <L7p:UseClientCert booleanValue=\"true\"/>\n" +
        "        <L7p:UserName stringValue=\"someuser\"/>\n" +
        "        <L7p:VerifyServerCert booleanValue=\"true\"/>\n" +
        "    </L7p:FtpRoutingAssertion>\n" +
        "</wsp:Policy>";

    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(FtpRoutingAssertion.class);
        WspReader wspr = new WspReader(registry);

        FtpRoutingAssertion assertion = (FtpRoutingAssertion) wspr.parseStrictly(POLICY, WspReader.INCLUDE_DISABLED);

        assertEquals("SSL", assertion.getClientCertKeyAlias());
        assertEquals(FtpCredentialsSource.SPECIFIED, assertion.getCredentialsSource());
        assertEquals("/somepath", assertion.getDirectory());
        assertEquals("ftp_aa", assertion.getArguments());
        assertEquals(FtpFileNameSource.ARGUMENT, assertion.getFileNameSource());
        assertEquals("somehost", assertion.getHostName());
        assertEquals("990", assertion.getPort());
        assertEquals("someuser", assertion.getUserName());
        assertEquals("pass123", assertion.getPassword());
        assertEquals(FtpSecurity.FTPS_IMPLICIT, assertion.getSecurity());
        assertEquals(true, assertion.isUseClientCert());
        assertEquals(true, assertion.isVerifyServerCert());
    }

}
