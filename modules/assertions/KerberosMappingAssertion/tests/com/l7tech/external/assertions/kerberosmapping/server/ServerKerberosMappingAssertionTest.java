package com.l7tech.external.assertions.kerberosmapping.server;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.kerberosmapping.KerberosMappingAssertion;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * Test the KerberosMappingAssertion.
 */
public class ServerKerberosMappingAssertionTest extends TestCase {

    //- PUBLIC

    public ServerKerberosMappingAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerKerberosMappingAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test that if a mapping has an empty UPN Suffix the realm is removed.
     */
    public void testRealmRemoval() throws Exception {
        KerberosMappingAssertion kma = new KerberosMappingAssertion();
        kma.setMappings( new String[]{"QAWIN2003.COM!!"} );

        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.addCredentials( createKerberosLoginCreds("user@QAWIN2003.COM") );
        ServerKerberosMappingAssertion skma = new ServerKerberosMappingAssertion(kma, null);
        skma.checkRequest( context );

        List<LoginCredentials> creds = context.getCredentials();

        assertTrue("Mapped credentials present", !creds.isEmpty());
        LoginCredentials mappedCreds = creds.get( 0 );
                
        assertTrue("Mapped credentials are kerberos", mappedCreds.getPayload() instanceof KerberosServiceTicket);
        KerberosServiceTicket kst = (KerberosServiceTicket) mappedCreds.getPayload();

        assertEquals("Credential has no realm", "user", kst.getClientPrincipalName());
    }

    /**
     * Test that if a mapping has an empty UPN Suffix the realm is removed.
     */
    public void testRealmRemovalWithUPN() throws Exception {
        KerberosMappingAssertion kma = new KerberosMappingAssertion();
        kma.setMappings( new String[]{"QAWIN2003.COM!!"} );

        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.addCredentials( createKerberosLoginCreds("user@qaWin2003.com@QAWIN2003.COM") );
        ServerKerberosMappingAssertion skma = new ServerKerberosMappingAssertion(kma, null);
        skma.checkRequest( context );

        List<LoginCredentials> creds = context.getCredentials();

        assertTrue("Mapped credentials present", !creds.isEmpty());
        LoginCredentials mappedCreds = creds.get( 0 );

        assertTrue("Mapped credentials are kerberos", mappedCreds.getPayload() instanceof KerberosServiceTicket);
        KerberosServiceTicket kst = (KerberosServiceTicket) mappedCreds.getPayload();

        assertEquals("Credential has no realm", "user@qaWin2003.com", kst.getClientPrincipalName());
    }

    /**
     * Test that if a mapping has a UPN Suffix it is used.
     */
    public void testRealmToSuffixMapping() throws Exception {
        KerberosMappingAssertion kma = new KerberosMappingAssertion();
        kma.setMappings( new String[]{"QAWIN2003.COM!!qaWin2003.com"} );

        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.addCredentials( createKerberosLoginCreds("user@QAWIN2003.COM") );
        ServerKerberosMappingAssertion skma = new ServerKerberosMappingAssertion(kma, null);
        skma.checkRequest( context );

        List<LoginCredentials> creds = context.getCredentials();

        assertTrue("Mapped credentials present", !creds.isEmpty());
        LoginCredentials mappedCreds = creds.get( 0 );

        assertTrue("Mapped credentials are kerberos", mappedCreds.getPayload() instanceof KerberosServiceTicket);
        KerberosServiceTicket kst = (KerberosServiceTicket) mappedCreds.getPayload();

        assertEquals("Credential has mapped suffix", "user@qaWin2003.com", kst.getClientPrincipalName());
    }

    /**
     * Test realm with no mapping is passed through without change 
     */
    public void testRealmPassthrough() throws Exception {
        KerberosMappingAssertion kma = new KerberosMappingAssertion();

        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.addCredentials( createKerberosLoginCreds("user@QAWIN2003.COM") );
        ServerKerberosMappingAssertion skma = new ServerKerberosMappingAssertion(kma, null);
        skma.checkRequest( context );

        List<LoginCredentials> creds = context.getCredentials();

        assertTrue("Mapped credentials present", !creds.isEmpty());
        LoginCredentials mappedCreds = creds.get( 0 );

        assertTrue("Mapped credentials are kerberos", mappedCreds.getPayload() instanceof KerberosServiceTicket);
        KerberosServiceTicket kst = (KerberosServiceTicket) mappedCreds.getPayload();

        assertEquals("Credential unmodified", "user@QAWIN2003.COM", kst.getClientPrincipalName());
    }

    /**
     * Test when multiple mappings are used.
     */
    public void testMultipleRealmMappings() throws Exception {
        runMultipleMappingTest("user@QAWIN2003.COM", "user@qaWin2003_1.com");
        runMultipleMappingTest("user@CHILD.QAWIN2003.COM", "user@qaWin2003_2.com");
        runMultipleMappingTest("user@CHILD2.QAWIN2003.COM", "user@qaWin2003_3.com");
        runMultipleMappingTest("user@CHILD3.QAWIN2003.COM", "user@CHILD3.QAWIN2003.COM");
    }

    //- PRIVATE

    private LoginCredentials createKerberosLoginCreds( final String principal ) {
        // create dummy ticket
        KerberosServiceTicket replacementServiceTicket =
            new KerberosServiceTicket(
                    principal,
                    "http/test.l7tech.com@QAWIN2003.COM",
                    new byte[16],
                    System.currentTimeMillis() + (300000L),
                    new KerberosGSSAPReqTicket( new byte[1024] ) );

        // creds for ticket
        return new LoginCredentials(
                    null,
                    null,
                    CredentialFormat.KERBEROSTICKET,
                    HttpNegotiate.class,
                    null,
                    replacementServiceTicket);
    }

    private void runMultipleMappingTest( String principal, String expectedResult ) throws Exception {
        KerberosMappingAssertion kma = new KerberosMappingAssertion();
        kma.setMappings( new String[]{"QAWIN2003.COM!!qaWin2003_1.com",
                                      "CHILD.QAWIN2003.COM!!qaWin2003_2.com",
                                      "CHILD2.QAWIN2003.COM!!qaWin2003_3.com"} );

        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.addCredentials( createKerberosLoginCreds(principal) );
        ServerKerberosMappingAssertion skma = new ServerKerberosMappingAssertion(kma, null);
        skma.checkRequest( context );

        List<LoginCredentials> creds = context.getCredentials();

        assertTrue("Mapped credentials present", !creds.isEmpty());
        LoginCredentials mappedCreds = creds.get( 0 );

        assertTrue("Mapped credentials are kerberos", mappedCreds.getPayload() instanceof KerberosServiceTicket);
        KerberosServiceTicket kst = (KerberosServiceTicket) mappedCreds.getPayload();

        assertEquals("Credential mapped correctly", expectedResult, kst.getClientPrincipalName());

    }
}
