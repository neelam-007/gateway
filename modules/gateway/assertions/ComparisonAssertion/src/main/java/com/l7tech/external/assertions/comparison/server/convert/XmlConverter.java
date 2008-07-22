/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.xml.ElementCursor;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Attempts to convert the specified value into an {@link Element}, with the following rules:
 * <ul>
 * <li>If the value is already an {@link Element}, return it;
 * <li>If the value is an {@link ElementCursor}, return {@link com.l7tech.xml.ElementCursor#asDomElement()};
 * <li>If the value is a {@link CharSequence}, parse it and return its root element.
 * </ul>
 * @author alex
 */
public class XmlConverter implements ValueConverter<Element> {
    public Element convert(Object val) throws ConversionException {
        if (val instanceof CharSequence) {
            String s = val.toString();
            try {
                return XmlUtil.stringToDocument(s).getDocumentElement();
            } catch (SAXException e) {
                throw new ConversionException("Unable to parse value", e);
            }
        } else if (val instanceof Element) {
            return (Element)val;
        } else if (val instanceof ElementCursor) {
            return ((ElementCursor)val).asDomElement();
        } else {
            throw new ConversionException("Unsupported value type: " + val.getClass().getName());
        }
    }
}
