package com.l7tech.external.assertions.siteminder;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.test.BugId;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Element;

import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 11/1/13
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteMinderExternalReferenceTest {
    static final String REFERENCES_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n";
    static final String REFERENCES_END = "</exp:References>";

    static final String SM_REFERENCE_ELEM_1 = "    <SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
            "        <GOID>4371e03e76a66cadd128dbfd08d9d9e6</GOID>\n" +
            "        <siteMinderConfiguration id=\"4371e03e76a66cadd128dbfd08d9d9e6\"\n" +
            "            version=\"0\" xmlns:ns2=\"http://ns.l7tech.com/secureSpan/1.0/core\">\n" +
            "            <ns2:name>yuri-12sp3compat-shared</ns2:name>\n" +
            "            <address>127.0.0.1</address>\n" +
            "            <clusterThreshold>50</clusterThreshold>\n" +
            "            <enabled>true</enabled>\n" +
            "            <fipsmode>1</fipsmode>\n" +
            "            <hostConfiguration>Layer7HostSettings</hostConfiguration>\n" +
            "            <hostname>yuri-12sp3compat-shared</hostname>\n" +
            "            <ipcheck>false</ipcheck>\n" +
            "            <nonClusterFailover>false</nonClusterFailover>\n" +
            "            <properties>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.address</key>\n" +
            "                    <value>10.7.34.32</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.authentication.port</key>\n" +
            "                    <value>44442</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.accounting.port</key>\n" +
            "                    <value>44441</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.connection.max</key>\n" +
            "                    <value>3</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.connection.min</key>\n" +
            "                    <value>1</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.authorization.port</key>\n" +
            "                    <value>44443</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.connection.step</key>\n" +
            "                    <value>1</value>\n" +
            "                </entry>\n" +
            "                <entry>\n" +
            "                    <key>server.0.0.timeout</key>\n" +
            "                    <value>60</value>\n" +
            "                </entry>\n" +
            "            </properties>\n" +
            "            <updateSSOToken>false</updateSSOToken>\n" +
            "            <userName>siteminder</userName>\n" +
            "        </siteMinderConfiguration>\n" +
            "    </SiteMinderConfigurationReference>\n";
    public static final String SITEMINDER_REF_GOID = "4371e03e76a66cadd128dbfd08d9d9e6";
    public static final String REPLACE_GOID = "4371e03e76a66cadd128dbfd08d9d9c4";

    @Mock
    private ExternalReferenceFinder mockFinder;

    private SiteMinderExternalReference fixture;
    private SiteMinderConfiguration siteMinderConfiguration;


    @Before
    public void setUp() throws Exception {
        siteMinderConfiguration = new SiteMinderConfiguration();
        siteMinderConfiguration.setGoid(new Goid(SITEMINDER_REF_GOID));
        siteMinderConfiguration.setAddress("127.0.0.1");
        siteMinderConfiguration.setName("yuri-12sp3compat-shared");
        siteMinderConfiguration.setClusterThreshold(50);
        siteMinderConfiguration.setEnabled(true);
        siteMinderConfiguration.setFipsmode(1);
        siteMinderConfiguration.setHostConfiguration("Layer7HostSettings");
        siteMinderConfiguration.setHostname("yuri-12sp3compat-shared");
        siteMinderConfiguration.setIpcheck(false);
        siteMinderConfiguration.setUpdateSSOToken(false);
        siteMinderConfiguration.setUserName("siteminder");
        Map<String,String> siteMinderProps = new LinkedHashMap<>();
        siteMinderProps.put("server.0.0.address", "10.7.34.32");
        siteMinderProps.put("server.0.0.authentication.port", "44442");
        siteMinderProps.put("server.0.0.accounting.port", "44441");
        siteMinderProps.put("server.0.0.connection.max", "3");
        siteMinderProps.put("server.0.0.connection.min", "1");
        siteMinderProps.put("server.0.0.authorization.port", "44443");
        siteMinderProps.put("server.0.0.connection.step", "1");
        siteMinderProps.put("server.0.0.timeout", "60");

        siteMinderConfiguration.setProperties(siteMinderProps);
        fixture = new SiteMinderExternalReference(mockFinder, siteMinderConfiguration);
    }

    @After
    public void tearDown() throws Exception {

    }

    @BugId("SSG-8969")
    @Test
    public void constructorFindException() throws Exception {
        final Goid goid = new Goid(0, 1);
        when(mockFinder.findSiteMinderConfigurationByID(goid)).thenThrow(new FindException());
        final SiteMinderExternalReference ref = new SiteMinderExternalReference(mockFinder, goid);
        assertEquals(goid.toString(), ref.getRefId());
        assertNull(ref.getSiteMinderConfiguration());
    }

    @Test
    public void constructorNullConfig() throws Exception {
        final SiteMinderExternalReference ref = new SiteMinderExternalReference(mockFinder, (SiteMinderConfiguration) null);
        assertNull(ref.getSiteMinderConfiguration());
        assertNull(ref.getRefId());
    }

    @Test
    public void serializeToRefElementTest() throws Exception {
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();

        fixture.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(REFERENCES_BEGIN + SM_REFERENCE_ELEM_1 + REFERENCES_END, asXml.trim());
    }

    @BugId("SSG-8969")
    @Test
    public void serializeToRefElementNullSiteMinderConfig() throws Exception {
        final Goid goid = new Goid(0, 1);
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "        <GOID>" + goid.toString() + "</GOID>\n" +
                "    </SiteMinderConfigurationReference>\n" +
                "</exp:References>";
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        when(mockFinder.findSiteMinderConfigurationByID(goid)).thenThrow(new FindException());
        final SiteMinderExternalReference ref = new SiteMinderExternalReference(mockFinder, goid);
        ref.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(expected, asXml.trim());
    }

    @Test
    public void serializeToRefElementNullSiteMinderConfigAndIdentifier() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n" +
                "    <SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "        <GOID>" + SiteMinderConfiguration.DEFAULT_GOID.toString() + "</GOID>\n" +
                "    </SiteMinderConfigurationReference>\n" +
                "</exp:References>";
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        final SiteMinderExternalReference ref = new SiteMinderExternalReference(mockFinder, (SiteMinderConfiguration) null);
        ref.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(expected, asXml.trim());
    }

    @Test
    public void shouldVerifyWhenReferenceFound() throws Exception {
        when(mockFinder.findSiteMinderConfigurationByID(new Goid(SITEMINDER_REF_GOID))).thenReturn(siteMinderConfiguration);
        assertTrue(fixture.verifyReference());
    }

    @Test
    public void shouldNotVerifyWhenReferenceNotFound() throws Exception {
        when(mockFinder.findSiteMinderConfigurationByID(any(Goid.class))).thenReturn(isNull(SiteMinderConfiguration.class));
        assertFalse(fixture.verifyReference());
    }

    @Test
    public void shouldLocalizeAssertion() throws Exception {
        SiteMinderCheckProtectedAssertion assertion = new SiteMinderCheckProtectedAssertion();
        SiteMinderConfiguration newSiteMinderConfig = new SiteMinderConfiguration();
        newSiteMinderConfig.setGoid(new Goid(REPLACE_GOID));
        newSiteMinderConfig.setName("yuri-1251only-native");
        fixture.setLocalizeReplace(REPLACE_GOID);
        assertion.setAgentGoid(new Goid(SITEMINDER_REF_GOID));
        assertion.setAgentId(siteMinderConfiguration.getName());
        when(mockFinder.findSiteMinderConfigurationByID(new Goid(REPLACE_GOID))).thenReturn(newSiteMinderConfig);
        assertTrue(fixture.localizeAssertion(assertion));
        assertEquals(REPLACE_GOID, assertion.getAgentGoid().toString());
        assertEquals("yuri-1251only-native", assertion.getAgentId());
    }

    @BugId("SSG-8969")
    @Test
    public void localizeAssertionNullSiteMinderConfig() throws Exception {
        final Goid foreignGoid = new Goid(0, 1);
        final SiteMinderExternalReference ref = new SiteMinderExternalReference(mockFinder, foreignGoid);
        ref.setLocalizeReplace(REPLACE_GOID);
        final SiteMinderCheckProtectedAssertion assertion = new SiteMinderCheckProtectedAssertion();
        assertion.setAgentGoid(foreignGoid);
        final SiteMinderConfiguration replacement = new SiteMinderConfiguration();
        replacement.setGoid(new Goid(REPLACE_GOID));
        when(mockFinder.findSiteMinderConfigurationByID(foreignGoid)).thenThrow(new FindException());
        when(mockFinder.findSiteMinderConfigurationByID(new Goid(REPLACE_GOID))).thenReturn(replacement);
        assertTrue(ref.localizeAssertion(assertion));
        assertEquals(REPLACE_GOID, assertion.getAgentGoid().toString());
        verify(mockFinder).findSiteMinderConfigurationByID(foreignGoid);
        verify(mockFinder).findSiteMinderConfigurationByID(new Goid(REPLACE_GOID));
    }

    @Test
    public void shouldParseReferenceFromElement() throws Exception {
         SiteMinderExternalReference reference = (SiteMinderExternalReference)SiteMinderExternalReference.parseFromElement(mockFinder, XmlUtil.parse(SM_REFERENCE_ELEM_1).getDocumentElement());
         assert reference != null;
         assertEquals(siteMinderConfiguration, reference.getSiteMinderConfiguration());
         assertEquals(SITEMINDER_REF_GOID, reference.getRefId());
    }

    @BugId("SSG-8969")
    @Test
    public void parseFromElementNoSiteMinderConfig() throws Exception {
        final Goid goid = new Goid(0, 1);
        final String noSiteMinderConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "    <GOID>" + goid.toString() + "</GOID>\n" +
                "</SiteMinderConfigurationReference>";
        final SiteMinderExternalReference reference = (SiteMinderExternalReference)SiteMinderExternalReference.parseFromElement(mockFinder, XmlUtil.parse(noSiteMinderConfig).getDocumentElement());
        assertNull(reference.getSiteMinderConfiguration());
        assertEquals(goid.toString(), reference.getRefId());
    }

    @Test(expected= InvalidDocumentFormatException.class)
    public void parseFromElementNoSiteMinderConfigOrGoid() throws Exception {
        final String noSiteMinderConfigOrGoid = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "</SiteMinderConfigurationReference>";
        SiteMinderExternalReference.parseFromElement(mockFinder, XmlUtil.parse(noSiteMinderConfigOrGoid).getDocumentElement());
    }

    @Test(expected= InvalidDocumentFormatException.class)
    public void parseFromElementNoSiteMinderConfigInvalidGoid() throws Exception {
        final String invalidGoid = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "    <GOID>abc</GOID>\n" +
                "</SiteMinderConfigurationReference>";
        SiteMinderExternalReference.parseFromElement(mockFinder, XmlUtil.parse(invalidGoid).getDocumentElement());
    }

    @Test(expected= InvalidDocumentFormatException.class)
    public void parseFromElementNoSiteMinderConfigInvalidGoidXml() throws Exception {
        final Goid goid = new Goid(0, 1);
        final String duplicateGoidElements = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
                "    <GOID>" + goid.toString() + "</GOID>\n" +
                "    <GOID>" + goid.toString() + "</GOID>\n" +
                "</SiteMinderConfigurationReference>";
        SiteMinderExternalReference.parseFromElement(mockFinder, XmlUtil.parse(duplicateGoidElements).getDocumentElement());
    }

    @Test
    public void shouldCompareSiteMinderReferences() throws Exception {
        SiteMinderExternalReference ref1 = new SiteMinderExternalReference(mockFinder, siteMinderConfiguration);
        when(mockFinder.findSiteMinderConfigurationByID(new Goid(SITEMINDER_REF_GOID))).thenReturn(siteMinderConfiguration);
        SiteMinderExternalReference ref2 = new SiteMinderExternalReference(mockFinder, new Goid(SITEMINDER_REF_GOID));
        assertEquals(ref1,ref2);
    }
}
