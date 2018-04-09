package com.l7tech.external.assertions.email;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;
import static org.junit.Assert.*;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test the EmailAssertion.
 */
public class EmailAssertionTest {

    private static final String HELLO_WORLD_BASE64TEXT = "SGVsbG8gV29ybGQh";
    private static WspReader wspReader;

    @BeforeClass
    public static void beforeClassSetup() {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.registerAssertion(EmailAssertion.class);
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
        SyspropUtil.setProperty( "com.l7tech.policy.wsp.checkAccessors", "true" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
                "com.l7tech.policy.wsp.checkAccessors"
        );
    }

    @Test
    public void testReproBug2214TabsInEmail() throws Exception {
        final String body = "Hello World!\nfoo\r\nbar baz blah\tbleet blot";

        EmailAssertion ema = new EmailAssertion();
        ema.setSubject("Hi");
        ema.setSourceEmailAddress("donotreply@l7ssg.com");
        ema.setTargetEmailAddress("foo@example.com");
        ema.messageString(body);

        String emXml = WspWriter.getPolicyXml(ema);
        EmailAssertion got = (EmailAssertion)wspReader.parseStrictly(emXml, INCLUDE_DISABLED);

        assertEquals(got.messageString(), body);
    }

    @Test
    public void testPolicyImportExportWithMessageField() throws IOException {
        final String importPolicyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                "<wsp:All wsp:Usage=\"Required\">" +
                "<L7p:EmailAlert>" +
                "<L7p:SmtpHost stringValue=\"kent.redmond.local\"/>" +
                "<L7p:Message stringValue=\"Hello World!\"/>" +
                "</L7p:EmailAlert>" +
                "</wsp:All>" +
                "</wsp:Policy>";

        // Base64message field should get initialized via Message field
        final Assertion policy = wspReader.parsePermissively(importPolicyXml, INCLUDE_DISABLED);
        final EmailAssertion emailAssertion = (EmailAssertion)policy.getAssertionWithOrdinal(2);
        assertEquals("SGVsbG8gV29ybGQh", emailAssertion.getBase64message());
        assertEquals("Hello World!", emailAssertion.messageString());

        // Base64message field should get exported but not Message field.
        final String exportPolicyXml = WspWriter.getPolicyXml(policy);
        assertTrue(exportPolicyXml.contains(":Email>"));
        assertTrue(exportPolicyXml.contains(":Base64message"));
        assertFalse(exportPolicyXml.contains(":Message"));
    }

    @Test
    public void testPolicyImportWithoutProtocolAndFormatFields() throws IOException {
        final String importPolicyXmlWithoutFormatField =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "<wsp:All wsp:Usage=\"Required\">" +
                        "<L7p:EmailAlert>" +
                        "<L7p:SmtpHost stringValue=\"kent.redmond.local\"/>" +
                        "<L7p:Message stringValue=\"Hello World!\"/>" +
                        "</L7p:EmailAlert>" +
                        "</wsp:All>" +
                        "</wsp:Policy>";
        final Assertion policy = wspReader.parsePermissively(importPolicyXmlWithoutFormatField, INCLUDE_DISABLED);
        final EmailAssertion emailAssertion = (EmailAssertion)policy.getAssertionWithOrdinal(2);
        assertTrue(emailAssertion.getProtocol() == EmailProtocol.PLAIN);
        assertTrue(emailAssertion.getFormat() == EmailFormat.PLAIN_TEXT);
    }

    @Test
    public void testPolicyImportWithProtocolAndFormatFields() throws IOException {
        final String importPolicyXmlWithoutFormatField =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "<wsp:All wsp:Usage=\"Required\">" +
                        "<L7p:EmailAlert>" +
                        "<L7p:SmtpHost stringValue=\"kent.redmond.local\"/>" +
                        "<L7p:Message stringValue=\"Hello World!\"/>" +
                        "<L7p:Protocol Protocol=\"STARTTLS\"/>" +
                        "<L7p:Format Format=\"HTML\"/>" +
                        "</L7p:EmailAlert>" +
                        "</wsp:All>" +
                        "</wsp:Policy>";
        final Assertion policy = wspReader.parsePermissively(importPolicyXmlWithoutFormatField, INCLUDE_DISABLED);
        final EmailAssertion emailAssertion = (EmailAssertion)policy.getAssertionWithOrdinal(2);
        assertTrue(emailAssertion.getProtocol() == EmailProtocol.STARTTLS);
        assertTrue(emailAssertion.getFormat() == EmailFormat.HTML);
    }

    @Test
    public void testPolicyExportWithProtocolFormatFields() throws IOException {
        final EmailAssertion emailAssertion = new EmailAssertion();
        emailAssertion.setBase64message(HELLO_WORLD_BASE64TEXT);
        emailAssertion.setProtocol(EmailProtocol.STARTTLS);
        emailAssertion.setFormat(EmailFormat.HTML);

        final String policyXml = WspWriter.getPolicyXml(emailAssertion);
        assertTrue(policyXml.contains(HELLO_WORLD_BASE64TEXT));
        assertTrue(policyXml.contains("Protocol=\"STARTTLS\""));
        assertTrue(policyXml.contains("Format=\"HTML\""));
    }

    @Test
    public void testPolicyImportExportWithNewExternalName() throws IOException {
        final String importPolicyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                        "<wsp:All wsp:Usage=\"Required\">" +
                        "<L7p:Email>" +
                        "<L7p:SmtpHost stringValue=\"kent.redmond.local\"/>" +
                        "<L7p:Message stringValue=\"Hello World!\"/>" +
                        "</L7p:Email>" +
                        "</wsp:All>" +
                        "</wsp:Policy>";

        // Base64message field should get initialized via Message field
        final Assertion policy = wspReader.parsePermissively(importPolicyXml, INCLUDE_DISABLED);
        final EmailAssertion emailAssertion = (EmailAssertion)policy.getAssertionWithOrdinal(2);
        assertEquals("Hello World!", emailAssertion.messageString());

        // Base64message field should get exported but not Message field.
        final String exportPolicyXml = WspWriter.getPolicyXml(policy);
        assertTrue(exportPolicyXml.contains(":Email>"));
        assertTrue(exportPolicyXml.contains(":Base64message"));
        assertFalse(exportPolicyXml.contains(":Message"));
    }

    @Test
    public void testVariablesUsedInAttachments() {
        final EmailAssertion emailAssertion = new EmailAssertion();
        final List<EmailAttachment> emailAttachments = new ArrayList<>();

        emailAttachments.add(new EmailAttachment("name1.txt", "source1", false));
        emailAttachments.add(new EmailAttachment("${name2}.doc", "source2", false));
        emailAssertion.setAttachments(emailAttachments);
        emailAssertion.setSmtpPort("${port}");

        assertTrue(emailAssertion.getVariablesUsed().length == 4);
        assertTrue(ArrayUtils.contains(emailAssertion.getVariablesUsed(), "port"));
        assertTrue(ArrayUtils.contains(emailAssertion.getVariablesUsed(), "name2"));
        assertTrue(ArrayUtils.contains(emailAssertion.getVariablesUsed(), "source1"));
        assertTrue(ArrayUtils.contains(emailAssertion.getVariablesUsed(), "source2"));
    }

}
