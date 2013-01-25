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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class EncapsulatedAssertionConfigExportUtilTest {
    private EncapsulatedAssertionConfigExportUtil util;
    private EncapsulatedAssertionConfig config;
    private Policy policy;
    private Set<EncapsulatedAssertionArgumentDescriptor> ins;
    private Set<EncapsulatedAssertionResultDescriptor> outs;
    private Map<String, String> properties;
    private DOMResult result = new DOMResult();

    @Before
    public void setup() throws Exception {
        util = new EncapsulatedAssertionConfigExportUtil();
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "myPolicy", "policyXml", false);
        ins = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        ins.add(createInput());
        outs = new HashSet<EncapsulatedAssertionResultDescriptor>();
        outs.add(createOutput());
        properties = new HashMap<String, String>();
        properties.put("prop1", "val1");
        createConfig();
    }

    @Test
    public void export() throws Exception {
        util.export(config, result);
        final String xml = XmlUtil.nodeToFormattedString(result.getNode());
        assertEquals(xmlFromConfig(config), xml);
    }

    @Test
    public void importFromNode() throws Exception {
        final EncapsulatedAssertionConfig imported = util.importFromNode(XmlUtil.parse(xmlFromConfig(this.config)));
        assertEquals(config.getName(), imported.getName());
        assertEquals(config.getProperties(), imported.getProperties());
        assertEquals(config.getResultDescriptors(), imported.getResultDescriptors());
        assertEquals(config.getArgumentDescriptors(), imported.getArgumentDescriptors());
        assertEquals(config.getGuid(), imported.getGuid());
        assertEquals(config.getId(), imported.getId());
        assertEquals(config.getVersion(), imported.getVersion());
        assertNull(imported.getPolicy());
    }

    private void createConfig() {
        config = new EncapsulatedAssertionConfig();
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

    private String xmlFromConfig(final EncapsulatedAssertionConfig config) throws TransformerException, SAXException, IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<enc:EncapsulatedAssertion guid=\"").append(config.getGuid());
        sb.append("\" id=\"").append(config.getId());
        sb.append("\" version=\"").append(config.getVersion()).append("\"\n");
        sb.append("xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\" xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\">");
        sb.append("<L7:name>").append(config.getName()).append("</L7:name>");
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
        sb.append("<enc:EncapsulatedResults>");
        for (final EncapsulatedAssertionResultDescriptor out : config.getResultDescriptors()) {
            sb.append("<enc:EncapsulatedResult id=\"").append(out.getId()).append("\" version=\"").append(out.getVersion()).append("\">");
            sb.append("<enc:ResultName>").append(out.getResultName()).append("</enc:ResultName>");
            sb.append("<enc:ResultType>").append(out.getResultType()).append("</enc:ResultType>");
            sb.append("</enc:EncapsulatedResult>");
        }
        sb.append("</enc:EncapsulatedResults>");
        sb.append("<enc:Properties>");
        for (final Map.Entry<String, String> entry : config.getProperties().entrySet()) {
            sb.append("<entry>");
            sb.append("<key xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">").append(entry.getKey()).append("</key>");
            sb.append("<value xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">").append(entry.getValue()).append("</value>");
            sb.append("</entry>");
        }
        sb.append("</enc:Properties>");
        sb.append("</enc:EncapsulatedAssertion>");
        final Document doc = XmlUtil.parse(sb.toString());
        return XmlUtil.nodeToFormattedString(doc);
    }
}
