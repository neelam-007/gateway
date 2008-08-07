/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml;

import com.l7tech.xml.tarari.TarariMessageContextImpl;
import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.SystemPropertyPrerequisite;
import com.l7tech.test.SystemPropertySwitchedRunner;
import org.xml.sax.SAXException;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Unit tests for an ElementCursor implementation.
 */
@RunWith(SystemPropertySwitchedRunner.class)
public class ElementCursorTest {


    private interface ElementCursorFactory {
        ElementCursor newElementCursor(String xml) throws SAXException;
    }

    private static class DomElementCursorFactory implements ElementCursorFactory {
        public ElementCursor newElementCursor(String xml) throws SAXException {
            return new DomElementCursor( XmlUtil.stringToDocument(xml));
        }
    }

    private static class TarariElementCursorFactory implements ElementCursorFactory {
        public ElementCursor newElementCursor(String xml) throws SAXException {
            try {
                final TarariMessageContextFactory mcf = TarariLoader.getMessageContextFactory();
                if (mcf == null)
                    throw new UnsatisfiedLinkError("No tarari hardware detected");
                TarariMessageContextImpl tmci =
                        (TarariMessageContextImpl)mcf.
                                makeMessageContext(new ByteArrayInputStream(xml.getBytes("UTF-8")));
                return tmci.getElementCursor();
            } catch (IOException e) {
                throw new SAXException(e);
            } catch ( SoftwareFallbackException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    }

    @Test
    @SystemPropertyPrerequisite(require = TarariLoader.ENABLE_PROPERTY)
    @Ignore("Need to work out why this fails and fix the environment on TeamCity")
    // TODO [steve] Fix Tarari test on TeamCity
    public void testTarariCursor() throws Exception {
        testAll(new TarariElementCursorFactory());
    }

    @Test
    public void testDomCursor() throws Exception {
        testAll(new DomElementCursorFactory());
    }

    private void testAll(ElementCursorFactory f) throws Exception {
        testSimple(f);
        testMixed(f);
    }

    private void assertEmptyStack( ElementCursor c) {
        try {
            c.popPosition();
            Assert.fail("Stack should have been empty");
        } catch (IllegalStateException e) {
            // Ok
        }
    }

    // Xml for testSimple
    private static final String SIMPLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<catalog xmlns=\"http://www.books.com/\">\n" +
            "<author>\n" +
            "\t<name>Joseph Conrad</name>\n" +
            "\t<booklist>\n" +
            "\t\t<book>\n" +
            "\t\t\t<name>The Secret Agent</name>\n" +
            "\t\t</book>\n" +
            "\t\t<book>\n" +
            "\t\t\t<name>Heart of Darkness</name>\n" +
            "\t\t</book>\n" +
            "\t\t<book>\n" +
            "\t\t\t<name>Nostromo: A Tale of the Seabord</name>\n" +
            "\t\t</book>\n" +
            "\t</booklist>\n" +
            "</author>\n" +
            "<author>\n" +
            "\t<name>Henry James</name>\n" +
            "\t<booklist>\n" +
            "\t\t<book>\n" +
            "\t\t\t<name>The Ambassadors</name>\n" +
            "\t\t</book>\n" +
            "\t\t<book>\n" +
            "\t\t\t<name>The Portrait of a Lady</name>\n" +
            "\t\t</book>\n" +
            "\t</booklist>\n" +
            "</author>\n" +
            "</catalog>";

    private static final String MIXED =
            "<hasmixed>this node has <mixed>mixed mode</mixed> content.</hasmixed>";

    private void testSimple(ElementCursorFactory f) throws SAXException {
        ElementCursor c = f.newElementCursor(SIMPLE);
        c.moveToDocumentElement();
        Assert.assertEquals("catalog", c.getLocalName());
        final String ns = "http://www.books.com/";
        Assert.assertEquals(ns, c.getNamespaceUri());
        Assert.assertNull(c.getPrefix());
        assertEmptyStack(c);
        c.pushPosition();
        Assert.assertTrue(c.moveToFirstChildElement());
        Assert.assertEquals("author", c.getLocalName());
        Assert.assertTrue(c.moveToFirstChildElement());
        Assert.assertEquals("name", c.getLocalName());
        Assert.assertEquals(ns, c.getNamespaceUri());
        Assert.assertFalse(c.moveToNextSiblingElement("flarf", ns));
        Assert.assertEquals("name", c.getLocalName());
        Assert.assertEquals(ns, c.getNamespaceUri());
        Assert.assertFalse(c.moveToNextSiblingElement("booklist", "uuid:asjdhf"));
        Assert.assertTrue(c.moveToNextSiblingElement("booklist", ns));
        Assert.assertTrue(c.moveToFirstChildElement());
        Assert.assertEquals("book", c.getLocalName());
        Assert.assertTrue(c.moveToFirstChildElement("name", ns));
        Assert.assertEquals("The Secret Agent", c.getTextValue());
        ElementCursor d = c.duplicate();
        d.popPosition(true);
        Assert.assertEquals("The Secret Agent", d.getTextValue());
        c.popPosition();
        Assert.assertEquals("catalog", c.getLocalName());
        assertEmptyStack(c);
        assertEmptyStack(d);
    }

    private void testMixed(ElementCursorFactory f) throws SAXException {
        ElementCursor c = f.newElementCursor(SIMPLE);
        c.moveToDocumentElement();
        Assert.assertFalse(c.containsMixedModeContent(true, false));

        c = f.newElementCursor(MIXED);
        c.moveToDocumentElement();
        Assert.assertTrue(c.containsMixedModeContent(true, false));
    }
}