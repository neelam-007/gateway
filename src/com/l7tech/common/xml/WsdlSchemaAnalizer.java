package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Parses schemas present in a wsdl and split them into two versions; one for input messages and
 * one for output messages.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 17, 2004<br/>
 * $Id$<br/>
 */
public class WsdlSchemaAnalizer {
    public WsdlSchemaAnalizer(Document wsdl) {
        this.wsdl = wsdl;
        this.schemaElements = extractSchemaElementFromWsdl(wsdl);
    }

    /**
     * This creates a list of schema elements that can only be present in requests and responses respectively.
     * Then, it makes two clones of the schemas and removes those elements in each of the clones to get as a result
     * schemas that are only applicable to requests and responses respectively.
     */
    public void splitInputOutputs() {
        if (schemaElements == null) return;
        NodeList inputlist = wsdl.getElementsByTagNameNS(WSDL_NS, "input");
        Collection intputQnames = new ArrayList();
        for (int i = 0; i < inputlist.getLength(); i++) {
            Element item = (Element)inputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                Collection schemaElementsForMsg = getMessagePartsElementNames(msg);
                intputQnames.addAll(schemaElementsForMsg);
            }
        }
        Collection outputQnames = new ArrayList();
        NodeList outputlist = wsdl.getElementsByTagNameNS(WSDL_NS, "output");
        for (int i = 0; i < outputlist.getLength(); i++) {
            Element item = (Element)outputlist.item(i);
            String msg = item.getAttribute("message");
            if (msg != null && msg.length() > 0) {
                Collection schemaElementsForMsg = getMessagePartsElementNames(msg);
                outputQnames.addAll(schemaElementsForMsg);
            }
        }
        ArrayList toberemovedfromoutputs = new ArrayList();
        for (Iterator iterator = intputQnames.iterator(); iterator.hasNext();) {
            QName qName = (QName) iterator.next();
            if (outputQnames.contains(qName)) {
                iterator.remove();
                toberemovedfromoutputs.add(qName);
            }
        }
        for (Iterator iterator = outputQnames.iterator(); iterator.hasNext();) {
            QName qName = (QName) iterator.next();
            if (intputQnames.contains(qName)) {
                iterator.remove();
            } else if (toberemovedfromoutputs.contains(qName)) {
                iterator.remove();
            }
        }
        ouputSchemas = removeQnamesFromSchemas(intputQnames);
        inputSchemas = removeQnamesFromSchemas(outputQnames);
    }

    private Collection getMessagePartsElementNames(String messageName) {
        if (messages == null) {
            messages = wsdl.getElementsByTagNameNS(WSDL_NS, "message");
        }
        String msgnamewoutprefix = messageName;
        int prefixendpos = messageName.indexOf(':');
        if (prefixendpos != -1) {
            msgnamewoutprefix = messageName.substring(prefixendpos+1);
        }
        ArrayList output = new ArrayList();
        // find messages elements that fit the name
        for (int i = 0; i < messages.getLength(); i++) {
            Element item = (Element)messages.item(i);
            String msgname = item.getAttribute("name");
            if (msgname == null) continue;
            if (msgname.equals(messageName) || msgname.equals(msgnamewoutprefix)) {
                // add all parts elements
                List parts = XmlUtil.findChildElementsByName(item, WSDL_NS, "part");
                for (Iterator iterator = parts.iterator(); iterator.hasNext();) {
                    Element part = (Element) iterator.next();
                    String elementname = part.getAttribute("element");
                    if (elementname != null && elementname.length() > 0) {
                        output.add(qnameFromElementName(elementname, part));
                    }
                }
            }
        }
        return output;
    }

    private QName qnameFromElementName(String elementname, Element elWhereDefined) {
        int prefixendpos = elementname.indexOf(':');
        if (prefixendpos != -1) {
            String prefix = elementname.substring(0, prefixendpos);
            String suffix = elementname.substring(prefixendpos+1);
            String ns = null;
            Element potentialNsDefiner = elWhereDefined;
            while (ns == null && potentialNsDefiner != null) {
                String attr = potentialNsDefiner.getAttribute("xmlns:"+prefix);
                if (attr != null && attr.length() > 0) {
                    ns = attr;
                    break;
                } else {
                    potentialNsDefiner = (Element)potentialNsDefiner.getParentNode();
                }
            }
            return new QName(ns, suffix);
        } else {
            return new QName(null, elementname);
        }
    }

    private NodeList extractSchemaElementFromWsdl(Document wsdl)  {
        if (wsdl == null) return null;
        NodeList potentiallists = wsdl.getDocumentElement().getElementsByTagName(WSDL_TYPES_ELNAME);
        Element typesel = null;
        switch (potentiallists.getLength()) {
            case 1:
                typesel = (Element)potentiallists.item(0);
                break;
            default:
                break;
        }
        if (typesel == null) {
            potentiallists = wsdl.getDocumentElement().getElementsByTagNameNS(WSDL_NS,
                                                                              WSDL_TYPES_ELNAME);
            typesel = null;
            switch (potentiallists.getLength()) {
                case 1:
                    typesel = (Element)potentiallists.item(0);
                    break;
                default:
                    break;
            }
        }

        if (typesel == null) {
            return null;
        }
        NodeList output = typesel.getElementsByTagNameNS(W3C_XML_SCHEMA, TOP_SCHEMA_ELNAME);
        // transpose the namespaces from schema parents
        Node node = typesel;
        while (node != null) {
            if (node instanceof Element) {
                Element el = (Element)node;
                NamedNodeMap attrsmap = el.getAttributes();
                for (int i = 0; i < attrsmap.getLength(); i++) {
                    Attr attrnode = (Attr)attrsmap.item(i);
                    if (attrnode.getName().startsWith("xmlns:")) {
                        for (int ii = 0; ii < output.getLength(); ii++) {
                            Element schemael = (Element)output.item(ii);
                            schemael.setAttribute(attrnode.getName(), attrnode.getValue());
                        }
                    }
                }
            }
            node = node.getParentNode();
        }
        return output;
    }

    private Element[] removeQnamesFromSchemas(Collection qnames) {
        Element[] output = new Element[schemaElements.getLength()];
        for (int i = 0; i < schemaElements.getLength(); i++) {
            // clone the schema
            output[i] = (Element)(schemaElements.item(i).cloneNode(true));
            // only keep the elements that are in the qnames passed
            String tns = output[i].getAttribute("targetNamespace");
            NodeList schemaChildren = output[i].getChildNodes();
            for (int ii = 0; ii < schemaChildren.getLength(); ii++) {
                Node child = schemaChildren.item(ii);
                if (child.getNodeType() != Node.ELEMENT_NODE) continue;
                Element el = (Element)child;
                String elName = el.getAttribute("name");
                if (qnames.contains(new QName(tns, elName))) {
                    output[i].removeChild(el);
                }
            }
        }
        return output;
    }

    public Element[] getFullSchemas() {
        if (schemaElements == null) return null;
        Element[] output = new Element[schemaElements.getLength()];
        for (int i = 0; i < schemaElements.getLength(); i++) {
            output[i] = (Element)schemaElements.item(i);
        }
        return output;
    }

    public Element[] getInputSchemas() {
        return inputSchemas;
    }

    public Element[] getOutputSchemas() {
        return ouputSchemas;
    }

    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String WSDL_TYPES_ELNAME = "types";
    public static final String TOP_SCHEMA_ELNAME = "schema";
    public static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    public Element[] inputSchemas;
    public Element[] ouputSchemas;

    private Document wsdl;
    private NodeList messages = null;
    private NodeList schemaElements = null;
}
