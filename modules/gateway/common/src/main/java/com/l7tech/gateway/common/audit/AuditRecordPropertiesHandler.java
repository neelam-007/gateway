package com.l7tech.gateway.common.audit;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Handler for unmarshalling of an audit record's additional properties
 * Properties includes:
 *          None.
 **/

public class AuditRecordPropertiesHandler extends DefaultHandler {
    private ArrayList<String> currentPath = new ArrayList<String>();
    private String currentElementValue = null;


    private String property1;
    Object getProperty1(){return property1;}

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(uri.equals(AuditDetailPropertiesDomMarshaller.NS))
            currentPath.add(localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(uri.equals(AuditDetailPropertiesDomMarshaller.NS))
        {
            property1 = currentElementValue;
        }
        currentPath.remove(currentPath.size()-1);
        currentElementValue = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentElementValue = new String(ch,start,length);
    }
}
