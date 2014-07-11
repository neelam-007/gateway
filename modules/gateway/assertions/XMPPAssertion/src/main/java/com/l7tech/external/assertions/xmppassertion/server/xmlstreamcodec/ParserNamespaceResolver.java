package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import java.util.Map;
import java.util.Stack;

/**
 * This is from the Apache Vysper project (org.apache.vysper.xml.sax.impl).
 * Trying to minimize changes from the original file so that we could switch
 * to using the vysper library.
 */
public class ParserNamespaceResolver {
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    private Stack<Map<String, String>> elements = new Stack<Map<String, String>>();

    public ParserNamespaceResolver() {
    }

    public void push(Map<String, String> elmXmlns) {
        elements.push(elmXmlns);
    }

    public void pop() {
        elements.pop();
    }

    public String resolveUri(String prefix) {
        if(prefix.equals("xml")) {
            return XML_NS;
        } else {
            for(int i = elements.size() - 1;i >= 0;i--) {
                Map<String, String> ns = elements.get(i);
                if(ns.containsKey(prefix)) {
                    return ns.get(prefix);
                }
            }
        }

        return null;
    }

    public String resolvePrefix(String uri) {
        if(uri.equals(XML_NS)) {
            return "xml";
        } else {
            for(int i = elements.size() - 1;i >= 0;i--) {
                Map<String, String> ns = elements.get(i);
                for(Map.Entry<String, String> entry : ns.entrySet()) {
                    if(entry.getValue().equals(uri)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;
    }
}
