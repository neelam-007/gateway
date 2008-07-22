/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import java.util.Calendar;

/**
 * @author mike
 */
public class AbstractSamlTokenStrategyTest extends TestCase {
    private static Logger log = Logger.getLogger(AbstractSamlTokenStrategyTest.class.getName());

    public AbstractSamlTokenStrategyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AbstractSamlTokenStrategyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testClockSkewDetection() throws Exception {
        TestStrategy goodStrat = new TestStrategy(Calendar.getInstance());
        SamlAssertion isGood = (SamlAssertion)goodStrat.getOrCreate(null);
        assertNotNull(isGood);
        assertFalse(goodStrat.isSawClockWarning());


        Calendar staleCalendar = Calendar.getInstance();
        staleCalendar.add(Calendar.MINUTE, 5);
        TestStrategy staleStrat = new TestStrategy(staleCalendar);
        SamlAssertion isStale = (SamlAssertion)staleStrat.getOrCreate(null);
        assertNotNull(isStale);
        assertTrue(staleStrat.isSawClockWarning());
    }

    private static class TestStrategy extends AbstractSamlTokenStrategy {
        private Calendar issueInstant;
        private boolean sawClockWarning;

        public TestStrategy(Calendar issueInstant) {
            super(SecurityTokenType.SAML_ASSERTION, new Object());
            this.issueInstant = issueInstant;
        }

        protected SamlAssertion acquireSamlAssertion(Ssg ssg) throws OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException, BadCredentialsException, IOException, HttpChallengeRequiredException {
            try {
                return new SamlAssertionV1( XmlUtil.stringToDocument(SAMP).getDocumentElement(), null) {
                    public Calendar getIssueInstant() {
                        return issueInstant;
                    }
                };
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }

        protected void tokenClockSkewWarning(long diff) {
            sawClockWarning = true;
        }

        public boolean isSawClockWarning() {
            return sawClockWarning;
        }
    }

    private static final String SAMP = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<saml:Assertion\n" +
                    "    xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"\n" +
                    "    xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
                    "    MajorVersion=\"1\"\n" +
                    "    MinorVersion=\"0\"\n" +
                    "    AssertionID=\"SAML-Assertion-786\"\n" +
                    "    Issuer=\"http://www.myEMarketPlace.com\"\n" +
                    "    IssueInstant=\"2003-03-11T02:00:00.173Z\">\n" +
                    "    <saml:Conditions\n" +
                    "        NotBefore=\"2003-03-11T02:00:00.173Z\"\n" +
                    "        NotOnOrAfter=\"2003-03-12T02:00:00.173Z\"/>\n" +
                    "    <saml:AuthenticationStatement\n" +
                    "        AuthenticationMethod=\"urn:ietf:rfc:3075\"\n" +
                    "        AuthenticationInstant=\"2003-03-11T02:00:00.173Z\">\n" +
                    "        <saml:Subject>\n" +
                    "            <saml:NameIdentifier\n" +
                    "                NameQualifier=\"http://www.myEMarketPlace.com\">\n" +
                    "                MyTourOperator\n" +
                    "            </saml:NameIdentifier>\n" +
                    "            <saml:SubjectConfirmation>\n" +
                    "                <saml:ConfirmationMethod>\n" +
                    "                    urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\n" +
                    "                </saml:ConfirmationMethod>\n" +
                    "                <!--\n" +
                    "                                <ds:KeyInfo>\n" +
                    "                                    <ds:KeyName>MyTourOperatorKey</ds:KeyName>\n" +
                    "                                    <ds:KeyValue> ... </ds:KeyValue>\n" +
                    "                                </ds:KeyInfo>\n" +
                    "                -->\n" +
                    "            </saml:SubjectConfirmation>\n" +
                    "        </saml:Subject>\n" +
                    "    </saml:AuthenticationStatement>\n" +
                    "    <!--    <ds:Signature>...</ds:Signature> -->\n" +
                    "</saml:Assertion>\n" +
                    "";
}
