package com.l7tech.gateway.common.audit;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

/**
 * Handler for unmarshalling of an audit detail's properties
 * Properties includes:
 *      AuditDetail::params
 */
public class AuditDetailPropertiesHandler extends DefaultHandler
{
    private List<String> parameters = new ArrayList<String>();

    private ArrayList<String> currentPath = new ArrayList<String>();

    private String currentElementValue = "";

    public String[] getParameters() {
        return parameters.toArray(new String[parameters.size()]);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
       if(uri.equals(AuditDetailPropertiesDomMarshaller.NS))
            currentPath.add(localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(uri.equals(AuditDetailPropertiesDomMarshaller.NS))
        {
            if(currentPath.size()>=2 && currentPath.get(currentPath.size()-2).equals("params")){
                if(currentPath.get(currentPath.size()-1).equals("param")){
                    parameters.add(currentElementValue);
                }
            }
            currentPath.remove(currentPath.size()-1);
            currentElementValue = "";
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(currentPath.size()>=2 && currentPath.get(currentPath.size()-2).equals("params")){
            if(currentPath.get(currentPath.size()-1).equals("param")){
                currentElementValue = currentElementValue + new String(ch,start,length);
            }
        }
    }
}
