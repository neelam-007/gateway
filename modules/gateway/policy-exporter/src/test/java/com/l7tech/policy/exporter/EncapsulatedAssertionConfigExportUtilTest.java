package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EncapsulatedAssertionConfigExportUtilTest {
    // very simple policy with no references
    private static final String POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" " +
            "xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:TrueAssertion/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
    private EncapsulatedAssertionConfig config = new EncapsulatedAssertionConfig();
    private Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "myPolicy", POLICY_XML, false);
    private Set<EncapsulatedAssertionArgumentDescriptor> ins = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
    private Set<EncapsulatedAssertionResultDescriptor> outs = new HashSet<EncapsulatedAssertionResultDescriptor>();
    private Map<String, String> properties = new HashMap<String, String>();
    private DOMResult result = new DOMResult();
    @Mock
    private ExternalReferenceFinder finder;
    @Mock
    private EntityResolver resolver;


    @Before
    public void setup() throws Exception {
        setupConfig();
        policy.setGuid("policyGuid");
        ins.add(createInput());
        outs.add(createOutput());
        properties.put("prop1", "val1");
    }

    @Test
    public void exportConfig() throws Exception {
        testExportConfig();
    }

    @Test
    public void exportConfigNullFields() throws Exception {
        config.setGuid(null);
        config.setName(null);
        config.setPolicy(null);
        config.setArgumentDescriptors(null);
        config.setResultDescriptors(null);
        config.setProperties(null);
        testExportConfig();
    }

    @Test
    public void exportConfigEmptyCollections() throws Exception {
        config.setArgumentDescriptors(Collections.<EncapsulatedAssertionArgumentDescriptor>emptySet());
        config.setResultDescriptors(Collections.<EncapsulatedAssertionResultDescriptor>emptySet());
        config.setProperties(Collections.<String, String>emptyMap());
        testExportConfig();
    }

    @Test
    public void exportConfigAndPolicy() throws Exception {
        // ensure there is no existing artifact version
        config.removeProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION);
        final Document document = EncapsulatedAssertionExportUtil.exportEncass(config, finder, resolver);
        final String xml = XmlUtil.nodeToFormattedString(document);
        assertEquals(xmlFromConfig(config, true, false), xml);
        // ensure there is now an artifact version
        assertTrue(xml.contains(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION));
        assertNotNull(config.getProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION));
    }

    @Test
    public void exportConfigAndPolicyExistingArtifactVersion() throws Exception {
        final String existingArtifactVersion = "shouldBeReplaced";
        config.putProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION, existingArtifactVersion);
        final Document document = EncapsulatedAssertionExportUtil.exportEncass(config, finder, resolver);
        final String xml = XmlUtil.nodeToFormattedString(document);
        assertEquals(xmlFromConfig(config, true, false), xml);
        assertTrue(xml.contains(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION));
        assertFalse(config.getProperty(EncapsulatedAssertionConfig.PROP_ARTIFACT_VERSION).equals(existingArtifactVersion));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportConfigAndPolicyNullPolicy() throws Exception {
        config.setPolicy(null);
        EncapsulatedAssertionExportUtil.exportEncass(config, finder, resolver);
    }

    @Test
    public void generateArtifactVersion() throws Exception {
        final String xml = "<xml>someXml</xml>";
        final String artifactVersion = EncapsulatedAssertionExportUtil.generateArtifactVersion(XmlUtil.parse(xml));
        assertFalse(artifactVersion.isEmpty());

        // if xml has not changed, artifact version should be the same
        final String noChange = EncapsulatedAssertionExportUtil.generateArtifactVersion(XmlUtil.parse(xml));
        assertEquals(artifactVersion, noChange);

        // if xml has changed, artifact version should also have changed
        final String changed = EncapsulatedAssertionExportUtil.generateArtifactVersion(XmlUtil.parse("<xml>xmlHasChanged</xml>"));
        assertFalse(artifactVersion.equals(changed));
    }

    @Test
    public void importFromNode() throws Exception {
        final EncapsulatedAssertionConfig imported = EncapsulatedAssertionExportUtil.importFromNode(XmlUtil.parse(xmlFromConfig(config, false, true)));
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
        final String xmlWithMissingFields = xmlFromConfig(config, false, true);
        final EncapsulatedAssertionConfig imported = EncapsulatedAssertionExportUtil.importFromNode(XmlUtil.parse(xmlWithMissingFields));
        assertNull(imported.getGuid());
        assertNull(imported.getName());
        assertNull(imported.getPolicy());
        assertTrue(imported.getArgumentDescriptors().isEmpty());
        assertTrue(imported.getResultDescriptors().isEmpty());
        assertTrue(imported.getProperties().isEmpty());
    }

    @Test
    public void importFromNodeResetOidsAndVersions() throws Exception {
        final EncapsulatedAssertionConfig imported = EncapsulatedAssertionExportUtil.importFromNode(XmlUtil.parse(xmlFromConfig(config, false, true)), true);
        assertEquals(EncapsulatedAssertionConfig.DEFAULT_GOID, imported.getGoid());
        assertEquals(0, imported.getVersion());
        for (final EncapsulatedAssertionArgumentDescriptor in : imported.getArgumentDescriptors()) {
            assertEquals(EncapsulatedAssertionArgumentDescriptor.DEFAULT_GOID, in.getGoid());
            assertEquals(0, in.getVersion());
        }
        for (final EncapsulatedAssertionResultDescriptor out : imported.getResultDescriptors()) {
            assertEquals(EncapsulatedAssertionResultDescriptor.DEFAULT_GOID, out.getGoid());
            assertEquals(0, out.getVersion());
        }
    }

    private void testExportConfig() throws IOException, TransformerException, SAXException {
        EncapsulatedAssertionExportUtil.exportConfig(config, result);
        final String xml = XmlUtil.nodeToFormattedString(result.getNode());
        assertEquals(xmlFromConfig(config, false, false), xml);
    }

    private void setupConfig() {
        config.setGuid("abc123");
        config.setName("MyEncapsulatedAssertion");
        config.setGoid(new Goid(0,1234L));
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
        out.setGoid(new Goid(0, 1L));
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
        in.setGoid(new Goid(0,1L));
        in.setVersion(0);
        return in;
    }

    /**
     * Manually build expected xml from a EncapsulatedAssertionConfig.
     *
     * @param config              the EncapsulatedAssertionConfig from which the xml will be built.
     * @param includePolicy       whether to include the backing policy.
     * @param includeIdAndVersion whether to include the EncapsulatedAssertionConfig id and version.
     */
    private String xmlFromConfig(final EncapsulatedAssertionConfig config, final boolean includePolicy, final boolean includeIdAndVersion) throws TransformerException, SAXException, IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        if (includePolicy) {
            sb.append("<exp:Export Version=\"3.0\"\n" +
                    "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                    "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">");
            sb.append("<exp:References/>");
            sb.append(POLICY_XML);
        }
        sb.append("<enc:EncapsulatedAssertion");
        if (config.getGuid() != null) {
            sb.append(" guid=\"").append(config.getGuid()).append("\"");
        }
        if (includeIdAndVersion) {
            sb.append(" id=\"").append(config.getId());
            sb.append("\" version=\"").append(config.getVersion()).append("\"");
        }
        sb.append(" xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\" xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\" " +
                "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
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
                sb.append("<key xsi:type=\"xs:string\">").append(entry.getKey()).append("</key>");
                sb.append("<value xsi:type=\"xs:string\">").append(entry.getValue()).append("</value>");
                sb.append("</entry>");
            }
            sb.append("</enc:Properties>");
        }
        sb.append("</enc:EncapsulatedAssertion>");
        if (includePolicy) {
            sb.append("</exp:Export>");
        }
        final Document doc = XmlUtil.parse(sb.toString());
        // format white space
        // also converts empty elements to use a self-closing tag
        return XmlUtil.nodeToFormattedString(doc);
    }
}
