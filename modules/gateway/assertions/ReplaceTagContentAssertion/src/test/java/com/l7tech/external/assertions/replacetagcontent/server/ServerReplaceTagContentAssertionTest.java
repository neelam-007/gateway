package com.l7tech.external.assertions.replacetagcontent.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.replacetagcontent.ReplaceTagContentAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ServerReplaceTagContentAssertionTest {
    private static final String TEXT_HTML = "text/html";
    private ReplaceTagContentAssertion assertion;
    private ServerReplaceTagContentAssertion serverAssertion;
    private PolicyEnforcementContext context;
    private Message request;
    private Message response;
    private TestAudit testAudit;

    @Before
    public void setup() throws PolicyAssertionException {
        assertion = new ReplaceTagContentAssertion();
        request = new Message();
        response = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    @Test(expected = PolicyAssertionException.class)
    public void toSearchTextIsNull() throws Exception {
        configureAssertion(null, "steak", "main");
    }

    @Test(expected = PolicyAssertionException.class)
    public void replacementTextIsNull() throws Exception {
        configureAssertion("chicken", null, "main");
    }

    @Test(expected = PolicyAssertionException.class)
    public void tagsAreNull() throws Exception {
        configureAssertion("chicken", "steak", null);
    }

    @Test
    public void replaceHtml() throws Exception {
        final String html = "<!DOCTYPE html>" +
                "<html>" +
                "<body>" +
                "<h1>My First Heading</h1>" +
                "<p>My first paragraph.</p>" +
                "</body>" +
                "</html>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("first", "very first", "h1,p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<!DOCTYPE html>" +
                "<html>" +
                "<body>" +
                "<h1>My First Heading</h1>" +
                "<p>My very first paragraph.</p>" +
                "</body>" +
                "</html>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceHtmlAttribute() throws Exception {
        final String html = "<h1 someAttribute=someValue>My First Heading</h1><p someAttribute=someValue>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("someAttribute", "replacedAttribute", "p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<h1 someAttribute=someValue>My First Heading</h1><p replacedAttribute=someValue>My first paragraph.</p>",
                new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceHtmlAttributeValue() throws Exception {
        final String html = "<h1 someAttribute=someValue>My First Heading</h1><p someAttribute=someValue>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("someValue", "replacedValue", "p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<h1 someAttribute=someValue>My First Heading</h1><p someAttribute=replacedValue>My first paragraph.</p>",
                new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceInvalidHtml() throws Exception {
        final String html = "<h1>not My First Heading</h1><p>My paragraph tag is not closed.<h1>My Second Heading</h1>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("not", "_NOT_", "p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<h1>not My First Heading</h1><p>My paragraph tag is _NOT_ closed.<h1>My Second Heading</h1>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void tagsCaseInsensitive() throws Exception {
        final String html = "<p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("paragraph", "parag", "P");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<p>My first parag.</p>", new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void tagsWhitespaceTrimmed() throws Exception {
        final String html = "<h1>My first Heading</h1><p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        // whitespace between comma-separated tags
        configureAssertion("first", "FIRST", " p, h1");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<h1>My FIRST Heading</h1><p>My FIRST paragraph.</p>", new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void searchIsCaseSensitiveByDefault() throws Exception {
        final String html = "<p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("FIRST", "shouldNotBeReplaced", "p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals(html, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_REPLACEMENTS));
    }

    @Test
    public void searchCaseInsensitive() throws Exception {
        final String html = "<p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("FIRST", "second", "p");
        assertion.setCaseSensitive(false);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<p>My second paragraph.</p>", new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void messageNotInitialized() throws Exception {
        configureAssertion("foo", "bar", "p");
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("", new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_REPLACEMENTS));
    }

    @Test
    public void messagePartAlreadyDestructivelyRead() throws Exception {
        final String html = "<h1>My First Heading</h1><p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        // destructively read the message
        request.getMimeKnob().getEntireMessageBodyAsInputStream(true);
        configureAssertion("foo", "bar", "p");

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_SUCH_PART));
    }

    @Test
    public void replaceHtmlSearchForTextNotFound() throws Exception {
        final String html = "<h1>My First Heading</h1><p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("someTextThatDoesNotExist", "replacement", "p");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        // no change expected
        assertEquals(html, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_REPLACEMENTS));
    }

    @Test
    public void replaceHtmlTagNotFound() throws Exception {
        final String html = "<h1>My First Heading</h1><p>My first paragraph.</p>";
        request.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        configureAssertion("first", "replacement", "someTagThatDoesNotExist");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        // no change expected
        assertEquals(html, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.TAG_NOT_FOUND));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_REPLACEMENTS));
    }

    @Test
    public void otherMessageTarget() throws Exception {
        final String html = "<p>My first paragraph.</p>";
        final Message myMsg = context.getOrCreateTargetMessage(new MessageTargetableSupport("myMsg"), false);
        myMsg.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("myMsg");
        configureAssertion("paragraph", "parag", "P");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<p>My first parag.</p>", new String(IOUtils.slurpStream(myMsg.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void otherMessageTargetNotFound() throws Exception {
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("doesNotExist");
        configureAssertion("paragraph", "parag", "P");

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.MESSAGE_TARGET_ERROR));
    }

    @Test
    public void responseMessageTarget() throws Exception {
        final String html = "<p>My first paragraph.</p>";
        response.initialize(ContentTypeHeader.parseValue(TEXT_HTML), html.getBytes());
        assertion.setTarget(TargetMessageType.RESPONSE);
        configureAssertion("paragraph", "parag", "P");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals("<p>My first parag.</p>", new String(IOUtils.slurpStream(response.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceXml() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("chicken", "steak", "main");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<dinner><appetizer>chicken salad</appetizer><main>bbq steak</main><dessert>ice cream</dessert></dinner>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceText() throws Exception {
        final String textWithTags = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.TEXT_DEFAULT, textWithTags.getBytes());
        configureAssertion("chicken", "steak", "main");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<dinner><appetizer>chicken salad</appetizer><main>bbq steak</main><dessert>ice cream</dessert></dinner>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void replaceUsingContextVariables() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        context.setVariable("toSearch", "chicken");
        context.setVariable("toReplace", "steak");
        context.setVariable("tags", "main");
        configureAssertion("${toSearch}", "${toReplace}", "${tags}");
        serverAssertion = new ServerReplaceTagContentAssertion(assertion);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<dinner><appetizer>chicken salad</appetizer><main>bbq steak</main><dessert>ice cream</dessert></dinner>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void toSearchTextIsEmpty() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("", "steak", "main");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_SEARCH_TEXT));
    }

    @Test
    public void toSearchAndReplacementTextContainWhitespace() throws Exception {
        final String xml = "<dinner><appetizer>chicken  salad</appetizer><main>bbq  chicken</main><dessert>ice  cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("  ", " ", "main");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        final String expected = "<dinner><appetizer>chicken  salad</appetizer><main>bbq chicken</main><dessert>ice  cream</dessert></dinner>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void toSearchContextVariableDoesNotExist() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("${doesNotExist}", "steak", "main");
        initServerAssertion();

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_SEARCH_TEXT));
    }

    @Test
    public void toReplaceContextVariableDoesNotExist() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("chicken", "${doesNotExist}", "main");
        initServerAssertion();

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        // to be consistent with regex assertion, we replace with an empty string
        final String expected = "<dinner><appetizer>chicken salad</appetizer><main>bbq </main><dessert>ice cream</dessert></dinner>";
        assertEquals(expected, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
    }

    @Test
    public void tagsContextVariableDoesNotExist() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("chicken", "steak", "${doesNotExist}");
        initServerAssertion();

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_TAGS_TEXT));
    }

    @Test
    public void tagsAllWhitespace() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("chicken", "steak", "    ");
        initServerAssertion();

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_TAGS_TEXT));
    }

    /**
     * Individual tags which are whitespace should be ignored.
     */
    @Test
    public void eachTagIsWhitespace() throws Exception {
        final String xml = "<dinner><appetizer>chicken salad</appetizer><main>bbq chicken</main><dessert>ice cream</dessert></dinner>";
        request.initialize(ContentTypeHeader.XML_DEFAULT, xml.getBytes());
        configureAssertion("chicken", "steak", "  ,  ");
        initServerAssertion();

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertEquals(xml, new String(IOUtils.slurpStream(request.getMimeKnob().getEntireMessageBodyAsInputStream())));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_TAG_TEXT));
    }

    private void configureAssertion(final String searchFor, final String replaceWith, final String commaSeparatedTags) throws PolicyAssertionException {
        if (searchFor != null) {
            assertion.setSearchFor(searchFor);
        }
        if (replaceWith != null) {
            assertion.setReplaceWith(replaceWith);
        }
        if (commaSeparatedTags != null) {
            assertion.setTagsToSearch(commaSeparatedTags);
        }
        initServerAssertion();
    }

    private void initServerAssertion() throws PolicyAssertionException {
        serverAssertion = new ServerReplaceTagContentAssertion(assertion);
        testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
    }
}
