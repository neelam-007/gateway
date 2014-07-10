package com.l7tech.policy.builder;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.wsp.WspConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class PolicyBuilderTest {
    private PolicyBuilder builder;

    @Before
    public void setup() {
        builder = new PolicyBuilder();
        final AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(SetVariableAssertion.class);
        WspConstants.setTypeMappingFinder(registry);
    }

    @Test
    public void basePolicy() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\"/>\n" +
                "</wsp:Policy>\n";
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void appendAssertion() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\"/>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.appendAssertion(new AllAssertion());
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void appendAssertionWithComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:assertionComment>\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:assertionComment>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.appendAssertion(new AllAssertion(), "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void comment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"foo\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.comment("foo", true);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void commentDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"foo\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.comment("foo", false);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void setContextVariable() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\"YmFy\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"foo\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.setContextVariable("foo", "bar");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void setContextVariableMessage() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\"YmFy\"/>\n" +
                "            <L7p:ContentType stringValue=\"text/xml\"/>\n" +
                "            <L7p:DataType variableDataType=\"message\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"foo\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.setContextVariable("foo", "bar", DataType.MESSAGE, "text/xml");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContextVariableMessageNullContentType() throws Exception {
        builder.setContextVariable("foo", "bar", DataType.MESSAGE, null);
    }

    @Test
    public void setContextVariables() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"Yg==\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"2\"/>\n" +
                "            </L7p:SetVariable>\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"YQ==\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"1\"/>\n" +
                "            </L7p:SetVariable>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        final Map<String, String> vars = new HashMap<>();
        vars.put("1", "a");
        vars.put("2", "b");
        builder.setContextVariables(vars, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void setContextVariablesWithComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"Yg==\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"2\"/>\n" +
                "            </L7p:SetVariable>\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"YQ==\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"1\"/>\n" +
                "            </L7p:SetVariable>\n" +
                "            <L7p:assertionComment>\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"// SET CONSTANTS\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:assertionComment>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        final Map<String, String> vars = new HashMap<>();
        vars.put("1", "a");
        vars.put("2", "b");
        builder.setContextVariables(vars, "// SET CONSTANTS");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void regexContains() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.REQUEST, null, "foo", null, false, true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void regexReplace() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "            <L7p:Replace booleanValue=\"true\"/>\n" +
                "            <L7p:Replacement stringValue=\"bar\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.REQUEST, null, "foo", "bar", false, true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void regexDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.REQUEST, null, "foo", null, false, false, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void regexWithComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"// LOOK FOR FOO\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.REQUEST, null, "foo", null, false, true, "// LOOK FOR FOO");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void regexOtherTarget() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"otherMsg\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "            <L7p:Replace booleanValue=\"true\"/>\n" +
                "            <L7p:Replacement stringValue=\"bar\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.OTHER, "otherMsg", "foo", "bar", false, true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void regexOtherTargetTypeWithNullTargetName() {
        builder.regex(TargetMessageType.OTHER, null, "foo", "bar", false, true, null);
    }

    @Test
    public void regexCaseInsensitive() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:CaseInsensitive booleanValue=\"true\"/>\n" +
                "            <L7p:Regex stringValue=\"foo\"/>\n" +
                "            <L7p:Replace booleanValue=\"true\"/>\n" +
                "            <L7p:Replacement stringValue=\"bar\"/>\n" +
                "        </L7p:Regex>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.regex(TargetMessageType.REQUEST, null, "foo", "bar", true, true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void multipleRegex() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:Regex>\n" +
                "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                <L7p:Regex stringValue=\"foo\"/>\n" +
                "            </L7p:Regex>\n" +
                "            <L7p:Regex>\n" +
                "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                <L7p:Regex stringValue=\"bar\"/>\n" +
                "            </L7p:Regex>\n" +
                "            <L7p:assertionComment>\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:assertionComment>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final Regex reg1 = new Regex();
        reg1.setAutoTarget(false);
        reg1.setRegex("foo");
        final Regex reg2 = new Regex();
        reg2.setAutoTarget(false);
        reg2.setRegex("bar");
        builder.multipleRegex(Arrays.asList(new Regex[]{reg1, reg2}), true, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void multipleRegexDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All L7p:Enabled=\"false\" wsp:Usage=\"Required\">\n" +
                "            <L7p:Regex>\n" +
                "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                <L7p:Regex stringValue=\"foo\"/>\n" +
                "            </L7p:Regex>\n" +
                "            <L7p:assertionComment>\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:assertionComment>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final Regex reg1 = new Regex();
        reg1.setAutoTarget(false);
        reg1.setRegex("foo");
        builder.multipleRegex(Arrays.asList(new Regex[]{reg1}), false, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void multipleRegexNullComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:Regex>\n" +
                "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                <L7p:Regex stringValue=\"foo\"/>\n" +
                "            </L7p:Regex>\n" +
                "        </wsp:All>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final Regex reg1 = new Regex();
        reg1.setAutoTarget(false);
        reg1.setRegex("foo");
        builder.multipleRegex(Arrays.asList(new Regex[]{reg1}), true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void urlEncode() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:EncodeDecode xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:SourceVariableName stringValue=\"foo\"/>\n" +
                "            <L7p:TargetDataType variableDataType=\"string\"/>\n" +
                "            <L7p:TargetVariableName stringValue=\"bar\"/>\n" +
                "            <L7p:TransformType transformType=\"URL_ENCODE\"/>\n" +
                "        </L7p:EncodeDecode>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.urlEncode("foo", "bar", null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void urlEncodeWithComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:EncodeDecode xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:SourceVariableName stringValue=\"foo\"/>\n" +
                "            <L7p:TargetDataType variableDataType=\"string\"/>\n" +
                "            <L7p:TargetVariableName stringValue=\"bar\"/>\n" +
                "            <L7p:TransformType transformType=\"URL_ENCODE\"/>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\" xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"// ENCODE FOO\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "        </L7p:EncodeDecode>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.urlEncode("foo", "bar", "// ENCODE FOO");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void manageCookieDomains() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value nameValuePair=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"localhost:8080\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieAttributes>\n" +
                "            <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value cookieCriteria=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"whitby.redmond.local\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieCriteria>\n" +
                "            <L7p:Operation operation=\"UPDATE\"/>\n" +
                "            <L7p:Target target=\"REQUEST\"/>\n" +
                "        </L7p:ManageCookie>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "whitby.redmond.local", "localhost:8080", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void manageCookieDomainsDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value nameValuePair=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"localhost:8080\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieAttributes>\n" +
                "            <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value cookieCriteria=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"whitby.redmond.local\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieCriteria>\n" +
                "            <L7p:Operation operation=\"UPDATE\"/>\n" +
                "            <L7p:Target target=\"REQUEST\"/>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "        </L7p:ManageCookie>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "whitby.redmond.local", "localhost:8080", false, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void manageCookieDomainsWithComment() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value nameValuePair=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"localhost:8080\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieAttributes>\n" +
                "            <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value cookieCriteria=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"whitby.redmond.local\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieCriteria>\n" +
                "            <L7p:Operation operation=\"UPDATE\"/>\n" +
                "            <L7p:Target target=\"REQUEST\"/>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\" xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"// REWRITE COOKIE DOMAINS\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "        </L7p:ManageCookie>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "whitby.redmond.local", "localhost:8080", true, "// REWRITE COOKIE DOMAINS");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test
    public void manageCookieDomainsOtherTargetMessage() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "            <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value nameValuePair=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"localhost:8080\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieAttributes>\n" +
                "            <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                <L7p:entry>\n" +
                "                    <L7p:key stringValue=\"domain\"/>\n" +
                "                    <L7p:value cookieCriteria=\"included\">\n" +
                "                        <L7p:Key stringValue=\"domain\"/>\n" +
                "                        <L7p:Value stringValue=\"whitby.redmond.local\"/>\n" +
                "                    </L7p:value>\n" +
                "                </L7p:entry>\n" +
                "            </L7p:CookieCriteria>\n" +
                "            <L7p:Operation operation=\"UPDATE\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"myMsg\"/>\n" +
                "        </L7p:ManageCookie>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        builder.replaceHttpCookieDomains(TargetMessageType.OTHER, "myMsg", "whitby.redmond.local", "localhost:8080", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()).trim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void manageCookieDomainsOtherTargetWithNullOtherTargetName() throws Exception {
        builder.replaceHttpCookieDomains(TargetMessageType.OTHER, null, "whitby.redmond.local", "localhost:8080", true, null);
    }

    @Test
    public void manageCookieNames() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ForEachLoop L7p:Usage=\"Required\"\n" +
                "            loopVariable=\"request.http.cookienames\" variablePrefix=\"cookiename\">\n" +
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHtjb29raWVuYW1lLmN1cnJlbnR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"l7_tmp_cname\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                        <L7p:Expression1 stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                        <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                        <L7p:Predicates predicates=\"included\">\n" +
                "                            <L7p:item binary=\"included\">\n" +
                "                                <L7p:RightValue stringValue=\"foo\"/>\n" +
                "                                <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                            </L7p:item>\n" +
                "                        </L7p:Predicates>\n" +
                "                    </L7p:ComparisonAssertion>\n" +
                "                    <L7p:Regex>\n" +
                "                        <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                        <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_cname\"/>\n" +
                "                        <L7p:Regex stringValue=\"foo\"/>\n" +
                "                        <L7p:Replace booleanValue=\"true\"/>\n" +
                "                        <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                        <L7p:Target target=\"OTHER\"/>\n" +
                "                    </L7p:Regex>\n" +
                "                    <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value nameValuePair=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieAttributes>\n" +
                "                        <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value cookieCriteria=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${cookiename.current}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieCriteria>\n" +
                "                        <L7p:Operation operation=\"UPDATE\"/>\n" +
                "                        <L7p:Target target=\"REQUEST\"/>\n" +
                "                    </L7p:ManageCookie>\n" +
                "                </wsp:All>\n" +
                "                <L7p:TrueAssertion/>\n" +
                "            </wsp:OneOrMore>\n" +
                "        </L7p:ForEachLoop>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.replaceHttpCookieNames(TargetMessageType.REQUEST, null, "foo", "bar", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void manageCookieNamesDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ForEachLoop L7p:Enabled=\"false\" L7p:Usage=\"Required\"\n" +
                "            loopVariable=\"request.http.cookienames\" variablePrefix=\"cookiename\">\n" +
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHtjb29raWVuYW1lLmN1cnJlbnR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"l7_tmp_cname\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                        <L7p:Expression1 stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                        <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                        <L7p:Predicates predicates=\"included\">\n" +
                "                            <L7p:item binary=\"included\">\n" +
                "                                <L7p:RightValue stringValue=\"foo\"/>\n" +
                "                                <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                            </L7p:item>\n" +
                "                        </L7p:Predicates>\n" +
                "                    </L7p:ComparisonAssertion>\n" +
                "                    <L7p:Regex>\n" +
                "                        <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                        <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_cname\"/>\n" +
                "                        <L7p:Regex stringValue=\"foo\"/>\n" +
                "                        <L7p:Replace booleanValue=\"true\"/>\n" +
                "                        <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                        <L7p:Target target=\"OTHER\"/>\n" +
                "                    </L7p:Regex>\n" +
                "                    <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value nameValuePair=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieAttributes>\n" +
                "                        <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value cookieCriteria=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${cookiename.current}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieCriteria>\n" +
                "                        <L7p:Operation operation=\"UPDATE\"/>\n" +
                "                        <L7p:Target target=\"REQUEST\"/>\n" +
                "                    </L7p:ManageCookie>\n" +
                "                </wsp:All>\n" +
                "                <L7p:TrueAssertion/>\n" +
                "            </wsp:OneOrMore>\n" +
                "        </L7p:ForEachLoop>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.replaceHttpCookieNames(TargetMessageType.REQUEST, null, "foo", "bar", false, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void manageCookieNamesOtherTarget() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:ForEachLoop L7p:Usage=\"Required\"\n" +
                "            loopVariable=\"myMsg.http.cookienames\" variablePrefix=\"cookiename\">\n" +
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHtjb29raWVuYW1lLmN1cnJlbnR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"l7_tmp_cname\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                        <L7p:Expression1 stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                        <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                        <L7p:Predicates predicates=\"included\">\n" +
                "                            <L7p:item binary=\"included\">\n" +
                "                                <L7p:RightValue stringValue=\"foo\"/>\n" +
                "                                <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                            </L7p:item>\n" +
                "                        </L7p:Predicates>\n" +
                "                    </L7p:ComparisonAssertion>\n" +
                "                    <L7p:Regex>\n" +
                "                        <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                        <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_cname\"/>\n" +
                "                        <L7p:Regex stringValue=\"foo\"/>\n" +
                "                        <L7p:Replace booleanValue=\"true\"/>\n" +
                "                        <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                        <L7p:Target target=\"OTHER\"/>\n" +
                "                    </L7p:Regex>\n" +
                "                    <L7p:ManageCookie xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                        <L7p:CookieAttributes mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value nameValuePair=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${l7_tmp_cname}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieAttributes>\n" +
                "                        <L7p:CookieCriteria mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"name\"/>\n" +
                "                                <L7p:value cookieCriteria=\"included\">\n" +
                "                                    <L7p:Key stringValue=\"name\"/>\n" +
                "                                    <L7p:Value stringValue=\"${cookiename.current}\"/>\n" +
                "                                </L7p:value>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:CookieCriteria>\n" +
                "                        <L7p:Operation operation=\"UPDATE\"/>\n" +
                "                        <L7p:Target target=\"OTHER\"/>\n" +
                "                        <L7p:OtherTargetMessageVariable stringValue=\"myMsg\"/>\n" +
                "                    </L7p:ManageCookie>\n" +
                "                </wsp:All>\n" +
                "                <L7p:TrueAssertion/>\n" +
                "            </wsp:OneOrMore>\n" +
                "        </L7p:ForEachLoop>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.replaceHttpCookieNames(TargetMessageType.OTHER, "myMsg", "foo", "bar", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void manageCookieNamesOtherTargetWithNullOtherTargetName() throws Exception {
        builder.replaceHttpCookieNames(TargetMessageType.OTHER, null, "whitby.redmond.local", "localhost:8080", true, null);
    }

    @Test
    public void routeForwardAllDoNotFailOnError() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:FailOnErrorStatus booleanValue=\"false\"/>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://whitby.redmond.local\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "        </L7p:HttpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.routeForwardAll("http://whitby.redmond.local", false);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void routeForwardAllFailOnError() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://whitby.redmond.local\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "        </L7p:HttpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.routeForwardAll("http://whitby.redmond.local", true);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void rewriteHeader() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${request.http.header.Location}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                            <L7p:Negated booleanValue=\"true\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZXF1ZXN0Lmh0dHAuaGVhZGVyLkxvY2F0aW9ufQ==\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"l7_tmp_header\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:Regex>\n" +
                "                    <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                    <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_header\"/>\n" +
                "                    <L7p:PatternContainsVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:Regex stringValue=\"foo\"/>\n" +
                "                    <L7p:Replace booleanValue=\"true\"/>\n" +
                "                    <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                    <L7p:Target target=\"OTHER\"/>\n" +
                "                </L7p:Regex>\n" +
                "                <L7p:AddHeader>\n" +
                "                    <L7p:HeaderName stringValue=\"Location\"/>\n" +
                "                    <L7p:HeaderValue stringValue=\"${l7_tmp_header}\"/>\n" +
                "                    <L7p:RemoveExisting booleanValue=\"true\"/>\n" +
                "                </L7p:AddHeader>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHeader(TargetMessageType.REQUEST, null, "Location", "foo", "bar", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void rewriteHeaderDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore L7p:Enabled=\"false\" wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${request.http.header.Location}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                            <L7p:Negated booleanValue=\"true\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZXF1ZXN0Lmh0dHAuaGVhZGVyLkxvY2F0aW9ufQ==\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"l7_tmp_header\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:Regex>\n" +
                "                    <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                    <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_header\"/>\n" +
                "                    <L7p:PatternContainsVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:Regex stringValue=\"foo\"/>\n" +
                "                    <L7p:Replace booleanValue=\"true\"/>\n" +
                "                    <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                    <L7p:Target target=\"OTHER\"/>\n" +
                "                </L7p:Regex>\n" +
                "                <L7p:AddHeader>\n" +
                "                    <L7p:HeaderName stringValue=\"Location\"/>\n" +
                "                    <L7p:HeaderValue stringValue=\"${l7_tmp_header}\"/>\n" +
                "                    <L7p:RemoveExisting booleanValue=\"true\"/>\n" +
                "                </L7p:AddHeader>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHeader(TargetMessageType.REQUEST, null, "Location", "foo", "bar", false, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void rewriteHeaderOtherTargetMessage() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${myMsg.http.header.Location}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                            <L7p:Negated booleanValue=\"true\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtteU1zZy5odHRwLmhlYWRlci5Mb2NhdGlvbn0=\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"l7_tmp_header\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:Regex>\n" +
                "                    <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "                    <L7p:OtherTargetMessageVariable stringValue=\"l7_tmp_header\"/>\n" +
                "                    <L7p:PatternContainsVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:Regex stringValue=\"foo\"/>\n" +
                "                    <L7p:Replace booleanValue=\"true\"/>\n" +
                "                    <L7p:Replacement stringValue=\"bar\"/>\n" +
                "                    <L7p:Target target=\"OTHER\"/>\n" +
                "                </L7p:Regex>\n" +
                "                <L7p:AddHeader>\n" +
                "                    <L7p:HeaderName stringValue=\"Location\"/>\n" +
                "                    <L7p:HeaderValue stringValue=\"${l7_tmp_header}\"/>\n" +
                "                    <L7p:OtherTargetMessageVariable stringValue=\"myMsg\"/>\n" +
                "                    <L7p:RemoveExisting booleanValue=\"true\"/>\n" +
                "                    <L7p:Target target=\"OTHER\"/>\n" +
                "                </L7p:AddHeader>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHeader(TargetMessageType.OTHER, "myMsg", "Location", "foo", "bar", true, null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rewriteHeaderOtherTargetWithNullOtherTargetMessageName() throws Exception {
        builder.rewriteHeader(TargetMessageType.OTHER, null, "Location", "foo", "bar", true, null);
    }

    @Test
    public void rewriteHtml() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${request.http.header.content-type}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                            <L7p:RightValue stringValue=\"html\"/>\n" +
                "                            <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:ReplaceTagContent xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:ReplaceWith stringValue=\"bar\"/>\n" +
                "                    <L7p:SearchFor stringValue=\"foo\"/>\n" +
                "                    <L7p:TagsToSearch stringValue=\"a,p,script\"/>\n" +
                "                    <L7p:Target target=\"REQUEST\"/>\n" +
                "                </L7p:ReplaceTagContent>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHtml(TargetMessageType.REQUEST, null, Collections.singleton("foo"), "bar", "a,p,script", null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void rewriteHtmlOtherTargetMessage() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${myMsg.http.header.content-type}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                            <L7p:RightValue stringValue=\"html\"/>\n" +
                "                            <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:ReplaceTagContent xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:ReplaceWith stringValue=\"bar\"/>\n" +
                "                    <L7p:SearchFor stringValue=\"foo\"/>\n" +
                "                    <L7p:TagsToSearch stringValue=\"a,p,script\"/>\n" +
                "                    <L7p:Target target=\"OTHER\"/>\n" +
                "                    <L7p:OtherTargetMessageVariable stringValue=\"myMsg\"/>\n" +
                "                </L7p:ReplaceTagContent>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHtml(TargetMessageType.OTHER, "myMsg", Collections.singleton("foo"), "bar", "a,p,script", null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rewriteHtmlOtherTargetWithNullOtherTargetMessageName() throws Exception {
        builder.rewriteHtml(TargetMessageType.OTHER, null, Collections.singleton("foo"), "bar", "a,p,script", null);
    }

    @Test
    public void rewriteHtmlMultipleSearch() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${request.http.header.content-type}\"/>\n" +
                "                    <L7p:ExpressionIsVariable booleanValue=\"false\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                            <L7p:RightValue stringValue=\"html\"/>\n" +
                "                            <L7p:Operator operator=\"CONTAINS\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:ReplaceTagContent xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:ReplaceWith stringValue=\"bar\"/>\n" +
                "                    <L7p:SearchFor stringValue=\"foo2\"/>\n" +
                "                    <L7p:TagsToSearch stringValue=\"a,p,script\"/>\n" +
                "                    <L7p:Target target=\"REQUEST\"/>\n" +
                "                </L7p:ReplaceTagContent>\n" +
                "                <L7p:ReplaceTagContent xmlns:L7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                "                    <L7p:ReplaceWith stringValue=\"bar\"/>\n" +
                "                    <L7p:SearchFor stringValue=\"foo\"/>\n" +
                "                    <L7p:TagsToSearch stringValue=\"a,p,script\"/>\n" +
                "                    <L7p:Target target=\"REQUEST\"/>\n" +
                "                </L7p:ReplaceTagContent>\n" +
                "            </wsp:All>\n" +
                "            <L7p:TrueAssertion/>\n" +
                "        </wsp:OneOrMore>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.rewriteHtml(TargetMessageType.REQUEST, null, new HashSet<>(Arrays.asList("foo", "foo2")), "bar", "a,p,script", null);
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void addHeader() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddHeader>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "            <L7p:HeaderName stringValue=\"Host\"/>\n" +
                "            <L7p:HeaderValue stringValue=\"localhost\"/>\n" +
                "        </L7p:AddHeader>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "localhost", false, true, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void replaceHeader() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddHeader>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "            <L7p:HeaderName stringValue=\"Host\"/>\n" +
                "            <L7p:HeaderValue stringValue=\"localhost\"/>\n" +
                "            <L7p:RemoveExisting booleanValue=\"true\"/>\n" +
                "        </L7p:AddHeader>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "localhost", true, true, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void addHeaderDisabled() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddHeader>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "            <L7p:Enabled booleanValue=\"false\"/>\n" +
                "            <L7p:HeaderName stringValue=\"Host\"/>\n" +
                "            <L7p:HeaderValue stringValue=\"localhost\"/>\n" +
                "        </L7p:AddHeader>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "localhost", false, false, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test
    public void addHeaderOtherTarget() throws Exception {
        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AddHeader>\n" +
                "            <L7p:AssertionComment assertionComment=\"included\">\n" +
                "                <L7p:Properties mapValue=\"included\">\n" +
                "                    <L7p:entry>\n" +
                "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
                "                        <L7p:value stringValue=\"test\"/>\n" +
                "                    </L7p:entry>\n" +
                "                </L7p:Properties>\n" +
                "            </L7p:AssertionComment>\n" +
                "            <L7p:HeaderName stringValue=\"Host\"/>\n" +
                "            <L7p:HeaderValue stringValue=\"localhost\"/>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"myMsg\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "        </L7p:AddHeader>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
        builder.addOrReplaceHeader(TargetMessageType.OTHER, "myMsg", "Host", "localhost", false, true, "test");
        assertEquals(expected, XmlUtil.nodeToFormattedString(builder.getPolicy()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addHeaderOtherTargetNullOtherTargetName() throws Exception {
        builder.addOrReplaceHeader(TargetMessageType.OTHER, null, "Host", "localhost", false, true, "test");
    }

    @Test
    public void createRegexAssertion() throws Exception {
        final Regex regex = PolicyBuilder.createRegexAssertion(TargetMessageType.RESPONSE, null, "foo", null, false, false);
        assertEquals(TargetMessageType.RESPONSE, regex.getTarget());
        assertNull(regex.getOtherTargetMessageVariable());
        assertFalse(regex.isAutoTarget());
        assertEquals("foo", regex.getRegex());
        assertFalse(regex.isReplace());
        assertNull(regex.getReplacement());
        assertFalse(regex.isCaseInsensitive());
        assertFalse(regex.isEnabled());
    }

    @Test
    public void createRegexAssertionWithReplacement() throws Exception {
        final Regex regex = PolicyBuilder.createRegexAssertion(TargetMessageType.RESPONSE, null, "foo", "bar", true, true);
        assertEquals(TargetMessageType.RESPONSE, regex.getTarget());
        assertNull(regex.getOtherTargetMessageVariable());
        assertFalse(regex.isAutoTarget());
        assertEquals("foo", regex.getRegex());
        assertTrue(regex.isReplace());
        assertEquals("bar", regex.getReplacement());
        assertTrue(regex.isCaseInsensitive());
        assertTrue(regex.isEnabled());
    }

    @Test
    public void createRegexAssertionOtherTarget() throws Exception {
        final Regex regex = PolicyBuilder.createRegexAssertion(TargetMessageType.OTHER, "other", "foo", null, false, false);
        assertEquals(TargetMessageType.OTHER, regex.getTarget());
        assertEquals("other", regex.getOtherTargetMessageVariable());
        assertFalse(regex.isAutoTarget());
        assertEquals("foo", regex.getRegex());
        assertFalse(regex.isReplace());
        assertNull(regex.getReplacement());
        assertFalse(regex.isCaseInsensitive());
        assertFalse(regex.isEnabled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRegexAssertionOtherTargetNullOtherTargetName() throws Exception {
        PolicyBuilder.createRegexAssertion(TargetMessageType.OTHER, null, "foo", null, false, false);
    }
}