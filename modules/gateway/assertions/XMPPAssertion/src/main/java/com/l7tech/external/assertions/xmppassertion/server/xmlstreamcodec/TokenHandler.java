package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.buffer.IoBuffer;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is from the Apache Vysper project (org.apache.vysper.xml.sax.impl.XMLParser).
 * Trying to minimize changes from the original file so that we could switch
 * to using the vysper library.
 */
public class TokenHandler {
    private static enum State {
        START,
        IN_TAG,
        IN_DECLARATION,
        IN_END_TAG,
        AFTER_START_NAME,
        AFTER_END_NAME,
        IN_EMPTY_TAG,
        AFTER_ATTRIBUTE_NAME,
        AFTER_ATTRIBUTE_EQUALS,
        AFTER_ATTRIBUTE_FIRST_QUOTE,
        AFTER_ATTRIBUTE_VALUE,
        AFTER_COMMENT_BANG,
        AFTER_COMMENT_DASH1,
        AFTER_COMMENT_DASH2,
        AFTER_COMMENT,
        AFTER_COMMENT_CLOSING_DASH1,
        AFTER_COMMENT_CLOSING_DASH2,
        AFTER_COMMENT_ENDING_DASH1,
        AFTER_COMMENT_ENDING_DASH2,
        CLOSED
    }

    private static final String nameStartChar = ":A-Z_a-z\\u00C0-\\u00D6-\\u00D8-\\u00F6-\\u00F8-\\u02FF-\\u0370-\\u037D-\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD";
    private static final String nameChar = nameStartChar + "-\\.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
    private static final Pattern NAME_PATTERN = Pattern.compile("^[" + nameStartChar + "][" + nameChar + "]*$");
    private static final Pattern NAME_PREFIX_PATTERN = Pattern.compile("^xml", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNESCAPE_UNICODE_PATTERN = Pattern.compile("\\&\\#(x?)(.+);");

    private State state = State.START;

    private String qname;
    private Map<String, String> attributes;
    private String attributeName;

    private ParserNamespaceResolver nsResolver = new ParserNamespaceResolver();

    private Stack<String> elements = new Stack<String>();
    private boolean sentStartDocument = false;

    private XMLStreamContentHandler contentHandler;
    private XMLTokenizer tokenizer;

    public TokenHandler(XMLStreamContentHandler contentHandler) {
        this.contentHandler = contentHandler;
        contentHandler.setTokenHandler(this);
        tokenizer = new XMLTokenizer(this);
    }

    public void reset() {
        if(!elements.isEmpty()) {
            String value = elements.pop();
            elements.clear();
            elements.push(value);
        }
    }

    public void parse(IoBuffer buffer) throws SAXException {
        if(state == State.CLOSED) {
            throw new SAXException("Parser is closed");
        }

        /*if(!byteBuffer.hasRemaining()) {
            contentHandler.sendEmptyMessage();
        }*/

        try {
            tokenizer.parse(buffer);
        } catch(RuntimeException e) {
            state = State.CLOSED;
        }
    }

    public void token(char c, String token) throws SAXException {
        switch(state) {
        case START:
            if(c == '<') {
                state = State.IN_TAG;
                attributes = new HashMap<String, String>();
            } else {
                characters(token);
            }
            break;
        case IN_TAG:
            if(c == '/') {
                state = State.IN_END_TAG;
            } else if(c == '?') {
                state = State.IN_DECLARATION;
            } else if(c == '!') {
                state = State.AFTER_COMMENT_BANG;
            } else {
                if(token != null && isValidName(token)) {
                    qname = token;
                    state = State.AFTER_START_NAME;
                } else {
                    state = State.CLOSED;
                    return;
                }
            }
            break;
        case IN_END_TAG:
            qname = token;
            state = State.AFTER_END_NAME;
            break;
        case AFTER_START_NAME:
            if(c == '>') {
                if(state == State.AFTER_START_NAME) {
                    startElement();
                    state = State.START;
                    attributes = null;
                } else if(state == State.AFTER_END_NAME) {
                    state = State.START;
                    endElement();
                }
            } else if(c == '/') {
                state = State.IN_EMPTY_TAG;
            } else {
                attributeName = token;
                state = State.AFTER_ATTRIBUTE_NAME;
            }
            break;
        case AFTER_ATTRIBUTE_NAME:
            if(c == '=') {
                state = State.AFTER_ATTRIBUTE_EQUALS;
            } else {
                state = State.CLOSED;
            }
            break;
        case AFTER_ATTRIBUTE_EQUALS:
            if(c == '"' || c == '\'') {
                state = State.AFTER_ATTRIBUTE_FIRST_QUOTE;
            }
            break;
        case AFTER_ATTRIBUTE_FIRST_QUOTE:
            attributes.put(attributeName, unescape(token));
            state = State.AFTER_ATTRIBUTE_VALUE;
            break;
        case AFTER_ATTRIBUTE_VALUE:
            if(c == '"' || c == '\'') {
                state = State.AFTER_START_NAME;
            } else {
                state = State.CLOSED;
            }
            break;
        case AFTER_END_NAME:
            if(c == '>') {
                state = State.START;
                endElement();
            }
            break;
        case IN_EMPTY_TAG:
            if(c == '>') {
                startElement();
                attributes = null;

                state = State.START;
                endElement();
            }
            break;
        case AFTER_COMMENT_BANG:
            if(c == '-') {
                state = State.AFTER_COMMENT_DASH1;
            } else {
                state = State.CLOSED;
                return;
            }
            break;
        case AFTER_COMMENT_DASH1:
            if(c == '-') {
                state = State.AFTER_COMMENT_DASH2;
            } else {
                state = State.CLOSED;
                return;
            }
            break;
        case AFTER_COMMENT_DASH2:
            if(c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else {
                state = State.AFTER_COMMENT;
            }
            break;
        case AFTER_COMMENT:
            if(c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH1;
            } else if(c == '>') {
                state = State.CLOSED;
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH1:
            if(c == '-') {
                state = State.AFTER_COMMENT_CLOSING_DASH2;
            } else {
                state = State.CLOSED;
                return;
            }
            break;
        case AFTER_COMMENT_CLOSING_DASH2:
            if(c == '>') {
                state = State.START;
            } else {
                state = State.CLOSED;
                return;
            }
            break;
        case IN_DECLARATION:
            if(c == '>') {
                state = State.START;
            }
            break;
        }
    }

    private void characters(String s) throws SAXException {
        if(elements.isEmpty()) {
            startDocument();
            if(s.trim().length() > 0) {
                state = State.CLOSED;
            }
        }
    }

    private boolean isValidName(String name) {
        return NAME_PATTERN.matcher(name).find() && !NAME_PREFIX_PATTERN.matcher(name).find();
    }

    private void xmlDeclaration() {
        //
    }

    private void startDocument() throws SAXException {
        if(!sentStartDocument) {
            contentHandler.startDocument();
            sentStartDocument = true;
        }
    }

    private void startElement() throws SAXException {
        if(elements.isEmpty()) {
            startDocument();
        }

        Map<String, String> nsDeclarations = new HashMap<String, String>();
        for(Map.Entry<String, String> attribute : attributes.entrySet()) {
            if(attribute.getKey().equals("xmlns")) {
                nsDeclarations.put("", attribute.getValue());
            } else if(attribute.getKey().startsWith("xmlns:")) {
                nsDeclarations.put(attribute.getKey().substring(6), attribute.getValue());
            }
        }
        nsResolver.push(nsDeclarations);

        List<Attribute> nonNsAttributes = new ArrayList<Attribute>();
        for(Map.Entry<String, String> attribute : attributes.entrySet()) {
            String attQname = attribute.getKey();

            nonNsAttributes.add(new Attribute(attQname, null, attQname, attribute.getValue()));
        }

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if(uri == null) {
            if(prefix.length() > 0) {
                state = State.CLOSED;
                return;
            } else {
                uri = "";
            }
        }

        String localName = extractLocalName(qname);

        elements.add(fullyQualifiedName(uri, qname));

        contentHandler.startElement(uri, localName, qname, new DefaultAttributes(nonNsAttributes));
    }

    private String extractLocalName(String qname) {
        int index = qname.indexOf(':');

        if(index > -1) {
            return qname.substring(index + 1);
        } else {
            return qname;
        }
    }

    private String extractNsPrefix(String qname) {
        int index = qname.indexOf(':');

        if(index > -1) {
            return qname.substring(0, index);
        } else {
            return "";
        }
    }

    private String fullyQualifiedName(String uri, String qname) {
        return "{" + uri + "}" + qname;
    }

    private void endElement() throws SAXException {
        if(state == State.CLOSED) {
            return;
        }

        String prefix = extractNsPrefix(qname);
        String uri = nsResolver.resolveUri(prefix);
        if(uri == null) {
            if(prefix.length() > 0) {
                state = State.CLOSED;
                return;
            } else {
                uri = "";
            }
        }

        nsResolver.pop();

        String localName = extractLocalName(qname);

        String fqn = elements.pop();
        if(fqn.equals(fullyQualifiedName(uri, qname))) {
            contentHandler.endElement(uri, localName, qname);

            if(elements.isEmpty()) {
                contentHandler.endDocument();
                state = State.CLOSED;
            }
        } else {
            state = State.CLOSED;
        }
    }

    private String unescape(String s) {
        s = s.replace("&amp;", "&").replace("&gt;", "<").replace("&lt;", "<").replace("&apos;", "'").replace("&quot;", "\"");

        StringBuffer sb = new StringBuffer();

        Matcher matcher = UNESCAPE_UNICODE_PATTERN.matcher(s);
        int end = 0;
        while(matcher.find()) {
            boolean isHex = matcher.group(1).equals("x");
            String unicodeCode = matcher.group(2);

            int base = isHex ? 16 : 10;
            int i = Integer.valueOf(unicodeCode, base).intValue();
            char[] c = Character.toChars(i);
            sb.append(s.substring(end, matcher.start()));
            end = matcher.end();
            sb.append(c);
        }
        sb.append(s.substring(end, s.length()));

        return sb.toString();
    }

    public XMLStreamContentHandler getContentHandler() {
        return contentHandler;
    }

    public boolean isDone() {
        return contentHandler.isDone();
    }
}
