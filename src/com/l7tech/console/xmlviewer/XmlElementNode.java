/*
 * $Id$
 *
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is eXchaNGeR browser code. (org.xngr.browser.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import org.dom4j.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * The node for the XML tree, containing an XML element.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XmlElementNode extends DefaultMutableTreeNode {
    private static final int MAX_LINE_LENGTH = 80;
    private ExchangerElement element = null;
    private Viewer viewer = null;
    private Line[] lines = null;

    private boolean isEndTag = false;

    /**
     * Constructs the node for the XML element.
     *
     * @param element the XML element.
     */
    public XmlElementNode(Viewer viewer, ExchangerElement element) {
        this(viewer, element, false);
    }

    /**
     * Constructs the the XML element node.
     *
     * @param element the XML element.
     */
    public XmlElementNode(Viewer viewer, ExchangerElement element, boolean end) {
        this.element = element;
        this.viewer = viewer;

        isEndTag = end;

        if (!isEndTag()) {
            if (!isMixed(element)) {
                for (int i = 0; i < element.nodeCount(); i++) {
                    Node node = element.node(i);

                    if (node instanceof ExchangerElement) {
                        add(new XmlElementNode(viewer, (ExchangerElement)node));
                    } else if ((node instanceof Comment) && viewer.showComments()) {
                        add(new XmlCommentNode(viewer, (Comment)node));
                    }
                }

                List elements = element.elements();
                // create an end node...
                if (elements != null && elements.size() > 0) {
                    add(new XmlElementNode(viewer, element, true));
                }
            }
        }

        format();
    }

    /**
     * Constructs the the XML element node.
     *
     * @param the XML comment element.
     */
    public XmlElementNode(Viewer viewer) {
        this.viewer = viewer;
    }

    public boolean isEndTag() {
        return isEndTag;
    }

    public void update() {
        for (int i = 0; i < getChildCount(); i++) {
            XmlElementNode node = (XmlElementNode)getChildAt(i);
            node.update();
        }

        format();
    }

    /**
     * Returns the formatted lines for this element.
     *
     * @return the formatted Lines.
     */
    public Line[] getLines() {
        return lines;
    }

    /**
     * Returns the viewer.
     *
     * @return the viewer.
     */
    public Viewer getViewer() {
        return viewer;
    }

    /**
     * Constructs the node for the XML element.
     *
     * @param element the XML element.
     */
    public ExchangerElement getElement() {
        return element;
    }

    private void format() {
        Vector lines = new Vector();
        Line current = new Line();
        lines.add(current);

        if (isEndTag()) {
            current = parseEndTag(lines, current, element);
        } else {
            current = parseElement(lines, current, element);
        }

        this.lines = new Line[lines.size()];

        for (int i = 0; i < lines.size(); i++) {
            this.lines[i] = (Line)lines.elementAt(i);
        }
    }

    protected Line parseElement(Vector lines, Line current, ExchangerElement elem) {

        if (isMixed(elem)) {
            current = parseMixedElement(lines, current, elem);

            if (viewer.showValues()) {
                current = parseEndTag(lines, current, elem);
            }
        } else {
            current = parseStartTag(lines, current, elem);

            if (viewer.showValues()) {
                if (elem.hasContent()) {
                    current = parseContent(lines, current, elem.getText().trim());

                    if (elem.isTextOnly()) {
                        current = parseEndTag(lines, current, elem);
                    }
                }
            }
        }

        return current;
    }

    // Elements parsed here can be both mixed and normal but then contained in a mixed element...
    protected Line parseMixedElement(Vector lines, Line current, ExchangerElement elem) {

        current = parseStartTag(lines, current, elem);

        if (viewer.showValues()) {
            if (isMixed(elem)) {
                for (int i = 0; i < elem.nodeCount(); i++) {
                    Node node = elem.node(i);

                    if (node instanceof ExchangerElement) {
                        current = parseMixedElement(lines, current, (ExchangerElement)node);
                    } else if ((node instanceof Text) || (node instanceof CDATA) || (node instanceof Entity)) {
                        String text = node.getText();
                        current = parseContent(lines, current, text);
                    } else if ((node instanceof Comment) && viewer.showComments()) {
                        current = parseComment(lines, current, (Comment)node);
                    }
                }
            } else {
                List elements = (List)elem.elements();

                if (elements != null && elements.size() > 0) {
                    Iterator iterator = elements.iterator();

                    while (iterator.hasNext()) {
                        current = parseMixedElement(lines, current, (ExchangerElement)iterator.next());
                    }

                    current = parseEndTag(lines, current, elem);
                } else if (elem.hasContent()) {
                    current = parseContent(lines, current, elem.getText());
                    current = parseEndTag(lines, current, elem);
                }
            }
        }

        return current;
    }

    protected Line parseComment(Vector lines, Line current, Comment comment) {
        StyledElement styledElement = new StyledElement();
        styledElement.addString(COMMENT_START);

        current.addStyledElement(styledElement);
        current = parseCommentContent(lines, current, comment.getText());

        styledElement = new StyledElement();
        styledElement.addString(COMMENT_END);
        current.addStyledElement(styledElement);

        return current;
    }

    // Create a styled version of the start-tag.
    protected Line parseStartTag(Vector lines, Line current, Element elem) {
        StyledElement styledElement = new StyledElement();
        styledElement.addString(OPEN_BRACKET);

        if (viewer.showNamespaces()) {
            String prefix = elem.getNamespacePrefix();

            if (prefix != null && prefix.length() > 0) {
                styledElement.addString(new ElementPrefix(prefix));
                styledElement.addString(ELEMENT_COLON);
            }
        }

        styledElement.addString(new ElementName(elem.getName()));
        current.addStyledElement(styledElement);

        if (viewer.showNamespaces()) {
            Namespace ns = elem.getNamespace();

            if (ns != null) {
                ExchangerElement parent = (ExchangerElement)elem.getParent();

                if (parent != null) {
                    Namespace prev = parent.getNamespaceForPrefix(ns.getPrefix());

                    if (prev == null || !ns.equals(prev)) {
                        StyledElement sns = formatNamespace(ns);

                        if (sns != null) {
                            if (current.length() + sns.length() + 1 > MAX_LINE_LENGTH) {
                                current = new Line();
                                lines.add(current);
                                current.addStyledString(TAB);
                            } else {
                                current.addStyledString(SPACE);
                            }

                            current.addStyledElement(sns);
                        }
                    }
                } else {
                    StyledElement sns = formatNamespace(ns);

                    if (sns != null) {
                        if (current.length() + sns.length() + 1 > MAX_LINE_LENGTH) {
                            current = new Line();
                            lines.add(current);
                            current.addStyledString(TAB);
                        } else {
                            current.addStyledString(SPACE);
                        }

                        current.addStyledElement(sns);
                    }
                }
            }

            List namespaces = elem.additionalNamespaces();

            if (namespaces != null && namespaces.size() > 0) {
                Iterator iterator = namespaces.iterator();

                for (int i = 0; i < namespaces.size(); i++) {
                    StyledElement sns = formatNamespace((Namespace)iterator.next());

                    if (sns != null) {
                        if (current.length() + sns.length() + 1 > MAX_LINE_LENGTH) {
                            current = new Line();
                            lines.add(current);
                            current.addStyledString(TAB);
                        } else {
                            current.addStyledString(SPACE);
                        }

                        current.addStyledElement(sns);
                    }
                }
            }
        }

        if (viewer.showAttributes()) {
            List attributes = elem.attributes();

            if (attributes != null && attributes.size() > 0) {
                Iterator iterator = attributes.iterator();

                for (int i = 0; i < attributes.size(); i++) {
                    StyledElement sa = formatAttribute((Attribute)iterator.next());

                    if (current.length() + sa.length() + 1 > MAX_LINE_LENGTH) {
                        current = new Line();
                        lines.add(current);
                        current.addStyledString(TAB);
                    } else {
                        current.addStyledString(SPACE);
                    }

                    current.addStyledElement(sa);
                }
            }
        }

        if (!elem.hasContent()) {
            current.addStyledString(SLASH);
        } else if (elem.isTextOnly() && !viewer.showValues()) {
            current.addStyledString(SLASH);
        }

        current.addStyledString(CLOSE_BRACKET);

        return current;
    }

    // Create a styled version of the element content.
    protected Line parseContent(Vector lines, Line current, String text) {

        if ((current.length() + 1 >= MAX_LINE_LENGTH) && (text.length() > 0)) {
            current = new Line();
            lines.add(current);
            current.addStyledString(TAB);
        }

        if (text.length() > 0) {
            boolean parsed = false;

            while (!parsed) {
                int length = MAX_LINE_LENGTH - (current.length() + 1);

                if (length > text.length()) {
                    int index = 0;

                    if (text.indexOf("\n") != -1) {
                        index = text.indexOf("\n");
                    } else if (text.indexOf("\r") != -1) {
                        index = text.indexOf("\r");
                    } else {
                        index = text.length();
                    }

                    if (index != 0) {
                        String string = text.substring(0, index);
                        current.addStyledString(new ElementValue(string));
                    }

                    if (index == text.length()) {
                        parsed = true;
                    } else {
                        text = text.substring(index + 1, text.length());
                    }
                } else {
                    int index = 0;
                    String sub = text.substring(0, length);

                    if (sub.indexOf("\n") != -1) {
                        index = sub.indexOf("\n");
                    } else if (sub.indexOf("\r") != -1) {
                        index = sub.indexOf("\r");
                    } else if (sub.lastIndexOf(" ") != -1) {
                        index = sub.lastIndexOf(" ");
                    }

                    if (index != 0) {
                        String string = sub.substring(0, index);
                        current.addStyledString(new ElementValue(string));

                        text = text.substring(index + 1, text.length());
                    } else { // Text is too long without any whitespaces...
                        int nlindex = text.indexOf("\n");
                        int rindex = text.indexOf("\r");
                        int spindex = sub.indexOf(" ");

                        if (nlindex == -1) {
                            nlindex = Integer.MAX_VALUE;
                        }
                        if (rindex == -1) {
                            rindex = Integer.MAX_VALUE;
                        }
                        if (spindex == -1) {
                            spindex = Integer.MAX_VALUE;
                        }

                        index = Math.min(nlindex, rindex);
                        index = Math.min(index, spindex);
                        index = Math.min(index, text.length());

                        String string = text.substring(0, index);
                        current.addStyledString(new ElementValue(string));

                        if (index == text.length()) {
                            parsed = true;
                        } else {
                            text = text.substring(index + 1, text.length());
                        }
                    }
                }

                if (!parsed) {
                    current = new Line();
                    lines.add(current);
                    current.addStyledString(TAB);
                }
            }
        }

        return current;
    }

    protected Line parseCommentContent(Vector lines, Line current, String text) {

        if ((current.length() + 1 >= MAX_LINE_LENGTH) && (text.length() > 0)) {
            current = new Line();
            lines.add(current);
            current.addStyledString(TAB);
        }

        if (text.length() > 0) {
            boolean parsed = false;

            while (!parsed) {
                int length = MAX_LINE_LENGTH - (current.length() + 1);

                if (length > text.length()) {
                    int index = 0;

                    if (text.indexOf("\n") != -1) {
                        index = text.indexOf("\n");
                    } else if (text.indexOf("\r") != -1) {
                        index = text.indexOf("\r");
                    } else {
                        index = text.length();
                    }

                    if (index != 0) {
                        String string = text.substring(0, index);
                        current.addStyledString(new CommentText(string));
                    }

                    if (index == text.length()) {
                        parsed = true;
                    } else {
                        text = text.substring(index + 1, text.length());
                    }
                } else {
                    int index = 0;
                    String sub = text.substring(0, length);

                    if (sub.indexOf("\n") != -1) {
                        index = sub.indexOf("\n");
                    } else if (sub.indexOf("\r") != -1) {
                        index = sub.indexOf("\r");
                    } else if (sub.lastIndexOf(" ") != -1) {
                        index = sub.lastIndexOf(" ");
                    }

                    if (index != 0) {
                        String string = sub.substring(0, index);
                        current.addStyledString(new CommentText(string));

                        text = text.substring(index + 1, text.length());
                    } else { // Text is too long without any whitespaces...
                        int nlindex = text.indexOf("\n");
                        int rindex = text.indexOf("\r");
                        int spindex = sub.indexOf(" ");

                        if (nlindex == -1) {
                            nlindex = Integer.MAX_VALUE;
                        }
                        if (rindex == -1) {
                            rindex = Integer.MAX_VALUE;
                        }
                        if (spindex == -1) {
                            spindex = Integer.MAX_VALUE;
                        }

                        index = Math.min(nlindex, rindex);
                        index = Math.min(index, spindex);
                        index = Math.min(index, text.length());

                        String string = text.substring(0, index);
                        current.addStyledString(new CommentText(string));

                        if (index == text.length()) {
                            parsed = true;
                        } else {
                            text = text.substring(index + 1, text.length());
                        }
                    }
                }

                if (!parsed) {
                    current = new Line();
                    lines.add(current);
                    current.addStyledString(TAB);
                }
            }
        }

        return current;
    }

    // Create a styled version of the end-tag.
    protected Line parseEndTag(Vector lines, Line current, Element elem) {
        StyledElement styledEnd = new StyledElement();
        styledEnd.addString(OPEN_BRACKET);
        styledEnd.addString(SLASH);

        if (viewer.showNamespaces()) {
            String prefix = elem.getNamespacePrefix();

            if (prefix != null && prefix.length() > 0) {
                styledEnd.addString(new ElementPrefix(prefix));
                styledEnd.addString(ELEMENT_COLON);
            }
        }

        styledEnd.addString(new ElementName(elem.getName()));
        styledEnd.addString(CLOSE_BRACKET);
        current.addStyledElement(styledEnd);

        return current;
    }

    private StyledElement formatAttribute(Attribute a) {
        StyledElement styledAttribute = new StyledElement();

        String name = a.getName();
        String value = a.getValue();

        if (viewer.showNamespaces()) {
            String prefix = a.getNamespacePrefix();

            if (prefix != null && prefix.length() > 0) {
                styledAttribute.addString(new AttributePrefix(prefix));
                styledAttribute.addString(ATTRIBUTE_COLON);
            }
        }

        styledAttribute.addString(new AttributeName(name));
        styledAttribute.addString(ATTRIBUTE_ASIGN);
        styledAttribute.addString(new AttributeValue(value));

        return styledAttribute;
    }

    private StyledElement formatNamespace(Namespace n) {
        StyledElement styledNamespace = null;

        String prefix = n.getPrefix();
        String value = n.getText();

        if (value != null && value.length() > 0) {
            styledNamespace = new StyledElement();
            styledNamespace.addString(NAMESPACE_NAME);

            if (prefix != null && prefix.length() > 0) {
                styledNamespace.addString(NAMESPACE_COLON);
                styledNamespace.addString(new NamespacePrefix(prefix));
            }

            styledNamespace.addString(NAMESPACE_ASIGN);
            styledNamespace.addString(new NamespaceURI(value));
        }

        return styledNamespace;
    }

    public class StyledElement {
        private Vector strings = null;

        public StyledElement() {
            strings = new Vector();
        }

        public void addString(StyledString string) {
            strings.addElement(string);
        }

        public int length() {
            int result = 0;

            for (int i = 0; i < strings.size(); i++) {
                result += ((StyledString)strings.elementAt(i)).getText().length();
            }

            return result;
        }

        public Vector getStrings() {
            return strings;
        }
    }

    public class Line {
        private Vector strings = null;

        public Line() {
            strings = new Vector();
        }

        public void addStyledString(StyledString string) {
            strings.add(string);
        }

        public void addStyledElement(StyledElement element) {
            Vector strings = element.getStrings();

            for (int i = 0; i < strings.size(); i++) {
                addStyledString((StyledString)strings.elementAt(i));
            }
        }

        public StyledString[] getStyledStrings() {
            StyledString[] ss = new StyledString[strings.size()];

            for (int i = 0; i < strings.size(); i++) {
                ss[i] = (StyledString)strings.elementAt(i);
            }

            return ss;
        }

        public int length() {
            int result = 0;

            for (int i = 0; i < strings.size(); i++) {
                result += ((StyledString)strings.elementAt(i)).getText().length();
            }

            return result;
        }
		
//		public int getWidth() {
//			
//			int result = 0;
//			
//			for ( int i = 0; i < strings.size(); i++) {
//				Font font = ((StyledString)strings.elementAt(i)).getFont();
//				String text = ((StyledString)strings.elementAt(i)).getText();
//				
//				if ( font == BOLD_FONT) {
//					result += BOLD_FONT_METRICS.stringWidth( text);
//				} else if ( font == ITALIC_FONT) {
//					result += ITALIC_FONT_METRICS.stringWidth( text);
//				} else {
//					result += PLAIN_FONT_METRICS.stringWidth( text);
//				}
//			}
//			
//			return result;
//		}

        public String getText() {
            String result = "";

            for (int i = 0; i < strings.size(); i++) {
                result += ((StyledString)strings.elementAt(i)).getText();
            }

            return result;
        }

//		public int getHeight() {
//			return BOLD_FONT_METRICS.getHeight();
//		}
    }

    private static boolean isWhiteSpace(Node node) {
        return node.getText().trim().length() == 0;
    }

    // solves a problem in the Element that hasMixedContent returns true when the content
    // has comment information.
    private static boolean isMixed(ExchangerElement element) {
        if (element.hasMixedContent()) {
            boolean elementFound = false;
            boolean textFound = false;

            for (int i = 0; i < element.nodeCount(); i++) {
                Node node = element.node(i);

                if (node instanceof ExchangerElement) {
                    elementFound = true;
                } else if ((node instanceof Text) || (node instanceof CDATA) || (node instanceof Entity)) {
                    if (!isWhiteSpace(node)) {
                        textFound = true;
                    }
                }

                if (textFound && elementFound) {
                    return true;
                }
            }
        }

        return false;
    }

    private static final Font PLAIN_FONT = createDefaultFont();
    private static final Font BOLD_FONT = PLAIN_FONT.deriveFont(Font.BOLD);
    private static final Font ITALIC_FONT = PLAIN_FONT.deriveFont(Font.ITALIC);

    private static final FontMetrics PLAIN_FONT_METRICS = (new XmlCellRenderer()).getFontMetrics(PLAIN_FONT);
    private static final FontMetrics BOLD_FONT_METRICS = (new XmlCellRenderer()).getFontMetrics(BOLD_FONT);
    private static final FontMetrics ITALIC_FONT_METRICS = (new XmlCellRenderer()).getFontMetrics(ITALIC_FONT);

    private static final Color BRACKET_COLOR = new Color(102, 102, 102);
    private static final Font BRACKET_FONT = PLAIN_FONT;

    private static final Color COMMENT_CONTROL_COLOR = BRACKET_COLOR;
    private static final Font COMMENT_CONTROL_FONT = BRACKET_FONT;

    private static final Color COMMENT_COLOR = new Color(153, 153, 153);
    private static final Font COMMENT_FONT = ITALIC_FONT;

    private static final Color SLASH_COLOR = BRACKET_COLOR;
    private static final Font SLASH_FONT = BRACKET_FONT;

    private static final Color ELEMENT_PREFIX_COLOR = new Color(0, 102, 102);
    private static final Font ELEMENT_PREFIX_FONT = ITALIC_FONT;

    private static final Color ELEMENT_COLON_COLOR = BRACKET_COLOR;
    private static final Font ELEMENT_COLON_FONT = BRACKET_FONT;

    private static final Color ELEMENT_NAME_COLOR = new Color(0, 51, 102);
    private static final Font ELEMENT_NAME_FONT = BOLD_FONT;

    private static final Color NAMESPACE_PREFIX_COLOR = ELEMENT_PREFIX_COLOR;
    private static final Font NAMESPACE_PREFIX_FONT = ELEMENT_PREFIX_FONT;

    private static final Color NAMESPACE_COLON_COLOR = BRACKET_COLOR;
    private static final Font NAMESPACE_COLON_FONT = BRACKET_FONT;

    private static final Color NAMESPACE_ASIGN_COLOR = BRACKET_COLOR;
    private static final Font NAMESPACE_ASIGN_FONT = BRACKET_FONT;

    private static final Color NAMESPACE_NAME_COLOR = new Color(102, 102, 102);
    private static final Font NAMESPACE_NAME_FONT = BOLD_FONT;

    private static final Color NAMESPACE_URI_COLOR = new Color(0, 51, 51);
    private static final Font NAMESPACE_URI_FONT = PLAIN_FONT;

    private static final Color ATTRIBUTE_PREFIX_COLOR = ELEMENT_PREFIX_COLOR;
    private static final Font ATTRIBUTE_PREFIX_FONT = ELEMENT_PREFIX_FONT;

    private static final Color ATTRIBUTE_COLON_COLOR = BRACKET_COLOR;
    private static final Font ATTRIBUTE_COLON_FONT = BRACKET_FONT;

    private static final Color ATTRIBUTE_ASIGN_COLOR = BRACKET_COLOR;
    private static final Font ATTRIBUTE_ASIGN_FONT = BRACKET_FONT;

    private static final Color ATTRIBUTE_NAME_COLOR = new Color(153, 51, 51);
    private static final Font ATTRIBUTE_NAME_FONT = BOLD_FONT;

    private static final Color ATTRIBUTE_VALUE_COLOR = new Color(102, 0, 0);
    private static final Font ATTRIBUTE_VALUE_FONT = PLAIN_FONT;

    private static final Color ELEMENT_VALUE_COLOR = Color.black;
    private static final Font ELEMENT_VALUE_FONT = PLAIN_FONT;

    private static final StyledString COMMENT_START = new StyledString("<!--", COMMENT_CONTROL_COLOR, COMMENT_CONTROL_FONT);
    private static final StyledString COMMENT_END = new StyledString("-->", COMMENT_CONTROL_COLOR, COMMENT_CONTROL_FONT);
    private static final StyledString SPACE = new StyledString(" ", Color.black, PLAIN_FONT);
    private static final StyledString TAB = new StyledString("  ", Color.black, PLAIN_FONT);
    private static final StyledString SLASH = new StyledString("/", SLASH_COLOR, SLASH_FONT);
    private static final StyledString ATTRIBUTE_ASIGN = new StyledString("=", ATTRIBUTE_ASIGN_COLOR, ATTRIBUTE_ASIGN_FONT);
    private static final StyledString ATTRIBUTE_COLON = new StyledString(":", ATTRIBUTE_COLON_COLOR, ATTRIBUTE_COLON_FONT);
    private static final StyledString NAMESPACE_ASIGN = new StyledString("=", NAMESPACE_ASIGN_COLOR, NAMESPACE_ASIGN_FONT);
    private static final StyledString NAMESPACE_COLON = new StyledString(":", NAMESPACE_COLON_COLOR, NAMESPACE_COLON_FONT);
    private static final StyledString NAMESPACE_NAME = new StyledString("xmlns", NAMESPACE_NAME_COLOR, NAMESPACE_NAME_FONT);
    private static final StyledString ELEMENT_COLON = new StyledString(":", ELEMENT_COLON_COLOR, ELEMENT_COLON_FONT);

    private static final StyledString OPEN_BRACKET = new StyledString("<", BRACKET_COLOR, BRACKET_FONT);
    private static final StyledString CLOSE_BRACKET = new StyledString(">", BRACKET_COLOR, BRACKET_FONT);

    public class CommentText extends StyledString {
        public CommentText(String text) {
            super(text, COMMENT_COLOR, COMMENT_FONT);
        }
    }

    public class ElementValue extends StyledString {
        public ElementValue(String text) {
            super(text, ELEMENT_VALUE_COLOR, ELEMENT_VALUE_FONT);
        }
    }

    public class AttributeValue extends StyledString {
        public AttributeValue(String text) {
            super("\"" + text + "\"", ATTRIBUTE_VALUE_COLOR, ATTRIBUTE_VALUE_FONT);
        }
    }

    public class AttributePrefix extends StyledString {
        public AttributePrefix(String text) {
            super(text, ATTRIBUTE_PREFIX_COLOR, ATTRIBUTE_PREFIX_FONT);
        }
    }

    public class AttributeName extends StyledString {
        public AttributeName(String text) {
            super(text, ATTRIBUTE_NAME_COLOR, ATTRIBUTE_NAME_FONT);
        }
    }

    public class NamespaceURI extends StyledString {
        public NamespaceURI(String text) {
            super("\"" + text + "\"", NAMESPACE_URI_COLOR, NAMESPACE_URI_FONT);
        }
    }

    public class NamespacePrefix extends StyledString {
        public NamespacePrefix(String text) {
            super(text, NAMESPACE_PREFIX_COLOR, NAMESPACE_PREFIX_FONT);
        }
    }

    public class ElementName extends StyledString {
        public ElementName(String text) {
            super(text, ELEMENT_NAME_COLOR, ELEMENT_NAME_FONT);
        }
    }

    public class ElementPrefix extends StyledString {
        public ElementPrefix(String text) {
            super(text, ELEMENT_PREFIX_COLOR, ELEMENT_PREFIX_FONT);
        }
    }

    private static Font createDefaultFont() {
        Font font = null;
        Component component = new XmlCellRenderer();

        // test to find out if the monospaced font has the same
        // width for every style...
        String testString = "<Test test:nms=\"http://test.org\"/>";

        Font mono = new Font("Monospaced", Font.PLAIN, 12);
        FontMetrics fm = component.getFontMetrics(mono);
        int plainWidth = fm.stringWidth(testString);

        Font monoBold = new Font("Monospaced", Font.BOLD, 12);
        fm = component.getFontMetrics(monoBold);
        int boldWidth = fm.stringWidth(testString);

        Font monoItalic = new Font("Monospaced", Font.ITALIC, 12);
        fm = component.getFontMetrics(monoItalic);
        int italicWidth = fm.stringWidth(testString);

        if (plainWidth == boldWidth && boldWidth == italicWidth) {	// && italicWidth == italicBoldWidth) {
            font = mono;
        } else { // use the build-in fixed pitch font...
//			System.err.println( "INFO: Monospaced font not fully supported, using Lucida font instead!");
            font = new Font("Lucida Sans Typewriter", Font.PLAIN, 12);
        }

        return font;
    }
} 
