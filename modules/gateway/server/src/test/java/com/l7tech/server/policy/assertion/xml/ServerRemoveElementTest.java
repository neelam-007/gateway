package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ServerRemoveElementTest {
    /** Test XML for insert element tests. */
    private static final String INSERT_DOC = "<a><b/><c><d/><e/></c><f/></a>";

    @Test
    public void testRemoveSingleElement() throws Exception {
        Message mess = makemess("<a><b/></a>");
        Element b = XmlUtil.findOnlyOneChildElement(mess.getXmlKnob().getDocumentWritable().getDocumentElement());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", b);

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<a/>", toString(mess.getXmlKnob()));
    }

    @Test
    @BugNumber(10749)
    public void testRemoveSingleElementUsingIndex() throws Exception {
        Message mess = makemess("<a><b/><c/><d/></a>");
        Element b = XmlUtil.findOnlyOneChildElementByName(mess.getXmlKnob().getDocumentWritable().getDocumentElement(), (String)null, "b");
        Element c = XmlUtil.findOnlyOneChildElementByName(mess.getXmlKnob().getDocumentWritable().getDocumentElement(), (String)null, "c");
        Element d = XmlUtil.findOnlyOneChildElementByName(mess.getXmlKnob().getDocumentWritable().getDocumentElement(), (String)null, "d");
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", new Element[] { b, c, d });

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel[1]");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<a><b/><d/></a>", toString(mess.getXmlKnob()));
    }

    @Test
    public void testRemoveSingleElement_andUseMimeKnob() throws Exception {
        Message mess = makemess("<a><b/></a>");
        Element b = XmlUtil.findOnlyOneChildElement(mess.getXmlKnob().getDocumentWritable().getDocumentElement());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", b);

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<a/>", toString(mess.getMimeKnob()));
    }

    @Test
    public void testRemoveSingleElement_targetNotXml() throws Exception {
        Message mess = new Message();
        mess.initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, "blah blah blah".getBytes(Charsets.UTF8));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", XmlUtil.stringAsDocument("<b/>").getDocumentElement());

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NOT_APPLICABLE, result);
    }

    @Test
    public void testRemoveMultipleElementsArray() throws Exception {
        Message mess = makemess("<a><b/><c/><container><d/><e>estuff</e><f/></container></a>");
        final Element docel = mess.getXmlKnob().getDocumentWritable().getDocumentElement();
        Element c = XmlUtil.findExactlyOneChildElementByName(docel, null, "c");
        Element container = XmlUtil.findExactlyOneChildElementByName(docel, null, "container");
        Element e = XmlUtil.findExactlyOneChildElementByName(container, null, "e");
        Element[] elms = new Element[] { c, e };

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", elms);

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<a><b/><container><d/><f/></container></a>", XmlUtil.nodeToString(mess.getXmlKnob().getDocumentReadOnly()));
    }

    @Test
    public void testRemoveMultipleElementsCollection() throws Exception {
        Message mess = makemess("<a><b/><c/><container><d/><e>estuff</e><f/></container></a>");
        final Element docel = mess.getXmlKnob().getDocumentWritable().getDocumentElement();
        Element c = XmlUtil.findExactlyOneChildElementByName(docel, null, "c");
        Element container = XmlUtil.findExactlyOneChildElementByName(docel, null, "container");
        Element e = XmlUtil.findExactlyOneChildElementByName(container, null, "e");

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", new HashSet<Element>(Arrays.asList(c, e)));

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<a><b/><container><d/><f/></container></a>", XmlUtil.nodeToString(mess.getXmlKnob().getDocumentReadOnly()));
    }

    @Test
    public void testRemoveMultipleElementsCollection_ArrayStore() throws Exception {
        Message mess = makemess("<a><b/><c/><container><d/><e>estuff</e><f/></container></a>");
        final Element docel = mess.getXmlKnob().getDocumentWritable().getDocumentElement();
        Element c = XmlUtil.findExactlyOneChildElementByName(docel, null, "c");
        Element container = XmlUtil.findExactlyOneChildElementByName(docel, null, "container");
        Element e = XmlUtil.findExactlyOneChildElementByName(container, null, "e");

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("remel", new HashSet<Object>(Arrays.asList(c, new Object(), e)));

        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("remel");

        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = checkRequest(sass, context);
        assertEquals("Should fail when the input variable points at a collection that contains non-Element values", AssertionStatus.FAILED, result);

        assertEquals("<a><b/><c/><container><d/><e>estuff</e><f/></container></a>", XmlUtil.nodeToString(mess.getXmlKnob().getDocumentReadOnly()));
    }

    @Test
    public void testInsertSingleElement_firstChild() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.FIRST_CHILD);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><blarg>This is some text content</blarg><d/><e/></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_firstChild_noExistingFirstChild() throws Exception {
        PolicyEnforcementContext context = makeInsertPec("<a><b/><c/><f/></a>");
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.FIRST_CHILD);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><blarg>This is some text content</blarg></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_lastChild() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.LAST_CHILD);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><d/><e/><blarg>This is some text content</blarg></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_previousSibling() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.PREVIOUS_SIBLING);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><blarg>This is some text content</blarg><c><d/><e/></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_previousSibling_noPrevSibling() throws Exception {
        PolicyEnforcementContext context = makeInsertPec("<a><c><d/><e/></c><f/></a>");
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.PREVIOUS_SIBLING);
        runCheck(ass, context, AssertionStatus.NONE, "<a><blarg>This is some text content</blarg><c><d/><e/></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_previousSibling_prevSiblingOfDocumentElement() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        context.setVariable("existingEl", context.getRequest().getXmlKnob().getDocumentWritable().getDocumentElement());
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.PREVIOUS_SIBLING);
        runCheck(ass, context, AssertionStatus.SERVER_ERROR, INSERT_DOC);
    }

    @Test
    public void testInsertSingleElement_nextSibling() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.NEXT_SIBLING);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><d/><e/></c><blarg>This is some text content</blarg><f/></a>");
    }

    @Test
    public void testInsertSingleElement_nextSibling_noExistingNextSib() throws Exception {
        PolicyEnforcementContext context = makeInsertPec("<a><b/><c><d/><e/></c></a>");
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.NEXT_SIBLING);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><d/><e/></c><blarg>This is some text content</blarg></a>");
    }

    @Test
    public void testInsertSingleElement_parseFragment() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        context.setVariable("elToInsert", "<blah>This is a <fragment/> which will be parsed and <em>inserted</em>.</blah>");
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.FIRST_CHILD);
        runCheck(ass, context, AssertionStatus.NONE, "<a><b/><c><blah>This is a <fragment/> which will be parsed and <em>inserted</em>.</blah><d/><e/></c><f/></a>");
    }

    @Test
    public void testInsertSingleElement_parseFragment_badXml() throws Exception {
        PolicyEnforcementContext context = makeInsertPec();
        context.setVariable("elToInsert", "<blah>This is a malformed <fragment/> which will NOT be parsed properly.");
        RemoveElement ass = makeInsertAssertion(RemoveElement.ElementLocation.FIRST_CHILD);
        runCheck(ass, context, AssertionStatus.FAILED, INSERT_DOC);
    }

    private RemoveElement makeInsertAssertion(RemoveElement.ElementLocation loc) {
        RemoveElement ass = new RemoveElement();
        ass.setElementFromVariable("existingEl");
        ass.setElementToInsertVariable("elToInsert");
        ass.setInsertedElementLocation(loc);
        return ass;
    }

    private PolicyEnforcementContext makeInsertPec() throws SAXException, IOException, TooManyChildElementsException, MissingRequiredElementException {
        return makeInsertPec(INSERT_DOC);
    }

    private PolicyEnforcementContext makeInsertPec(String xml) throws SAXException, IOException, TooManyChildElementsException, MissingRequiredElementException {
        Message mess = makemess(xml);
        final Document doc = mess.getXmlKnob().getDocumentReadOnly();
        final Element docel = doc.getDocumentElement();
        Element c = XmlUtil.findExactlyOneChildElementByName(docel, null, "c");

        Element n = XmlUtil.createEmptyDocument().createElement("blarg");
        n.setTextContent("This is some text content");

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
        context.setVariable("existingEl", c);
        context.setVariable("elToInsert", n);
        return context;
    }

    void runCheck(RemoveElement ass, PolicyEnforcementContext context, AssertionStatus expectedStatus, String expectedMess) throws IOException, PolicyAssertionException, SAXException, NoSuchPartException {
        ServerRemoveElement sass = new ServerRemoveElement(ass);
        AssertionStatus result = checkRequest(sass, context);
        assertEquals(expectedStatus, result);
        assertEquals(expectedMess, toString(context.getRequest().getXmlKnob()));
        assertEquals(expectedMess, toString(context.getRequest().getMimeKnob()));
    }

    // Wraps checkRequest and emulates an AssertionStatusException, just as a ServerCompositeAssertion would.
    static AssertionStatus checkRequest(ServerAssertion sass, PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            return sass.checkRequest(context);
        } catch (AssertionStatusException e) {
            return e.getAssertionStatus();
        }
    }

    static Message makemess(String mess) throws IOException {
        return new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(mess.getBytes(Charsets.UTF8)));
    }

    static String toString(MimeKnob mimeKnob) throws NoSuchPartException, IOException {
        return new String(IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream()), mimeKnob.getOuterContentType().getEncoding());
    }

    static String toString(XmlKnob xmlKnob) throws IOException, SAXException {
        return XmlUtil.nodeToString(xmlKnob.getDocumentReadOnly());
    }
}
