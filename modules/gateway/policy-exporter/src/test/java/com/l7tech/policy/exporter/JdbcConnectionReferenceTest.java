package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.policy.assertion.JdbcConnectionable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JdbcConnectionReferenceTest {
    private static final String NAME = "testConnection";
    private static final String DRIVER = "testDriverClass";
    private static final String URL = "testJdbcUrl";
    private static final String USER = "testUser";
    private JdbcConnectionReference ref;
    private StubJdbcConnectionable connectionable;
    private JdbcConnection connection;
    private Map<String, Object> properties;
    @Mock
    private ExternalReferenceFinder finder;

    @Before
    public void setup() throws Exception {
        properties = new HashMap<String, Object>();
        properties.put("testProp", "testVal");
        connectionable = new StubJdbcConnectionable(NAME);
        connection = new JdbcConnection();
        connection.setName(NAME);
        connection.setDriverClass(DRIVER);
        connection.setJdbcUrl(URL);
        connection.setUserName(USER);
        connection.setAdditionalProperties(properties);
        when(finder.getJdbcConnection(NAME)).thenReturn(connection);
    }

    @Test
    public void constructorSetsValues() {
        ref = new JdbcConnectionReference(finder, connectionable);
        assertEquals(NAME, ref.getConnectionName());
        assertEquals(DRIVER, ref.getDriverClass());
        assertEquals(URL, ref.getJdbcUrl());
        assertEquals(USER, ref.getUserName());
        assertEquals(properties, ref.getAdditionalProps());
    }

    @Test
    public void parseFromElement() throws Exception {
        final Element element = XmlUtil.parse(createFormattedXml(false)).getDocumentElement();
        final JdbcConnectionReference result = JdbcConnectionReference.parseFromElement(finder, element);
        assertEquals(NAME, result.getConnectionName());
        assertEquals(DRIVER, result.getDriverClass());
        assertEquals(URL, result.getJdbcUrl());
        assertEquals(USER, result.getUserName());
        assertEquals(properties, result.getAdditionalProps());
    }

    @Test
    public void parseFromElementEmptyAdditionalProperties() throws Exception {
        properties.clear();
        final Element element = XmlUtil.parse(createFormattedXml(false)).getDocumentElement();
        final JdbcConnectionReference result = JdbcConnectionReference.parseFromElement(finder, element);
        assertTrue(result.getAdditionalProps().isEmpty());
    }

    @Test
    public void parseFromElementNoAdditionalPropertiesElement() throws Exception {
        properties = null;
        final Element element = XmlUtil.parse(createFormattedXml(false)).getDocumentElement();
        final JdbcConnectionReference result = JdbcConnectionReference.parseFromElement(finder, element);
        assertTrue(result.getAdditionalProps().isEmpty());
    }

    @Test
    public void serializeToRefElement() throws Exception {
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        ref = new JdbcConnectionReference(finder, connectionable);
        ref.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(createFormattedXml(true), asXml);
    }

    @Test
    public void serializeToRefElementEmptyProperties() throws Exception {
        properties.clear();
        connection.setAdditionalProperties(properties);
        final Element element = XmlUtil.createEmptyDocument("References", "exp", "http://www.layer7tech.com/ws/policy/export").getDocumentElement();
        ref = new JdbcConnectionReference(finder, connectionable);
        ref.serializeToRefElement(element);
        final String asXml = XmlUtil.nodeToFormattedString(element);
        assertEquals(createFormattedXml(true), asXml);
    }

    @Test
    public void verifyReference() throws Exception {
        ref = new JdbcConnectionReference(finder, connectionable);
        assertTrue(ref.verifyReference());
    }

    @Test
    public void verifyReferenceDifferentAdditionalProperties() throws Exception {
        ref = new JdbcConnectionReference(finder, connectionable);
        ref.getAdditionalProps().put("additionalKey", "additionalValue");
        assertFalse(ref.verifyReference());
    }

    private class StubJdbcConnectionable implements JdbcConnectionable {
        private String connectionName;

        private StubJdbcConnectionable(final String connectionName) {
            this.connectionName = connectionName;
        }

        @Override
        public String getConnectionName() {
            return connectionName;
        }

        @Override
        public void setConnectionName(String connectionName) {
            this.connectionName = connectionName;
        }
    }

    private String createFormattedXml(boolean includeReferencesElement) throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        if (includeReferencesElement) {
            stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<exp:References xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\">");
        }
        stringBuilder.append("<JdbcConnectionReference RefType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\">\n" +
                "<ConnectionName>" + NAME + "</ConnectionName>\n" +
                "<DriverClass>" + DRIVER + "</DriverClass>\n" +
                "<JdbcUrl>" + URL + "</JdbcUrl>\n" +
                "<UserName>" + USER + "</UserName>");
        if (properties != null) {
            stringBuilder.append("<AdditionalProperties>");
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                stringBuilder.append("<Property>" +
                        "<Name>" + entry.getKey() + "</Name>" +
                        "<Value>" + entry.getValue() + "</Value>" +
                        "</Property>");
            }
            stringBuilder.append("</AdditionalProperties>");
        }
        stringBuilder.append("</JdbcConnectionReference>");
        if (includeReferencesElement) {
            stringBuilder.append("</exp:References>");
        }
        final String preFormatted = stringBuilder.toString();
        // this will also format empty AdditionalProperties elements to self closing elements
        final Document parsedDoc = XmlUtil.parse(preFormatted);
        return XmlUtil.nodeToFormattedString(parsedDoc.getDocumentElement());
    }
}
