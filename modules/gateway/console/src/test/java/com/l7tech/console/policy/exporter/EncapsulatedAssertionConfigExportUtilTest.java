package com.l7tech.console.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class EncapsulatedAssertionConfigExportUtilTest {
    private EncapsulatedAssertionConfigExportUtil util;
    private EncapsulatedAssertionConfig config = new EncapsulatedAssertionConfig();
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "myPolicy", "policyXml", false);
    private Set<EncapsulatedAssertionArgumentDescriptor> ins = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
    private Set<EncapsulatedAssertionResultDescriptor> outs = new HashSet<EncapsulatedAssertionResultDescriptor>();
    private Map<String, String> properties = new HashMap<String, String>();
    private DOMResult result = new DOMResult();

    @Before
    public void setup() throws Exception {
        util = new EncapsulatedAssertionConfigExportUtil();
        setupConfig();
        policy.setGuid("policyGuid");
        ins.add(createInput());
        outs.add(createOutput());
        properties.put("prop1", "val1");
    }

    @Test
    public void export() throws Exception {
        testExport();
    }

    @Test
    public void exportNullFields() throws Exception {
        config.setGuid(null);
        config.setName(null);
        config.setPolicy(null);
        config.setArgumentDescriptors(null);
        config.setResultDescriptors(null);
        config.setProperties(null);
        testExport();
    }

    @Test
    public void exportEmptyCollections() throws Exception {
        config.setArgumentDescriptors(Collections.<EncapsulatedAssertionArgumentDescriptor>emptySet());
        config.setResultDescriptors(Collections.<EncapsulatedAssertionResultDescriptor>emptySet());
        config.setProperties(Collections.<String, String>emptyMap());
        testExport();
    }

    @Test
    public void importFromNode() throws Exception {
        final EncapsulatedAssertionConfig imported = util.importFromNode(XmlUtil.parse(xmlFromConfig(config)));
        assertEquals(config.getName(), imported.getName());
        assertEquals(config.getProperties(), imported.getProperties());
        assertEquals(config.getResultDescriptors(), imported.getResultDescriptors());
        assertEquals(config.getArgumentDescriptors(), imported.getArgumentDescriptors());
        assertEquals(config.getGuid(), imported.getGuid());
        assertEquals(config.getId(), imported.getId());
        assertEquals(config.getVersion(), imported.getVersion());
        final Policy simplifiedPolicy = imported.getPolicy();
        assertEquals("policyGuid", simplifiedPolicy.getGuid());
        assertEquals("myPolicy", simplifiedPolicy.getName());
        assertNull(simplifiedPolicy.getXml());
        for (final EncapsulatedAssertionArgumentDescriptor in : imported.getArgumentDescriptors()) {
            assertEquals(imported, in.getEncapsulatedAssertionConfig());
        }
        for (final EncapsulatedAssertionResultDescriptor out : imported.getResultDescriptors()) {
            assertEquals(imported, out.getEncapsulatedAssertionConfig());
        }
    }

    @Test
    public void importFromNodeMissingFields() throws Exception {
        config.setGuid(null);
        config.setName(null);
        config.setPolicy(null);
        config.setArgumentDescriptors(null);
        config.setResultDescriptors(null);
        config.setProperties(null);
        final String xmlWithMissingFields = xmlFromConfig(config);
        final EncapsulatedAssertionConfig imported = util.importFromNode(XmlUtil.parse(xmlWithMissingFields));
        assertNull(imported.getGuid());
        assertNull(imported.getName());
        assertNull(imported.getPolicy());
        assertTrue(imported.getArgumentDescriptors().isEmpty());
        assertTrue(imported.getResultDescriptors().isEmpty());
        assertTrue(imported.getProperties().isEmpty());
    }

    private void testExport() throws IOException, TransformerException, SAXException {
        util.export(config, result);
        final String xml = XmlUtil.nodeToFormattedString(result.getNode());
        assertEquals(xmlFromConfig(config), xml);
    }

    private void setupConfig() {
        config.setGuid("abc123");
        config.setName("MyEncapsulatedAssertion");
        config.setOid(1234L);
        config.setVersion(0);
        config.setPolicy(policy);
        config.setArgumentDescriptors(ins);
        config.setResultDescriptors(outs);
        config.setProperties(properties);
    }

    private EncapsulatedAssertionResultDescriptor createOutput() {
        final EncapsulatedAssertionResultDescriptor out = new EncapsulatedAssertionResultDescriptor();
        out.setResultName("output1");
        out.setResultType(DataType.STRING.getShortName());
        out.setEncapsulatedAssertionConfig(config);
        out.setOid(1L);
        out.setVersion(0);
        return out;
    }

    private EncapsulatedAssertionArgumentDescriptor createInput() {
        final EncapsulatedAssertionArgumentDescriptor in = new EncapsulatedAssertionArgumentDescriptor();
        in.setArgumentName("param1");
        in.setArgumentType(DataType.STRING.getShortName());
        in.setGuiLabel("param1Label");
        in.setEncapsulatedAssertionConfig(config);
        in.setGuiPrompt(true);
        in.setOrdinal(1);
        in.setOid(1L);
        in.setVersion(0);
        return in;
    }

    /**
     * Manually build expected xml from a EncapsulatedAssertionConfig.
     */
    private String xmlFromConfig(final EncapsulatedAssertionConfig config) throws TransformerException, SAXException, IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<enc:EncapsulatedAssertion");
        if (config.getGuid() != null) {
            sb.append(" guid=\"").append(config.getGuid()).append("\"");
        }
        sb.append(" id=\"").append(config.getId());
        sb.append("\" version=\"").append(config.getVersion()).append("\"\n");
        sb.append("xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\" xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\">");
        if (config.getName() != null) {
            sb.append("<L7:name>").append(config.getName()).append("</L7:name>");
        }
        if (config.getPolicy() != null) {
            sb.append("<enc:Policy guid=\"").append(config.getPolicy().getGuid()).append("\" name=\"").append(config.getPolicy().getName()).append("\"/>");
        }
        if (config.getArgumentDescriptors() != null) {
            sb.append("<enc:EncapsulatedAssertionArguments>");
            for (final EncapsulatedAssertionArgumentDescriptor in : config.getArgumentDescriptors()) {
                sb.append("<enc:EncapsulatedAssertionArgument id=\"").append(in.getId()).append("\" version=\"").append(in.getVersion()).append("\">");
                sb.append("<enc:ArgumentName>").append(in.getArgumentName()).append("</enc:ArgumentName>");
                sb.append("<enc:ArgumentType>").append(in.getArgumentType()).append("</enc:ArgumentType>");
                sb.append("<enc:GuiLabel>").append(in.getGuiLabel()).append("</enc:GuiLabel>");
                sb.append("<enc:GuiPrompt>").append(in.isGuiPrompt()).append("</enc:GuiPrompt>");
                sb.append("<enc:Ordinal>").append(in.getOrdinal()).append("</enc:Ordinal>");
                sb.append("</enc:EncapsulatedAssertionArgument>");
            }
            sb.append("</enc:EncapsulatedAssertionArguments>");
        }
        if (config.getResultDescriptors() != null) {
            sb.append("<enc:EncapsulatedAssertionResults>");
            for (final EncapsulatedAssertionResultDescriptor out : config.getResultDescriptors()) {
                sb.append("<enc:EncapsulatedAssertionResult id=\"").append(out.getId()).append("\" version=\"").append(out.getVersion()).append("\">");
                sb.append("<enc:ResultName>").append(out.getResultName()).append("</enc:ResultName>");
                sb.append("<enc:ResultType>").append(out.getResultType()).append("</enc:ResultType>");
                sb.append("</enc:EncapsulatedAssertionResult>");
            }
            sb.append("</enc:EncapsulatedAssertionResults>");
        }
        if (config.getProperties() != null) {
            sb.append("<enc:Properties>");
            for (final Map.Entry<String, String> entry : config.getProperties().entrySet()) {
                sb.append("<entry>");
                sb.append("<key xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">").append(entry.getKey()).append("</key>");
                sb.append("<value xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">").append(entry.getValue()).append("</value>");
                sb.append("</entry>");
            }
            sb.append("</enc:Properties>");
        }
        sb.append("</enc:EncapsulatedAssertion>");
        final Document doc = XmlUtil.parse(sb.toString());
        // format white space
        // also converts empty elements to use a self-closing tag
        return XmlUtil.nodeToFormattedString(doc);
    }
}
