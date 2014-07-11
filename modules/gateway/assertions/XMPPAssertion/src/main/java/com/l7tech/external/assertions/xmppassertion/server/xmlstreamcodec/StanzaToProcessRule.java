package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public interface StanzaToProcessRule {
    public boolean processStartElement(Stack<String> elements,
                                       String uri,
                                       String localName,
                                       String qName,
                                       Attributes atts);

    public boolean processEndElement(Stack<String> elements,
                                     String uri,
                                     String localName,
                                     String qName);
}
