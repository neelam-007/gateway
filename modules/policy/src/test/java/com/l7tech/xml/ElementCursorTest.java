/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.tarari.TarariMessageContextImpl;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.SoftwareFallbackException;
import com.l7tech.message.TarariMessageContextFactory;
import com.l7tech.common.io.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Unit tests for an ElementCursor implementation.
 */
public class ElementCursorTest extends TestCase {
    private static Logger log = Logger.getLogger(ElementCursorTest.class.getName());

    public ElementCursorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ElementCursorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

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

    public void testTarariCursor() throws Exception {
        try {
            testAll(new TarariElementCursorFactory());
        } catch (UnsatisfiedLinkError e) {
            log.warning("Tarari test not performed -- hardware not found: " + ExceptionUtils.getMessage(e));
        }
    }

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
            fail("Stack should have been empty");
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
        assertEquals("catalog", c.getLocalName());
        final String ns = "http://www.books.com/";
        assertEquals(ns, c.getNamespaceUri());
        assertNull(c.getPrefix());
        assertEmptyStack(c);
        c.pushPosition();
        assertTrue(c.moveToFirstChildElement());
        assertEquals("author", c.getLocalName());
        assertTrue(c.moveToFirstChildElement());
        assertEquals("name", c.getLocalName());
        assertEquals(ns, c.getNamespaceUri());
        assertFalse(c.moveToNextSiblingElement("flarf", ns));
        assertEquals("name", c.getLocalName());
        assertEquals(ns, c.getNamespaceUri());
        assertFalse(c.moveToNextSiblingElement("booklist", "uuid:asjdhf"));
        assertTrue(c.moveToNextSiblingElement("booklist", ns));
        assertTrue(c.moveToFirstChildElement());
        assertEquals("book", c.getLocalName());
        assertTrue(c.moveToFirstChildElement("name", ns));
        assertEquals("The Secret Agent", c.getTextValue());
        ElementCursor d = c.duplicate();
        d.popPosition(true);
        assertEquals("The Secret Agent", d.getTextValue());
        c.popPosition();
        assertEquals("catalog", c.getLocalName());
        assertEmptyStack(c);
        assertEmptyStack(d);
    }

    private void testMixed(ElementCursorFactory f) throws SAXException {
        ElementCursor c = f.newElementCursor(SIMPLE);
        c.moveToDocumentElement();
        assertFalse(c.containsMixedModeContent(true, false));

        c = f.newElementCursor(MIXED);
        c.moveToDocumentElement();
        assertTrue(c.containsMixedModeContent(true, false));
    }
}