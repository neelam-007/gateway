package com.l7tech.external.assertions.logmessagetosyslog;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.util.InvalidDocumentFormatException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class tests the behaviour of the LogMessageToSysLogExternalReference class to ensure that the correct behaviour
 * is observed on export and import of a policy containing the Log Message to Syslog assertion
 *
 * @author huaal03
 * @see LogMessageToSysLogExternalReference
 */

@RunWith(PowerMockRunner.class)
public class LogMessageToSysLogExternalReferenceTest {
    private static final String REFERENCES_START =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">\n";
    private static final String REFERENCES_END =
            "</exp:References>";

    private static final String SINK_NAME = "syslogwrite_test12345";
    private static final String SINK_DESC = "abc12345";
    private static final String SINK_TYPE = "SYSLOG";
    private static final String SINK_SEVR = "ALL";

    private static final String REF_EL_GOID_MATCH_VARS =
            "    <LogMessageToSysLogExternalReference RefType=\"com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReference\">\n" +
            "        <GOID>00000000000000010000000000000000</GOID>\n" +
            "        <LogSinkName>"+SINK_NAME+"</LogSinkName>\n" +
            "        <LogSinkDescription>"+SINK_DESC+"</LogSinkDescription>\n" +
            "        <LogSinkType>"+SINK_TYPE+"</LogSinkType>\n" +
            "        <LogSinkSeverity>"+SINK_SEVR+"</LogSinkSeverity>\n" +
            "    </LogMessageToSysLogExternalReference>\n";

    private static final String REF_EL_GOID_MATCH_VARS_MALFORMED =
            "    <LogMessageToSysLogExternalReference RefType=\"com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReference\">\n" +
            "        <GOID>00000000000000010000000000000000</GOID>\n" +
            // "        <LogSinkName>"+SINK_NAME+"</LogSinkName>\n" +
            "        <LogSinkDescription>"+SINK_DESC+"</LogSinkDescription>\n" +
            "        <LogSinkType>"+SINK_TYPE+"</LogSinkType>\n" +
            "        <LogSinkSeverity>"+SINK_SEVR+"</LogSinkSeverity>\n" +
            "    </LogMessageToSysLogExternalReference>\n";

    private static final String REF_EL_GOID_MATCH_VARS_WRONG_ASSERTION =
            "    <SiteMinderConfigurationReference RefType=\"com.l7tech.external.assertions.siteminder.SiteMinderExternalReference\">\n" +
            "        <GOID>00000000000000010000000000000000</GOID>\n" +
            "        <LogSinkName>"+SINK_NAME+"</LogSinkName>\n" +
            "        <LogSinkDescription>"+SINK_DESC+"</LogSinkDescription>\n" +
            "        <LogSinkType>"+SINK_TYPE+"</LogSinkType>\n" +
            "        <LogSinkSeverity>"+SINK_SEVR+"</LogSinkSeverity>\n" +
            "    </SiteMinderConfigurationReference>\n";

    private static final String REF_EL_GOID_MATCH =
            "    <LogMessageToSysLogExternalReference RefType=\"com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReference\">\n" +
            "        <GOID>00000000000000010000000000000000</GOID>\n" +
            "        <LogSinkName>syslogwrite_test</LogSinkName>\n" +
            "        <LogSinkDescription>abc</LogSinkDescription>\n" +
            "        <LogSinkType>SYSLOG</LogSinkType>\n" +
            "        <LogSinkSeverity>ALL</LogSinkSeverity>\n" +
            "    </LogMessageToSysLogExternalReference>\n";

    private ExternalReferenceFinder finder;
    private LogMessageToSysLogExternalReference fixture;

    @Mock
    private Registry mockRegistry;
    private LogSinkAdmin mockLogSinkAdmin;
    private Goid goid;
    private SinkConfiguration mockSinkConfiguration;

    @Before
    public void setUp() throws Exception {
        mockSinkConfiguration = new SinkConfiguration();
        mockSinkConfiguration.setGoid(new Goid(1, 0)); // 00000000000000010000000000000000
        mockSinkConfiguration.setName("syslogwrite_test");
        mockSinkConfiguration.setEnabled(false);
        mockSinkConfiguration.setDescription("abc");
        mockSinkConfiguration.setType(SinkConfiguration.SinkType.SYSLOG);
        mockSinkConfiguration.setSeverity(SinkConfiguration.SeverityThreshold.ALL);
        mockSinkConfiguration.addSyslogHostEntry("0.0.0.0:514");
        mockSinkConfiguration.setCategories(SinkConfiguration.CATEGORY_AUDITS + ',' +SinkConfiguration.CATEGORY_GATEWAY_LOGS);

        Registry.setDefault(mockRegistry);
        mockLogSinkAdmin = mock(LogSinkAdmin.class);

        when(mockRegistry.getLogSinkAdmin()).thenReturn(mockLogSinkAdmin);
        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(mockSinkConfiguration.getGoid())).thenReturn(mockSinkConfiguration);

        fixture = new LogMessageToSysLogExternalReference(finder, mockSinkConfiguration.getGoid());
        goid = new Goid(1, 0);
    }

    @Test
    public void constructorNormalCase() throws Exception {

        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(mockSinkConfiguration);

        LogMessageToSysLogExternalReference ref = new LogMessageToSysLogExternalReference(finder, goid);

        assertEquals(mockSinkConfiguration.getGoid().toString(), ref.getRefId());
        assertEquals(mockSinkConfiguration.getName(), ref.getLogSinkName());
        assertEquals(mockSinkConfiguration.getDescription(), ref.getLogSinkDescription());
        assertEquals(mockSinkConfiguration.getType(), ref.getLogSinkType());
        assertEquals(mockSinkConfiguration.getSeverity(), ref.getLogSinkSeverity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorExportSinkConfigWhenTheLogSinkGoidDoesNotExist() throws Exception {
        goid = new Goid(1234, 5678);
        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(null);

        LogMessageToSysLogExternalReference ref = new LogMessageToSysLogExternalReference(finder, goid);
    }

    @Test(expected = NullPointerException.class)
    public void constructorFindException() throws Exception {
        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenThrow(new FindException());
        goid = null;

        LogMessageToSysLogExternalReference ref = new LogMessageToSysLogExternalReference(finder, goid);
        assertTrue(true);
    }

    @Test
    public void serializeToRefElementTest() throws Exception {
        Element element = XmlUtil.createEmptyDocument("References", "exp",
                "http://www.layer7tech.com/ws/policy/export").getDocumentElement();

        fixture.serializeToRefElement(element);
        String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(REFERENCES_START + REF_EL_GOID_MATCH + REFERENCES_END, asXml.trim());
    }

    @Test
    public void verifyTrueWhenReferenceGoidMatch() throws Exception {
        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(mockSinkConfiguration);
        assertTrue(fixture.verifyReference());
    }

    @Test
    public void verifyTrueWhenReferenceNameTypeMatch() throws Exception {
        Collection<SinkConfiguration> list = new ArrayList<>();

        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(null);

        mockSinkConfiguration = new SinkConfiguration();
        mockSinkConfiguration.setGoid(new Goid(111, 222));
        mockSinkConfiguration.setName("syslogwrite_test");
        mockSinkConfiguration.setEnabled(false);
        mockSinkConfiguration.setDescription("Switched sink configuration");
        mockSinkConfiguration.setType(SinkConfiguration.SinkType.SYSLOG);
        mockSinkConfiguration.setSeverity(SinkConfiguration.SeverityThreshold.ALL);
        mockSinkConfiguration.addSyslogHostEntry("0.0.0.0:514");
        mockSinkConfiguration.setCategories(SinkConfiguration.CATEGORY_AUDITS + ',' + SinkConfiguration.CATEGORY_GATEWAY_LOGS);

        list.add(mockSinkConfiguration);

        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(null);
        when(mockLogSinkAdmin.findAllSinkConfigurations()).thenReturn(list);

        assertTrue(fixture.verifyReference());

        assertEquals(mockSinkConfiguration.getGoid().toString(), fixture.getRefId());
    }

    @Test
    public void verifyFalseWhenNoReferenceFound() throws Exception {
        when(mockLogSinkAdmin.getSinkConfigurationByPrimaryKey(goid)).thenReturn(null);
        assertFalse(fixture.verifyReference());
    }

    @Test
    public void testLocalizeAssertionWithIgnore() throws Exception {
        LogMessageToSysLogAssertion assertion = new LogMessageToSysLogAssertion();
        fixture.setLocalizeIgnore();

        Boolean res = fixture.localizeAssertion(assertion);

        assertTrue(res);
    }

    @Test
    public void testLocalizeAssertionWithDelete() throws Exception {
        LogMessageToSysLogAssertion mockAssertion = mock(LogMessageToSysLogAssertion.class);
        when(mockAssertion.getSyslogGoid()).thenReturn(new Goid(0,0));

        fixture.setLocalizeDelete();

        Boolean res = fixture.localizeAssertion(mockAssertion);

        assertFalse(res);
    }

    @Test
    public void testLocalizeAssertionWithReplace() throws Exception {
        goid = new Goid(0, 1);
        LogMessageToSysLogAssertion assertion = new LogMessageToSysLogAssertion();
        fixture.setLocalizeReplace(goid);

        Boolean res = fixture.localizeAssertion(assertion);

        assertEquals(goid.toString(), fixture.getRefId());
        assertEquals("00000000000000000000000000000001", fixture.getRefId());
        assertTrue(res);
    }

    @Test
    public void testStaticParseNormalCase() throws Exception {
        LogMessageToSysLogExternalReference reference = LogMessageToSysLogExternalReference.parseFromElement(finder, XmlUtil.parse(REF_EL_GOID_MATCH_VARS).getDocumentElement());
        assert reference != null;
        assertEquals(SINK_NAME, reference.getLogSinkName());
        assertEquals(SINK_DESC, reference.getLogSinkDescription());
        assertEquals(SINK_TYPE, reference.getLogSinkType().toString());
        assertEquals(SINK_SEVR, reference.getLogSinkSeverity().toString());
    }

    @Test
    public void testStaticParseMissingFieldCase() throws Exception {
        LogMessageToSysLogExternalReference reference = LogMessageToSysLogExternalReference.parseFromElement(finder, XmlUtil.parse(REF_EL_GOID_MATCH_VARS_MALFORMED).getDocumentElement());
        assert reference != null;
        assertEquals(null, reference.getLogSinkName());
        assertEquals(SINK_DESC, reference.getLogSinkDescription());
        assertEquals(SINK_TYPE, reference.getLogSinkType().toString());
        assertEquals(SINK_SEVR, reference.getLogSinkSeverity().toString());
    }

    @Test(expected = InvalidDocumentFormatException.class)
    public void testStaticParseWrongAssertionCase() throws Exception {
        LogMessageToSysLogExternalReference reference = LogMessageToSysLogExternalReference.parseFromElement(finder, XmlUtil.parse(REF_EL_GOID_MATCH_VARS_WRONG_ASSERTION).getDocumentElement());
    }
}
